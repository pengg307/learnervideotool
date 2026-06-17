package com.aigenerator.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class MessageType {
    USER_TEXT, USER_VOICE, USER_IMAGE,
    AI_TEXT, AI_IMAGE, AI_VIDEO,
    SYSTEM, ERROR, LOADING
}

enum class GenerationMode {
    IMAGE, VIDEO, IMAGE_TO_IMAGE, IMAGE_TO_VIDEO
}

enum class AIProvider {
    OPENAI, AGNES
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val type: MessageType = MessageType.USER_TEXT,
    val mediaUrl: String? = null,
    val localMediaPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val generationMode: GenerationMode? = null,
    val sessionId: String = ""
)

data class OpenAIImageRequest(
    val prompt: String,
    val model: String = "dall-e-3",
    val n: Int = 1,
    val size: String = "1024x1024",
    val quality: String = "standard",
    val response_format: String = "url"
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "gpt-4",
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 500
)

data class OpenAIImageResponse(
    val created: Long = 0,
    val data: List<ImageData> = emptyList()
)

data class ImageData(
    val url: String? = null,
    val b64_json: String? = null,
    val revised_prompt: String? = null
)

data class OpenAIChatResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList()
)

data class Choice(
    val message: ChatMessage,
    val finish_reason: String = ""
)



// ============ AGNES API MODELS ============

data class AgnesImageRequest(
    val model: String = "agnes-image-2.0-flash",
    val prompt: String,
    val size: String = "1024x1024",
    val image: List<String>? = null,
    val return_base64: Boolean? = null,
    @SerializedName("extra_body")
    val extraBody: ExtraBody? = null
)

data class ExtraBody(
    @SerializedName("response_format")
    val responseFormat: String? = null
)

data class AgnesImageResponse(
    val data: List<AgnesImageData> = emptyList()
)

data class AgnesImageData(
    val url: String? = null,
    val b64_json: String? = null
)

data class AgnesVideoRequest(
    val model: String = "agnes-video-v2.0",
    val prompt: String,
    val height: Int = 768,
    val width: Int = 1152,
    @SerializedName("num_frames")
    val numFrames: Int = 121,
    @SerializedName("frame_rate")
    val frameRate: Int = 24
)

data class AgnesVideoResponse(
    val id: String = "",
    @SerializedName("task_id")
    val taskId: String = "",
    @SerializedName("video_id")
    val videoId: String = "",
    val `object`: String = "",
    val model: String = "",
    val status: String = "",
    val progress: Int = 0,
    @SerializedName("created_at")
    val createdAt: Long = 0,
    val seconds: String = "",
    val size: String = ""
)

// ============ APP SETTINGS ============

data class AppSettings(
    // OpenAI
    val openAiApiKey: String = "",
    // Agnes AI
    val agnesApiKey: String = "",
    val agnesImageModel: String = "agnes-image-2.0-flash",
    val agnesVideoModel: String = "agnes-video-v2.0",
    // Image settings
    val defaultImageModel: String = "dall-e-3",
    val defaultImageWidth: Int = 1024,
    val defaultImageHeight: Int = 1024,
    val enableNegativePrompt: Boolean = true,
    val saveToGallery: Boolean = true,
    val selectedProvider: AIProvider = AIProvider.OPENAI,
    // Video parameters
    val videoWidth: Int = 1152,
    val videoHeight: Int = 768,
    val videoNumFrames: Int = 121,
    val videoFrameRate: Int = 24
)