package com.kelompok1.fandomhub.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.ArtistBadge
import com.kelompok1.fandomhub.ui.components.StandardHeader

@Composable
fun MarketChatsScreen(
    repository: FandomRepository,
    currentUser: UserEntity,
    onBack: () -> Unit,
    onNavigateToChat: (Int) -> Unit
) {
    val chatPartners = repository.getChatPartners(currentUser.id, "MARKET").collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            StandardHeader(
                title = "Market Chats",
                onBack = onBack
            )
        }
    ) { padding ->
        if (chatPartners.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No market conversations yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(chatPartners.value) { partnerId ->
                    val user = produceState<UserEntity?>(initialValue = null, key1 = partnerId) {
                        value = repository.getUserById(partnerId)
                    }
                    val unreadCount = repository.getChatUnreadCount(currentUser.id, partnerId, "MARKET").collectAsState(initial = 0)
                    
                    user.value?.let { partner ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onNavigateToChat(partnerId) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = partner.profileImage,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = partner.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (unreadCount.value > 0) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        ArtistBadge(visible = partner.role == "ARTIST")
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (unreadCount.value > 0) "${unreadCount.value} New Messages" else "@${partner.username}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (unreadCount.value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (unreadCount.value > 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                
                                if (unreadCount.value > 0) {
                                    Surface(
                                        color = Color(0xFFE91E63),
                                        shape = CircleShape,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = unreadCount.value.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Chat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
