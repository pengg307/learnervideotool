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
import com.aigenerator.app.network.OpenAIApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
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
    private val settingsRepo: SettingsRepository,
    private val dao: MessageDao
) {

    // ============ OPENAI ============

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
            if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                AIResult.Success(response.body()!!.data[0].url ?: "")
            } else {
                AIResult.Error("OpenAI Error ${response.code()}: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            AIResult.Error("OpenAI exception: ${e.message}")
        }
    }

    suspend fun chatWithGPT(messages: List<ChatMessage>): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepo.getSettings()
            if (settings.openAiApiKey.isBlank()) {
                return@withContext AIResult.Error("OpenAI API key not set.")
            }
            val request = ChatRequest(model = "gpt-4o-mini", messages = messages)
            val response = openAI.chat("Bearer ${settings.openAiApiKey}", request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.choices.isNotEmpty()) {
                    AIResult.Success(body.choices[0].message.content)
                } else {
                    AIResult.Error("Empty response from GPT")
                }
            } else {
                AIResult.Error("OpenAI Chat Error ${response.code()}: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            AIResult.Error("Chat exception: ${e.message}")
        }
    }

    // ============ AGNES AI ============

    suspend fun generateImageAgnes(
        prompt: String,
        size: String = "1024x1024"
    ): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepo.getSettings()
            if (settings.agnesApiKey.isBlank()) {
                return@withContext AIResult.Error("Agnes API key not set. Go to Settings.")
            }
            
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val requestBody = JSONObject().apply {
                put("model", "agnes-image-2.0-flash")
                put("prompt", prompt)
                put("size", size)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.agnes-ai.com/v1/images/generations")
                .addHeader("Authorization", "Bearer ${settings.agnesApiKey}")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val data = json.getJSONArray("data")
                if (data.length() > 0) {
                    val url = data.getJSONObject(0).getString("url")
                    AIResult.Success(url)
                } else {
                    AIResult.Error("No image data in response")
                }
            } else {
                AIResult.Error("Agnes Image Error ${response.code()}: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            AIResult.Error("Agnes Image exception: ${e.message}")
        }
    }

    suspend fun generateVideoAgnes(
        prompt: String,
        height: Int = 768,
        width: Int = 1152,
        numFrames: Int = 121,
        frameRate: Int = 24
    ): Flow<AIResult<String>> = flow {
        emit(AIResult.Loading())
        try {
            val settings = settingsRepo.getSettings()
            if (settings.agnesApiKey.isBlank()) {
                emit(AIResult.Error("Agnes API key not set. Go to Settings."))
                return@flow
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val requestBody = JSONObject().apply {
                put("model", "agnes-video-v2.0")
                put("prompt", prompt)
                put("height", height)
                put("width", width)
                put("num_frames", numFrames)
                put("frame_rate", frameRate)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.agnes-ai.com/v1/videos/generations")
                .addHeader("Authorization", "Bearer ${settings.agnesApiKey}")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val taskId = json.getString("task_id")
                val videoId = json.getString("video_id")
                
                emit(AIResult.Success("Task submitted: $taskId"))
                
                // Poll for completion
                var attempts = 0
                while (attempts < 200) {
                    delay(3000)
                    attempts++
                    
                    val statusReq = Request.Builder()
                        .url("https://api.agnes-ai.com/v1/tasks/$taskId")
                        .addHeader("Authorization", "Bearer ${settings.agnesApiKey}")
                        .get()
                        .build()
                    
                    val statusResp = client.newCall(statusReq).execute()
                    if (statusResp.isSuccessful) {
                        val statusJson = JSONObject(statusResp.body?.string() ?: "{}")
                        val status = statusJson.optString("status", "unknown")
                        val progress = statusJson.optInt("progress", 0)
                        
                        if (status == "completed") {
                            val url = statusJson.optString("output", "")
                            if (url.isNotEmpty()) {
                                emit(AIResult.Success(url))
                            } else {
                                emit(AIResult.Error("Task completed but no URL"))
                            }
                            return@flow
                        } else if (status == "failed") {
                            val errorMsg = statusJson.optString("error", "Unknown error")
                            emit(AIResult.Error("Video generation failed: $errorMsg"))
                            return@flow
                        } else {
                            emit(AIResult.Loading())
                        }
                    }
                }
                emit(AIResult.Error("Video generation timed out"))
            } else {
                emit(AIResult.Error("Failed to submit video: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(AIResult.Error("Video exception: ${e.message}"))
        }
    }

    // ============ UTILITIES ============

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    suspend fun getAllGenerated(): List<Message> = dao.getAllGenerated()
    suspend fun getMessages(sessionId: String): List<Message> = dao.getBySession(sessionId)
    suspend fun saveMessage(message: Message) = dao.insert(message)
    suspend fun deleteMessage(id: String) = dao.deleteById(id)
    suspend fun clearSession(sessionId: String) = dao.deleteBySession(sessionId)
}
