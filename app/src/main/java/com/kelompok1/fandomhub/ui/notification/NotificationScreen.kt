package com.kelompok1.fandomhub.ui.notification

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.kelompok1.fandomhub.ui.components.StandardHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    repository: FandomRepository,
    currentUser: UserEntity,
    onNavigateToPost: (Int) -> Unit,
    onNavigateToMerch: (Int) -> Unit,
    onNavigateToFollowers: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Explicitly type the state
    val notificationsListState = repository.getNotifications(currentUser.id)
        .collectAsState(initial = emptyList<FandomRepository.NotificationUiModel>())
    val notifications = notificationsListState.value

    LaunchedEffect(Unit) {
        repository.markAllNotificationsRead(currentUser.id)
    }

    // Filter Logic
    val filters = if (currentUser.role == "ARTIST") {
        listOf("All", "Followers", "Interactions")
    } else {
        listOf("All", "Updates", "Interactions")
    }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredNotifications = notifications.filter { notification ->
        when (selectedFilter) {
            "All" -> true
            "Followers" -> notification.type == "FOLLOW"
            "Updates" -> notification.type == "POST" || notification.type == "MERCH"
            "Interactions" -> {
                if (currentUser.role == "ARTIST") {
                     notification.type == "LIKE_POST" || notification.type == "COMMENT" || notification.type == "REPLY"
                } else {
                     notification.type == "REPLY" || notification.type == "LIKE_POST"
                }
            }
            else -> true
        }
    }

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Notifications",
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    repository.deleteAllNotifications(currentUser.id)
                                    Toast.makeText(context, "Notifications cleared", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                             Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Filter Chips
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
            
            HorizontalDivider()

            if (filteredNotifications.isEmpty()) {
                 Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (notifications.isEmpty()) "No notifications yet" else "No $selectedFilter notifications", 
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredNotifications,
                        key = { model -> model.timestamp.toString() + model.type + model.referenceId }
                    ) { notification ->
                        NotificationItem(notification, onNavigateToPost, onNavigateToMerch, onNavigateToFollowers)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: FandomRepository.NotificationUiModel,
    onNavigateToPost: (Int) -> Unit,
    onNavigateToMerch: (Int) -> Unit,
    onNavigateToFollowers: (Int) -> Unit
) {
    val icon = when (notification.type) {
        "POST" -> Icons.AutoMirrored.Filled.Article
        "LIKE_POST" -> Icons.Default.Favorite
        "COMMENT" -> Icons.AutoMirrored.Filled.Comment
        "REPLY" -> Icons.AutoMirrored.Filled.Reply
        "MERCH" -> Icons.Default.ShoppingBag
        else -> Icons.Default.Notifications
    }

    val color = when (notification.type) {
        "LIKE_POST" -> Color.Red
        "MERCH" -> Color.Green
        else -> MaterialTheme.colorScheme.primary
    }

    ListItem(
        modifier = Modifier.clickable {
             when (notification.type) {
                "MERCH" -> onNavigateToMerch(notification.referenceId)
                "FOLLOW" -> onNavigateToFollowers(notification.referenceId)
                else -> onNavigateToPost(notification.referenceId)
            }
        },
        leadingContent = {
            if (notification.avatar != null) {
                // Show User Avatar
                AsyncImage(
                    model = notification.avatar,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback Icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        headlineContent = { Text(notification.message, fontWeight = FontWeight.Bold) }, // Main message is now the descriptive one
        supportingContent = {
             Text(
                text = "${notification.title} • ${SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(notification.timestamp))}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
    HorizontalDivider()
}
