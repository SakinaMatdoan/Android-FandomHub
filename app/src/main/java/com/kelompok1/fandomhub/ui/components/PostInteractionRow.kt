package com.kelompok1.fandomhub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.CommentEntity
import com.kelompok1.fandomhub.data.local.SavedPostEntity
import kotlinx.coroutines.launch

@Composable
fun PostInteractionRow(
    postId: Int,
    userId: Int, // Current logged in user
    repository: FandomRepository,
    onCommentClick: () -> Unit,
    isFollowing: Boolean
) {
    // State Collection
    val isLiked = repository.isLiked(postId, userId).collectAsState(initial = false)
    val likeCount = repository.getLikeCount(postId).collectAsState(initial = 0)
    
    val commentCount = repository.getCommentCount(postId).collectAsState(initial = 0)
    
    val isSaved = repository.isSaved(postId, userId).collectAsState(initial = false)
    val savedCount = repository.getSavedCount(postId).collectAsState(initial = 0)

    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Like List Dialog State
    var showLikeList by remember { mutableStateOf(false) }

    if (showLikeList) {
        val likers by repository.getPostLikers(postId).collectAsState(initial = emptyList())
        UserListDialog(
            title = "Likes",
            users = likers,
            onDismiss = { showLikeList = false }
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like Button & Count
        InteractionButton(
            icon = if (isLiked.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            tint = if (isLiked.value) Color.Red else MaterialTheme.colorScheme.onSurface,
            count = likeCount.value,
            onClick = {
                if (isFollowing) {
                    scope.launch {
                         repository.toggleLike(postId, userId, isLiked.value)
                    }
                } else {
                    android.widget.Toast.makeText(context, "You must follow the fandom to like posts.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onCountClick = {
                if (likeCount.value > 0) showLikeList = true
            }
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        // ... rest unchanged
        // Comment Button & Count
        InteractionButton(
            icon = Icons.Default.ChatBubbleOutline,
            count = commentCount.value,
            onClick = {
                if (isFollowing) {
                    onCommentClick()
                } else {
                    android.widget.Toast.makeText(context, "You must follow the fandom to comment.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f)) // Push Save to end

        // Save Button & Count
        InteractionButton(
            icon = if (isSaved.value) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            tint = if (isSaved.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            count = savedCount.value,
            onClick = {
                 scope.launch {
                     repository.toggleSave(postId, userId, isSaved.value)
                 }
            }
        )
    }
}

@Composable
fun InteractionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    count: Int,
    onClick: () -> Unit,
    onCountClick: (() -> Unit)? = null // Optional callback for count click
) {
    if (onCountClick != null) {
        // Split click areas
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = tint, 
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClick() }
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onCountClick() }
                )
            }
        }
    } else {
        // Unified click area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


