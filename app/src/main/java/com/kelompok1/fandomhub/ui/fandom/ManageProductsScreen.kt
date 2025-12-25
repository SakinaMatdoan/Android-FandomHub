package com.kelompok1.fandomhub.ui.fandom

import android.widget.Toast
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.utils.copyUriToInternalStorage
import kotlinx.coroutines.launch

@Composable
fun ManageProductsScreen(
    repository: FandomRepository,
    artistId: Int,
    onBack: () -> Unit,
    onNavigateToProductForm: (Int?) -> Unit,
    onNavigateToMonitoring: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Artist check
    var artist by remember { mutableStateOf<UserEntity?>(null) }
    
    // Fetch Products (By Artist)
    val products = if (artist != null) repository.getProductsByArtist(artist!!.id).collectAsState(initial = emptyList()) else remember { mutableStateOf(emptyList()) }
    
    LaunchedEffect(artistId) {
        val a = repository.getUserById(artistId)
        artist = a
    }
    
    Scaffold(
        topBar = {
            StandardHeader(
                title = "Manage Merchandise", 
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onNavigateToProductForm(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Product")
                    }
                }
            )
        }
    ) { padding ->
        if (artist == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (products.value.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No products yet. Click + to add.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(16.dp)
                ) {
                    items(products.value) { product ->
                         Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                            onClick = { onNavigateToMonitoring(product.id) }
                         ) {
                             Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                 AsyncImage(
                                     model = product.images.firstOrNull(),
                                     contentDescription = null,
                                     modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray),
                                     contentScale = ContentScale.Crop
                                 )
                                 Spacer(modifier = Modifier.width(12.dp))
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                     Text("Rp ${product.price.toInt()} • Stock: ${product.stock}", style = MaterialTheme.typography.bodySmall)
                                 }
                                 Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                             }
                         }
                    }
                }
            }
        }
    }
}
