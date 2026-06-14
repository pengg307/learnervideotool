package com.aigenerator.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigenerator.app.model.Message
import com.aigenerator.app.repository.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(private val repo: AIRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<Message>>(emptyList())
    val items = _items.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch { _items.value = repo.getAllGenerated() }
    fun delete(id: String) = viewModelScope.launch { repo.deleteMessage(id); load() }
}
