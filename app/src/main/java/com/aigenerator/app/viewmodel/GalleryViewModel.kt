package com.aigenerator.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import com.aigenerator.app.repository.AIRepository
import com.aigenerator.app.repository.AIResult  // ✅ Add this import
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
                    is AIResult.Success -> {
                        callback(true, "Videos combined successfully!")
                        load()
                    }
                    is AIResult.Error -> {
                        callback(false, result.message)
                    }
                    is AIResult.Loading -> {
                        // Handle loading state if needed
                        callback(false, "Processing...")
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
                    MessageType.AI_IMAGE -> {
                        try {
                            // Try to download and save image
                            val savedPath = repository.downloadAndSaveImage(mediaUrl, message.content)
                            if (savedPath != null) {
                                // Update the message with local path
                                val updatedMessage = message.copy(
                                    localMediaPath = savedPath,
                                    isSaved = true
                                )
                                repository.saveMessage(updatedMessage)
                                AIResult.Success(savedPath)
                            } else {
                                AIResult.Error("Failed to save image")
                            }
                        } catch (e: Exception) {
                            AIResult.Error("Error: ${e.message}")
                        }
                    }
                    MessageType.AI_VIDEO -> {
                        try {
                            val savedPath = repository.downloadAndSaveVideo(mediaUrl, message.content)
                            if (savedPath != null) {
                                val updatedMessage = message.copy(
                                    localMediaPath = savedPath,
                                    isSaved = true
                                )
                                repository.saveMessage(updatedMessage)
                                AIResult.Success(savedPath)
                            } else {
                                AIResult.Error("Failed to save video")
                            }
                        } catch (e: Exception) {
                            AIResult.Error("Error: ${e.message}")
                        }
                    }
                    else -> {
                        AIResult.Error("Unsupported media type")
                    }
                }

                when (result) {
                    is AIResult.Success -> {
                        callback(true, "Saved successfully!")
                        load()
                    }
                    is AIResult.Error -> {
                        callback(false, result.message)
                    }
                    is AIResult.Loading -> {
                        callback(false, "Loading...")
                    }
                }
            } catch (e: Exception) {
                callback(false, "Error: ${e.message}")
            }
        }
    }
}