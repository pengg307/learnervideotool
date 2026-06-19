package com.aigenerator.app.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.aigenerator.app.database.MessageDao
import com.aigenerator.app.model.AIProvider
import com.aigenerator.app.model.AppSettings
import com.aigenerator.app.model.ChatMessage
import com.aigenerator.app.model.ChatRequest
import com.aigenerator.app.model.GenerationMode
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import com.aigenerator.app.model.OpenAIImageRequest
import com.aigenerator.app.network.OpenAIApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
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
            if (response.isSuccessful) {
                val url = response.body()?.data?.firstOrNull()?.url
                if (url != null) {
                    val savedPath = downloadAndSaveImage(url, prompt)
                    if (savedPath != null) {
                        val message = Message(
                            content = prompt,
                            type = MessageType.AI_IMAGE,
                            mediaUrl = url,
                            localMediaPath = savedPath,
                            generationMode = GenerationMode.IMAGE,
                            isSaved = true,
                            sessionId = "gallery"
                        )
                        dao.insert(message)
                        return@withContext AIResult.Success(savedPath)
                    }
                    AIResult.Success(url)
                } else {
                    AIResult.Error("No image URL returned")
                }
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

    // ============ AGNES AI ============

    suspend fun generateImageAgnes(
        prompt: String,
        apiKey: String,
        modelName: String = "agnes-image-2.0-flash",
        size: String = "1024x1024",
        inputImage: String? = null,
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
                if (returnBase64) {
                    put("return_base64", true)
                }
                if (responseFormat.isNotBlank()) {
                    val extraBody = JSONObject().apply {
                        put("response_format", responseFormat)
                    }
                    put("extra_body", extraBody)
                }
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
                .addHeader("Authorization", "Bearer $apiKey")
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
                        val savedPath = downloadAndSaveImage(imageUrl, prompt)
                        if (savedPath != null) {
                            val message = Message(
                                content = prompt,
                                type = MessageType.AI_IMAGE,
                                mediaUrl = imageUrl,
                                localMediaPath = savedPath,
                                generationMode = GenerationMode.IMAGE,
                                isSaved = true,
                                sessionId = "gallery"
                            )
                            dao.insert(message)
                            return@withContext AIResult.Success(savedPath)
                        }
                        return@withContext AIResult.Success(imageUrl)
                    } else {
                        AIResult.Error("No image URL or Base64 in response: $responseBody")
                    }
                } else {
                    val directUrl = jsonResponse.optString("url", "")
                        .ifBlank { jsonResponse.optString("output", "") }
                    if (directUrl.isNotBlank()) {
                        val savedPath = downloadAndSaveImage(directUrl, prompt)
                        if (savedPath != null) {
                            val message = Message(
                                content = prompt,
                                type = MessageType.AI_IMAGE,
                                mediaUrl = directUrl,
                                localMediaPath = savedPath,
                                generationMode = GenerationMode.IMAGE,
                                isSaved = true,
                                sessionId = "gallery"
                            )
                            dao.insert(message)
                            return@withContext AIResult.Success(savedPath)
                        }
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

    suspend fun generateVideoAgnes(
        prompt: String,
        apiKey: String,
        modelName: String = "agnes-video-v2.0",
        height: Int = 768,
        width: Int = 1152,
        numFrames: Int = 121,
        frameRate: Int = 24,
        onProgress: ((Int) -> Unit)? = null,
        autoSave: Boolean = true
    ): AIResult<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext AIResult.Error("Agnes API key not set. Go to Settings.")
            }

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
                .addHeader("Authorization", "Bearer $apiKey")
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

            val statusUrl = "https://apihub.agnes-ai.com/agnesapi?video_id=$videoId"

            var attempts = 0
            val maxAttempts = 120
            var lastStatus = "queued"
            var lastProgress = 0

            while (attempts < maxAttempts) {
                delay(3000)
                
                val statusRequest = Request.Builder()
                    .url(statusUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
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
                                .ifBlank { statusJson.optString("remixed_from_video_id", "") }
                                .ifBlank { statusJson.optString("remixedfromvideoid", "") }
                                .ifBlank { statusJson.optJSONObject("result")?.optString("url", "") ?: "" }
                                .ifBlank { statusJson.optJSONObject("data")?.optString("url", "") ?: "" }
                            
                            if (videoUrl.isNotBlank()) {
                                if (autoSave) {
                                    val savedPath = downloadAndSaveVideo(videoUrl, prompt)
                                    if (savedPath != null) {
                                        val message = Message(
                                            content = prompt,
                                            type = MessageType.AI_VIDEO,
                                            mediaUrl = videoUrl,
                                            localMediaPath = savedPath,
                                            generationMode = GenerationMode.VIDEO,
                                            isSaved = true,
                                            sessionId = "gallery"
                                        )
                                        dao.insert(message)
                                        return@withContext AIResult.Success(savedPath)
                                    }
                                }
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

    // ============ VIDEO COMBINING ============

    suspend fun combineVideosToGallery(selectedItems: List<Message>): AIResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepo.getSettings()
                val apiKey = settings.agnesApiKey

                if (apiKey.isBlank()) {
                    return@withContext AIResult.Error("Agnes API key not set. Go to Settings.")
                }

                val videoUrls = selectedItems.mapNotNull { 
                    when {
                        it.localMediaPath != null && File(it.localMediaPath).exists() -> it.localMediaPath
                        it.mediaUrl != null -> it.mediaUrl
                        else -> null
                    }
                }
                
                if (videoUrls.size < 2) {
                    return@withContext AIResult.Error("At least 2 videos required to combine.")
                }

                val endpoint = "https://apihub.agnes-ai.com/v1/videos/combine"

                val jsonBody = JSONObject().apply {
                    val urlsArray = org.json.JSONArray()
                    videoUrls.forEach { urlsArray.put(it) }
                    put("video_urls", urlsArray)
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build()

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val videoId = jsonResponse.optString("video_id", "")
                        .ifBlank { jsonResponse.optString("task_id", "") }
                        .ifBlank { jsonResponse.optString("id", "") }

                    if (videoId.isBlank()) {
                        return@withContext AIResult.Error("No video_id returned: $responseBody")
                    }

                    val statusUrl = "https://apihub.agnes-ai.com/agnesapi?video_id=$videoId"
                    var attempts = 0
                    val maxAttempts = 120

                    while (attempts < maxAttempts) {
                        delay(3000)

                        val statusRequest = Request.Builder()
                            .url(statusUrl)
                            .addHeader("Authorization", "Bearer $apiKey")
                            .get()
                            .build()

                        val statusResponse = client.newCall(statusRequest).execute()
                        val statusBody = statusResponse.body?.string()

                        if (statusResponse.isSuccessful && statusBody != null) {
                            val statusJson = JSONObject(statusBody)
                            val status = statusJson.optString("status", "unknown")

                            when (status) {
                                "completed", "succeeded", "success" -> {
                                    val videoUrl = statusJson.optString("video_url", "")
                                        .ifBlank { statusJson.optString("url", "") }
                                        .ifBlank { statusJson.optString("output", "") }

                                    if (videoUrl.isNotBlank()) {
                                        val savedPath = downloadAndSaveVideo(videoUrl, "combined_video")
                                        if (savedPath != null) {
                                            val combinedMessage = Message(
                                                id = java.util.UUID.randomUUID().toString(),
                                                sessionId = "gallery",
                                                content = "Combined video (${selectedItems.size} clips)",
                                                mediaUrl = videoUrl,
                                                localMediaPath = savedPath,
                                                type = MessageType.AI_VIDEO,
                                                timestamp = System.currentTimeMillis(),
                                                generationMode = GenerationMode.VIDEO,
                                                isSaved = true
                                            )
                                            dao.insert(combinedMessage)
                                            return@withContext AIResult.Success(savedPath)
                                        }
                                        return@withContext AIResult.Success(videoUrl)
                                    } else {
                                        return@withContext AIResult.Error("No URL in completed response: $statusBody")
                                    }
                                }
                                "failed", "error" -> {
                                    val errorMsg = statusJson.optString("error",
                                        statusJson.optString("message", "Unknown error"))
                                    return@withContext AIResult.Error("Combine failed: $errorMsg")
                                }
                                "canceled" -> {
                                    return@withContext AIResult.Error("Video combining was canceled.")
                                }
                            }
                        }
                        attempts++
                    }
                    AIResult.Error("Combine timed out after ${maxAttempts * 3} seconds.")
                } else {
                    AIResult.Error("API error: ${response.code} - ${responseBody ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                AIResult.Error("Error combining videos: ${e.message}")
            }
        }

    // ============ DATABASE OPERATIONS ============

    suspend fun getAllGenerated(): List<Message> = dao.getAllGenerated()
    suspend fun getMessages(sessionId: String): List<Message> = dao.getBySession(sessionId)
    suspend fun saveMessage(message: Message) = dao.insert(message)
    suspend fun deleteMessage(id: String) = dao.deleteById(id)
    suspend fun clearSession(sessionId: String) = dao.deleteBySession(sessionId)

    suspend fun getSavedMedia(): List<Message> = withContext(Dispatchers.IO) {
        dao.getAllGenerated().filter { it.isSaved && it.localMediaPath != null }
    }

    // ============ SAVE TO GALLERY FUNCTIONS ============

    suspend fun downloadAndSaveImage(url: String, name: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "AI_Image_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AI_Generator")
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val connection = URL(url).openConnection()
                        connection.connect()
                        val inputStream = connection.getInputStream()
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        return@withContext uri.toString()
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("AIRepository", "Error saving image: ${e.message}")
                null
            }
        }
    }

    suspend fun downloadAndSaveVideo(url: String, name: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "AI_Video_${System.currentTimeMillis()}.mp4"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AI_Generator")
                }

                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val connection = URL(url).openConnection()
                        connection.connect()
                        val inputStream = connection.getInputStream()
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        return@withContext uri.toString()
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("AIRepository", "Error saving video: ${e.message}")
                null
            }
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}