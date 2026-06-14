package com.aigenerator.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigenerator.app.model.AIProvider
import com.aigenerator.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(paddingValues: PaddingValues, vm: SettingsViewModel = hiltViewModel()) {
    val saved     by vm.settings.collectAsState()
    var s         by remember(saved) { mutableStateOf(saved) }
    var showOAI   by remember { mutableStateOf(false) }
    var showStab  by remember { mutableStateOf(false) }
    var showRep   by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()
        .padding(bottom = paddingValues.calculateBottomPadding())
        .verticalScroll(rememberScrollState())) {

        TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer))

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            SectionTitle("AI Provider")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    AIProvider.values().forEach { p ->
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = s.selectedProvider == p,
                                onClick = { s = s.copy(selectedProvider = p) })
                            Text(when (p) {
                                AIProvider.OPENAI       -> "OpenAI  (DALL-E 3 + GPT-4)"
                                AIProvider.STABILITY_AI -> "Stability AI  (SDXL)"
                                AIProvider.REPLICATE    -> "Replicate  (Open models)"
                            })
                        }
                    }
                }
            }

            SectionTitle("API Keys")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    KeyField("OpenAI Key (sk-...)",
                        s.openAiApiKey, { s = s.copy(openAiApiKey = it) },
                        showOAI,  { showOAI  = !showOAI  })
                    KeyField("Stability AI Key",
                        s.stabilityApiKey, { s = s.copy(stabilityApiKey = it) },
                        showStab, { showStab = !showStab })
                    KeyField("Replicate Key (r8_...)",
                        s.replicateApiKey, { s = s.copy(replicateApiKey = it) },
                        showRep,  { showRep  = !showRep  })
                }
            }

            SectionTitle("Image Settings")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Resolution", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(512 to 512, 768 to 768, 1024 to 1024).forEach { (w, h) ->
                            FilterChip(
                                selected = s.defaultImageWidth == w && s.defaultImageHeight == h,
                                onClick  = { s = s.copy(defaultImageWidth = w, defaultImageHeight = h) },
                                label    = { Text("${w}x${h}",
                                    style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Steps: ${s.defaultSteps}",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(value = s.defaultSteps.toFloat(),
                        onValueChange = { s = s.copy(defaultSteps = it.toInt()) },
                        valueRange = 10f..50f, steps = 39)
                    Text("CFG Scale: ${"%.1f".format(s.defaultCfgScale)}",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(value = s.defaultCfgScale,
                        onValueChange = { s = s.copy(defaultCfgScale = it) },
                        valueRange = 1f..20f)
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween,
                        Alignment.CenterVertically) {
                        Text("Auto-save to Gallery")
                        Switch(checked = s.saveToGallery,
                            onCheckedChange = { s = s.copy(saveToGallery = it) })
                    }
                }
            }

            if (s.selectedProvider == AIProvider.REPLICATE) {
                SectionTitle("Replicate Model Versions")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = s.replicateImageVersion,
                            onValueChange = { s = s.copy(replicateImageVersion = it) },
                            label = { Text("Image Model Version") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = s.replicateVideoVersion,
                            onValueChange = { s = s.copy(replicateVideoVersion = it) },
                            label = { Text("Video Model Version") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
            }

            Button(onClick = { vm.save(s); justSaved = true },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings")
            }

            if (justSaved) {
                LaunchedEffect(Unit) { delay(2000); justSaved = false }
                Card(Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Settings saved!")
                    }
                }
            }

            SectionTitle("About")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("Version",   "1.0.0")
                    InfoRow("Models",    "DALL-E 3, SDXL, SVD")
                    InfoRow("Providers", "OpenAI, Stability AI, Replicate")
                }
            }
        }
    }
}

@Composable fun SectionTitle(t: String) =
    Text(t, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

@Composable fun KeyField(
    label: String, value: String, onChange: (String) -> Unit,
    visible: Boolean, onToggle: () -> Unit
) = OutlinedTextField(value = value, onValueChange = onChange,
    label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
    visualTransformation = if (visible) VisualTransformation.None
                           else PasswordVisualTransformation(),
    trailingIcon = {
        IconButton(onClick = onToggle) {
            Icon(if (visible) Icons.Default.VisibilityOff
                 else Icons.Default.Visibility, null)
        }
    })

@Composable fun InfoRow(label: String, value: String) =
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
