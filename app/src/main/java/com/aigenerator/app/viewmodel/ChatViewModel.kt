package com.aigenerator.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigenerator.app.model.*
import com.aigenerator.app.repository.AIRepository
import com.aigenerator.app.repository.AIResult
import com.aigenerator.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
            content = """You are an AI creative assistant for image and video generation.
When a user wants an IMAGE, respond ONLY with: [IMAGE]: <detailed visual prompt>
When a user wants a VIDEO, respond ONLY with: [VIDEO]: <detailed visual prompt>
Otherwise have a short helpful conversation.
Make prompts highly descriptive for best results."""
        )
    )

    fun setMode(mode: GenerationMode) = _state.update { it.copy(currentMode = mode) }

    fun setUploadedImage(bitmap: Bitmap, uri: String) {
        _state.update { it.copy(uploadedBitmap = bitmap, uploadedImageUri = uri) }
        addMsg(Message(content = "Image uploaded ?", type = MessageType.USER_IMAGE,
            localMediaPath = uri, sessionId = _state.value.sessionId))
    }

    fun removeUploadedImage() = _state.update { it.copy(uploadedBitmap = null, uploadedImageUri = null) }

    fun sendMessage(text: String, isVoice: Boolean = false) {
        if (text.isBlank() || _state.value.isGenerating) return
        addMsg(Message(content = text,
            type = if (isVoice) MessageType.USER_VOICE else MessageType.USER_TEXT,
            sessionId = _state.value.sessionId))
        _state.update { it.copy(isGenerating = true) }
        viewModelScope.launch {
            val thinkId = UUID.randomUUID().toString()
            addMsg(Message(id = thinkId, content = "Thinking...",
                type = MessageType.LOADING, isLoading = true,
                sessionId = _state.value.sessionId))
            try {
                val settings = settingsRepo.getSettings()
                chatHistory.add(ChatMessage("user", text))
                when (val r = repo.chatWithGPT(chatHistory)) {
                    is AIResult.Success -> {
                        val reply = r.data
                        chatHistory.add(ChatMessage("assistant", reply))
                        removeMsg(thinkId)
                        when {
                            reply.startsWith("[IMAGE]:") -> {
                                val prompt = reply.removePrefix("[IMAGE]:").trim()
                                addMsg(Message(content = "?? Generating: "$prompt"",
                                    type = MessageType.AI_TEXT,
                                    sessionId = _state.value.sessionId))
                                generateImage(prompt, settings)
                            }
                            reply.startsWith("[VIDEO]:") -> {
                                val prompt = reply.removePrefix("[VIDEO]:").trim()
                                addMsg(Message(content = "?? Generating video: "$prompt"",
                                    type = MessageType.AI_TEXT,
                                    sessionId = _state.value.sessionId))
                                generateVideo(prompt, settings)
                            }
                            else -> addMsg(Message(content = reply,
                                type = MessageType.AI_TEXT,
                                sessionId = _state.value.sessionId))
                        }
                    }
                    is AIResult.Error -> { removeMsg(thinkId); directGenerate(text, settings) }
                    else -> removeMsg(thinkId)
                }
            } catch (e: Exception) {
                addMsg(Message(content = "? ${e.message}",
                    type = MessageType.ERROR, sessionId = _state.value.sessionId))
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    private suspend fun generateImage(prompt: String, settings: AppSettings) {
        val loadId = UUID.randomUUID().toString()
        addMsg(Message(id = loadId, content = "Creating your image...",
            type = MessageType.LOADING, isLoading = true, sessionId = _state.value.sessionId))
        val result = when (settings.selectedProvider) {
            AIProvider.OPENAI -> repo.generateImageOpenAI(prompt)
            AIProvider.STABILITY_AI -> repo.generateImageStability(prompt,
                width = settings.defaultImageWidth, height = settings.defaultImageHeight,
                steps = settings.defaultSteps, cfgScale = settings.defaultCfgScale)
            AIProvider.REPLICATE -> {
                var res: AIResult<String> = AIResult.Error("Not started")
                repo.generateWithReplicate(prompt, settings.replicateImageVersion,
                    mapOf("width" to settings.defaultImageWidth,
                          "height" to settings.defaultImageHeight,
                          "num_inference_steps" to settings.defaultSteps)
                ).collect { res = it }
                res
            }
        }
        removeMsg(loadId)
        handleResult(result, isVideo = false)
    }

    private suspend fun generateVideo(prompt: String, settings: AppSettings) {
        val loadId = UUID.randomUUID().toString()
        addMsg(Message(id = loadId, content = "Creating your video (2-5 min)...",
            type = MessageType.LOADING, isLoading = true, sessionId = _state.value.sessionId))
        var result: AIResult<String> = AIResult.Error("Not started")
        repo.generateWithReplicate(prompt, settings.replicateVideoVersion,
            mapOf("num_frames" to 25, "fps" to 8)
        ).collect { result = it }
        removeMsg(loadId)
        handleResult(result, isVideo = true)
    }

    private suspend fun directGenerate(text: String, settings: AppSettings) {
        when (_state.value.currentMode) {
            GenerationMode.IMAGE, GenerationMode.IMAGE_TO_IMAGE -> generateImage(text, settings)
            GenerationMode.VIDEO, GenerationMode.IMAGE_TO_VIDEO -> generateVideo(text, settings)
        }
    }

    private fun handleResult(result: AIResult<String>, isVideo: Boolean) {
        when (result) {
            is AIResult.Success -> {
                val msg = Message(
                    content = if (isVideo) "Here's your video! ??" else "Here's your image! ??",
                    type = if (isVideo) MessageType.AI_VIDEO else MessageType.AI_IMAGE,
                    mediaUrl = result.data, sessionId = _state.value.sessionId
                )
                addMsg(msg)
                viewModelScope.launch { repo.saveMessage(msg) }
            }
            is AIResult.Error -> addMsg(Message(content = "? ${result.message}",
                type = MessageType.ERROR, sessionId = _state.value.sessionId))
            else -> {}
        }
    }

    fun clearChat() = viewModelScope.launch {
        repo.clearSession(_state.value.sessionId)
        _state.value = ChatUiState(sessionId = UUID.randomUUID().toString())
        chatHistory.clear()
        chatHistory.add(ChatMessage("system",
            "You are an AI creative assistant for image and video generation."))
    }

    private fun addMsg(msg: Message) = _state.update { it.copy(messages = it.messages + msg) }
    private fun removeMsg(id: String) =
        _state.update { it.copy(messages = it.messages.filter { m -> m.id != id }) }
}
