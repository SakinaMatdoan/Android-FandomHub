package com.kelompok1.fandomhub.ui.fandom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader

import androidx.compose.ui.graphics.Color

// Data Class for Grid Items
data class DashboardItem(val title: String, val icon: ImageVector, val route: String, val badgeCount: Int = 0)

@Composable
fun FandomManagementScreen(
    repository: FandomRepository,
    artist: UserEntity,
    onNavigateToChat: (Int) -> Unit = {}, // Deprecated or unused in Dashboard
    onNavigateToMessageList: (String) -> Unit,
    onNavigateTo: (String) -> Unit // Helper for navigating to new screens
) {
    val context = LocalContext.current
    
    // Stats (Optional: fetch count of pending orders, etc to show on badges)
    val pendingOrdersCount = repository.getOrdersByArtist(artist.id).collectAsState(initial = emptyList()).value.count { it.status == "PENDING" }
    val unreadInquiries = repository.getTotalUnreadCountByType(artist.id, "MARKET").collectAsState(initial = 0).value
    
    val items = listOf(
        DashboardItem("Artist Profile", Icons.Default.Person, "profile", 0), // Renamed
        DashboardItem("Manage Merch", Icons.Default.ShoppingBag, "products", 0),
        DashboardItem("Manage Orders", Icons.Default.LocalShipping, "orders", pendingOrdersCount),
        DashboardItem("Subscription", Icons.Default.CardMembership, "subscription", 0),
        DashboardItem("Market Chats", Icons.Default.Chat, "inquiries", unreadInquiries),
        DashboardItem("Statistics", Icons.Default.Analytics, "statistics", 0)
    )

    Scaffold(
        topBar = {
            StandardHeader(title = "Management")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "General", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // General Section: Profile, Subscription, Statistics
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(items.find { it.route == "profile" }!!, Modifier.weight(1f)) { onNavigateTo("profile") }
                DashboardCard(items.find { it.route == "subscription" }!!, Modifier.weight(1f)) { onNavigateTo("subscription") }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(items.find { it.route == "statistics" }!!, Modifier.weight(0.48f)) { onNavigateTo("statistics") } // Half width approx
                Spacer(Modifier.weight(0.52f)) // Empty space to balance
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Store Management", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Market Section: Products, Orders, Inquiries
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(items.find { it.route == "products" }!!, Modifier.weight(1f)) { onNavigateTo("products") }
                DashboardCard(items.find { it.route == "orders" }!!, Modifier.weight(1f)) { onNavigateTo("orders") }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(items.find { it.route == "inquiries" }!!, Modifier.weight(0.48f)) { onNavigateTo("inquiries") }
                 Spacer(Modifier.weight(0.52f))
            }
        }
    }
}

@Composable
fun DashboardCard(item: DashboardItem, modifier: Modifier = Modifier, onClick: () -> Unit) { 
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(Modifier.fillMaxSize()) {
             Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(item.icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(8.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            
            if (item.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(item.badgeCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
