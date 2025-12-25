package com.kelompok1.fandomhub.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.utils.DateUtils

@Composable
fun AdminChatReviewScreen(
    repository: FandomRepository,
    user1Id: Int,
    user2Id: Int,
    onBack: () -> Unit
) {
    val messages = repository.getChatHistory(user1Id, user2Id, "SOCIAL").collectAsState(initial = emptyList())
    var user1 by remember { mutableStateOf<UserEntity?>(null) }
    var user2 by remember { mutableStateOf<UserEntity?>(null) }

    LaunchedEffect(user1Id, user2Id) {
        user1 = repository.getUserById(user1Id)
        user2 = repository.getUserById(user2Id)
    }

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Chat Review",
                subtitle = "${user1?.username} vs ${user2?.username}",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (messages.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No messages found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.value) { msg ->
                        val isUser1 = msg.senderId == user1Id
                        val senderName = if (isUser1) user1?.username else user2?.username
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = if (isUser1) 40.dp else 0.dp, vertical = 0.dp) 
                            // This layout style might be confusing. 
                            // Better: Just list them normally with name + time + content.
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = senderName ?: "Unknown",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        text = DateUtils.getRelativeTime(msg.timestamp), 
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
