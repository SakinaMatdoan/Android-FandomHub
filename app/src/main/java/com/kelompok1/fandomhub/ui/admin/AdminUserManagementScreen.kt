package com.kelompok1.fandomhub.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    repository: FandomRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pendingArtists = repository.getPendingArtists().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
             StandardHeader(
                title = "Admin: Approve Artists",
                onBack = onBack
             )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
        ) {
            if (pendingArtists.value.isEmpty()) {
                item { Text("Tidak ada artist pending.") }
            }
            items(pendingArtists.value) { artist ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(artist.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(artist.email, style = MaterialTheme.typography.bodyMedium)
                        Text("Username: ${artist.username}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.approveArtist(artist)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)) // Green
                            ) {
                                Text("Approve")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.rejectArtist(artist.id)
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}
