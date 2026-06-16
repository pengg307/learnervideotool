package com.aigenerator.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigenerator.app.R
import com.aigenerator.app.model.AIProvider
import com.aigenerator.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    vm: SettingsViewModel = hiltViewModel()
) {
    val savedSettings by vm.settings.collectAsState()
    var s             by remember(savedSettings) { mutableStateOf(savedSettings) }
    var showOAI       by remember { mutableStateOf(false) }
    var showStab      by remember { mutableStateOf(false) }
    var showRep       by remember { mutableStateOf(false) }
    var showAgnes     by remember { mutableStateOf(false) }
    var justSaved     by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ============ AI PROVIDER ============
            SectionTitle(stringResource(R.string.settings_ai_provider))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AIProvider.values().forEach { provider ->
                        Row(
                            modifier         = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = s.selectedProvider == provider,
                                onClick  = { s = s.copy(selectedProvider = provider) }
                            )
                            Text(
                                text = when (provider) {
                                    AIProvider.OPENAI       -> stringResource(R.string.provider_openai)
                                    AIProvider.STABILITY_AI -> stringResource(R.string.provider_stability)
                                    AIProvider.REPLICATE    -> stringResource(R.string.provider_replicate)
                                    AIProvider.AGNES        -> stringResource(R.string.provider_agnes)
                                }
                            )
                        }
                    }
                }
            }

            // ============ AGNES SETTINGS ============
            if (s.selectedProvider == AIProvider.AGNES) {
                SectionTitle(stringResource(R.string.settings_agnes))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ApiKeyField(
                            label = stringResource(R.string.agnes_api_key),
                            value = s.agnesApiKey,
                            onValueChange = { s = s.copy(agnesApiKey = it) },
                            visible = showAgnes,
                            onToggleVisibility = { showAgnes = !showAgnes }
                        )
                        
                        OutlinedTextField(
                            value         = s.agnesImageModel,
                            onValueChange = { s = s.copy(agnesImageModel = it) },
                            label         = { Text(stringResource(R.string.agnes_image_model)) },
                            placeholder   = { Text("agnes-image-2.0-flash") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                        
                        OutlinedTextField(
                            value         = s.agnesVideoModel,
                            onValueChange = { s = s.copy(agnesVideoModel = it) },
                            label         = { Text(stringResource(R.string.agnes_video_model)) },
                            placeholder   = { Text("agnes-video-v2.0") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                        
                        Text(
                            text = stringResource(R.string.agnes_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ============ VIDEO PARAMETERS ============
                SectionTitle(stringResource(R.string.settings_video))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = s.videoWidth.toString(),
                            onValueChange = { 
                                s = s.copy(videoWidth = it.toIntOrNull() ?: 1152) 
                            },
                            label = { Text(stringResource(R.string.video_width)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = s.videoHeight.toString(),
                            onValueChange = { 
                                s = s.copy(videoHeight = it.toIntOrNull() ?: 768) 
                            },
                            label = { Text(stringResource(R.string.video_height)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = s.videoNumFrames.toString(),
                            onValueChange = { 
                                s = s.copy(videoNumFrames = it.toIntOrNull() ?: 121) 
                            },
                            label = { Text(stringResource(R.string.video_frames)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = s.videoFrameRate.toString(),
                            onValueChange = { 
                                s = s.copy(videoFrameRate = it.toIntOrNull() ?: 24) 
                            },
                            label = { Text(stringResource(R.string.video_fps)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // ============ API KEYS ============
            SectionTitle(stringResource(R.string.settings_api_keys))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApiKeyField(stringResource(R.string.openai_key),
                        s.openAiApiKey,    { s = s.copy(openAiApiKey    = it) },
                        showOAI,  { showOAI  = !showOAI  })
                    ApiKeyField(stringResource(R.string.stability_key),
                        s.stabilityApiKey, { s = s.copy(stabilityApiKey = it) },
                        showStab, { showStab = !showStab })
                    ApiKeyField(stringResource(R.string.replicate_key),
                        s.replicateApiKey, { s = s.copy(replicateApiKey = it) },
                        showRep,  { showRep  = !showRep  })
                }
            }

            // ============ IMAGE SETTINGS ============
            SectionTitle(stringResource(R.string.settings_image))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.resolution), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(512 to 512, 768 to 768, 1024 to 1024).forEach { (w, h) ->
                            FilterChip(
                                selected = s.defaultImageWidth == w && s.defaultImageHeight == h,
                                onClick  = { s = s.copy(defaultImageWidth = w, defaultImageHeight = h) },
                                label    = { Text("${w}x${h}") }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.steps) + ": ${s.defaultSteps}",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value         = s.defaultSteps.toFloat(),
                        onValueChange = { s = s.copy(defaultSteps = it.toInt()) },
                        valueRange    = 10f..50f,
                        steps         = 39
                    )
                    Text(stringResource(R.string.cfg_scale) + ": ${"%.1f".format(s.defaultCfgScale)}",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value         = s.defaultCfgScale,
                        onValueChange = { s = s.copy(defaultCfgScale = it) },
                        valueRange    = 1f..20f
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.auto_save))
                        Switch(
                            checked         = s.saveToGallery,
                            onCheckedChange = { s = s.copy(saveToGallery = it) }
                        )
                    }
                }
            }

            // ============ REPLICATE SETTINGS ============
            if (s.selectedProvider == AIProvider.REPLICATE) {
                SectionTitle(stringResource(R.string.settings_replicate))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value         = s.replicateImageVersion,
                            onValueChange = { s = s.copy(replicateImageVersion = it) },
                            label         = { Text(stringResource(R.string.replicate_image_ver)) },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                        OutlinedTextField(
                            value         = s.replicateVideoVersion,
                            onValueChange = { s = s.copy(replicateVideoVersion = it) },
                            label         = { Text(stringResource(R.string.replicate_video_ver)) },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                    }
                }
            }

            // ============ SAVE BUTTON ============
            Button(
                onClick  = { vm.save(s); justSaved = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_settings))
            }

            if (justSaved) {
                LaunchedEffect(Unit) { delay(2000); justSaved = false }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier         = Modifier.padding(16.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_saved))
                    }
                }
            }

            // ============ ABOUT ============
            SectionTitle(stringResource(R.string.settings_about))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(stringResource(R.string.version),   "1.0.0")
                    InfoRow(stringResource(R.string.models),    "DALL-E 3, SDXL, SVD, Agnes Image/Video")
                    InfoRow(stringResource(R.string.providers), "OpenAI, Stability AI, Replicate, Agnes AI")
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        label               = { Text(label) },
        modifier            = Modifier.fillMaxWidth(),
        singleLine          = true,
        visualTransformation = if (visible) VisualTransformation.None
                               else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector        = if (visible) Icons.Default.VisibilityOff
                                         else Icons.Default.Visibility,
                    contentDescription = if (visible) stringResource(R.string.hide)
                                         else stringResource(R.string.show)
                )
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}