package com.kelompok1.fandomhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import com.kelompok1.fandomhub.data.local.UserEntity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.CommentEntity
import com.kelompok1.fandomhub.utils.DateUtils
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    postId: Int,
    currentUserId: Int,
    repository: FandomRepository,
    onDismiss: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentEntity?>(null) } // For Nested Replies
    val scope = rememberCoroutineScope()
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Fetch comments
    val comments = repository.getComments(postId).collectAsState(initial = emptyList())

    val currentUserProfile = produceState<String?>(initialValue = null) {
        value = repository.getUserById(currentUserId)?.profileImage
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // Ensure navigation bar space
                .padding(bottom = 16.dp) 
                .imePadding() // CRITICAL: Push whole column up by IME
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Comments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            HorizontalDivider()

            // List
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (comments.value.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No comments yet. Be the first!", color = Color.Gray)
                        }
                    }
                } else {
                    // Grouping Logic: Top level comments vs Replies
                    val topLevelComments = comments.value.filter { it.parentId == null }
                    
                    items(topLevelComments) { comment ->
                        CommentItem(
                            comment = comment,
                            allComments = comments.value,
                            repository = repository,
                            currentUserId = currentUserId,
                            onReplyClick = { parent, username ->
                                replyingTo = parent
                                commentText = "@$username "
                            }
                        )
                    }
                    
                    // Add bottom spacing so last comment isn't hidden by input
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Input Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Column {
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Replying to comment...", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        AsyncImage(
                            model = currentUserProfile.value,
                            contentDescription = "My Profile",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add a comment...") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp),
                            maxLines = 5,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    scope.launch {
                                        val newComment = CommentEntity(
                                            postId = postId,
                                            userId = currentUserId,
                                            content = commentText.trim(),
                                            timestamp = System.currentTimeMillis(),
                                            parentId = replyingTo?.id
                                        )
                                        repository.addComment(newComment)
                                        commentText = ""
                                        replyingTo = null
                                    }
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            // Spacer removed
        }
    }
}


