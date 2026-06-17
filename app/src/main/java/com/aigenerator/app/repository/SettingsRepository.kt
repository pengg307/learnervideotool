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
        val AGNES_KEY     = stringPreferencesKey("agnes_key")
        val AGNES_IMG_MODEL = stringPreferencesKey("agnes_img_model")
        val AGNES_VID_MODEL = stringPreferencesKey("agnes_vid_model")
        val IMG_MODEL     = stringPreferencesKey("image_model")
        val IMG_W         = intPreferencesKey("img_width")
        val IMG_H         = intPreferencesKey("img_height")
        val NEG           = booleanPreferencesKey("neg_prompt")
        val SAVE          = booleanPreferencesKey("save_gallery")
        val PROVIDER      = stringPreferencesKey("provider")
        // Video parameters
        val VIDEO_WIDTH      = intPreferencesKey("video_width")
        val VIDEO_HEIGHT     = intPreferencesKey("video_height")
        val VIDEO_NUM_FRAMES = intPreferencesKey("video_num_frames")
        val VIDEO_FRAME_RATE = intPreferencesKey("video_frame_rate")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            openAiApiKey          = prefs[Keys.OPENAI_KEY]  ?: "",
            agnesApiKey           = prefs[Keys.AGNES_KEY]   ?: "",
            agnesImageModel       = prefs[Keys.AGNES_IMG_MODEL] ?: "agnes-image-2.0-flash",
            agnesVideoModel       = prefs[Keys.AGNES_VID_MODEL] ?: "agnes-video-v2.0",
            defaultImageModel     = prefs[Keys.IMG_MODEL]   ?: "dall-e-3",
            defaultImageWidth     = prefs[Keys.IMG_W]       ?: 1024,
            defaultImageHeight    = prefs[Keys.IMG_H]       ?: 1024,
            enableNegativePrompt  = prefs[Keys.NEG]         ?: true,
            saveToGallery         = prefs[Keys.SAVE]        ?: true,
            selectedProvider      = try {
                AIProvider.valueOf(prefs[Keys.PROVIDER] ?: "AGNES")
            } catch (e: Exception) {
                AIProvider.AGNES
            },
            videoWidth            = prefs[Keys.VIDEO_WIDTH] ?: 1152,
            videoHeight           = prefs[Keys.VIDEO_HEIGHT] ?: 768,
            videoNumFrames        = prefs[Keys.VIDEO_NUM_FRAMES] ?: 121,
            videoFrameRate        = prefs[Keys.VIDEO_FRAME_RATE] ?: 24
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPENAI_KEY]    = settings.openAiApiKey
            prefs[Keys.AGNES_KEY]     = settings.agnesApiKey
            prefs[Keys.AGNES_IMG_MODEL] = settings.agnesImageModel
            prefs[Keys.AGNES_VID_MODEL] = settings.agnesVideoModel
            prefs[Keys.IMG_MODEL]     = settings.defaultImageModel
            prefs[Keys.IMG_W]         = settings.defaultImageWidth
            prefs[Keys.IMG_H]         = settings.defaultImageHeight
            prefs[Keys.NEG]           = settings.enableNegativePrompt
            prefs[Keys.SAVE]          = settings.saveToGallery
            prefs[Keys.PROVIDER]      = settings.selectedProvider.name
            // Video parameters
            prefs[Keys.VIDEO_WIDTH]      = settings.videoWidth
            prefs[Keys.VIDEO_HEIGHT]     = settings.videoHeight
            prefs[Keys.VIDEO_NUM_FRAMES] = settings.videoNumFrames
            prefs[Keys.VIDEO_FRAME_RATE] = settings.videoFrameRate
        }
    }
}