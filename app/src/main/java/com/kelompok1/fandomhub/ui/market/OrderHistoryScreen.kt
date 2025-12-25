package com.kelompok1.fandomhub.ui.market

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.OrderEntity
import com.kelompok1.fandomhub.utils.DateUtils
import com.kelompok1.fandomhub.ui.components.StandardHeader
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    repository: FandomRepository,
    currentUserId: Int,
    onBack: () -> Unit,
    onNavigateToOrderDetail: (Int) -> Unit,
    onNavigateToChat: (Int) -> Unit
) {
    // Collecting flow as state
    val ordersState = repository.getOrdersByUser(currentUserId).collectAsState(initial = emptyList())
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Active", "Completed")

    Scaffold(
        topBar = {
            StandardHeader(
                title = "My Orders",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Filter Logic
            val orders = ordersState.value
            val filteredOrders = orders.filter { order ->
                val isActive = order.status == "PENDING" || order.status == "PROCESSED" || order.status == "SHIPPED"
                if (selectedTab == 0) isActive else !isActive
            }.sortedByDescending { it.timestamp }

            if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ${tabs[selectedTab].lowercase()} orders.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredOrders) { order ->
                        OrderItemCard(
                            order, 
                            onClick = { onNavigateToOrderDetail(order.id) },
                            onChatClick = { onNavigateToChat(order.artistId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: OrderEntity, onClick: () -> Unit, onChatClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OrderStatusChip(status = order.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = DateUtils.formatTimestamp(order.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Rp ${String.format("%,.0f", order.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onChatClick, 
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat Seller", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: String) {
    val (color, text) = when (status) {
        "PENDING" -> MaterialTheme.colorScheme.secondaryContainer to "Pending"
        "PROCESSED" -> MaterialTheme.colorScheme.primaryContainer to "Processing"
        "SHIPPED" -> MaterialTheme.colorScheme.tertiaryContainer to "Shipped"
        "DELIVERED" -> Color(0xFFE8F5E9) to "Delivered" // Light Green
        "REFUND_REQUESTED" -> MaterialTheme.colorScheme.errorContainer to "Refund Requested"
        "REFUNDED" -> Color(0xFFC8E6C9) to "Refunded"
        "REFUND_REJECTED" -> MaterialTheme.colorScheme.surfaceVariant to "Refund Rejected"
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer to "Cancelled"
        else -> MaterialTheme.colorScheme.surfaceVariant to status
    }
    
    val textColor = when(status) {
        "DELIVERED", "REFUNDED" -> Color(0xFF2E7D32) // Dark Green
        "REFUND_REQUESTED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
