package com.kelompok1.fandomhub.ui.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

@Composable
fun MessageListScreen(
    repository: FandomRepository,
    currentUser: UserEntity,
    initialTab: String = "SOCIAL", // SOCIAL, MARKET
    onNavigateToChat: (Int, String) -> Unit,
    onManageSubscription: () -> Unit
) {
    // 0 = Social, 1 = Marketplace
    var selectedTab by remember { mutableStateOf(if (initialTab == "MARKET") 1 else 0) }
    val tabs = listOf("Messages", "Marketplace")
    
    var displayContacts by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(currentUser, selectedTab) {
        val type = if (selectedTab == 0) "SOCIAL" else "MARKET"
        
        if (type == "SOCIAL") {
            if (currentUser.role == "ARTIST") {
                // Fetch subscribers ONLY
                repository.getSubscribersWithStatus(currentUser.id)
                    .onEach { subscribers -> 
                        displayContacts = subscribers.map { it.user }
                    }
                    .collect()
            } else {
                repository.getSubscribedArtists(currentUser.id).collect { displayContacts = it }
            }
        } else {
            // MARKET Logic
            repository.getChatPartners(currentUser.id, "MARKET").collect { partnerIds ->
                val users = mutableListOf<UserEntity>()
                partnerIds.forEach { id ->
                    val user = repository.getUserById(id)
                    if (user != null) users.add(user)
                }
                displayContacts = users
            }
        }
    }

    Scaffold(
        topBar = {
             StandardHeader(
                title = if (currentUser.role == "ARTIST" && initialTab == "MARKET") "Market Chats" else "Messages",
                actions = {

                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Only show Tabs for FANS. Artists have separate entry points potentially, but assuming Artist only sees Social or Market based on where they came from might be limiting.
            // If Artist sees this screen, they might want to switch.
            // The constraint: `if (currentUser.role != "ARTIST")` around tabs might be wrong if Artist wants to chat Market.
            // But let's keep it if that was design. Actually, Artist usually accesses Market chat via specific actions?
            // Re-reading logic: `(currentUser.role != "ARTIST")`
            // If I am Artist, I see NO tabs. So I only see `selectedTab`.
            // If I came with `initialTab = SOCIAL`, I see Followers.
            // If I came with `initialTab = MARKET`, I see Market chats.
            // This seems intentional to separate Artist "DM" vs "Order Support".
            
            if (currentUser.role != "ARTIST") {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
            
            if (displayContacts.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTab == 0) 
                            (if (currentUser.role == "ARTIST") "No followers yet." else "Follow & Subscribe to artists to chat!") 
                        else "No marketplace chats yet.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn {
                     items(displayContacts) { user ->
                         var showMenu by remember { mutableStateOf(false) }
                         val chatType = if (selectedTab == 0) "SOCIAL" else "MARKET"
                         
                         // Helper for Unread Count
                         val unreadCount = produceState(initialValue = 0, key1 = user.id, key2 = chatType) {
                            repository.getChatUnreadCount(currentUser.id, user.id, chatType).collect { value = it }
                         }
                         
                         Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onNavigateToChat(user.id, chatType) },
                                        onLongClick = { showMenu = true }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    if (user.profileImage != null) {
                                        AsyncImage(model = user.profileImage, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = user.fullName.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (unreadCount.value > 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        com.kelompok1.fandomhub.ui.components.ArtistBadge(visible = user.role == "ARTIST")
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (unreadCount.value > 0) "${unreadCount.value} New Messages" else "@${user.username}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (unreadCount.value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (unreadCount.value > 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                }
                                
                                if (unreadCount.value > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = unreadCount.value.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onError
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
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete Conversation") },
                                    onClick = {
                                        scope.launch {
                                            repository.deleteChat(currentUser.id, user.id, chatType)
                                            showMenu = false
                                            android.widget.Toast.makeText(context, "Conversation deleted", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                         }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                     }
                }
            }
        }
    }
}
