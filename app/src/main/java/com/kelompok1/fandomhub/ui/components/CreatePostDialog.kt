package com.kelompok1.fandomhub.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    title: String = "Create New Post",
    initialContent: String = "",
    initialImages: List<String> = emptyList(),
    onConfirm: (String, List<String>) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var selectedImageUris by remember { mutableStateOf(initialImages.map { Uri.parse(it) }) }
    
    val imageCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            if (uri != null) {
                selectedImageUris = selectedImageUris + uri
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image Selection List
                Text("Images (${selectedImageUris.size}/5):", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris - uri },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    if (selectedImageUris.size < 5) {
                        item {
                           OutlinedButton(
                                onClick = { 
                                   val options = CropImageContractOptions(
                                       uri = null,
                                       cropImageOptions = CropImageOptions().apply {
                                            imageSourceIncludeGallery = true
                                            imageSourceIncludeCamera = true
                                            aspectRatioX = 1
                                            aspectRatioY = 1
                                            fixAspectRatio = true
                                       }
                                   )
                                   imageCropLauncher.launch(options)
                                },
                                modifier = Modifier.size(80.dp),
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                    Text("Add", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank() || selectedImageUris.isNotEmpty()) {
                        onConfirm(content.trim(), selectedImageUris.map { it.toString() })
                    }
                }
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
