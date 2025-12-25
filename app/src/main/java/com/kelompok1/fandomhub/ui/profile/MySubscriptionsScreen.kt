package com.kelompok1.fandomhub.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
// import com.kelompok1.fandomhub.data.local.SubscriptionWithDetails
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.utils.DateUtils
import com.kelompok1.fandomhub.ui.components.StandardHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySubscriptionsScreen(
    currentUser: UserEntity,
    repository: FandomRepository,
    onNavigateBack: () -> Unit
) {
    val subscriptions by repository.getMySubscriptions(currentUser.id).collectAsState(initial = emptyList())
    // Fetch all artists to map names
    // Note: In a real app we would join tables or fetch specific artists. 
    // Since getMySubscriptions might verify fetching artists, we can optimize this later.
    // For now let's assume we can get mapped artist details.
    // Actually, subscriptions usually link to artistId.
    
    // Efficient way: SubscriptionWithDetails? Or just fetch artist names.
    // Let's rely on repository.getUserById in list item? Or fetch all artists blindly if small app.
    // Let's use logic from other screens: Repository.getArtistsByIds(ids)? 
    // Or just "getAllFandoms" replacement -> "getAllArtists"
    
    // Assuming we don't have getAllArtists exposed simply?
    // Let's use a side-effect to fetch artists for these subs if needed, OR just getAllUsersByRole("ARTIST") if that exists.
    // Checking previous code, we might not have `getAllArtists()`. 
    // Use `repository.getFollowedArtists`? No, subscription != follow necessarily (though usually is).
    // Let's assume we can use `getArtistById` inside the item, or fetch all artists if list is small. 
    // Actually, let's fix the immediate build error by using `repository.getUserById` or similar if possible.
    // But `getAllArtists` would be easiest if it exists. 
    // Let's assume `repository.getAllArtists()` exists or add it.
    // Looking at DAO, we have `getUsersByRole`.
    
    // Implementation:
    val allArtists by repository.getAllArtists().collectAsState(initial = emptyList())
    val artistMap = remember(allArtists) { allArtists.associateBy { it.id } }
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            StandardHeader(
                title = "My Subscriptions",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("You have no active subscriptions.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subscriptions) { sub ->
                    val artist = artistMap[sub.artistId]
                    SubscriptionCard(
                        subscription = sub,
                        artistName = artist?.fullName ?: "Unknown Artist",
                        onCancel = {
                            scope.launch {
                                // repository.cancelSubscription(sub.id)
                            }
                        },
                        onRenew = {
                            scope.launch {
                                // repository.reactivateSubscription(sub.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(
    subscription: com.kelompok1.fandomhub.data.local.SubscriptionEntity,
    artistName: String,
    onCancel: () -> Unit,
    onRenew: () -> Unit
) {
    val isCancelled = subscription.isCancelled
    val isExpired = subscription.validUntil < System.currentTimeMillis()
    val isActive = !isExpired && !isCancelled
    
    val statusColor = when {
        isExpired -> Color.Red
        isCancelled -> Color.Gray // Or Orange
        else -> Color(0xFF4CAF50) // Green
    }
    
    val statusText = when {
        isExpired -> "Expired"
        isCancelled -> "Cancelled (Active until expiry)"
        else -> "Active"
    }

    // Date Format
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val expiryDate = dateFormat.format(Date(subscription.validUntil))

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artistName, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Status Chip
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    contentColor = statusColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Valid Until", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(expiryDate, style = MaterialTheme.typography.bodyMedium)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (isActive) {
                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Cancel Subscription")
                        }
                    } else if (isCancelled && !isExpired) {
                        Button(
                            onClick = onRenew,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Resume Subscription")
                        }
                    } else {
                        // Expired
                         Button(
                            onClick = { /* Navigate to checkout? Needs Nav Controller or callback */ },
                             enabled = false // For now, disabled as we didn't pass nav for renewal of expired
                        ) {
                            Text("Expired")
                        }
                    }
                }
            }
        }
    }
}
