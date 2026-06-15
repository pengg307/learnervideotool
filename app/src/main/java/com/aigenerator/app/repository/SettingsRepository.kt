package com.aigenerator.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aigenerator.app.model.AIProvider
import com.aigenerator.app.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "ai_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val OPENAI_KEY    = stringPreferencesKey("openai_key")
        val STAB_KEY      = stringPreferencesKey("stability_key")
        val REP_KEY       = stringPreferencesKey("replicate_key")
        val IMG_MODEL     = stringPreferencesKey("image_model")
        val VID_MODEL     = stringPreferencesKey("video_model")
        val IMG_W         = intPreferencesKey("img_width")
        val IMG_H         = intPreferencesKey("img_height")
        val STEPS         = intPreferencesKey("steps")
        val CFG           = floatPreferencesKey("cfg_scale")
        val NEG           = booleanPreferencesKey("neg_prompt")
        val SAVE          = booleanPreferencesKey("save_gallery")
        val PROVIDER      = stringPreferencesKey("provider")
        val REP_IMG_VER   = stringPreferencesKey("rep_img_ver")
        val REP_VID_VER   = stringPreferencesKey("rep_vid_ver")
        
        // NEW: Custom endpoint keys
        val CUSTOM_ENDPOINT_URL = stringPreferencesKey("custom_endpoint_url")
        val CUSTOM_API_KEY      = stringPreferencesKey("custom_api_key")
        val CUSTOM_MODEL_NAME   = stringPreferencesKey("custom_model_name")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            openAiApiKey          = prefs[Keys.OPENAI_KEY]  ?: "",
            stabilityApiKey       = prefs[Keys.STAB_KEY]    ?: "",
            replicateApiKey       = prefs[Keys.REP_KEY]     ?: "",
            defaultImageModel     = prefs[Keys.IMG_MODEL]   ?: "dall-e-3",
            defaultVideoModel     = prefs[Keys.VID_MODEL]   ?: "stable-video-diffusion",
            defaultImageWidth     = prefs[Keys.IMG_W]       ?: 1024,
            defaultImageHeight    = prefs[Keys.IMG_H]       ?: 1024,
            defaultSteps          = prefs[Keys.STEPS]       ?: 30,
            defaultCfgScale       = prefs[Keys.CFG]         ?: 7.0f,
            enableNegativePrompt  = prefs[Keys.NEG]         ?: true,
            saveToGallery         = prefs[Keys.SAVE]        ?: true,
            selectedProvider      = try {
                AIProvider.valueOf(prefs[Keys.PROVIDER] ?: "OPENAI")
            } catch (e: Exception) {
                AIProvider.OPENAI
            },
            replicateImageVersion = prefs[Keys.REP_IMG_VER]
                ?: "stability-ai/sdxl:39ed52f2319f9637e7e26c44e294bba72df75aae2bec46e7cb50be2f5b3aecf9",
            replicateVideoVersion = prefs[Keys.REP_VID_VER]
                ?: "stability-ai/stable-video-diffusion:3f0457e4619daac51203dedb472816fd4af51f3149fa7a9e0b5ffcf1b8172438",
            
            // NEW: Load custom endpoint settings
            customEndpointUrl     = prefs[Keys.CUSTOM_ENDPOINT_URL] ?: "",
            customApiKey          = prefs[Keys.CUSTOM_API_KEY] ?: "",
            customModelName       = prefs[Keys.CUSTOM_MODEL_NAME] ?: ""
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPENAI_KEY]  = settings.openAiApiKey
            prefs[Keys.STAB_KEY]    = settings.stabilityApiKey
            prefs[Keys.REP_KEY]     = settings.replicateApiKey
            prefs[Keys.IMG_MODEL]   = settings.defaultImageModel
            prefs[Keys.VID_MODEL]   = settings.defaultVideoModel
            prefs[Keys.IMG_W]       = settings.defaultImageWidth
            prefs[Keys.IMG_H]       = settings.defaultImageHeight
            prefs[Keys.STEPS]       = settings.defaultSteps
            prefs[Keys.CFG]         = settings.defaultCfgScale
            prefs[Keys.NEG]         = settings.enableNegativePrompt
            prefs[Keys.SAVE]        = settings.saveToGallery
            prefs[Keys.PROVIDER]    = settings.selectedProvider.name
            prefs[Keys.REP_IMG_VER] = settings.replicateImageVersion
            prefs[Keys.REP_VID_VER] = settings.replicateVideoVersion
            
            // NEW: Save custom endpoint settings
            prefs[Keys.CUSTOM_ENDPOINT_URL] = settings.customEndpointUrl
            prefs[Keys.CUSTOM_API_KEY]      = settings.customApiKey
            prefs[Keys.CUSTOM_MODEL_NAME]   = settings.customModelName
        }
    }
}