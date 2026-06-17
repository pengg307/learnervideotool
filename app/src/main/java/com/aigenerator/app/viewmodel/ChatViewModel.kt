package com.aigenerator.app.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aigenerator.app.database.MessageDao
import com.aigenerator.app.model.AIProvider
import com.aigenerator.app.model.ChatMessage
import com.aigenerator.app.model.GenerationMode
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import com.aigenerator.app.repository.AIRepository
import com.aigenerator.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ChatViewModel"

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val repository: AIRepository,
    private val settingsRepo: SettingsRepository,
    private val dao: MessageDao,
    @ApplicationContext private val context: Context
) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _sessionId = MutableStateFlow(UUID.randomUUID().toString())
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    init {
        viewModelScope.launch {
            _messages.value = dao.getAllMessages(_sessionId.value).sortedBy { it.timestamp }
        }
    }

    fun sendMessage(text: String, mode: GenerationMode = GenerationMode.IMAGE) {
        if (text.isBlank()) return

        // Add user message
        val userMsg = Message(
            content = text,
            type = MessageType.USER_TEXT,
            sessionId = _sessionId.value
        )
        addMessage(userMsg)

        // Show loading message
        val loadingMsg = Message(
            content = "Generating...",
            type = MessageType.LOADING,
            sessionId = _sessionId.value,
            isLoading = true,
            generationMode = mode
        )
        addMessage(loadingMsg)

        viewModelScope.launch {
            val result = when (mode) {
                GenerationMode.IMAGE -> {
                    val settings = settingsRepo.settingsFlow.first()
                    when (settings.selectedProvider) {
                        "OPENAI" -> repository.generateImageOpenAI(text)
                        "AGNES" -> repository.generateImageAgnes(text)
                        else -> repository.generateImageOpenAI(text)
                    }
                }
                GenerationMode.VIDEO -> {
                    repository.generateVideoAgnes(text)
                }
                else -> repository.generateImageOpenAI(text)
            }

            // Remove loading message
            val loadingIdx = _messages.value.indexOfFirst { it.isLoading }
            if (loadingIdx >= 0) {
                val newMessages = _messages.value.toMutableList()
                newMessages.removeAt(loadingIdx)
                _messages.value = newMessages
            }

            when (result) {
                is AIResult.Success -> {
                    val aiMsg = Message(
                        content = text,
                        type = when (mode) {
                            GenerationMode.IMAGE -> MessageType.AI_IMAGE
                            GenerationMode.VIDEO -> MessageType.AI_VIDEO
                            else -> MessageType.AI_TEXT
                        },
                        mediaUrl = result.data,
                        sessionId = _sessionId.value,
                        generationMode = mode
                    )
                    addMessage(aiMsg)
                }
                is AIResult.Error -> {
                    val errorMsg = Message(
                        content = "Error: ${result.message}",
                        type = MessageType.ERROR,
                        sessionId = _sessionId.value
                    )
                    addMessage(errorMsg)
                }
                is AIResult.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun addMessage(msg: Message) {
        viewModelScope.launch {
            dao.insert(msg)
            _messages.value = (_messages.value + msg).sortedBy { it.timestamp }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearSession(_sessionId.value)
            _messages.value = emptyList()
        }
    }

    fun getBitmapFromUrl(url: String): Bitmap? {
        return try {
            val urlObj = java.net.URL(url)
            val inputStream = urlObj.openConnection().getInputStream()
            android.graphics.BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URL: ${e.message}")
            null
        }
    }

    fun saveImageToLocal(url: String): String? {
        return try {
            val urlObj = java.net.URL(url)
            val inputStream = urlObj.openConnection().getInputStream()
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            val fileName = "${System.currentTimeMillis()}.png"
            val file = File(context.filesDir, fileName)
            val outputStream = file.outputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image: ${e.message}")
            null
        }
    }

    fun saveToGallery(imagePath: String) {
        try {
            val file = File(imagePath)
            val dest = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), file.name)
            file.copyTo(dest, overwrite = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to gallery: ${e.message}")
        }
    }
}
