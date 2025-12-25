package com.kelompok1.fandomhub.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@Composable
fun AdminSupportChatListScreen(
    repository: FandomRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit,
    onBack: () -> Unit
) {
    var displayContacts by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    // Fetch Support Chat Partners
    LaunchedEffect(currentUserId) {
        repository.getAllSupportChatPartners(currentUserId).collect { partnerIds ->
            val users = mutableListOf<UserEntity>()
            partnerIds.forEach { id ->
                val user = repository.getUserById(id)
                // Filter out other Admins, show only Fans/Artists
                if (user != null && user.role != "ADMIN") {
                    users.add(user)
                }
            }
            displayContacts = users
        }
    }

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Support Inbox",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
             if (displayContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No support messages yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(displayContacts) { user ->
                    // Get Unread Count for SUPPORT type
                    val unreadCount = produceState(initialValue = 0, key1 = user.id) {
                        repository.getChatUnreadCount(currentUserId, user.id, "SUPPORT").collect { value = it }
                    }

                    ListItem(
                        modifier = Modifier.clickable { onNavigateToChat(user.id) },
                        headlineContent = {
                            Text(
                                text = user.fullName,
                                fontWeight = if (unreadCount.value > 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "@${user.username}",
                                color = if (unreadCount.value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            if (user.profileImage != null) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    AsyncImage(
                                        model = user.profileImage,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            } else {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.fullName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (unreadCount.value > 0) {
                                    Badge { Text(unreadCount.value.toString()) }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Open Chat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
}
