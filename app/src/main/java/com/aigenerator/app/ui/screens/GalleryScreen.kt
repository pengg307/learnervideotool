package com.aigenerator.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import com.aigenerator.app.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    paddingValues: PaddingValues,
    vm: GalleryViewModel = hiltViewModel()
) {
    val items by vm.items.collectAsState()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Message?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<List<Message>>(emptyList()) }
    var activeSelectedIndex by remember { mutableStateOf(0) }
    var combineLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
        TopAppBar(
            title = { Text("Gallery", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            actions = {
                if (selectionMode) {
                    IconButton(onClick = {
                        selectionMode = false
                        selectedItems = emptyList()
                        activeSelectedIndex = 0
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                    IconButton(onClick = {
                        if (selectedItems.size < 2 || combineLoading) return@IconButton
                        combineLoading = true
                        vm.combineVideos(selectedItems) { success, message ->
                            combineLoading = false
                            if (!success) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            } else {
                                selectionMode = false
                                selectedItems = emptyList()
                                activeSelectedIndex = 0
                            }
                        }
                    }) {
                        if (combineLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Combine selected videos")
                        }
                    }
                } else {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        selectionMode = true
                        selectedItems = emptyList()
                        activeSelectedIndex = 0
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Select videos")
                    }
                }
            }
        )

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No generations yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Generate some images or videos first!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (selectionMode) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Tap video thumbnails to select clips, then combine them in order.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Selected clips: ${selectedItems.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(selectedItems) { index, selectedItem ->
                                Card(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clickable {
                                            activeSelectedIndex = index
                                        },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = selectedItem.mediaUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = {
                                if (activeSelectedIndex > 0) {
                                    val mutable = selectedItems.toMutableList()
                                    val temp = mutable[activeSelectedIndex - 1]
                                    mutable[activeSelectedIndex - 1] = mutable[activeSelectedIndex]
                                    mutable[activeSelectedIndex] = temp
                                    selectedItems = mutable.toList()
                                    activeSelectedIndex = activeSelectedIndex - 1
                                }
                            }) {
                                Text("Move up")
                            }
                            TextButton(onClick = {
                                if (activeSelectedIndex < selectedItems.lastIndex) {
                                    val mutable = selectedItems.toMutableList()
                                    val temp = mutable[activeSelectedIndex + 1]
                                    mutable[activeSelectedIndex + 1] = mutable[activeSelectedIndex]
                                    mutable[activeSelectedIndex] = temp
                                    selectedItems = mutable.toList()
                                    activeSelectedIndex = activeSelectedIndex + 1
                                }
                            }) {
                                Text("Move down")
                            }
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    val isSelected = selectedItems.any { it.id == item.id }
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                if (!selectionMode) {
                                    selected = item
                                    return@clickable
                                }
                                if (item.type != MessageType.AI_VIDEO) {
                                    Toast.makeText(
                                        context,
                                        "Only videos can be selected.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@clickable
                                }
                                selectedItems = if (isSelected) {
                                    selectedItems.filterNot { it.id == item.id }
                                } else {
                                    selectedItems + item
                                }
                                activeSelectedIndex = selectedItems
                                    .indexOfFirst { it.id == item.id }
                                    .coerceAtLeast(0)
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = item.mediaUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (item.type == MessageType.AI_VIDEO) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Video",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .align(Alignment.Center),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "${selectedItems.indexOfFirst { it.id == item.id } + 1}",
                                        modifier = Modifier.align(Alignment.Center),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = {
                Text(
                    if (item.type == MessageType.AI_VIDEO) "Generated Video"
                    else "Generated Image"
                )
            },
            text = {
                Column {
                    AsyncImage(
                        model = item.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.content, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.delete(item.id); selected = null }) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            }
        )
    }
}