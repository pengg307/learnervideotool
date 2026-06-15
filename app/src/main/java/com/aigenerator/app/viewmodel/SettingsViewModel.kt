package com.aigenerator.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigenerator.app.model.AppSettings
import com.aigenerator.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    val settings = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )

    fun save(newSettings: AppSettings) {
        viewModelScope.launch { repo.save(newSettings) }
    }
    
    // Optional: Helper method to save just the custom endpoint settings
    fun saveCustomEndpoint(url: String, apiKey: String, modelName: String) {
        viewModelScope.launch {
            val current = repo.getSettings()
            val updated = current.copy(
                customEndpointUrl = url,
                customApiKey = apiKey,
                customModelName = modelName
            )
            repo.save(updated)
        }
    }
}