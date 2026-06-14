package com.aigenerator.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigenerator.app.model.AIProvider
import com.aigenerator.app.model.AppSettings
import com.aigenerator.app.model.ChatMessage
import com.aigenerator.app.model.GenerationMode
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import com.aigenerator.app.repository.AIRepository
import com.aigenerator.app.repository.AIResult
import com.aigenerator.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isGenerating: Boolean = false,
    val currentMode: GenerationMode = GenerationMode.IMAGE,
    val uploadedBitmap: Bitmap? = null,
    val uploadedImageUri: String? = null,
    val sessionId: String = UUID.randomUUID().toString()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: AIRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val chatHistory = mutableListOf(
        ChatMessage(
            role = "system",
            content = "You are an AI creative assistant for image and video generation. " +
                "When a user wants an IMAGE respond ONLY with: [IMAGE]: <detailed visual prompt>. " +
                "When a user wants a VIDEO respond ONLY with: [VIDEO]: <detailed visual prompt>. " +
                "Otherwise have a short helpful conversation to understand their needs."
        )
    )

    fun setMode(mode: GenerationMode) {
        _state.update { it.copy(currentMode = mode) }
    }

    fun setUploadedImage(bitmap: Bitmap, uri: String) {
        _state.update { it.copy(uploadedBitmap = bitmap, uploadedImageUri = uri) }
        addMessage(
            Message(
                content = "Image uploaded successfully",
                type = MessageType.USER_IMAGE,
                localMediaPath = uri,
                sessionId = _state.value.sessionId
            )
        )
    }

    fun removeUploadedImage() {
        _state.update { it.copy(uploadedBitmap = null, uploadedImageUri = null) }
    }

    fun sendMessage(text: String, isVoice: Boolean = false) {
        if (text.isBlank() || _state.value.isGenerating) return

        addMessage(
            Message(
                content = text,
                type = if (isVoice) MessageType.USER_VOICE else MessageType.USER_TEXT,
                sessionId = _state.value.sessionId
            )
        )
        _state.update { it.copy(isGenerating = true) }

        viewModelScope.launch {
            val thinkingId = UUID.randomUUID().toString()
            addMessage(
                Message(
                    id = thinkingId,
                    content = "Thinking...",
                    type = MessageType.LOADING,
                    isLoading = true,
                    sessionId = _state.value.sessionId
                )
            )

            try {
                val settings = settingsRepo.getSettings()
                chatHistory.add(ChatMessage(role = "user", content = text))

                when (val chatResult = repo.chatWithGPT(chatHistory)) {
                    is AIResult.Success -> {
                        val reply = chatResult.data
                        chatHistory.add(ChatMessage(role = "assistant", content = reply))
                        removeMessage(thinkingId)

                        when {
                            reply.startsWith("[IMAGE]:") -> {
                                val prompt = reply.removePrefix("[IMAGE]:").trim()
                                addMessage(
                                    Message(
                                        content = "Generating image for: $prompt",
                                        type = MessageType.AI_TEXT,
                                        sessionId = _state.value.sessionId
                                    )
                                )
                                generateImage(prompt, settings)
                            }
                            reply.startsWith("[VIDEO]:") -> {
                                val prompt = reply.removePrefix("[VIDEO]:").trim()
                                addMessage(
                                    Message(
                                        content = "Generating video for: $prompt",
                                        type = MessageType.AI_TEXT,
                                        sessionId = _state.value.sessionId
                                    )
                                )
                                generateVideo(prompt, settings)
                            }
                            else -> {
                                addMessage(
                                    Message(
                                        content = reply,
                                        type = MessageType.AI_TEXT,
                                        sessionId = _state.value.sessionId
                                    )
                                )
                            }
                        }
                    }
                    is AIResult.Error -> {
                        removeMessage(thinkingId)
                        directGenerate(text, settings)
                    }
                    else -> removeMessage(thinkingId)
                }
            } catch (e: Exception) {
                addMessage(
                    Message(
                        content = "Error: ${e.message}",
                        type = MessageType.ERROR,
                        sessionId = _state.value.sessionId
                    )
                )
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    private suspend fun generateImage(prompt: String, settings: AppSettings) {
        val loadingId = UUID.randomUUID().toString()
        addMessage(
            Message(
                id = loadingId,
                content = "Creating your image...",
                type = MessageType.LOADING,
                isLoading = true,
                sessionId = _state.value.sessionId
            )
        )

        val result = when (settings.selectedProvider) {
            AIProvider.OPENAI -> repo.generateImageOpenAI(prompt)
            AIProvider.STABILITY_AI -> repo.generateImageStability(
                prompt = prompt,
                width = settings.defaultImageWidth,
                height = settings.defaultImageHeight,
                steps = settings.defaultSteps,
                cfgScale = settings.defaultCfgScale
            )
            AIProvider.REPLICATE -> {
                var finalResult: AIResult<String> = AIResult.Error("Not started")
                repo.generateWithReplicate(
                    prompt = prompt,
                    modelVersion = settings.replicateImageVersion,
                    extraParams = mapOf(
                        "width" to settings.defaultImageWidth,
                        "height" to settings.defaultImageHeight,
                        "num_inference_steps" to settings.defaultSteps
                    )
                ).collect { finalResult = it }
                finalResult
            }
        }

        removeMessage(loadingId)
        handleGenerationResult(result, isVideo = false)
    }

    private suspend fun generateVideo(prompt: String, settings: AppSettings) {
        val loadingId = UUID.randomUUID().toString()
        addMessage(
            Message(
                id = loadingId,
                content = "Creating your video, this can take 2-5 minutes...",
                type = MessageType.LOADING,
                isLoading = true,
                sessionId = _state.value.sessionId
            )
        )

        var finalResult: AIResult<String> = AIResult.Error("Not started")
        repo.generateWithReplicate(
            prompt = prompt,
            modelVersion = settings.replicateVideoVersion,
            extraParams = mapOf("num_frames" to 25, "fps" to 8)
        ).collect { finalResult = it }

        removeMessage(loadingId)
        handleGenerationResult(finalResult, isVideo = true)
    }

    private suspend fun directGenerate(text: String, settings: AppSettings) {
        when (_state.value.currentMode) {
            GenerationMode.IMAGE, GenerationMode.IMAGE_TO_IMAGE -> generateImage(text, settings)
            GenerationMode.VIDEO, GenerationMode.IMAGE_TO_VIDEO -> generateVideo(text, settings)
        }
    }

    private fun handleGenerationResult(result: AIResult<String>, isVideo: Boolean) {
        when (result) {
            is AIResult.Success -> {
                val message = Message(
                    content = if (isVideo) "Your video is ready!" else "Your image is ready!",
                    type = if (isVideo) MessageType.AI_VIDEO else MessageType.AI_IMAGE,
                    mediaUrl = result.data,
                    sessionId = _state.value.sessionId
                )
                addMessage(message)
                viewModelScope.launch { repo.saveMessage(message) }
            }
            is AIResult.Error -> {
                addMessage(
                    Message(
                        content = "Generation failed: ${result.message}",
                        type = MessageType.ERROR,
                        sessionId = _state.value.sessionId
                    )
                )
            }
            else -> {}
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repo.clearSession(_state.value.sessionId)
            _state.value = ChatUiState(sessionId = UUID.randomUUID().toString())
            chatHistory.clear()
            chatHistory.add(
                ChatMessage(
                    "system",
                    "You are an AI creative assistant for image and video generation."
                )
            )
        }
    }

    private fun addMessage(message: Message) {
        _state.update { it.copy(messages = it.messages + message) }
    }

    private fun removeMessage(id: String) {
        _state.update { state ->
            state.copy(messages = state.messages.filter { it.id != id })
        }
    }
}
