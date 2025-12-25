package com.kelompok1.fandomhub.ui.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
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
import coil.compose.rememberAsyncImagePainter
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.OrderEntity
import com.kelompok1.fandomhub.data.local.ProductEntity
import com.kelompok1.fandomhub.ui.components.formatRupiah
import com.kelompok1.fandomhub.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSalesScreen(
    repository: FandomRepository,
    currentUserId: Int,
    onBack: () -> Unit,
    onNavigateToProductForm: (Int?) -> Unit 
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Incoming Orders", "My Products")
    
    Scaffold(
        topBar = {
            com.kelompok1.fandomhub.ui.components.StandardHeader(
                title = "Artist Dashboard",
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { onNavigateToProductForm(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
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
            
            when(selectedTab) {
                0 -> IncomingOrdersTab(repository, currentUserId)
                1 -> MyProductsTab(repository, currentUserId, onEdit = onNavigateToProductForm)
            }
        }
    }
}

@Composable
fun IncomingOrdersTab(repository: FandomRepository, artistId: Int) {
    val ordersState = repository.getOrdersByArtist(artistId).collectAsState(initial = emptyList())
    val orders = ordersState.value
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No incoming orders yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(orders) { order ->
                ArtistOrderCard(
                    order = order,
                    onUpdateStatus = { newStatus ->
                        scope.launch {
                            repository.updateOrderStatus(order.id, newStatus)
                            Toast.makeText(context, "Order updated to $newStatus", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ArtistOrderCard(order: OrderEntity, onUpdateStatus: (String) -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Order #${order.id}", fontWeight = FontWeight.Bold)
                Text(DateUtils.formatTimestamp(order.timestamp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Total: ${formatRupiah(order.totalAmount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: ${order.status}")
            Text("Ship to: ${order.shippingAddress}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (order.status == "PENDING") {
                    Button(onClick = { onUpdateStatus("PROCESSED") }) {
                        Text("Process Order")
                    }
                } else if (order.status == "PROCESSED") {
                    Button(onClick = { onUpdateStatus("SHIPPED") }) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ship Order")
                    }
                } else if (order.status == "SHIPPED") {
                    OutlinedButton(onClick = { /* Check Receipt? */ }, enabled = false) {
                        Text("Waiting Delivery")
                    }
                }
            }
        }
    }
}

@Composable
fun MyProductsTab(repository: FandomRepository, artistId: Int, onEdit: (Int) -> Unit) {
    val productsState = repository.getProductsByArtist(artistId).collectAsState(initial = emptyList())
    val products = productsState.value
    
    if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("You haven't added any products yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, bottom=80.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(products) { product ->
                ArtistProductCard(product, onEdit = { onEdit(product.id) })
            }
        }
    }
}

@Composable
fun ArtistProductCard(product: ProductEntity, onEdit: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val image = product.images.firstOrNull()
            if (image != null) {
                Image(
                    painter = rememberAsyncImagePainter(image),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(60.dp).background(Color.Gray))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text("Stock: ${product.stock} | Sold: ${product.soldCount}", style = MaterialTheme.typography.bodySmall)
                Text(formatRupiah(product.price), color = MaterialTheme.colorScheme.primary)
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}
