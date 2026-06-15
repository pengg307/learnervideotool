package com.aigenerator.app.ui.screens

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aigenerator.app.model.GenerationMode
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import com.aigenerator.app.viewmodel.ChatViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    paddingValues: PaddingValues,
    vm: ChatViewModel = hiltViewModel()
) {
    val context   = LocalContext.current
    val state     by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showModes   by remember { mutableStateOf(false) }

    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { bmp ->
                    vm.setUploadedImage(bmp, it.toString())
                }
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.let { vm.sendMessage(it, isVoice = true) }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("AI Generator", fontWeight = FontWeight.Bold)
                    Text(
                        text = when (state.currentMode) {
                            GenerationMode.IMAGE          -> "Text to Image"
                            GenerationMode.VIDEO          -> "Text to Video"
                            GenerationMode.IMAGE_TO_IMAGE -> "Image to Image"
                            GenerationMode.IMAGE_TO_VIDEO -> "Image to Video"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = {
                IconButton(onClick = { showModes = !showModes }) {
                    Icon(Icons.Default.Tune, contentDescription = "Mode")
                }
                IconButton(onClick = { vm.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        AnimatedVisibility(visible = showModes) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        GenerationMode.IMAGE          to "Image",
                        GenerationMode.VIDEO          to "Video",
                        GenerationMode.IMAGE_TO_IMAGE to "Img to Img",
                        GenerationMode.IMAGE_TO_VIDEO to "Img to Video"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = state.currentMode == mode,
                            onClick  = { vm.setMode(mode); showModes = false },
                            label    = { Text(label) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.uploadedImageUri != null) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model              = state.uploadedImageUri,
                        contentDescription = null,
                        modifier           = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale       = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reference Image",
                            style = MaterialTheme.typography.labelMedium)
                        Text("Will be used for generation",
                            style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { vm.removeUploadedImage() }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }
        }

        LazyColumn(
            state               = listState,
            modifier            = Modifier.weight(1f).fillMaxWidth(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty()) {
                item { WelcomeCard(mode = state.currentMode) }
            }
            items(state.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }

        Surface(shadowElevation = 8.dp) {
            Column {
                HorizontalDivider()
                Row(
                    modifier            = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment   = Alignment.Bottom
                ) {
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        modifier      = Modifier.weight(1f).padding(horizontal = 4.dp),
                        placeholder   = {
                            Text(
                                if (isListening) "Listening..."
                                else "Describe what you want to generate..."
                            )
                        },
                        shape    = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        enabled  = !state.isGenerating && !isListening,
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { inputText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                                if (audioPermission.status.isGranted) {
                                    isListening = true
                                    speechLauncher.launch(
                                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE,
                                                Locale.getDefault()
                                            )
                                            putExtra(
                                                RecognizerIntent.EXTRA_PROMPT,
                                                "Describe what you want to generate..."
                                            )
                                        }
                                    )
                                } else {
                                    audioPermission.launchPermissionRequest()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff
                                          else Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = if (isListening) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank() && !state.isGenerating) {
                                vm.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier       = Modifier.size(48.dp),
                        containerColor = if (!state.isGenerating && inputText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) Color.White
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeCard(mode: GenerationMode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier                = Modifier.padding(24.dp),
            horizontalAlignment     = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector        = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier           = Modifier.size(56.dp),
                tint               = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text       = "AI Generator",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = when (mode) {
                    GenerationMode.IMAGE          ->
                        "Type or speak what image you want, then press Send"
                    GenerationMode.VIDEO          ->
                        "Describe a video scene and I will create it for you"
                    GenerationMode.IMAGE_TO_IMAGE ->
                        "Upload an image and describe how to transform it"
                    GenerationMode.IMAGE_TO_VIDEO ->
                        "Upload an image to animate it into a video"
                },
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            listOf(
                "A dragon flying over a neon city at night",
                "Cute cat in astronaut suit on Mars",
                "Underwater palace with glowing fish"
            ).forEach { example ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape    = RoundedCornerShape(8.dp),
                    color    = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ) {
                    Text(
                        text     = example,
                        modifier = Modifier.padding(10.dp, 6.dp),
                        style    = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isUser = message.type in listOf(
        MessageType.USER_TEXT,
        MessageType.USER_VOICE,
        MessageType.USER_IMAGE
    )

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier        = Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp), Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        when (message.type) {
            MessageType.USER_TEXT, MessageType.USER_VOICE ->
                Card(
                    shape  = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        modifier         = Modifier.padding(12.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.type == MessageType.USER_VOICE) {
                            Icon(Icons.Default.Mic, null,
                                Modifier.size(14.dp), Color.White.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(message.content, color = Color.White,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }

            MessageType.USER_IMAGE ->
                Card(shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        message.localMediaPath?.let { path ->
                            AsyncImage(
                                model              = path,
                                contentDescription = null,
                                modifier           = Modifier.size(180.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale       = ContentScale.Crop
                            )
                        }
                        Text("Reference image", modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }

            MessageType.AI_TEXT ->
                Card(
                    shape  = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(message.content, modifier = Modifier.padding(12.dp, 8.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }

            MessageType.AI_IMAGE ->
                Card(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(message.content, modifier = Modifier.padding(bottom = 4.dp),
                            style = MaterialTheme.typography.labelSmall)
                        message.mediaUrl?.let { url ->
                            AsyncImage(
                                model              = url,
                                contentDescription = "Generated image",
                                modifier           = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale       = ContentScale.FillWidth
                            )
                        }
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            TextButton(onClick = { }) {
                                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                            TextButton(onClick = { }) {
                                Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }
                }

            MessageType.AI_VIDEO ->
                Card(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(message.content, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        message.mediaUrl?.let {
                            Text("Video ready!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { }) {
                                Icon(Icons.Default.PlayCircle, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Play Video")
                            }
                        }
                    }
                }

            MessageType.LOADING ->
                Card(
                    shape  = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp, 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(message.content, style = MaterialTheme.typography.bodySmall)
                    }
                }

            MessageType.ERROR ->
                Card(
                    shape  = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, Modifier.size(16.dp),
                            MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(message.content, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }

            MessageType.SYSTEM ->
                Text(
                    text      = message.content,
                    modifier  = Modifier.fillMaxWidth().padding(4.dp),
                    style     = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier         = Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, Modifier.size(18.dp), Color.White)
            }
        }
    }
}
