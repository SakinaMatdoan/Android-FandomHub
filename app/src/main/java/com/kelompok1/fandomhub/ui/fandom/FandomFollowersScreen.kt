package com.kelompok1.fandomhub.ui.fandom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FandomFollowersScreen(
    artistId: Int, // Renamed from fandomId
    repository: FandomRepository,
    onBack: () -> Unit
) {
    val followersList by repository.getFollowers(artistId).collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var userToReport by remember { mutableStateOf<UserEntity?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val filteredFollowers = remember(followersList, searchQuery) {
        if (searchQuery.isBlank()) followersList
        else followersList.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            StandardHeader(title = "Followers", onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search followers...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            if (filteredFollowers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotEmpty()) "No followers found." else "No followers yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(items = filteredFollowers, key = { it.id }) { user ->
                        FollowerItem(
                            user = user,
                            onBlock = {
                                scope.launch {
                                    repository.blockUser(artistId, user.id)
                                    android.widget.Toast.makeText(context, "Blocked ${user.username}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReport = {
                                // Show dialog by setting state
                                userToReport = user
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
        if (userToReport != null && artistId != null) {
            com.kelompok1.fandomhub.ui.components.ReportDialog(
                onDismiss = { userToReport = null },
                onSubmit = { reason, description -> 
                    scope.launch {
                         val report = com.kelompok1.fandomhub.data.local.ReportEntity(
                            reporterId = artistId!!,
                            reportedId = userToReport!!.id,
                            type = "USER",
                            referenceId = userToReport!!.id,
                            reason = reason, 
                            description = description,
                            contentSnapshot = "Reported User: ${userToReport!!.username}"
                        )
                        val success = repository.reportUser(report)
                        userToReport = null
                        if (success) {
                            android.widget.Toast.makeText(context, "Report submitted", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Already reported", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
}

@Composable
fun FollowerItem(
    user: UserEntity,
    onBlock: () -> Unit,
    onReport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            if (user.profileImage != null) {
                coil.compose.AsyncImage(
                    model = user.profileImage,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.fullName.take(1).uppercase())
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        // Actions
        Row {
             TextButton(onClick = onReport) {
                Text("Report", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onBlock) {
                Text("Block")
            }
        }
    }
}
