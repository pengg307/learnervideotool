package com.aigenerator.app.ui.screens

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aigenerator.app.model.*
import com.aigenerator.app.viewmodel.ChatViewModel
import com.google.accompanist.permissions.*
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(paddingValues: PaddingValues, vm: ChatViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showModes by remember { mutableStateOf(false) }
    val audioPerm = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            ctx.contentResolver.openInputStream(it)?.use { s ->
                BitmapFactory.decodeStream(s)?.let { bmp -> vm.setUploadedImage(bmp, it.toString()) }
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isListening = false
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.let { vm.sendMessage(it, isVoice = true) }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {

        // ── Top Bar ──────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text("AI Generator", fontWeight = FontWeight.Bold)
                    Text(when (state.currentMode) {
                        GenerationMode.IMAGE          -> "Text → Image"
                        GenerationMode.VIDEO          -> "Text → Video"
                        GenerationMode.IMAGE_TO_IMAGE -> "Image → Image"
                        GenerationMode.IMAGE_TO_VIDEO -> "Image → Video"
                    }, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            },
            actions = {
                IconButton(onClick = { showModes = !showModes }) {
                    Icon(Icons.Default.Tune, "Mode")
                }
                IconButton(onClick = { vm.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, "Clear")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)
        )

        // ── Mode Selector ────────────────────────────────────────────────────
        AnimatedVisibility(showModes) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        GenerationMode.IMAGE to "?? Text→Image",
                        GenerationMode.VIDEO to "?? Text→Video",
                        GenerationMode.IMAGE_TO_IMAGE to "?? Img→Img",
                        GenerationMode.IMAGE_TO_VIDEO to "?? Img→Video"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = state.currentMode == mode,
                            onClick = { vm.setMode(mode); showModes = false },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // ── Uploaded Image Preview ────────────────────────────────────────────
        AnimatedVisibility(state.uploadedImageUri != null) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = state.uploadedImageUri, contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Reference Image", style = MaterialTheme.typography.labelMedium)
                        Text("Will be used for generation",
                            style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { vm.removeUploadedImage() }) {
                        Icon(Icons.Default.Close, "Remove")
                    }
                }
            }
        }

        // ── Messages ─────────────────────────────────────────────────────────
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.messages.isEmpty()) item { WelcomeCard(state.currentMode) }
            items(state.messages, key = { it.id }) { msg -> ChatBubble(msg) }
        }

        // ── Input Bar ────────────────────────────────────────────────────────
        Surface(shadowElevation = 8.dp) {
            Column {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.Bottom) {

                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Upload",
                            tint = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        placeholder = {
                            Text(if (isListening) "?? Listening..."
                                 else "Describe what you want to generate...")
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        enabled = !state.isGenerating && !isListening,
                        trailingIcon = {
                            if (input.isNotEmpty())
                                IconButton(onClick = { input = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                        }
                    )

                    // Voice button
                    IconButton(onClick = {
                        if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
                            if (audioPerm.status.isGranted) {
                                isListening = true
                                speechLauncher.launch(
                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT,
                                            "Describe what you want to generate...")
                                    })
                            } else audioPerm.launchPermissionRequest()
                        }
                    }) {
                        Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            "Voice",
                            tint = if (isListening) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary)
                    }

                    // ★ GENERATE button ★
                    FloatingActionButton(
                        onClick = {
                            if (input.isNotBlank() && !state.isGenerating) {
                                vm.sendMessage(input); input = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (!state.isGenerating && input.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (state.isGenerating)
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else
                            Icon(Icons.Default.Send, "Generate",
                                tint = if (input.isNotBlank()) Color.White
                                       else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── Welcome Card ─────────────────────────────────────────────────────────────
@Composable
fun WelcomeCard(mode: GenerationMode) {
    Card(Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("AI Generator", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(when (mode) {
                GenerationMode.IMAGE          -> "Type or speak what image you want, then press ?"
                GenerationMode.VIDEO          -> "Describe a video scene and I'll create it!"
                GenerationMode.IMAGE_TO_IMAGE -> "Upload an image and describe how to transform it"
                GenerationMode.IMAGE_TO_VIDEO -> "Upload an image to animate it into a video"
            }, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text("?? Examples:", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            listOf(
                "A dragon flying over a neon city at night",
                "Cute cat in astronaut suit on Mars",
                "Underwater palace with glowing fish"
            ).forEach { ex ->
                Surface(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {
                    Text(""$ex"", Modifier.padding(10.dp, 6.dp),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ── Chat Bubble ───────────────────────────────────────────────────────────────
@Composable
fun ChatBubble(msg: Message) {
    val isUser = msg.type in listOf(
        MessageType.USER_TEXT, MessageType.USER_VOICE, MessageType.USER_IMAGE)
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) {
            Box(Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp), Color.White)
            }
            Spacer(Modifier.width(6.dp))
        }
        when (msg.type) {
            MessageType.USER_TEXT, MessageType.USER_VOICE ->
                Card(shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary)) {
                    Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (msg.type == MessageType.USER_VOICE) {
                            Icon(Icons.Default.Mic, null, Modifier.size(14.dp),
                                Color.White.copy(alpha = 0.7f))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(msg.content, color = Color.White,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            MessageType.USER_IMAGE ->
                Card(shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        msg.localMediaPath?.let { p ->
                            AsyncImage(p, null,
                                Modifier.size(180.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop)
                        }
                        Text("?? Reference image",
                            Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            MessageType.AI_TEXT ->
                Card(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(msg.content, Modifier.padding(12.dp, 8.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            MessageType.AI_IMAGE ->
                Card(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text(msg.content, Modifier.padding(bottom = 4.dp),
                            style = MaterialTheme.typography.labelSmall)
                        msg.mediaUrl?.let { url ->
                            AsyncImage(model = url, contentDescription = "Generated Image",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.FillWidth)
                        }
                        Row(Modifier.padding(top = 4.dp)) {
                            TextButton(onClick = { }) {
                                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp)); Text("Save")
                            }
                            TextButton(onClick = { }) {
                                Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp)); Text("Share")
                            }
                        }
                    }
                }
            MessageType.AI_VIDEO ->
                Card(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text(msg.content, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        msg.mediaUrl?.let { url ->
                            Text("?? Video ready!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { }) {
                                Icon(Icons.Default.PlayCircle, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp)); Text("Play Video")
                            }
                        }
                    }
                }
            MessageType.LOADING ->
                Card(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(msg.content, style = MaterialTheme.typography.bodySmall)
                    }
                }
            MessageType.ERROR ->
                Card(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, Modifier.size(16.dp),
                            MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(msg.content, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            MessageType.SYSTEM ->
                Text(msg.content, Modifier.fillMaxWidth().padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isUser) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, Modifier.size(18.dp), Color.White)
            }
        }
    }
}
