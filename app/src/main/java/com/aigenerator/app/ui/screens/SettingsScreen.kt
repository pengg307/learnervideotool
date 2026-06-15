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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    var showCustom    by remember { mutableStateOf(false) }
    var justSaved     by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title  = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SectionTitle("AI Provider")
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
                                    AIProvider.OPENAI       -> "OpenAI (DALL-E 3 + GPT-4)"
                                    AIProvider.STABILITY_AI -> "Stability AI (SDXL)"
                                    AIProvider.REPLICATE    -> "Replicate (Open models)"
                                    AIProvider.CUSTOM       -> "Custom Endpoint"
                                }
                            )
                        }
                    }
                }
            }

            // Custom Endpoint Settings (shown only when Custom provider is selected)
            if (s.selectedProvider == AIProvider.CUSTOM) {
                SectionTitle("Custom Endpoint Settings")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value         = s.customEndpointUrl,
                            onValueChange = { s = s.copy(customEndpointUrl = it) },
                            label         = { Text("Endpoint URL") },
                            placeholder   = { Text("https://api.example.com/v1") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                        
                        ApiKeyField(
                            label = "Custom API Key",
                            value = s.customApiKey,
                            onValueChange = { s = s.copy(customApiKey = it) },
                            visible = showCustom,
                            onToggleVisibility = { showCustom = !showCustom }
                        )
                        
                        OutlinedTextField(
                            value         = s.customModelName,
                            onValueChange = { s = s.copy(customModelName = it) },
                            label         = { Text("Model Name") },
                            placeholder   = { Text("e.g., gpt-4, llama-2, sd-xl") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                        
                        Text(
                            text = "Note: Custom endpoint must be compatible with OpenAI API format",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionTitle("API Keys")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ApiKeyField("OpenAI Key (sk-...)",
                        s.openAiApiKey,    { s = s.copy(openAiApiKey    = it) },
                        showOAI,  { showOAI  = !showOAI  })
                    ApiKeyField("Stability AI Key",
                        s.stabilityApiKey, { s = s.copy(stabilityApiKey = it) },
                        showStab, { showStab = !showStab })
                    ApiKeyField("Replicate Key (r8_...)",
                        s.replicateApiKey, { s = s.copy(replicateApiKey = it) },
                        showRep,  { showRep  = !showRep  })
                }
            }

            SectionTitle("Image Settings")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resolution", style = MaterialTheme.typography.titleSmall)
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
                    Text("Steps: ${s.defaultSteps}",
                        style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value         = s.defaultSteps.toFloat(),
                        onValueChange = { s = s.copy(defaultSteps = it.toInt()) },
                        valueRange    = 10f..50f,
                        steps         = 39
                    )
                    Text("CFG Scale: ${"%.1f".format(s.defaultCfgScale)}",
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
                        Text("Auto-save to Gallery")
                        Switch(
                            checked         = s.saveToGallery,
                            onCheckedChange = { s = s.copy(saveToGallery = it) }
                        )
                    }
                }
            }

            if (s.selectedProvider == AIProvider.REPLICATE) {
                SectionTitle("Replicate Model Versions")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value         = s.replicateImageVersion,
                            onValueChange = { s = s.copy(replicateImageVersion = it) },
                            label         = { Text("Image Model Version") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                        OutlinedTextField(
                            value         = s.replicateVideoVersion,
                            onValueChange = { s = s.copy(replicateVideoVersion = it) },
                            label         = { Text("Video Model Version") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                    }
                }
            }

            Button(
                onClick  = { vm.save(s); justSaved = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings")
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
                        Text("Settings saved successfully!")
                    }
                }
            }

            SectionTitle("About")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Version",   "1.0.0")
                    InfoRow("Models",    "DALL-E 3, SDXL, SVD")
                    InfoRow("Providers", "OpenAI, Stability AI, Replicate, Custom")
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
                    contentDescription = if (visible) "Hide" else "Show"
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