package com.kelompok1.fandomhub.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.kelompok1.fandomhub.data.local.ReportEntity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.PostEntity
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.utils.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: PostEntity,
    author: UserEntity,
    currentUserId: Int,
    repository: FandomRepository,
    onCommentClick: () -> Unit,
    onPostClick: () -> Unit,
    isFollowing: Boolean = true,
    artistName: String? = null, // Renamed from fandomName
    onAuthorClick: () -> Unit = {},
    artist: UserEntity? = null // Renamed from fandom (UserEntity for the Artist context)
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onPostClick() }
            .padding(bottom = 8.dp)
    ) {
        
        // Header: Profile + Username + Time + More
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAuthorClick() } 
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = author.profileImage,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                if (artistName != null) {
                    Text(
                        text = "from $artistName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = author.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                   ArtistBadge(visible = author.role == "ARTIST")
                }
                Text(
                    text = DateUtils.getRelativeTime(post.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                var showMenu by remember { mutableStateOf(false) }
                var showReportDialog by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current

                var showEditDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var editContent by remember { mutableStateOf(post.content) }
                var editImages by remember { mutableStateOf(post.images) }

                val imagePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        editImages = editImages + uri.toString()
                    }
                }

                if (showReportDialog) {
                    ReportDialog(
                        onDismiss = { showReportDialog = false },
                        onSubmit = { reason, description ->
                            scope.launch {
                                val report = ReportEntity(
                                    reporterId = currentUserId,
                                    reportedId = author.id,
                                    type = "POST",
                                    referenceId = post.id,
                                    reason = reason,
                                    description = description,
                                    contentSnapshot = post.content + (if (post.images.isNotEmpty()) " | Image: ${post.images.first()}" else "")
                                )
                                val success = repository.reportUser(report)
                                showReportDialog = false
                                if (success) {
                                    android.widget.Toast.makeText(context, "Report submitted", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "You have already reported this post", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                
                if (showEditDialog) {
                    CreatePostDialog(
                        onDismiss = { showEditDialog = false },
                        title = "Edit Post",
                        initialContent = editContent,
                        initialImages = editImages,
                        onConfirm = { newContent, newImages ->
                            scope.launch {
                                repository.updatePost(post.id, newContent, newImages)
                                showEditDialog = false
                                android.widget.Toast.makeText(context, "Post updated", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }


                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE91E63)) },
                        title = { Text("Delete Post", color = MaterialTheme.colorScheme.onSurface) },
                        text = { Text("Are you sure you want to delete this post? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.deletePost(post.id)
                                        showDeleteDialog = false
                                        android.widget.Toast.makeText(context, "Post deleted", android.widget.Toast.LENGTH_SHORT).show()
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

                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (author.id == currentUserId) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray) },
                            onClick = { 
                                showMenu = false
                                editContent = post.content
                                editImages = post.images
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
                    
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { 
                            showMenu = false 
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Check out this post: https://fandomhub.com/post/${post.id}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                        }
                    )

                    if (author.id != currentUserId) {
                        DropdownMenuItem(
                            text = { Text("Report") },
                            onClick = { 
                                showMenu = false
                                if (isFollowing) {
                                    showReportDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, "You must follow the artist to report posts.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        if (currentUserId != author.id) {
                            DropdownMenuItem(
                                text = { Text("Block User", color = Color(0xFFE91E63)) },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        repository.blockUser(currentUserId, author.id)
                                        android.widget.Toast.makeText(context, "User blocked.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Text Content
        if (post.content.isNotBlank()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                ExpandableText(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Image Carousel (if images > 0)
        if (post.images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Double Tap State
            val isLiked by repository.isLiked(post.id, currentUserId).collectAsState(initial = false)
            val scope = rememberCoroutineScope()
            var showHeartAnimation by remember { mutableStateOf(false) } 
            val context = androidx.compose.ui.platform.LocalContext.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (artist != null && !artist.isInteractionEnabled && currentUserId != artist.id) {
                                     android.widget.Toast.makeText(context, "Interactions are disabled for this artist.", android.widget.Toast.LENGTH_SHORT).show()
                                     return@detectTapGestures
                                }

                                if (isFollowing) {
                                    if (!isLiked) { 
                                        scope.launch {
                                             repository.toggleLike(post.id, currentUserId, false) 
                                        }
                                    }
                                    showHeartAnimation = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(1000)
                                        showHeartAnimation = false
                                    }
                                } else {
                                     android.widget.Toast.makeText(context, "You must follow the artist to like posts.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onTap = { onPostClick() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {

                val pagerState = rememberPagerState(pageCount = { post.images.size })
                
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(
                        model = post.images[page],
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop 
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartAnimation,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Liked",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(100.dp)
                    )
                }

                if (post.images.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${post.images.size}",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

// Interactions
        PostInteractionRow(
            postId = post.id,
            userId = currentUserId,
            repository = repository,
            onCommentClick = onCommentClick,
            isFollowing = isFollowing,
            artist = artist // Pass artist
        )
        
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha=0.5f))
    }
}

@Composable
fun PostInteractionRow(
    postId: Int,
    userId: Int,
    repository: FandomRepository,
    onCommentClick: () -> Unit,
    isFollowing: Boolean,
    artist: UserEntity? = null
) {
    val likeCount by repository.getLikeCount(postId).collectAsState(initial = 0)
    val isLiked by repository.isLiked(postId, userId).collectAsState(initial = false)
    val commentCount by repository.getCommentCount(postId).collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { 
                if (artist != null && !artist.isInteractionEnabled && userId != artist.id) {
                     android.widget.Toast.makeText(context, "Interactions are disabled.", android.widget.Toast.LENGTH_SHORT).show()
                     return@IconButton
                }
                if (isFollowing) {
                    scope.launch {
                        repository.toggleLike(postId, userId, isLiked)
                    }
                } else {
                     android.widget.Toast.makeText(context, "You must follow the artist to like posts.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFE91E63) else Color.Gray
                )
            }
            Text("$likeCount", style = MaterialTheme.typography.bodyMedium)

            IconButton(onClick = {
                if (artist != null && !artist.isInteractionEnabled && userId != artist.id) {
                     android.widget.Toast.makeText(context, "Interactions are disabled.", android.widget.Toast.LENGTH_SHORT).show()
                     return@IconButton
                }
                 onCommentClick() 
            }) {
                Icon(Icons.Default.Comment, contentDescription = "Comment", tint = Color.Gray)
            }
            Text("$commentCount", style = MaterialTheme.typography.bodyMedium)
        }

        // Save Button
        val isSaved by repository.isSaved(postId, userId).collectAsState(initial = false)
        IconButton(onClick = { 
            scope.launch {
                repository.toggleSave(postId, userId, isSaved)
                val msg = if (isSaved) "Post removed from saved" else "Post saved"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }) {
            Icon(
                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Save",
                tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}
