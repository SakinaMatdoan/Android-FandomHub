package com.kelompok1.fandomhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.utils.DateUtils
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder

@Composable
fun CommentItem(
    comment: CommentEntity,
    allComments: List<CommentEntity>,
    repository: FandomRepository,
    currentUserId: Int, // Added parameter
    onReplyClick: (CommentEntity, String) -> Unit,
    depth: Int = 0
) {
    val scope = rememberCoroutineScope()
    val userState = produceState<UserEntity?>(initialValue = null) {
        value = repository.getUserById(comment.userId)
    }
    val authorName = userState.value?.fullName ?: "Unknown"
    val userProfile = userState.value?.profileImage
    
    // Display name logic
    val displayName = if (comment.userId == currentUserId) "$authorName - you" else authorName

    // Limit indentation to 1 level (Instagram style)
    // Only depth 1 gets padding. Deeper levels align with depth 1.
    val indent = if (depth == 1) 48.dp else 0.dp 
    
    Column(modifier = Modifier.padding(start = indent, top = 8.dp)) {
        Row(verticalAlignment = Alignment.Top) {
             AsyncImage(
                model = userProfile,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp)) // Increased form 8dp
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName, 
                        style = MaterialTheme.typography.labelLarge, // Slightly bolder/larger than bodySmall
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Artist Label / Verified Badge
                    ArtistBadge(visible = userState.value?.role == "ARTIST")
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateUtils.getRelativeTime(comment.timestamp) + if (comment.isEdited) " (edited)" else "", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        var showReportDialog by remember { mutableStateOf(false) }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        var showEditDialog by remember { mutableStateOf(false) }
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        var editContent by remember { mutableStateOf(comment.content) }
                        
                        if (showDeleteDialog) {
                             AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE91E63)) },
                                title = { Text("Delete Comment", color = MaterialTheme.colorScheme.onSurface) },
                                text = { Text("Are you sure you want to delete this comment? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                containerColor = MaterialTheme.colorScheme.surface,
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteComment(comment.id)
                                                showDeleteDialog = false
                                                android.widget.Toast.makeText(context, "Comment deleted", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                                    ) { Text("Delete", color = Color.White) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) { 
                                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface) 
                                    }
                                }
                            )
                        }

                        if (showEditDialog) {
                            AlertDialog(
                                onDismissRequest = { showEditDialog = false },
                                title = { Text("Edit Comment") },
                                text = {
                                    OutlinedTextField(
                                        value = editContent,
                                        onValueChange = { editContent = it },
                                        label = { Text("Comment") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        scope.launch {
                                            repository.updateComment(comment.id, editContent)
                                            showEditDialog = false
                                            android.widget.Toast.makeText(context, "Comment updated", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }) { Text("Save") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                                }
                            )
                        }

                        if (showReportDialog) {
                            ReportDialog(
                                onDismiss = { showReportDialog = false },
                                onSubmit = { reason, description ->
                                    // Submit Report
                                    scope.launch {
                                        val report = com.kelompok1.fandomhub.data.local.ReportEntity(
                                            reporterId = currentUserId,
                                            reportedId = comment.userId,
                                            type = "COMMENT",
                                            referenceId = comment.id,
                                            reason = reason, 
                                            description = description,
                                            contentSnapshot = comment.content
                                        )
                                        val success = repository.reportUser(report)
                                        showReportDialog = false
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Comment reported.", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "You have already reported this comment", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }

                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (comment.userId == currentUserId) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray) },
                                    onClick = { 
                                        showMenu = false 
                                        editContent = comment.content
                                        showEditDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray) },
                                    onClick = { 
                                        showMenu = false 
                                        showDeleteDialog = true
                                    }
                                )
                                Divider()
                            }
                            
                            if (comment.userId != currentUserId) {
                                // Report
                                DropdownMenuItem(
                                    text = { Text("Report") },
                                    onClick = { 
                                        showMenu = false 
                                        showReportDialog = true
                                    }
                                )
                                
                                // Block (If not self)
                                DropdownMenuItem(
                                    text = { Text("Block User", color = Color(0xFFE91E63)) },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            repository.blockUser(currentUserId, comment.userId)
                                            android.widget.Toast.makeText(context, "User blocked.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp)) // Add space between name and content

                ExpandableText(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(4.dp)) // Add space between content and reply button

                val isLiked = repository.isCommentLiked(comment.id, currentUserId).collectAsState(initial = false)
                val scope = rememberCoroutineScope()
                var showLikeList by remember { mutableStateOf(false) }

                if (showLikeList) {
                    val likers by repository.getCommentLikers(comment.id).collectAsState(initial = emptyList())
                    UserListDialog(
                        title = "Likes",
                        users = likers,
                        onDismiss = { showLikeList = false }
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Like Section
                    Icon(
                        if (isLiked.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { 
                                scope.launch {
                                    repository.toggleCommentLike(comment.id, currentUserId, isLiked.value)
                                }
                            },
                        tint = if (isLiked.value) Color.Red else Color.Gray
                    )
                    
                    if (comment.likeCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable { showLikeList = true }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Reply Section
                    Text(
                        text = "Reply", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onReplyClick(comment, authorName) }
                    )
                }
            }
        }
        
        // Render Replies (Recursive) with Collapse/Expand
        val replies = allComments.filter { it.parentId == comment.id }
        
        if (replies.isNotEmpty()) {
            var showReplies by remember { mutableStateOf(false) }
            
            if (!showReplies) {
                // Button aligns with expected reply indentation
                val buttonStartPadding = if (depth == 0) 48.dp else 0.dp
                Text(
                    text = "View ${replies.size} more replies",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(start = buttonStartPadding, top = 4.dp, bottom = 8.dp)
                        .clickable { showReplies = true }
                )
            } else {
                replies.forEach { reply ->
                    CommentItem(
                        comment = reply,
                        allComments = allComments,
                        repository = repository,
                        currentUserId = currentUserId,
                        onReplyClick = onReplyClick,
                        depth = depth + 1
                    )
                }
            }
        }
    }
}
