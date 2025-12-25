package com.kelompok1.fandomhub.ui.market

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.CartItemWithProduct
import com.kelompok1.fandomhub.ui.components.formatRupiah
import com.kelompok1.fandomhub.ui.navigation.Screen
import com.kelompok1.fandomhub.ui.components.StandardHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    repository: FandomRepository,
    navController: NavController,
    currentUserId: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Explicitly expecting CartItemWithProduct now
    val cartItemsState = repository.getCartItems(currentUserId).collectAsState(initial = emptyList<CartItemWithProduct>())
    val cartItems: List<CartItemWithProduct> = cartItemsState.value
    
    // productsMap no longer needed as we have the product in the item
    
    val totalAmount = cartItems.sumOf { item -> 
        (item.product.price * item.cartItem.quantity)
    }

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Shopping Cart",
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total", style = MaterialTheme.typography.labelLarge)
                            Text(
                                formatRupiah(totalAmount),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                navController.navigate(Screen.Checkout.route)
                            }
                        ) {
                            Text("Checkout")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your cart is empty", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = {
                            scope.launch { 
                                try {
                                    repository.addToCart(currentUserId, item.product.id, 1) 
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDecrement = {
                            scope.launch {
                                try {
                                    if (item.cartItem.quantity > 1) {
                                        repository.updateCartItem(item.cartItem.copy(quantity = item.cartItem.quantity - 1))
                                    } else {
                                        repository.removeCartItem(item.cartItem)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItemWithProduct,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                if (item.product.images.isNotEmpty()) {
                    AsyncImage(
                        model = item.product.images.first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(formatRupiah(item.product.price), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement) {
                     Icon(if(item.cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove, 
                          contentDescription = "Remove", 
                          tint = if(item.cartItem.quantity==1) MaterialTheme.colorScheme.error else LocalContentColor.current)
                }
                Text("${item.cartItem.quantity}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onIncrement) {
                     Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    }
}
