package com.aigenerator.app.ui.screens

import com.aigenerator.app.model.GenerationMode
import java.util.Locale

data class ChatScreenStrings(
    val chatTitle: String,
    val chatSubtitle: String,
    val modeImage: String,
    val modeVideo: String,
    val modeImg2Img: String,
    val modeImg2Video: String,
    val referenceImage: String,
    val referenceImageDesc: String,
    val removeImage: String,
    val welcomeTitle: String,
    val welcomeDesc: String,
    val example1: String,
    val example2: String,
    val example3: String,
    val inputPlaceholder: String,
    val inputPlaceholderListening: String,
    val clearText: String,
    val voiceInput: String,
    val send: String,
    val save: String,
    val saving: String,
    val share: String,
    val playVideo: String,
    val imageSaved: String,
    val saveFailed: String,
    val videoReady: String
)

fun getChatScreenStrings(mode: GenerationMode): ChatScreenStrings {
    val isChinese = Locale.getDefault().language == "zh"
    
    val subtitle = when (mode) {
        GenerationMode.IMAGE -> if (isChinese) "文生图" else "Text to Image"
        GenerationMode.VIDEO -> if (isChinese) "文生视频" else "Text to Video"
        GenerationMode.IMAGE_TO_IMAGE -> if (isChinese) "图生图" else "Image to Image"
        GenerationMode.IMAGE_TO_VIDEO -> if (isChinese) "图生视频" else "Image to Video"
    }
    
    val welcomeDesc = when (mode) {
        GenerationMode.IMAGE -> if (isChinese) "输入或说出你想要的图像，然后发送" else "Type or speak what image you want, then press Send"
        GenerationMode.VIDEO -> if (isChinese) "描述视频场景，我来为你创建" else "Describe a video scene and I will create it for you"
        GenerationMode.IMAGE_TO_IMAGE -> if (isChinese) "上传图像并描述如何变换" else "Upload an image and describe how to transform it"
        GenerationMode.IMAGE_TO_VIDEO -> if (isChinese) "上传图像并将其动画化为视频" else "Upload an image to animate it into a video"
    }
    
    return ChatScreenStrings(
        chatTitle = if (isChinese) "AI 生成器" else "AI Generator",
        chatSubtitle = subtitle,
        modeImage = if (isChinese) "图像" else "Image",
        modeVideo = if (isChinese) "视频" else "Video",
        modeImg2Img = if (isChinese) "图转图" else "Img to Img",
        modeImg2Video = if (isChinese) "图转视频" else "Img to Video",
        referenceImage = if (isChinese) "参考图像" else "Reference Image",
        referenceImageDesc = if (isChinese) "将用于生成" else "Will be used for generation",
        removeImage = if (isChinese) "移除图像" else "Remove Image",
        welcomeTitle = if (isChinese) "AI 生成器" else "AI Generator",
        welcomeDesc = welcomeDesc,
        example1 = if (isChinese) "夜晚霓虹城市上空飞行的龙" else "A dragon flying over a neon city at night",
        example2 = if (isChinese) "火星上穿宇航服的可爱猫" else "Cute cat in astronaut suit on Mars",
        example3 = if (isChinese) "发光的鱼游动的水下宫殿" else "Underwater palace with glowing fish",
        inputPlaceholder = if (isChinese) "描述你想要生成的内容…" else "Describe what you want to generate...",
        inputPlaceholderListening = if (isChinese) "正在聆听…" else "Listening...",
        clearText = if (isChinese) "清除" else "Clear",
        voiceInput = if (isChinese) "语音输入" else "Voice input",
        send = if (isChinese) "发送" else "Send",
        save = if (isChinese) "保存" else "Save",
        saving = if (isChinese) "保存中…" else "Saving...",
        share = if (isChinese) "分享" else "Share",
        playVideo = if (isChinese) "播放视频" else "Play Video",
        imageSaved = if (isChinese) "图像已保存到相册！" else "Image saved to gallery!",
        saveFailed = if (isChinese) "保存失败" else "Save failed",
        videoReady = if (isChinese) "您的视频已准备就绪！" else "Your video is ready!"
    )
}