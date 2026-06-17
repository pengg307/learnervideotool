package com.aigenerator.app.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val openAiApiKey: String = "",
    val agnesApiKey: String = "",
    val agnesImageModel: String = "agnes-image-2.0-flash",
    val agnesVideoModel: String = "agnes-video-v2.0",
    val defaultImageModel: String = "dall-e-3",
    val defaultImageWidth: Int = 1024,
    val defaultImageHeight: Int = 1024,
    val selectedProvider: String = "OPENAI",
    val saveToGallery: Boolean = true
) {
    companion object {
        fun fromPrefs(prefs: Preferences): AppSettings {
            val openAiKey = prefs[stringPreferencesKey("openai_api_key")] ?: ""
            val agnesKey = prefs[stringPreferencesKey("agnes_api_key")] ?: ""
            val agnesImgModel = prefs[stringPreferencesKey("agnes_image_model")] ?: "agnes-image-2.0-flash"
            val agnesVidModel = prefs[stringPreferencesKey("agnes_video_model")] ?: "agnes-video-v2.0"
            val defaultModel = prefs[stringPreferencesKey("default_image_model")] ?: "dall-e-3"
            val width = prefs[stringPreferencesKey("default_image_width")]?.toIntOrNull() ?: 1024
            val height = prefs[stringPreferencesKey("default_image_height")]?.toIntOrNull() ?: 1024
            val provider = prefs[stringPreferencesKey("selected_provider")] ?: "OPENAI"
            val saveGallery = prefs[booleanPreferencesKey("save_to_gallery")] ?: true
            return AppSettings(openAiKey, agnesKey, agnesImgModel, agnesVidModel, defaultModel, width, height, provider, saveGallery)
        }
    }
}

class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings.fromPrefs(prefs)
    }

    suspend fun save(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("openai_api_key")] = settings.openAiApiKey
            prefs[stringPreferencesKey("agnes_api_key")] = settings.agnesApiKey
            prefs[stringPreferencesKey("agnes_image_model")] = settings.agnesImageModel
            prefs[stringPreferencesKey("agnes_video_model")] = settings.agnesVideoModel
            prefs[stringPreferencesKey("default_image_model")] = settings.defaultImageModel
            prefs[stringPreferencesKey("default_image_width")] = settings.defaultImageWidth.toString()
            prefs[stringPreferencesKey("default_image_height")] = settings.defaultImageHeight.toString()
            prefs[stringPreferencesKey("selected_provider")] = settings.selectedProvider
            prefs[booleanPreferencesKey("save_to_gallery")] = settings.saveToGallery
        }
    }
}
