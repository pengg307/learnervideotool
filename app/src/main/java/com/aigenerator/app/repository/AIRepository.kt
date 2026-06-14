package com.aigenerator.app.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.aigenerator.app.database.MessageDao
import com.aigenerator.app.model.*
import com.aigenerator.app.network.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class AIResult<T> {
    data class Success<T>(val data: T) : AIResult<T>()
    data class Error<T>(val message: String) : AIResult<T>()
    class Loading<T> : AIResult<T>()
}

@Singleton
class AIRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openAI: OpenAIApiService,
    private val stability: StabilityApiService,
    private val replicate: ReplicateApiService,
    private val settingsRepo: SettingsRepository,
    private val dao: MessageDao
) {

    // ─── OpenAI Image ────────────────────────────────────────────────────────

    suspend fun generateImageOpenAI(prompt: String, size: String = "1024x1024"): AIResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val s = settingsRepo.getSettings()
                if (s.openAiApiKey.isBlank())
                    return@withContext AIResult.Error("OpenAI API key not set. Go to Settings.")
                val resp = openAI.generateImage(
                    "Bearer ${s.openAiApiKey}",
                    OpenAIImageRequest(prompt = prompt, size = size)
                )
                if (resp.isSuccessful)
                    AIResult.Success(resp.body()?.data?.firstOrNull()?.url
                        ?: return@withContext AIResult.Error("No URL returned"))
                else
                    AIResult.Error("OpenAI Error ${resp.code()}: ${resp.errorBody()?.string()}")
            }.getOrElse { AIResult.Error("Network error: ${it.message}") }
        }

    // ─── OpenAI Chat ─────────────────────────────────────────────────────────

    suspend fun chatWithGPT(messages: List<ChatMessage>): AIResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val s = settingsRepo.getSettings()
                if (s.openAiApiKey.isBlank())
                    return@withContext AIResult.Error("OpenAI API key not set.")
                val resp = openAI.chat("Bearer ${s.openAiApiKey}", ChatRequest(messages = messages))
                if (resp.isSuccessful)
                    AIResult.Success(resp.body()?.choices?.firstOrNull()?.message?.content
                        ?: return@withContext AIResult.Error("Empty response"))
                else
                    AIResult.Error("Chat error ${resp.code()}: ${resp.errorBody()?.string()}")
            }.getOrElse { AIResult.Error("Network error: ${it.message}") }
        }

    // ─── Stability AI ────────────────────────────────────────────────────────

    suspend fun generateImageStability(
        prompt: String,
        negativePrompt: String = "",
        width: Int = 1024,
        height: Int = 1024,
        steps: Int = 30,
        cfgScale: Float = 7.0f,
        engineId: String = "stable-diffusion-xl-1024-v1-0"
    ): AIResult<String> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settingsRepo.getSettings()
            if (s.stabilityApiKey.isBlank())
                return@withContext AIResult.Error("Stability AI key not set. Go to Settings.")
            val prompts = mutableListOf(StabilityTextPrompt(prompt, 1.0f))
            if (negativePrompt.isNotBlank()) prompts.add(StabilityTextPrompt(negativePrompt, -1.0f))
            val resp = stability.textToImage(
                "Bearer ${s.stabilityApiKey}", engineId = engineId,
                request = StabilityTextToImageBody(prompts, cfgScale, width, height, steps)
            )
            if (resp.isSuccessful)
                AIResult.Success("data:image/png;base64," +
                    (resp.body()?.artifacts?.firstOrNull()?.base64
                        ?: return@withContext AIResult.Error("No image data")))
            else
                AIResult.Error("Stability Error ${resp.code()}: ${resp.errorBody()?.string()}")
        }.getOrElse { AIResult.Error("Network error: ${it.message}") }
    }

    // ─── Replicate ───────────────────────────────────────────────────────────

    fun generateWithReplicate(
        prompt: String,
        modelVersion: String,
        extraParams: Map<String, Any> = emptyMap()
    ): Flow<AIResult<String>> = flow {
        emit(AIResult.Loading())
        runCatching {
            val s = settingsRepo.getSettings()
            if (s.replicateApiKey.isBlank()) {
                emit(AIResult.Error("Replicate API key not set. Go to Settings."))
                return@runCatching
            }
            val input = mutableMapOf<String, Any>("prompt" to prompt).apply { putAll(extraParams) }
            val createResp = replicate.createPrediction(
                "Token ${s.replicateApiKey}",
                ReplicateRequest(version = modelVersion, input = input)
            )
            if (!createResp.isSuccessful) {
                emit(AIResult.Error("Failed to start: ${createResp.errorBody()?.string()}"))
                return@runCatching
            }
            val id = createResp.body()?.id
                ?: run { emit(AIResult.Error("No prediction ID")); return@runCatching }

            repeat(100) {
                delay(3000)
                val statusResp = replicate.getPrediction("Token ${s.replicateApiKey}", id)
                if (statusResp.isSuccessful) {
                    when (statusResp.body()?.status) {
                        "succeeded" -> {
                            val out = statusResp.body()?.output
                            val url = when (out) {
                                is String   -> out
                                is List<*>  -> out.firstOrNull()?.toString() ?: ""
                                else        -> out?.toString() ?: ""
                            }
                            emit(if (url.isNotBlank()) AIResult.Success(url)
                                 else AIResult.Error("Empty output"))
                            return@repeat
                        }
                        "failed"   -> { emit(AIResult.Error(statusResp.body()?.error ?: "Failed")); return@repeat }
                        "canceled" -> { emit(AIResult.Error("Canceled")); return@repeat }
                    }
                }
            }
            emit(AIResult.Error("Timed out after 5 minutes"))
        }.onFailure { emit(AIResult.Error("Error: ${it.message}")) }
    }

    // ─── Database ────────────────────────────────────────────────────────────

    suspend fun getAllGenerated(): List<Message> = dao.getAllGenerated()
    suspend fun getMessages(sid: String): List<Message> = dao.getBySession(sid)
    suspend fun saveMessage(m: Message) = dao.insert(m)
    suspend fun deleteMessage(id: String) = dao.deleteById(id)
    suspend fun clearSession(sid: String) = dao.deleteBySession(sid)

    // ─── Helpers ─────────────────────────────────────────────────────────────

    fun bitmapToBase64(bitmap: Bitmap): String {
        val s = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, s)
        return Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)
    }
}
