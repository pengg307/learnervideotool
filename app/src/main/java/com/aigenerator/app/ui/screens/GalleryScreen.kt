package com.aigenerator.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import com.aigenerator.app.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(paddingValues: PaddingValues, vm: GalleryViewModel = hiltViewModel()) {
    val items by vm.items.collectAsState()
    var selected by remember { mutableStateOf<Message?>(null) }

    Column(Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
        TopAppBar(
            title = { Text("Gallery", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer),
            actions = {
                IconButton(onClick = { vm.load() }) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
            }
        )

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    Spacer(Modifier.height(16.dp))
                    Text("No generations yet", style = MaterialTheme.typography.titleMedium)
                    Text("Generate some images or videos first!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    Card(Modifier.aspectRatio(1f).clickable { selected = item },
                        shape = RoundedCornerShape(12.dp)) {
                        Box {
                            AsyncImage(model = item.mediaUrl, contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                            if (item.type == MessageType.AI_VIDEO)
                                Icon(Icons.Default.PlayCircle, "Video",
                                    Modifier.size(40.dp).align(Alignment.Center),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(0.9f))
                        }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(if (item.type == MessageType.AI_VIDEO) "Generated Video"
                          else "Generated Image") },
            text = {
                Column {
                    AsyncImage(model = item.mediaUrl, contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(280.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit)
                    Spacer(Modifier.height(8.dp))
                    Text(item.content, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.delete(item.id); selected = null }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            }
        )
    }
}
