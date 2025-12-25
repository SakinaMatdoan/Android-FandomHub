package com.kelompok1.fandomhub.ui.post

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.CommentEntity
import com.kelompok1.fandomhub.data.local.PostEntity
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.CommentItem
import com.kelompok1.fandomhub.ui.components.PostItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    repository: FandomRepository,
    currentUserId: Int,
    onBack: () -> Unit
) {
    // Collect post data
    val postState = produceState<PostEntity?>(initialValue = null, key1 = postId) {
        value = repository.getPostById(postId)
    }
    // Collect comments
    val comments = repository.getComments(postId).collectAsState(initial = emptyList())
    
    val post = postState.value
    val scope = rememberCoroutineScope()
    
    // Fetch author state lifted
    var author by remember { mutableStateOf<UserEntity?>(null) }
    
    // Check Follow Status (Artist Context)
    val isFollowingState = produceState(initialValue = false, key1 = post) {
        if (post != null) {
            repository.isFollowing(currentUserId, post.artistId).collect { value = it }
        }
    }
    val isFollowing = isFollowingState.value
    val context = androidx.compose.ui.platform.LocalContext.current

    // Fetch Artist (Context)
    val artistState = produceState<UserEntity?>(initialValue = null, key1 = post) {
        if (post != null) {
            value = repository.getUserById(post.artistId)
        }
    }
    val artist = artistState.value

    LaunchedEffect(post) {
        if (post != null) {
            author = repository.getUserById(post.authorId)
        }
    }
    
    // Comment Input State
    var commentText by remember { mutableStateOf(TextFieldValue("")) }
    var replyingTo by remember { mutableStateOf<CommentEntity?>(null) }
    
    // User Profile for Input
    val currentUserProfile = produceState<String?>(initialValue = null) {
        value = repository.getUserById(currentUserId)?.profileImage
    }
    
    val isOwner = post?.artistId == currentUserId
    val isArtist = isOwner
    val isInteractionEnabled = artist?.isInteractionEnabled ?: true
    val canInteract = isFollowing || isOwner

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing, // Handle Keyboard/IME (Chat Style)
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .height(48.dp) 
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { 
                         Icon(Icons.Default.ArrowBack, "Back") 
                    } 
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (post == null) {
                 Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                     CircularProgressIndicator()
                 }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                       if (author != null) {
                           PostItem(
                               post = post,
                               author = author!!,
                               currentUserId = currentUserId,
                               repository = repository,
                               onCommentClick = { },
                               onPostClick = { },
                               isFollowing = canInteract,
                               artist = artist // Pass artist entity
                           )
                       }
                    }

                    // Comment Header with Count
                    item {
                        Text(
                            text = "Comments (${comments.value.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Comments
                    val topLevelComments = comments.value.filter { it.parentId == null }
                    
                    if (topLevelComments.isEmpty()) {
                        item {
                            Text(
                                "No comments yet.", 
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray
                            )
                        }
                    } else {
                        items(topLevelComments) { comment ->
                             Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                 CommentItem(
                                    comment = comment,
                                    allComments = comments.value,
                                    repository = repository,
                                    currentUserId = currentUserId,
                                    onReplyClick = { parent, username ->
                                        replyingTo = parent
                                        val mention = "@$username "
                                        commentText = TextFieldValue(mention, TextRange(mention.length))
                                    }
                                )
                             }
                        }
                    }
                }
            }
            
            // Input Area (Moved from BottomBar to here for IME handling)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
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
                            .padding(horizontal = 8.dp, vertical = 8.dp), // Chat Padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        AsyncImage(
                            model = currentUserProfile.value,
                            contentDescription = "My Profile",
                            modifier = Modifier
                                .size(32.dp) // Adjusted size slightly
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Chat-style Input
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(if (replyingTo != null) "Reply..." else "Type a comment...") },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50), // Fully rounded like Chat
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            maxLines = 3 // Limit lines
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (!isInteractionEnabled && !isArtist) {
                                    android.widget.Toast.makeText(context, "Comments are disabled for this artist.", android.widget.Toast.LENGTH_SHORT).show()
                                } else if (!canInteract) {
                                    android.widget.Toast.makeText(context, "You must follow the artist to comment.", android.widget.Toast.LENGTH_SHORT).show()
                                } else if (commentText.text.isNotBlank()) {
                                    scope.launch {
                                        val newComment = CommentEntity(
                                            postId = postId,
                                            userId = currentUserId,
                                            content = commentText.text.trim(),
                                            timestamp = System.currentTimeMillis(),
                                            parentId = replyingTo?.id
                                        )
                                        repository.addComment(newComment)
                                        commentText = TextFieldValue("")
                                        replyingTo = null
                                    }
                                }
                            },
                            enabled = (isInteractionEnabled || isArtist) && (commentText.text.isNotBlank() || !canInteract), 
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if ((isInteractionEnabled || isArtist) && canInteract) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
