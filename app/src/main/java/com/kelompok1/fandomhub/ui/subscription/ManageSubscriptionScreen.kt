package com.kelompok1.fandomhub.ui.subscription

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.data.FandomRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionScreen(
    repository: FandomRepository,
    currentUserId: Int,
    onNavigateToChat: (Int) -> Unit, 
    onBack: () -> Unit
) {
    var artist by remember { mutableStateOf<UserEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Form fields
    var isDmActive by remember { mutableStateOf(false) }
    var priceStr by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf("30") }
    var benefits by remember { mutableStateOf("") }
    
    // Fetch Subscribers
    val subscribers by produceState<List<com.kelompok1.fandomhub.data.local.SubscriberWithStatus>>(initialValue = emptyList(), key1 = artist) {
        if (artist != null) {
            repository.getSubscribersWithStatus(artist!!.id).collect { value = it }
        }
    }

    LaunchedEffect(currentUserId) {
        // Fetch Artist Profile (Self)
        val f = repository.getUserById(currentUserId)
        artist = f
        if (f != null) {
            isDmActive = f.isDmActive
            priceStr = f.subscriptionPrice?.toString() ?: ""
            durationStr = f.subscriptionDuration?.toString() ?: "30"
            benefits = f.subscriptionBenefits ?: ""
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            com.kelompok1.fandomhub.ui.components.StandardHeader(
                title = "Manage Subscription",
                onBack = onBack
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (artist == null) {
             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Artist profile not found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ... Existing Form Code ...
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                         Text("DM & Subscription Status", style = MaterialTheme.typography.titleMedium)
                         Spacer(Modifier.height(8.dp))
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Text("Enable Subscription Gating")
                             Spacer(Modifier.weight(1f))
                             Switch(checked = isDmActive, onCheckedChange = { isDmActive = it })
                         }
                         Text(
                             "If enabled, fans must follow and subscribe to DM you.",
                             style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                    }
                }

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Monthly Price (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it },
                    label = { Text("Duration (Days)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = benefits,
                    onValueChange = { benefits = it },
                    label = { Text("Subscription Benefits (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = { Text("e.g. Exclusive Badges, Priority Reply, etc.")}
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull()
                        val duration = durationStr.toIntOrNull()
                        
                        if (price != null && duration != null) {
                            val updatedArtist = artist!!.copy(
                                isDmActive = isDmActive,
                                subscriptionPrice = price,
                                subscriptionDuration = duration,
                                subscriptionBenefits = benefits
                            )
                            scope.launch {
                                repository.updateUser(updatedArtist)
                                artist = updatedArtist // Update Local State
                                Toast.makeText(context, "Settings Updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        } else {
                             Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
                
                HorizontalDivider()
                
                // Active Subscribers List
                Text(
                    "Active Subscribers", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (subscribers.isEmpty()) {
                    Text("No active subscribers yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    subscribers.forEach { item ->
                        val fan = item.user
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToChat(fan.id) },
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                             Row(
                                 modifier = Modifier.padding(12.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Surface(
                                      shape = CircleShape,
                                      modifier = Modifier.size(40.dp),
                                      color = MaterialTheme.colorScheme.surfaceVariant
                                 ) {
                                     if (fan.profileImage != null) {
                                         coil.compose.AsyncImage(
                                             model = fan.profileImage,
                                             contentDescription = null,
                                             contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                         )
                                     }
                                 }
                                 Spacer(Modifier.width(16.dp))
                                 Column {
                                     Text(fan.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                     Text("@${fan.username}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                     Text(
                                         text = com.kelompok1.fandomhub.utils.DateUtils.getRemainingDays(item.validUntil),
                                         style = MaterialTheme.typography.labelSmall,
                                         color = MaterialTheme.colorScheme.primary
                                     )
                                 }
                                 Spacer(Modifier.weight(1f))
                                 Icon(Icons.Default.ChevronRight, contentDescription = null)
                             }
                        }
                    }
                }
            }
        }
    }
}
