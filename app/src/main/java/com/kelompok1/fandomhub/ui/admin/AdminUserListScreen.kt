package com.kelompok1.fandomhub.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader

@Composable
fun AdminUserListScreen(
    userType: String, // "ARTIST" or "FAN"
    repository: FandomRepository,
    onNavigateToDetail: (Int) -> Unit,
    onBack: () -> Unit
) {
    val flow = remember(userType) {
         if (userType == "ARTIST") repository.getAllArtistsAdmin() else repository.getAllFans()
    }
    val usersList by flow.collectAsState(initial = emptyList<UserEntity>())
    
    val title = if (userType == "ARTIST") "All Artists" else "All Fans"

    // Search State
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter Users
    val filteredUsers = if (searchQuery.isBlank()) {
        usersList
    } else {
        usersList.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) || 
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            StandardHeader(title = title, onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name or username...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredUsers.isEmpty()) {
                     item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No users found.", color = Color.Gray)
                        }
                    }
                }
                
                items(filteredUsers) { user ->
                    val scope = rememberCoroutineScope()
                    AdminUserItem(
                        user = user,
                        onClick = { onNavigateToDetail(user.id) },
                        onSuspend = { 
                            scope.launch { 
                                // Suspend for 7 days
                                repository.suspendUserDirect(user.id, 7L * 24 * 60 * 60 * 1000, 0) 
                            }
                        },
                        onBan = {
                            scope.launch { repository.banUser(user.id) }
                        },
                        onUnsuspend = {
                            scope.launch { repository.unsuspendUser(user.id) }
                        },
                        onDelete = {
                            scope.launch { repository.deleteUser(user.id) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminUserItem(
    user: UserEntity,
    onClick: () -> Unit,
    onSuspend: () -> Unit,
    onBan: () -> Unit,
    onUnsuspend: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (user.profileImage != null) {
                AsyncImage(
                    model = user.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.fullName.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Status: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = if (user.isSuspended) "SUSPENDED" else user.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.isSuspended) Color.Red else Color.Green,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (!user.isSuspended) {
                        DropdownMenuItem(
                            text = { Text("Suspend (7 Days)") },
                            onClick = { 
                                onSuspend()
                                expanded = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ban (Permanent)") },
                            onClick = { 
                                onBan()
                                expanded = false 
                            },
                            colors = MenuDefaults.itemColors(textColor = Color.Red)
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Unsuspend / Restore") },
                            onClick = { 
                                onUnsuspend()
                                expanded = false 
                            }
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Delete User") },
                        onClick = { 
                            onDelete()
                            expanded = false 
                        },
                        colors = MenuDefaults.itemColors(textColor = Color.Red)
                    )
                }
            }
        }
    }
}
