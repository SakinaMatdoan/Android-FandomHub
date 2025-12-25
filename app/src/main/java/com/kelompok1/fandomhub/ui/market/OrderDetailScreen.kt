package com.kelompok1.fandomhub.ui.market

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Start // Likely typo, update if needed or remove
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.ui.components.formatRupiah
import com.kelompok1.fandomhub.utils.DateUtils

// Helper model for displaying order items (Snapshots)
data class OrderItemSnapshot(
    val productId: Int,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val image: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    repository: FandomRepository,
    orderId: Int,
    onBack: () -> Unit,
    onNavigateToChat: (Int) -> Unit
) {
    var order by remember { mutableStateOf<com.kelompok1.fandomhub.data.local.OrderEntity?>(null) }
    
    LaunchedEffect(orderId) {
        order = repository.getOrderById(orderId)
    }
    
    // Pair(ProductId, ProductName)
    var showReviewDialog by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            com.kelompok1.fandomhub.ui.components.StandardHeader(
                title = "Order Details",
                onBack = onBack,
                actions = {
                    if (order != null) {
                         IconButton(onClick = { onNavigateToChat(order!!.artistId) }) {
                             Icon(Icons.Default.Chat, contentDescription = "Chat Seller", tint = MaterialTheme.colorScheme.primary)
                         }
                    }
                }
            )
        }
    ) { padding ->
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentOrder = order!!
            
            // Parse items using the new Snapshot model
            val itemType = object : TypeToken<List<OrderItemSnapshot>>() {}.type
            val items: List<OrderItemSnapshot> = try {
                Gson().fromJson(currentOrder.itemsJson, itemType)
            } catch (e: Exception) {
                emptyList()
            }

            // Calculations based on stored Total (assuming fixed shipping logic for MVP)
            val shippingCost = 15000.0
            val amountWithoutShipping = currentOrder.totalAmount - shippingCost
            val subtotal = amountWithoutShipping / 1.11
            val tax = subtotal * 0.11

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // --- Header ---
                item {
                    Text(
                        text = "Order #${currentOrder.id}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateUtils.formatTimestamp(currentOrder.timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // OrderStatusChip inline
                    SuggestionChip(
                        onClick = {},
                        label = { Text(currentOrder.status) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = when(currentOrder.status) {
                                "PENDING" -> Color(0xFFFFF3E0)
                                "PROCESSED" -> Color(0xFFE3F2FD)
                                "SHIPPED" -> Color(0xFFE8F5E9)
                                "DELIVERED" -> Color(0xFFE0F2F1)
                                else -> Color.LightGray
                            },
                            labelColor = Color.Black
                        ),
                        border = null
                    )
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // --- Items ---
                item {
                    Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(items) { item ->
                    OrderDetailItemCard(
                        item, 
                        showReviewButton = currentOrder.status == "DELIVERED",
                        onReviewClick = {
                            showReviewDialog = Pair(item.productId, item.productName)
                        }
                    )
                }

                // --- Shipping & Payment ---
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Shipping Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(currentOrder.shippingAddress, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(currentOrder.paymentMethod, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }

                // --- Summary ---
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Payment Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                         Text("Subtotal (Approx)", color = Color.Gray)
                         Text(formatRupiah(subtotal))
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                         Text("Tax (11%)", color = Color.Gray)
                         Text(formatRupiah(tax))
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                         Text("Shipping Cost", color = Color.Gray)
                         Text(formatRupiah(shippingCost))
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Paid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            formatRupiah(currentOrder.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                item {
                    if (currentOrder.status == "SHIPPED") {
                        val scope = rememberCoroutineScope()
                        Button(
                            onClick = { 
                                scope.launch {
                                    repository.updateOrderStatus(currentOrder.id, "DELIVERED")
                                    order = currentOrder.copy(status = "DELIVERED")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Pesanan Diterima (Confirm Receipt)")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (currentOrder.status == "DELIVERED") {
                        OutlinedButton(
                            onClick = {}, 
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Order Completed")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Review Items (Optional)", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Refund Section
                    if (currentOrder.status == "REFUND_REQUESTED") {
                         Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                             Text("Refund Requested. Waiting for seller approval.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                         }
                         Spacer(modifier = Modifier.height(16.dp))
                    } else if (currentOrder.status == "REFUNDED") {
                         Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                             Text("Order Refunded.", modifier = Modifier.padding(16.dp), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                         }
                         Spacer(modifier = Modifier.height(16.dp))
                    } else if (currentOrder.status !in listOf("CANCELLED", "DELIVERED")) {
                         TextButton(
                             onClick = { 
                                 scope.launch { 
                                     repository.updateOrderStatus(currentOrder.id, "REFUND_REQUESTED") 
                                     order = currentOrder.copy(status = "REFUND_REQUESTED")
                                 } 
                             },
                             modifier = Modifier.fillMaxWidth()
                         ) {
                             Text("Request Refund", color = MaterialTheme.colorScheme.error)
                         }
                    }
                }
            }
            
            // Re-render items with review button if DELIVERED
            // We need to manage state for dialog outside LazyColumn
            if (showReviewDialog != null) {
                ReviewDialog(
                    productName = showReviewDialog!!.second,
                    onDismiss = { showReviewDialog = null },
                    onSubmit = { rating, comment ->
                         scope.launch {
                             repository.addReview(
                                 com.kelompok1.fandomhub.data.local.ReviewEntity(
                                     productId = showReviewDialog!!.first,
                                     userId = currentOrder.userId,
                                     rating = rating,
                                     comment = comment,
                                     timestamp = System.currentTimeMillis()
                                 )
                             )
                             showReviewDialog = null
                         }
                    }
                )
            }
        }
    }
}

@Composable
fun OrderDetailItemCard(
    item: OrderItemSnapshot, 
    showReviewButton: Boolean = false,
    onReviewClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.image.isNullOrEmpty()) {
                 Image(
                    painter = rememberAsyncImagePainter(item.image),
                    contentDescription = item.productName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "${item.quantity} x ${formatRupiah(item.price)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Text(
                text = formatRupiah(item.price * item.quantity),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
            if (showReviewButton) {
                OutlinedButton(
                    onClick = onReviewClick,
                    modifier = Modifier.align(Alignment.End).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Write Review", style = MaterialTheme.typography.labelSmall)
                }
            }
    }
}

@Composable
fun ReviewDialog(
    productName: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review $productName") },
        text = {
            Column {
                Text("Rating: $rating/5")
                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.clickable { rating = i }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
