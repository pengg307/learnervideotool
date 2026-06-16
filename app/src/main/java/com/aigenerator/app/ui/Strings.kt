package com.aigenerator.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aigenerator.app.R
import com.aigenerator.app.model.GenerationMode

/**
 * Centralized string resources to avoid compiler issues with stringResource()
 */
object AppStrings {
    
    // Chat Screen
    @Composable
    fun chatTitle() = stringResource(R.string.chat_title)
    
    @Composable
    fun chatSubtitle(mode: GenerationMode): String {
        return when (mode) {
            GenerationMode.IMAGE -> stringResource(R.string.chat_subtitle_image)
            GenerationMode.VIDEO -> stringResource(R.string.chat_subtitle_video)
            GenerationMode.IMAGE_TO_IMAGE -> stringResource(R.string.chat_subtitle_img2img)
            GenerationMode.IMAGE_TO_VIDEO -> stringResource(R.string.chat_subtitle_img2video)
        }
    }
    
    @Composable
    fun modeImage() = stringResource(R.string.mode_image)
    @Composable
    fun modeVideo() = stringResource(R.string.mode_video)
    @Composable
    fun modeImg2Img() = stringResource(R.string.mode_img2img)
    @Composable
    fun modeImg2Video() = stringResource(R.string.mode_img2video)
    
    @Composable
    fun referenceImage() = stringResource(R.string.reference_image)
    @Composable
    fun referenceImageDesc() = stringResource(R.string.reference_image_desc)
    @Composable
    fun removeImage() = stringResource(R.string.remove_image)
    
    @Composable
    fun welcomeTitle() = stringResource(R.string.welcome_title)
    
    @Composable
    fun welcomeDesc(mode: GenerationMode): String {
        return when (mode) {
            GenerationMode.IMAGE -> stringResource(R.string.welcome_image_desc)
            GenerationMode.VIDEO -> stringResource(R.string.welcome_video_desc)
            GenerationMode.IMAGE_TO_IMAGE -> stringResource(R.string.welcome_img2img_desc)
            GenerationMode.IMAGE_TO_VIDEO -> stringResource(R.string.welcome_img2video_desc)
        }
    }
    
    @Composable
    fun example1() = stringResource(R.string.example_1)
    @Composable
    fun example2() = stringResource(R.string.example_2)
    @Composable
    fun example3() = stringResource(R.string.example_3)
    
    @Composable
    fun inputPlaceholder() = stringResource(R.string.input_placeholder)
    @Composable
    fun inputPlaceholderListening() = stringResource(R.string.input_placeholder_listening)
    
    @Composable
    fun clearText() = stringResource(R.string.clear_text)
    @Composable
    fun voiceInput() = stringResource(R.string.voice_input)
    @Composable
    fun send() = stringResource(R.string.send)
    
    @Composable
    fun save() = stringResource(R.string.save)
    @Composable
    fun saving() = stringResource(R.string.saving)
    @Composable
    fun share() = stringResource(R.string.share)
    @Composable
    fun playVideo() = stringResource(R.string.play_video)
    
    @Composable
    fun imageSaved() = stringResource(R.string.image_saved)
    @Composable
    fun saveFailed() = stringResource(R.string.save_failed)
    @Composable
    fun videoReady() = stringResource(R.string.video_ready)
    
    // Settings Screen
    @Composable
    fun settingsTitle() = stringResource(R.string.settings_title)
    @Composable
    fun settingsAiProvider() = stringResource(R.string.settings_ai_provider)
    @Composable
    fun providerOpenAI() = stringResource(R.string.provider_openai)
    @Composable
    fun providerStability() = stringResource(R.string.provider_stability)
    @Composable
    fun providerReplicate() = stringResource(R.string.provider_replicate)
    @Composable
    fun providerAgnes() = stringResource(R.string.provider_agnes)
    
    @Composable
    fun settingsAgnes() = stringResource(R.string.settings_agnes)
    @Composable
    fun agnesApiKey() = stringResource(R.string.agnes_api_key)
    @Composable
    fun agnesImageModel() = stringResource(R.string.agnes_image_model)
    @Composable
    fun agnesVideoModel() = stringResource(R.string.agnes_video_model)
    @Composable
    fun agnesNote() = stringResource(R.string.agnes_note)
    
    @Composable
    fun settingsApiKeys() = stringResource(R.string.settings_api_keys)
    @Composable
    fun openAiKey() = stringResource(R.string.openai_key)
    @Composable
    fun stabilityKey() = stringResource(R.string.stability_key)
    @Composable
    fun replicateKey() = stringResource(R.string.replicate_key)
    
    @Composable
    fun settingsImage() = stringResource(R.string.settings_image)
    @Composable
    fun resolution() = stringResource(R.string.resolution)
    @Composable
    fun steps() = stringResource(R.string.steps)
    @Composable
    fun cfgScale() = stringResource(R.string.cfg_scale)
    @Composable
    fun autoSave() = stringResource(R.string.auto_save)
    
    @Composable
    fun settingsVideo() = stringResource(R.string.settings_video)
    @Composable
    fun videoWidth() = stringResource(R.string.video_width)
    @Composable
    fun videoHeight() = stringResource(R.string.video_height)
    @Composable
    fun videoFrames() = stringResource(R.string.video_frames)
    @Composable
    fun videoFps() = stringResource(R.string.video_fps)
    
    @Composable
    fun settingsReplicate() = stringResource(R.string.settings_replicate)
    @Composable
    fun replicateImageVer() = stringResource(R.string.replicate_image_ver)
    @Composable
    fun replicateVideoVer() = stringResource(R.string.replicate_video_ver)
    
    @Composable
    fun saveSettings() = stringResource(R.string.save_settings)
    @Composable
    fun settingsSaved() = stringResource(R.string.settings_saved)
    
    @Composable
    fun settingsAbout() = stringResource(R.string.settings_about)
    @Composable
    fun version() = stringResource(R.string.version)
    @Composable
    fun models() = stringResource(R.string.models)
    @Composable
    fun providers() = stringResource(R.string.providers)
    
    @Composable
    fun hide() = stringResource(R.string.hide)
    @Composable
    fun show() = stringResource(R.string.show)
    
    @Composable
    fun error() = stringResource(R.string.error)
    @Composable
    fun retry() = stringResource(R.string.retry)
}