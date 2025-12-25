package com.kelompok1.fandomhub.ui.subscription

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import kotlinx.coroutines.launch
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.ui.components.ArtistBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionCheckoutScreen(
    repository: FandomRepository,
    currentUserId: Int,
    artistId: Int, // Renamed from fandomId
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var artist by remember { mutableStateOf<UserEntity?>(null) }
    var selectedMethod by remember { mutableStateOf("GoPay") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(artistId) {
        artist = repository.getArtistById(artistId)
    }

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Checkout Subscription",
                onBack = onBack
            )
        }
    ) { padding ->
        val isInteractionEnabled = artist?.isInteractionEnabled ?: true
        val subscriptionPrice = artist?.subscriptionPrice
        val subscriptionDuration = artist?.subscriptionDuration

        if (artist != null && !isInteractionEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = Color(0xFFE91E63)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Subscription Unavailable", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Subscriptions are currently disabled for this artist.", 
                        color = Color.Gray
                    )
                }
            }
        } else if (artist == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), 
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Subscribe to ${artist!!.fullName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        ArtistBadge(visible = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rp $subscriptionPrice / $subscriptionDuration days",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!artist!!.subscriptionBenefits.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Benefits:", fontWeight = FontWeight.Bold)
                                Text(artist!!.subscriptionBenefits!!)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Select Payment Method:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("GoPay", "OVO", "Dana").forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMethod = method }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(method)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (subscriptionPrice != null && subscriptionDuration != null) {
                                scope.launch {
                                     // Subscribe for Configured Duration
                                     val durationMillis = subscriptionDuration * 24 * 60 * 60 * 1000L
                                     repository.subscribe(currentUserId, artistId, durationMillis)
                                     Toast.makeText(context, "Welcome to the inner circle!", Toast.LENGTH_LONG).show()
                                     onSuccess()
                                }
                            } else {
                                Toast.makeText(context, "Subscription not configured.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Pay Now")
                    }
                }
            }
        }
    }
}
