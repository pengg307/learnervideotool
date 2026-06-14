package com.aigenerator.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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
    private object K {
        val OPENAI_KEY  = stringPreferencesKey("openai_key")
        val STAB_KEY    = stringPreferencesKey("stability_key")
        val REP_KEY     = stringPreferencesKey("replicate_key")
        val IMG_MODEL   = stringPreferencesKey("image_model")
        val VID_MODEL   = stringPreferencesKey("video_model")
        val IMG_W       = intPreferencesKey("img_width")
        val IMG_H       = intPreferencesKey("img_height")
        val STEPS       = intPreferencesKey("steps")
        val CFG         = floatPreferencesKey("cfg_scale")
        val NEG         = booleanPreferencesKey("neg_prompt")
        val SAVE        = booleanPreferencesKey("save_gallery")
        val PROVIDER    = stringPreferencesKey("provider")
        val REP_IMG_VER = stringPreferencesKey("rep_img_ver")
        val REP_VID_VER = stringPreferencesKey("rep_vid_ver")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            openAiApiKey          = p[K.OPENAI_KEY]  ?: "",
            stabilityApiKey       = p[K.STAB_KEY]    ?: "",
            replicateApiKey       = p[K.REP_KEY]     ?: "",
            defaultImageModel     = p[K.IMG_MODEL]   ?: "dall-e-3",
            defaultVideoModel     = p[K.VID_MODEL]   ?: "stable-video-diffusion",
            defaultImageWidth     = p[K.IMG_W]       ?: 1024,
            defaultImageHeight    = p[K.IMG_H]       ?: 1024,
            defaultSteps          = p[K.STEPS]       ?: 30,
            defaultCfgScale       = p[K.CFG]         ?: 7.0f,
            enableNegativePrompt  = p[K.NEG]         ?: true,
            saveToGallery         = p[K.SAVE]        ?: true,
            selectedProvider      = try {
                AIProvider.valueOf(p[K.PROVIDER] ?: "OPENAI")
            } catch (e: Exception) { AIProvider.OPENAI },
            replicateImageVersion = p[K.REP_IMG_VER]
                ?: "stability-ai/sdxl:39ed52f2319f9637e7e26c44e294bba72df75aae2bec46e7cb50be2f5b3aecf9",
            replicateVideoVersion = p[K.REP_VID_VER]
                ?: "stability-ai/stable-video-diffusion:3f0457e4619daac51203dedb472816fd4af51f3149fa7a9e0b5ffcf1b8172438"
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun save(s: AppSettings) {
        context.dataStore.edit { p ->
            p[K.OPENAI_KEY]  = s.openAiApiKey
            p[K.STAB_KEY]    = s.stabilityApiKey
            p[K.REP_KEY]     = s.replicateApiKey
            p[K.IMG_MODEL]   = s.defaultImageModel
            p[K.VID_MODEL]   = s.defaultVideoModel
            p[K.IMG_W]       = s.defaultImageWidth
            p[K.IMG_H]       = s.defaultImageHeight
            p[K.STEPS]       = s.defaultSteps
            p[K.CFG]         = s.defaultCfgScale
            p[K.NEG]         = s.enableNegativePrompt
            p[K.SAVE]        = s.saveToGallery
            p[K.PROVIDER]    = s.selectedProvider.name
            p[K.REP_IMG_VER] = s.replicateImageVersion
            p[K.REP_VID_VER] = s.replicateVideoVersion
        }
    }
}
