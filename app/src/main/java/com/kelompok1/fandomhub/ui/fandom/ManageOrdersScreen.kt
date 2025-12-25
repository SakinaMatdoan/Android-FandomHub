package com.kelompok1.fandomhub.ui.fandom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun ManageOrdersScreen(
    repository: FandomRepository,
    artistId: Int,
    onBack: () -> Unit,
    onNavigateToChat: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val orders = repository.getOrdersByArtist(artistId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            StandardHeader(title = "Manage Orders", onBack = onBack)
        }
    ) { padding ->
            Column(Modifier.padding(padding)) {
                 // Status Filter
                 val statuses = listOf("All", "Pending", "Processed", "Shipped", "Delivered", "Refund")
                 var selectedStatus by remember { mutableStateOf("All") }
                 
                 ScrollableTabRow(
                     selectedTabIndex = statuses.indexOf(selectedStatus), 
                     edgePadding = 16.dp,
                     containerColor = Color.Transparent,
                     contentColor = MaterialTheme.colorScheme.primary,
                     indicator = { tabPositions ->
                         TabRowDefaults.Indicator(
                             modifier = Modifier.tabIndicatorOffset(tabPositions[statuses.indexOf(selectedStatus)]),
                             color = MaterialTheme.colorScheme.primary
                         )
                     }
                 ) {
                     statuses.forEach { status ->
                         Tab(
                             selected = selectedStatus == status,
                             onClick = { selectedStatus = status },
                             text = { Text(status) }
                         )
                     }
                 }
                 
                 val filteredOrders = remember(orders.value, selectedStatus) {
                     if (selectedStatus == "All") orders.value
                     else if (selectedStatus == "Refund") orders.value.filter { it.status.contains("REFUND") }
                     else orders.value.filter { it.status.equals(selectedStatus, ignoreCase = true) }
                 }

                 if (filteredOrders.isEmpty()) {
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text("No orders found.", color = Color.Gray)
                     }
                 } else {
                     LazyColumn(
                         modifier = Modifier
                             .fillMaxSize()
                             .padding(16.dp)
                     ) {
                         items(filteredOrders) { order ->
                             Card(
                                 modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                             ) {
                                 Column(modifier = Modifier.padding(12.dp)) {
                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                             Text("Order #${order.id}", fontWeight = FontWeight.Bold)
                                             IconButton(onClick = { onNavigateToChat(order.userId) }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                                 Icon(Icons.Default.Chat, contentDescription = "Chat Buyer", tint = MaterialTheme.colorScheme.primary)
                                             }
                                         }
                                         Text(order.status, color = if (order.status == "DELIVERED") Color.Green else if (order.status == "REFUND_REQUESTED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                    
                                    // Fetch Buyer Info
                                    val buyer = produceState<com.kelompok1.fandomhub.data.local.UserEntity?>(initialValue = null, key1 = order.userId) {
                                        value = repository.getUserById(order.userId)
                                    }.value

                                    if (buyer != null) {
                                         Text("Buyer: ${buyer.fullName} (@${buyer.username})", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text("Address: ${order.shippingAddress}", style = MaterialTheme.typography.bodySmall)
                                    Text("Payment: ${order.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Parse Items
                                    val itemType = object : com.google.gson.reflect.TypeToken<List<com.kelompok1.fandomhub.ui.market.OrderItemSnapshot>>() {}.type
                                    val items: List<com.kelompok1.fandomhub.ui.market.OrderItemSnapshot> = try {
                                        com.google.gson.Gson().fromJson(order.itemsJson, itemType)
                                    } catch (e: Exception) { emptyList() }

                                    if (items.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha=0.5f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        items.forEach { item ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("${item.productName} x${item.quantity}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                                Text("Rp ${item.price.toInt()}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha=0.5f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    
                                    Text("Total: Rp ${order.totalAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Date: ${DateUtils.formatTimestamp(order.timestamp)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                     
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                         if (order.status == "PENDING") {
                                             Button(onClick = { scope.launch { repository.updateOrderStatus(order.id, "PROCESSED") } }, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                                 Text("Process")
                                             }
                                         } else if (order.status == "PROCESSED") {
                                             Button(onClick = { scope.launch { repository.updateOrderStatus(order.id, "SHIPPED") } }, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                                 Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                                 Spacer(modifier = Modifier.width(4.dp))
                                                 Text("Ship")
                                             }
                                         } else if (order.status == "SHIPPED") {
                                              OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                                 Text("Waiting for User")
                                             }
                                         } else if (order.status == "REFUND_REQUESTED") {
                                             Row {
                                                 Button(
                                                     onClick = { scope.launch { repository.updateOrderStatus(order.id, "REFUNDED") } },
                                                     modifier = Modifier.height(36.dp),
                                                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                                     contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                 ) {
                                                     Text("Approve Refund")
                                                 }
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 OutlinedButton(
                                                     onClick = { scope.launch { repository.updateOrderStatus(order.id, "REFUND_REJECTED") } },
                                                     modifier = Modifier.height(36.dp),
                                                     contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                 ) {
                                                     Text("Reject")
                                                 }
                                             }
                                         } else if (order.status == "REFUNDED") {
                                              OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.height(36.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                                 Text("Refunded")
                                             }
                                         } else {
                                             OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.height(36.dp)) {
                                                 Text("Completed")
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                     }
                 }
            }
    }
}
