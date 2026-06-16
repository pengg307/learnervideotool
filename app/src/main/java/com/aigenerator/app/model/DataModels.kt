package com.aigenerator.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    OPENAI, STABILITY_AI, REPLICATE, CUSTOM
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

data class StabilityTextToImageBody(
    val text_prompts: List<StabilityTextPrompt>,
    val cfg_scale: Float = 7.0f,
    val width: Int = 1024,
    val height: Int = 1024,
    val steps: Int = 30,
    val samples: Int = 1
)

data class StabilityTextPrompt(
    val text: String,
    val weight: Float = 1.0f
)

data class ReplicateRequest(
    val version: String,
    val input: Map<String, Any>
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

data class StabilityImageResponse(
    val artifacts: List<Artifact> = emptyList()
)

data class Artifact(
    val base64: String = "",
    val seed: Long = 0,
    val finishReason: String = ""
)

data class ReplicateResponse(
    val id: String = "",
    val status: String = "",
    val output: Any? = null,
    val error: String? = null
)

data class AppSettings(
    val openAiApiKey: String = "",
    val stabilityApiKey: String = "",
    val replicateApiKey: String = "",
    val defaultImageModel: String = "dall-e-3",
    val defaultVideoModel: String = "stable-video-diffusion",
    val defaultImageWidth: Int = 1024,
    val defaultImageHeight: Int = 1024,
    val defaultSteps: Int = 30,
    val defaultCfgScale: Float = 7.0f,
    val enableNegativePrompt: Boolean = true,
    val saveToGallery: Boolean = true,
    val selectedProvider: AIProvider = AIProvider.OPENAI,
    val replicateImageVersion: String =
        "stability-ai/sdxl:39ed52f2319f9637e7e26c44e294bba72df75aae2bec46e7cb50be2f5b3aecf9",
    val replicateVideoVersion: String =
        "stability-ai/stable-video-diffusion:3f0457e4619daac51203dedb472816fd4af51f3149fa7a9e0b5ffcf1b8172438",
    // Custom endpoint fields
    val customEndpointUrl: String = "",              // Image endpoint
    val customVideoEndpointUrl: String = "",         // Video endpoint
    val customApiKey: String = "",
    val customModelName: String = "",
    // Video generation parameters
    val videoWidth: Int = 1152,
    val videoHeight: Int = 768,
    val videoNumFrames: Int = 121,
    val videoFrameRate: Int = 24
)