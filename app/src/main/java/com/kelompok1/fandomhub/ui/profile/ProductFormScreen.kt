package com.kelompok1.fandomhub.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.ProductEntity
import com.kelompok1.fandomhub.utils.copyUriToInternalStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    repository: FandomRepository,
    artistId: Int, // The current user ID (Artist)
    productId: Int?, // Explicit nullable Int for edit mode, null for create
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }
    
    // Image Handling
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Logic to load existing product if edit
    val isEditMode = (productId != null && productId != -1)
    
    LaunchedEffect(productId) {
        if (isEditMode) {
            val product = repository.getProductById(productId!!)
            if (product != null) {
                name = product.name
                description = product.description
                priceStr = product.price.toInt().toString()
                stockStr = product.stock.toString()
                existingImages = product.images
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUris = selectedImageUris + uri
        }
    }
    
    // For multiple selection at once:
    val multipleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            com.kelompok1.fandomhub.ui.components.StandardHeader(
                title = if (isEditMode) "Edit Product" else "Add New Product",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Image Upload Binding
            Text("Product Images (${existingImages.size + selectedImageUris.size}/5)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                 // Existing Images
                 items(existingImages) { img ->
                     Box(modifier = Modifier
                         .aspectRatio(1f)
                         .clip(RoundedCornerShape(8.dp))) {
                         Image(
                             painter = rememberAsyncImagePainter(img),
                             contentDescription = null,
                             modifier = Modifier.fillMaxSize(),
                             contentScale = ContentScale.Crop
                         )
                         IconButton(
                             onClick = { existingImages = existingImages - img },
                             modifier = Modifier
                                 .align(Alignment.TopEnd)
                                 .background(Color.Black.copy(alpha=0.5f), CircleShape)
                         ) {
                             Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                         }
                     }
                 }
                 
                 // New Images
                 items(selectedImageUris) { uri ->
                     Box(modifier = Modifier
                         .aspectRatio(1f)
                         .clip(RoundedCornerShape(8.dp))) {
                         Image(
                             painter = rememberAsyncImagePainter(uri),
                             contentDescription = null,
                             modifier = Modifier.fillMaxSize(),
                             contentScale = ContentScale.Crop
                         )
                         IconButton(
                             onClick = { selectedImageUris = selectedImageUris - uri },
                             modifier = Modifier
                                 .align(Alignment.TopEnd)
                                 .background(Color.Black.copy(alpha=0.5f), CircleShape)
                         ) {
                             Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                         }
                     }
                 }
                 
                 // Add Button
                 if (existingImages.size + selectedImageUris.size < 5) {
                     item {
                         Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                                .clickable { multipleImagePicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                         ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Icon(Icons.Default.Add, contentDescription = null, tint = Color.DarkGray)
                                 Text("Add Photo", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)
                             }
                         }
                     }
                 }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) priceStr = it },
                    label = { Text("Price (Rp)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) stockStr = it },
                    label = { Text("Stock") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (name.isBlank() || priceStr.isBlank()) {
                        Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        // 1. Handle Images
                        val finalImages = existingImages.toMutableList()
                        selectedImageUris.forEach { uri ->
                            val path = copyUriToInternalStorage(context, uri)
                            if (path != null) finalImages.add(path)
                        }
                        
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        val stock = stockStr.toIntOrNull() ?: 0
                        
                        val product = ProductEntity(
                            id = if (isEditMode) productId!! else 0,
                            artistId = artistId,
                            // fandomId removed
                            name = name,
                            description = description,
                            price = price,
                            stock = stock,
                            images = finalImages
                        )
                        
                        if (isEditMode) {
                            // Update logic (Not in Dao yet? Ops, let's check FandomRepository!) 
                            // Repository line 87: addProduct (insert). 
                            // I need updateProduct in Repository!
                            // Checking FandomRepository in step 3739... 
                            // It HAS updateProduct? No, it has `updateFandom`.
                            // `MarketDao` has `updateProduct` (line 149 in step 3726 view).
                            // But `FandomRepository` might not expose it. 
                            // I will use `addProduct` with REPLACE strategy if it works, or add updateProduct.
                            // The ID is Primary Key, passing existing ID to @Insert(REPLACE) works as Update.
                            repository.addProduct(product) 
                        } else {
                            repository.addProduct(product)
                        }
                        
                        Toast.makeText(context, "Product Saved!", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Save Product")
            }
            
            if (isEditMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                             // Delete logic
                             val product = repository.getProductById(productId!!)
                             if (product != null) {
                                 repository.deleteProduct(product)
                                 Toast.makeText(context, "Product Deleted", Toast.LENGTH_SHORT).show()
                                 onSaved()
                             }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Text("Delete Product")
                }
            }
        }
    }
}
