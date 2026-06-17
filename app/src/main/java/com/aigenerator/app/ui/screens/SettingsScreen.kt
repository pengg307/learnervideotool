package com.aigenerator.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigenerator.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var openAiKey by remember { mutableStateOf(settings.openAiApiKey) }
    var agnesKey by remember { mutableStateOf(settings.agnesApiKey) }
    var agnesImgModel by remember { mutableStateOf(settings.agnesImageModel) }
    var agnesVidModel by remember { mutableStateOf(settings.agnesVideoModel) }
    var selectedProvider by remember { mutableStateOf(settings.selectedProvider) }
    var saveGallery by remember { mutableStateOf(settings.saveToGallery) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.save(settings.copy(
                            openAiApiKey = openAiKey,
                            agnesApiKey = agnesKey,
                            agnesImageModel = agnesImgModel,
                            agnesVideoModel = agnesVidModel,
                            selectedProvider = selectedProvider,
                            saveToGallery = saveGallery
                        ))
                        onBack()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = openAiKey,
                onValueChange = { openAiKey = it },
                label = { Text("OpenAI API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = agnesKey,
                onValueChange = { agnesKey = it },
                label = { Text("Agnes AI API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = agnesImgModel,
                onValueChange = { agnesImgModel = it },
                label = { Text("Agnes Image Model") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = agnesVidModel,
                onValueChange = { agnesVidModel = it },
                label = { Text("Agnes Video Model") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            ExposedDropdownMenuBox(
                expanded = false,
                onExpandedChange = {}
            ) {
                TextField(
                    value = selectedProvider,
                    onValueChange = { selectedProvider = it },
                    label = { Text("Default Provider") },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Switch(
                checked = saveGallery,
                onCheckedChange = { saveGallery = it },
                label = { Text("Save to Gallery") }
            )
        }
    }
}
