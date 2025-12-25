package com.kelompok1.fandomhub.ui.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable // Added
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch // Scope
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.ui.components.StandardHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    repository: FandomRepository,
    currentUserId: Int,
    otherUserId: Int,
    onBack: () -> Unit,
    onNavigateToCheckout: (Int) -> Unit, // Navigate to Artist ID
    chatType: String = "SOCIAL"
) {
    val messages = if (chatType == "SUPPORT") {
        repository.getSupportChatHistory(otherUserId).collectAsState(initial = emptyList())
    } else {
        repository.getChatHistory(currentUserId, otherUserId, chatType).collectAsState(initial = emptyList())
    }
    
    val scope = rememberCoroutineScope() 
    var textState by remember { mutableStateOf("") }
    var otherUser by remember { mutableStateOf<com.kelompok1.fandomhub.data.local.UserEntity?>(null) }
    var isBlocked by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(false) }
    var currentArtistId by remember { mutableStateOf<Int?>(null) } // Replaced Fandom ID with Artist ID
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var chatDisabledReason by remember { mutableStateOf<String?>(null) }
    var remainingTimeText by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(otherUserId) {
        otherUser = repository.getUserById(otherUserId)
        
        // 1. Check Blocking (Mutual)
        val myBlock = repository.isBlocked(currentUserId, otherUserId)
        val theirBlock = repository.isBlocked(otherUserId, currentUserId)
        
        isBlocked = myBlock // For menu state
        
        if (myBlock) {
            chatDisabledReason = "You have blocked this user"
        } else if (theirBlock) {
             chatDisabledReason = "You have been blocked by this user"
        }
        
        // 2. Check Subscription (If not blocked and is Fan-Artist pair) - ONLY FOR SOCIAL CHAT
        if (chatDisabledReason == null && chatType == "SOCIAL") {
            val user = repository.getUserById(currentUserId)
            val other = repository.getUserById(otherUserId) 
            
            var fanId: Int? = null
            var artistId: Int? = null
            
            if (user?.role == "FANS" && other?.role == "ARTIST") {
                fanId = currentUserId
                artistId = otherUserId
            } else if (user?.role == "ARTIST" && other?.role == "FANS") {
                 fanId = otherUserId
                 artistId = currentUserId
            }
            
            if (fanId != null && artistId != null) {
                 // Direct artist check
                 currentArtistId = artistId
                 val sub = repository.getSubscription(artistId, fanId)
                 val isValid = sub != null && sub.validUntil > System.currentTimeMillis() && !sub.isCancelled
                 
                 if (!isValid) {
                     chatDisabledReason = "Subscription expired or cancelled"
                 } else {
                     // Valid Subscription logic
                     if (user?.role == "FANS") {
                         isSubscribed = true 
                     }
                     
                     // Display Remaining Time
                     remainingTimeText = com.kelompok1.fandomhub.utils.DateUtils.getRemainingDays(sub!!.validUntil)
                 }
            }
        }

        // Mark as Read
        repository.markChatAsRead(currentUserId, otherUserId, chatType)
    }
    
    // Auto-scroll to bottom
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(messages.value.size) { 
        if (messages.value.isNotEmpty()) {
            listState.animateScrollToItem(0) // Reverse layout
        }
    }
    
    // Check Artist Interaction Status
    val isInteractionEnabledState = produceState(initialValue = true, key1 = otherUser) {
        if (otherUser != null && otherUser?.role == "ARTIST") {
             // Fetch fresh artist data to check isInteractionEnabled
             val freshArtist = repository.getUserById(otherUser!!.id)
             value = freshArtist?.isInteractionEnabled ?: true
        }
    }
    val isInteractionEnabled = isInteractionEnabledState.value

    val headerTitle = when (chatType) {
        "SUPPORT" -> "FandomHub Support"
        "MARKET" -> (otherUser?.fullName ?: "Chat") + " (Marketplace)"
        else -> otherUser?.fullName ?: "Chat"
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            StandardHeader(
                title = headerTitle,
                profileImage = if (chatType == "SUPPORT") null else otherUser?.profileImage, // Maybe use logo for support?
                subtitle = if (chatType == "SUPPORT") "Official Help Center" else if (remainingTimeText != null) remainingTimeText else if (otherUser != null) "@${otherUser?.username}" else null,
                isArtist = otherUser?.role == "ARTIST" && chatType != "SUPPORT", // Hide Verified badge for support unless we want it
                onBack = onBack,
                actions = {
                    if (chatType != "SUPPORT") {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = "More")
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isBlocked) {
                            DropdownMenuItem(
                                text = { Text("Unblock User") },
                                onClick = {
                                    scope.launch {
                                        repository.unblockUser(currentUserId, otherUserId)
                                        isBlocked = false
                                        showMenu = false
                                        android.widget.Toast.makeText(context, "User unblocked", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Block User") },
                                onClick = {
                                    scope.launch {
                                        repository.blockUser(currentUserId, otherUserId)
                                        isBlocked = true
                                        showMenu = false
                                        android.widget.Toast.makeText(context, "User blocked", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        
                        if (isSubscribed) {
                            DropdownMenuItem(
                                text = { Text("Cancel Subscription") },
                                onClick = {
                                    scope.launch {
                                        if (currentArtistId != null) {
                                            repository.cancelSubscription(currentUserId, currentArtistId!!)
                                            android.widget.Toast.makeText(context, "Subscription successfully cancelled", android.widget.Toast.LENGTH_SHORT).show()
                                            isSubscribed = false
                                            chatDisabledReason = "Subscription expired or cancelled"
                                            showMenu = false
                                        }
                                    }
                                }
                            )
                        }
                        
                         DropdownMenuItem(
                            text = { Text("Report User") },
                            onClick = {
                                showReportDialog = true
                                showMenu = false
                            }
                        )
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
            // Warning Banner if Disabled
            if (!isInteractionEnabled && chatType == "SOCIAL" && otherUser?.role == "ARTIST") {
                 Surface(
                     color = MaterialTheme.colorScheme.errorContainer,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     Text(
                         "Messages are disabled for this artist.",
                         color = MaterialTheme.colorScheme.onErrorContainer,
                         modifier = Modifier.padding(16.dp),
                         style = MaterialTheme.typography.bodySmall
                     )
                 }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false, 
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = listState 
            ) {
                items(messages.value) { msg ->
                    val isMe = if (chatType == "SUPPORT") msg.senderId != otherUserId else msg.senderId == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = if (isMe) {
                                androidx.compose.foundation.shape.RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                            } else {
                                androidx.compose.foundation.shape.RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                            },
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = msg.content,
                                modifier = Modifier.padding(12.dp),
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Input Area
            if (chatDisabledReason != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFCE4EC),
                        contentColor = Color(0xFF880E4F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = null,
                            tint = Color(0xFFE91E63)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = chatDisabledReason ?: "Chat disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        if (isBlocked) {
                             Spacer(modifier = Modifier.height(16.dp))
                             Button(
                                 onClick = {
                                    scope.launch {
                                        repository.unblockUser(currentUserId, otherUserId)
                                        isBlocked = false
                                        // Re-check if they blocked us
                                        if (repository.isBlocked(otherUserId, currentUserId)) {
                                             chatDisabledReason = "You have been blocked by this user"
                                        } else {
                                             chatDisabledReason = null // Enable chat
                                        }
                                        android.widget.Toast.makeText(context, "User unblocked", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                 },
                                 colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE91E63),
                                    contentColor = Color.White
                                 )
                             ) {
                                 Text("Unblock User")
                             }
                        }
                        
                        if (chatDisabledReason == "Subscription expired or cancelled" && currentArtistId != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigateToCheckout(currentArtistId!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Renew Subscription")
                            }
                        }
                    }
                }
            } else {
                Row(
                   modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type here...") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50), // Fully rounded
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textState.isNotBlank()) {
                                if (!isInteractionEnabled && chatType == "SOCIAL" && otherUser?.role == "ARTIST") {
                                     android.widget.Toast.makeText(context, "Messaging is disabled by the artist.", android.widget.Toast.LENGTH_SHORT).show()
                                     return@IconButton
                                }
                                scope.launch {
                                    val msg = com.kelompok1.fandomhub.data.local.MessageEntity(
                                        senderId = currentUserId,
                                        receiverId = otherUserId,
                                        content = textState,
                                        timestamp = System.currentTimeMillis(),
                                        type = chatType
                                    )
                                    repository.sendMessage(msg)
                                    textState = ""
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp), 
                        enabled = textState.isNotBlank() && (chatType != "SOCIAL" || isInteractionEnabled || otherUser?.role != "ARTIST")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(24.dp),
                            tint = if (textState.isNotBlank() && (chatType != "SOCIAL" || isInteractionEnabled || otherUser?.role != "ARTIST")) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        com.kelompok1.fandomhub.ui.components.ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, description ->
                scope.launch {
                    val report = com.kelompok1.fandomhub.data.local.ReportEntity(
                        reporterId = currentUserId,
                        reportedId = otherUserId,
                        type = "USER",
                        referenceId = otherUserId,
                        reason = reason,
                        description = description,
                        contentSnapshot = "Reported User Profile"
                    )
                    val success = repository.reportUser(report)
                    showReportDialog = false
                    if (success) {
                        android.widget.Toast.makeText(context, "Report submitted", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "You have already reported this user", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}
