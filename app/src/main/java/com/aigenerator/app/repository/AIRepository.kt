package com.aigenerator.app.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.aigenerator.app.database.MessageDao
import com.aigenerator.app.model.AIProvider
import com.aigenerator.app.model.AppSettings
import com.aigenerator.app.model.ChatMessage
import com.aigenerator.app.model.ChatRequest
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.OpenAIImageRequest
import com.aigenerator.app.model.ReplicateRequest
import com.aigenerator.app.model.StabilityTextPrompt
import com.aigenerator.app.model.StabilityTextToImageBody
import com.aigenerator.app.network.OpenAIApiService
import com.aigenerator.app.network.ReplicateApiService
import com.aigenerator.app.network.StabilityApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class AIResult<out T> {
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

    suspend fun generateImageOpenAI(
        prompt: String,
        size: String = "1024x1024"
    ): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepo.getSettings()
            if (settings.openAiApiKey.isBlank()) {
                return@withContext AIResult.Error("OpenAI API key not set. Go to Settings.")
            }
            val response = openAI.generateImage(
                "Bearer ${settings.openAiApiKey}",
                OpenAIImageRequest(prompt = prompt, size = size)
            )
            if (response.isSuccessful) {
                val url = response.body()?.data?.firstOrNull()?.url
                if (url != null) AIResult.Success(url)
                else AIResult.Error("No image URL returned")
            } else {
                AIResult.Error("OpenAI Error ${response.code()}: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            AIResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun chatWithGPT(messages: List<ChatMessage>): AIResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepo.getSettings()
                if (settings.openAiApiKey.isBlank()) {
                    return@withContext AIResult.Error("OpenAI API key not set.")
                }
                val response = openAI.chat(
                    "Bearer ${settings.openAiApiKey}",
                    ChatRequest(messages = messages)
                )
                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (content != null) AIResult.Success(content)
                    else AIResult.Error("Empty response from GPT")
                } else {
                    AIResult.Error("Chat error ${response.code()}: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                AIResult.Error("Network error: ${e.message}")
            }
        }

    suspend fun generateImageStability(
        prompt: String,
        negativePrompt: String = "",
        width: Int = 1024,
        height: Int = 1024,
        steps: Int = 30,
        cfgScale: Float = 7.0f,
        engineId: String = "stable-diffusion-xl-1024-v1-0"
    ): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepo.getSettings()
            if (settings.stabilityApiKey.isBlank()) {
                return@withContext AIResult.Error("Stability AI key not set. Go to Settings.")
            }
            val prompts = mutableListOf(StabilityTextPrompt(prompt, 1.0f))
            if (negativePrompt.isNotBlank()) {
                prompts.add(StabilityTextPrompt(negativePrompt, -1.0f))
            }
            val response = stability.textToImage(
                "Bearer ${settings.stabilityApiKey}",
                engineId = engineId,
                request = StabilityTextToImageBody(
                    text_prompts = prompts,
                    cfg_scale = cfgScale,
                    width = width,
                    height = height,
                    steps = steps
                )
            )
            if (response.isSuccessful) {
                val base64Data = response.body()?.artifacts?.firstOrNull()?.base64
                if (base64Data != null) {
                    AIResult.Success("data:image/png;base64,$base64Data")
                } else {
                    AIResult.Error("No image data returned")
                }
            } else {
                AIResult.Error("Stability Error ${response.code()}: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            AIResult.Error("Network error: ${e.message}")
        }
    }

    fun generateWithReplicate(
        prompt: String,
        modelVersion: String,
        extraParams: Map<String, Any> = emptyMap()
    ): Flow<AIResult<String>> = flow {
        emit(AIResult.Loading())
        try {
            val settings = settingsRepo.getSettings()
            if (settings.replicateApiKey.isBlank()) {
                emit(AIResult.Error("Replicate API key not set. Go to Settings."))
                return@flow
            }
            val input = mutableMapOf<String, Any>("prompt" to prompt)
            input.putAll(extraParams)

            val createResponse = replicate.createPrediction(
                "Token ${settings.replicateApiKey}",
                ReplicateRequest(version = modelVersion, input = input)
            )

            if (!createResponse.isSuccessful) {
                emit(AIResult.Error("Failed to start generation: ${createResponse.errorBody()?.string()}"))
                return@flow
            }

            val predictionId = createResponse.body()?.id
            if (predictionId == null) {
                emit(AIResult.Error("No prediction ID returned"))
                return@flow
            }

            var attempts = 0
            while (attempts < 100) {
                delay(3000)
                val statusResponse = replicate.getPrediction(
                    "Token ${settings.replicateApiKey}",
                    predictionId
                )
                if (statusResponse.isSuccessful) {
                    val body = statusResponse.body()
                    when (body?.status) {
                        "succeeded" -> {
                            val output = body.output
                            val url = when (output) {
                                is String  -> output
                                is List<*> -> output.firstOrNull()?.toString() ?: ""
                                else       -> output?.toString() ?: ""
                            }
                            if (url.isNotBlank()) {
                                emit(AIResult.Success(url))
                            } else {
                                emit(AIResult.Error("Empty output from model"))
                            }
                            return@flow
                        }
                        "failed"   -> {
                            emit(AIResult.Error(body?.error ?: "Generation failed"))
                            return@flow
                        }
                        "canceled" -> {
                            emit(AIResult.Error("Generation was canceled"))
                            return@flow
                        }
                        else -> { /* still running, keep polling */ }
                    }
                }
                attempts++
            }
            emit(AIResult.Error("Timed out after 5 minutes"))
        } catch (e: Exception) {
            emit(AIResult.Error("Error: ${e.message}"))
        }
    }

    // ==================== NEW CUSTOM ENDPOINT FUNCTION ====================
    
    suspend fun generateImageCustom(
        prompt: String,
        endpointUrl: String,
        apiKey: String,
        modelName: String,
        width: Int = 1024,
        height: Int = 1024,
        steps: Int = 30,
        cfgScale: Float = 7.0f
    ): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (endpointUrl.isBlank()) {
                return@withContext AIResult.Error("Custom endpoint URL is not set. Go to Settings.")
            }
            if (apiKey.isBlank()) {
                return@withContext AIResult.Error("Custom API key is not set. Go to Settings.")
            }

            // Build the request URL (assuming OpenAI-compatible endpoint)
            val url = if (endpointUrl.endsWith("/")) {
                "${endpointUrl}images/generations"
            } else {
                "$endpointUrl/images/generations"
            }

            // Create JSON request body (OpenAI-compatible format)
            val jsonBody = JSONObject().apply {
                put("prompt", prompt)
                put("model", modelName.ifBlank { "default" })
                put("n", 1)
                put("size", "${width}x${height}")
                put("steps", steps)
                put("cfg_scale", cfgScale)
            }

            // Create HTTP client and request
            val client = OkHttpClient()
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            // Execute request
            val response = client.newCall(request).execute()
            
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                // Parse response to get image URL (OpenAI-compatible format)
                val jsonResponse = JSONObject(responseBody)
                val imageUrl = jsonResponse
                    .optJSONArray("data")
                    ?.optJSONObject(0)
                    ?.optString("url", "")
                    ?: ""
                
                if (imageUrl.isNotBlank()) {
                    AIResult.Success(imageUrl)
                } else {
                    // Try alternative response formats
                    val fallbackUrl = jsonResponse.optString("output", "")
                    if (fallbackUrl.isNotBlank()) {
                        AIResult.Success(fallbackUrl)
                    } else {
                        AIResult.Error("No image URL in response: $responseBody")
                    }
                }
            } else {
                AIResult.Error("Custom endpoint error: ${response.code} - ${responseBody ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            AIResult.Error("Custom endpoint network error: ${e.message}")
        }
    }

    // ==================== END CUSTOM ENDPOINT FUNCTION ====================

    suspend fun getAllGenerated(): List<Message> = dao.getAllGenerated()
    suspend fun getMessages(sessionId: String): List<Message> = dao.getBySession(sessionId)
    suspend fun saveMessage(message: Message) = dao.insert(message)
    suspend fun deleteMessage(id: String) = dao.deleteById(id)
    suspend fun clearSession(sessionId: String) = dao.deleteBySession(sessionId)

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}