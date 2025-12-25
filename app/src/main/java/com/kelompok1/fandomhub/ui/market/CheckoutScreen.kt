package com.kelompok1.fandomhub.ui.market

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.CartItemWithProduct
import com.kelompok1.fandomhub.data.local.OrderEntity
import com.kelompok1.fandomhub.ui.components.formatRupiah
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    repository: FandomRepository,
    currentUserId: Int,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val cartItemsState = repository.getCartItems(currentUserId).collectAsState(initial = emptyList<CartItemWithProduct>())
    val cartItems: List<CartItemWithProduct> = cartItemsState.value
    
    val scope = rememberCoroutineScope()
    
    // Form State
    var address by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

    // Calculation (Using product price from Relation)
    val subtotal = cartItems.sumOf { (it.product.price * it.cartItem.quantity).toDouble() }
    val tax = subtotal * 0.11 // 11% Tax
    val shippingCost = 15000.0 // Fixed shipping cost
    val grandTotal = subtotal + tax + shippingCost

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Checkout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your cart is empty.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp) // Space for pay button
                ) {
                    // --- Order Details (Items) ---
                    item {
                        Text(
                            text = "Order Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    items(cartItems) { item ->
                        CheckoutItemCard(item)
                    }

                    // --- Shipping Address ---
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Shipping Address",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Full Address") },
                            placeholder = { Text("Street, City, Zip Code") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            singleLine = false
                        )
                    }

                    // --- Payment Method ---
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PaymentMethodSelector(
                            selectedMethod = selectedPaymentMethod,
                            onSelect = { selectedPaymentMethod = it }
                        )
                    }

                    // --- Summary ---
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SummaryRow("Subtotal", subtotal)
                                SummaryRow("Tax (11%)", tax)
                                SummaryRow("Shipping Cost", shippingCost)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "Total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        formatRupiah(grandTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Sticky Bottom Button ---
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (showError != null) {
                        Text(
                            text = showError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            if (address.isBlank()) {
                                showError = "Please enter your shipping address."
                            } else if (selectedPaymentMethod == null) {
                                showError = "Please select a payment method."
                            } else {
                                isProcessing = true
                                scope.launch {
                                    try {
                                        // Serialize Data - Mapping Relation to DTO if needed for JSON, 
                                        // but storing it as is works if we want; 
                                        // however, OrderEntity needs JSON.
                                        // We should store a snapshot of the item details, NOT just the relation ID,
                                        // because product price might change later.
                                        // We'll create a simple list of objects to serialize.
                                        val orderItems = cartItems.map { 
                                            mapOf(
                                                "productId" to it.product.id,
                                                "productName" to it.product.name,
                                                "price" to it.product.price,
                                                "quantity" to it.cartItem.quantity,
                                                "image" to (it.product.images.firstOrNull() ?: "")
                                            )
                                        }
                                        
                                        val itemsJson = Gson().toJson(orderItems)
                                        val order = OrderEntity(
                                            userId = currentUserId,
                                            artistId = cartItems.firstOrNull()?.product?.artistId ?: 0, // Assume single artist for MVP
                                            totalAmount = grandTotal,
                                            status = "PENDING",
                                            shippingAddress = address,
                                            paymentMethod = selectedPaymentMethod!!,
                                            itemsJson = itemsJson,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        repository.createOrder(order)
                                        isProcessing = false
                                        onPaymentSuccess()
                                    } catch (e: Exception) {
                                        isProcessing = false
                                        showError = e.message ?: "Transaction failed"
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing && cartItems.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Pay Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutItemCard(item: CartItemWithProduct) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use first image or placeholder
        val imageUrl = item.product.images.firstOrNull()
        
        if (!imageUrl.isNullOrEmpty()) {
             Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = item.product.name,
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
            Text(text = item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = "${item.cartItem.quantity} x ${formatRupiah(item.product.price)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        
        Text(
            text = formatRupiah(item.product.price * item.cartItem.quantity),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PaymentMethodSelector(
    selectedMethod: String?,
    onSelect: (String) -> Unit
) {
    val methods = listOf("Bank Transfer (BCA)", "E-Wallet (GoPay)", "Credit Card")
    
    Column {
        methods.forEach { method ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(method) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (method == selectedMethod),
                    onClick = { onSelect(method) }
                )
                Text(
                    text = method,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(formatRupiah(amount), style = MaterialTheme.typography.bodyMedium)
    }
}
