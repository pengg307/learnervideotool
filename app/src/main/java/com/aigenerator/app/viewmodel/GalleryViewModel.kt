package com.aigenerator.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigenerator.app.model.Message
import com.aigenerator.app.repository.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: AIRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<Message>>(emptyList())
    val items = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getAllGenerated()
                _items.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(id)
                load()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun combineVideos(selectedItems: List<Message>, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.combineVideosToGallery(selectedItems)
                when (result) {
                    is AIRepository.AIResult.Success -> {
                        callback(true, "Videos combined successfully!")
                        load()
                    }
                    is AIRepository.AIResult.Error -> {
                        callback(false, result.message)
                    }
                    is AIRepository.AIResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                callback(false, "Error: ${e.message}")
            }
        }
    }

    fun saveMediaToGallery(message: Message, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // If already saved, just return success
                if (message.isSaved) {
                    callback(true, "Already saved")
                    return@launch
                }

                // If it has a media URL, try to save it
                val mediaUrl = message.mediaUrl
                if (mediaUrl.isNullOrBlank()) {
                    callback(false, "No media URL found")
                    return@launch
                }

                // Use repository to save based on type
                val result = when (message.type) {
                    com.aigenerator.app.model.MessageType.AI_IMAGE -> {
                        // Try to download and save image
                        try {
                            // We need to re-download and save
                            val savedPath = repository.downloadAndSaveImage(mediaUrl, message.content)
                            if (savedPath != null) {
                                // Update the message with local path
                                val updatedMessage = message.copy(
                                    localMediaPath = savedPath,
                                    isSaved = true
                                )
                                repository.saveMessage(updatedMessage)
                                AIRepository.AIResult.Success(savedPath)
                            } else {
                                AIRepository.AIResult.Error("Failed to save image")
                            }
                        } catch (e: Exception) {
                            AIRepository.AIResult.Error("Error: ${e.message}")
                        }
                    }
                    com.aigenerator.app.model.MessageType.AI_VIDEO -> {
                        try {
                            val savedPath = repository.downloadAndSaveVideo(mediaUrl, message.content)
                            if (savedPath != null) {
                                val updatedMessage = message.copy(
                                    localMediaPath = savedPath,
                                    isSaved = true
                                )
                                repository.saveMessage(updatedMessage)
                                AIRepository.AIResult.Success(savedPath)
                            } else {
                                AIRepository.AIResult.Error("Failed to save video")
                            }
                        } catch (e: Exception) {
                            AIRepository.AIResult.Error("Error: ${e.message}")
                        }
                    }
                    else -> {
                        AIRepository.AIResult.Error("Unsupported media type")
                    }
                }

                when (result) {
                    is AIRepository.AIResult.Success -> {
                        callback(true, "Saved successfully!")
                        load()
                    }
                    is AIRepository.AIResult.Error -> {
                        callback(false, result.message)
                    }
                    else -> {
                        callback(false, "Unknown error")
                    }
                }
            } catch (e: Exception) {
                callback(false, "Error: ${e.message}")
            }
        }
    }
}