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
    private val stability: StabilityApiService,
    private val replicate: ReplicateApiService,
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

    // ============ STABILITY AI ============

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

    // ============ REPLICATE ============

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

    // ============ AGNES AI ============

    /**
     * Generate image using Agnes AI API
     * Supports text-to-image and image-to-image
     * Endpoint: POST https://apihub.agnes-ai.com/v1/images/generations
     */
    suspend fun generateImageAgnes(
        prompt: String,
        apiKey: String,
        modelName: String = "agnes-image-2.0-flash",
        size: String = "1024x1024",
        inputImage: String? = null,  // URL or Base64 for image-to-image
        returnBase64: Boolean = false,
        responseFormat: String = "url"
    ): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext AIResult.Error("Agnes API key not set. Go to Settings.")
            }

            val endpoint = "https://apihub.agnes-ai.com/v1/images/generations"

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("prompt", prompt)
                put("size", size)
                inputImage?.let {
                    val imagesArray = org.json.JSONArray()
                    imagesArray.put(it)
                    put("image", imagesArray)
                }
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey.trim()")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val dataArray = jsonResponse.optJSONArray("data")
                if (dataArray != null && dataArray.length() > 0) {
                    val firstItem = dataArray.getJSONObject(0)
                    var imageUrl = firstItem.optString("url", "")
                    if (imageUrl.isBlank()) {
                        val b64 = firstItem.optString("b64_json", "")
                        if (b64.isNotBlank()) {
                            imageUrl = "data:image/png;base64,$b64"
                        }
                    }
                    if (imageUrl.isNotBlank()) {
                        AIResult.Success(imageUrl)
                    } else {
                        AIResult.Error("No image URL or Base64 in response: $responseBody")
                    }
                } else {
                    val directUrl = jsonResponse.optString("url", "")
                        .ifBlank { jsonResponse.optString("output", "") }
                    if (directUrl.isNotBlank()) {
                        AIResult.Success(directUrl)
                    } else {
                        AIResult.Error("No image data in response: $responseBody")
                    }
                }
            } else {
                AIResult.Error("Agnes API error: ${response.code} - ${responseBody ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            AIResult.Error("Network error: ${e.message}")
        }
    }

    /**
     * Generate video using Agnes AI API with async polling
     * Endpoint: POST https://apihub.agnes-ai.com/v1/videos
     * Polling: GET https://apihub.agnes-ai.com/agnesapi?video_id=<VIDEO_ID>
     */
    suspend fun generateVideoAgnes(
        prompt: String,
        apiKey: String,
        modelName: String = "agnes-video-v2.0",
        height: Int = 768,
        width: Int = 1152,
        numFrames: Int = 121,
        frameRate: Int = 24,
        onProgress: ((Int) -> Unit)? = null
    ): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext AIResult.Error("Agnes API key not set. Go to Settings.")
            }

            // ============ STEP 1: Create Video Task ============
            val endpoint = "https://apihub.agnes-ai.com/v1/videos"
            
            val createBody = JSONObject().apply {
                put("model", modelName)
                put("prompt", prompt)
                put("height", height)
                put("width", width)
                put("num_frames", numFrames)
                put("frame_rate", frameRate)
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            val requestBody = createBody.toString()
                .toRequestBody("application/json".toMediaType())

            val createRequest = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey.trim()")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val createResponse = client.newCall(createRequest).execute()
            val createResponseBody = createResponse.body?.string()

            if (!createResponse.isSuccessful || createResponseBody == null) {
                return@withContext AIResult.Error("Failed to create video task: ${createResponse.code} - ${createResponseBody ?: "Unknown error"}")
            }

            val jsonResponse = JSONObject(createResponseBody)
            val videoId = jsonResponse.optString("video_id", "")
                .ifBlank { jsonResponse.optString("task_id", "") }
                .ifBlank { jsonResponse.optString("id", "") }

            if (videoId.isBlank()) {
                return@withContext AIResult.Error("No video_id in response: $createResponseBody")
            }

            // ============ STEP 2: Poll for Result ============
            val statusUrl = "https://apihub.agnes-ai.com/agnesapi?video_id=$videoId"

            var attempts = 0
            val maxAttempts = 120  // 6 minutes max
            var lastStatus = "queued"
            var lastProgress = 0

            while (attempts < maxAttempts) {
                delay(3000)
                
                val statusRequest = Request.Builder()
                    .url(statusUrl)
                    .addHeader("Authorization", "Bearer $apiKey.trim()")
                    .get()
                    .build()

                val statusResponse = client.newCall(statusRequest).execute()
                val statusBody = statusResponse.body?.string()

                if (statusResponse.isSuccessful && statusBody != null) {
                    val statusJson = JSONObject(statusBody)
                    val status = statusJson.optString("status", "unknown")
                    val progress = statusJson.optInt("progress", 0)
                    lastStatus = status
                    lastProgress = progress

                    onProgress?.invoke(progress)

                    when (status) {
                        "completed", "succeeded", "success" -> {
                            var videoUrl = statusJson.optString("video_url", "")
                                .ifBlank { statusJson.optString("url", "") }
                                .ifBlank { statusJson.optString("output", "") }
                                .ifBlank { statusJson.optJSONObject("result")?.optString("url", "") ?: "" }
                                .ifBlank { statusJson.optJSONObject("data")?.optString("url", "") ?: "" }
                            
                            if (videoUrl.isNotBlank()) {
                                return@withContext AIResult.Success(videoUrl)
                            } else {
                                return@withContext AIResult.Error("Video completed but no URL found: $statusBody")
                            }
                        }
                        "failed", "error" -> {
                            val errorMsg = statusJson.optString("error", statusJson.optString("message", "Unknown error"))
                            return@withContext AIResult.Error("Video generation failed: $errorMsg")
                        }
                        "canceled" -> {
                            return@withContext AIResult.Error("Video generation was canceled")
                        }
                        else -> { /* still processing */ }
                    }
                }
                attempts++
            }

            AIResult.Error("Video generation timed out. Last status: $lastStatus ($lastProgress%)")

        } catch (e: Exception) {
            AIResult.Error("Network error: ${e.message}")
        }
    }

    // ============ DATABASE OPERATIONS ============

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