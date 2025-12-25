package com.kelompok1.fandomhub.ui.market

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.ProductEntity
import com.kelompok1.fandomhub.data.local.ReviewReplyEntity
import com.kelompok1.fandomhub.data.local.ReviewWithReplies
import com.kelompok1.fandomhub.ui.components.formatRupiah
import com.kelompok1.fandomhub.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    productId: Int,
    repository: FandomRepository,
    navController: NavController,
    currentUserId: Int
) {
    var productState by remember { mutableStateOf<ProductEntity?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Fetch Product
    LaunchedEffect(productId) {
        productState = repository.getProductById(productId)
    }

    // Fetch Reviews (With Replies)
    val reviewsState = repository.getReviewsByProduct(productId).collectAsState(initial = emptyList())
    val reviews = reviewsState.value
    
    // Pagination State
    var visibleReviewsCount by remember { mutableStateOf(5) }

    // Check Follow Status
    val isFollowingState = produceState(initialValue = false, key1 = productState) {
        if (productState != null) {
            // Directly check following status using artistId as "artistId"
            repository.isFollowing(currentUserId, productState!!.artistId).collect { value = it }
        }
    }
    val isFollowing = isFollowingState.value

    if (productState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val product = productState!!

        Scaffold(
            topBar = {
                // ... (TopBar code skipped) ...
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
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Text(
                            "Product Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        
                        IconButton(
                            onClick = { navController.navigate(Screen.Cart.route) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                modifier = Modifier.size(20.dp)
                            )
                        }


                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            var showReportDialog by remember { mutableStateOf(false) }
                            val context = LocalContext.current
                            
                        if (showReportDialog) {
                            com.kelompok1.fandomhub.ui.components.ReportDialog(
                                onDismiss = { showReportDialog = false },
                                onSubmit = { reason, description -> 
                                    scope.launch {
                                        val report = com.kelompok1.fandomhub.data.local.ReportEntity(
                                            reporterId = currentUserId,
                                            reportedId = product.artistId,
                                            type = "PRODUCT",
                                            referenceId = product.id,
                                            reason = reason,
                                            description = description,
                                            contentSnapshot = "Product: ${product.name}"
                                        )
                                        val success = repository.reportUser(report)
                                        showReportDialog = false
                                        if (success) {
                                            Toast.makeText(context, "Product reported.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Already reported.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                            
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = { 
                                        showMenu = false 
                                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out this product: https://fandomhub.com/product/${product.id}")
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }
                                )
                                if (productState?.artistId != currentUserId) {
                                    DropdownMenuItem(
                                        text = { Text("Report") },
                                        onClick = { 
                                            showMenu = false 
                                            if (isFollowing) {
                                                showReportDialog = true
                                            } else {
                                                Toast.makeText(context, "You must follow the fandom to report products.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    val isOutOfStock = product.stock <= 0
                    
                    Button(
                        onClick = {
                            if (!isFollowing) {
                                Toast.makeText(context, "You must follow the fandom to buy items.", Toast.LENGTH_SHORT).show()
                            } else if (!isOutOfStock) {
                                scope.launch {
                                    try {
                                        repository.addToCart(currentUserId, product.id)
                                        Toast.makeText(context, "Added to Cart!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isOutOfStock, // Keep enabled if stock present, check follow on click for better UX feedback? or disable? User wants restriction. Better to allow click and tell them WHY.
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOutOfStock) Color.Gray else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isOutOfStock) "Sold Out" else "Add to Cart")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = {
                             if (!isFollowing) {
                                Toast.makeText(context, "You must follow the fandom to buy items.", Toast.LENGTH_SHORT).show()
                             } else if (!isOutOfStock) {
                                scope.launch {
                                    repository.addToCart(currentUserId, product.id)
                                    navController.navigate(Screen.Cart.route)
                                }
                            }
                        },
                        enabled = !isOutOfStock,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOutOfStock) Color.LightGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isOutOfStock) "Sold Out" else "Buy Now")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Product Image Slider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.LightGray)
                ) {
                    if (product.images.isNotEmpty()) {
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { product.images.size })
                        
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = product.images[page],
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Indicators
                        if (product.images.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(product.images.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             Text("No Image")
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Price & Name
                    Text(
                        text = formatRupiah(product.price),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stats
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                        Text("${product.rating} (${reviews.size} reviews)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Sold ${product.soldCount}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Stock ${product.stock}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Seller Info & Chat
                    val sellerState = produceState<com.kelompok1.fandomhub.data.local.UserEntity?>(initialValue = null, key1 = product.artistId) {
                        value = repository.getUserById(product.artistId)
                    }

                    if (sellerState.value != null) {
                        val seller = sellerState.value!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = seller.profileImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(seller.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Seller", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            Button(
                                onClick = { 
                                    navController.navigate(Screen.ChatDetail.createRoute(seller.id, "MARKET")) 
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat")
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                    
                    // Description
                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Reviews Section
                    Text("Reviews (${reviews.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (reviews.isEmpty()) {
                        Text("No reviews yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        reviews.take(visibleReviewsCount).forEach { reviewItem ->
                            ReviewItem(
                                reviewWithReplies = reviewItem,
                                currentUserId = currentUserId,
                                productArtistId = product.artistId,
                                onReply = { content ->
                                    scope.launch {
                                        repository.addReviewReply(
                                            ReviewReplyEntity(
                                                reviewId = reviewItem.review.id,
                                                userId = currentUserId,
                                                content = content,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                        Toast.makeText(context, "Reply sent!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onReport = { reason, description -> 
                                    scope.launch {
                                        val report = com.kelompok1.fandomhub.data.local.ReportEntity(
                                            reporterId = currentUserId,
                                            reportedId = reviewItem.review.userId,
                                            type = "COMMENT", // Treating Review as Comment for now or use "REVIEW"
                                            referenceId = reviewItem.review.id,
                                            reason = reason,
                                            description = description,
                                            contentSnapshot = reviewItem.review.comment
                                        )
                                        val success = repository.reportUser(report)
                                        if (success) {
                                            Toast.makeText(context, "Review reported.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Already reported.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        
                        if (reviews.size > visibleReviewsCount) {
                            TextButton(
                                onClick = { visibleReviewsCount += 5 },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Load More Reviews")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    reviewWithReplies: ReviewWithReplies,
    currentUserId: Int,
    productArtistId: Int,
    onReply: (String) -> Unit,
    onReport: (String, String) -> Unit // Added callback
) {
    var expanded by remember { mutableStateOf(false) }
    var isReplying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val review = reviewWithReplies.review
    val maxChars = 150

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        imageVector = if (i < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    com.kelompok1.fandomhub.utils.DateUtils.formatTimestamp(review.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.weight(1f))

                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    var showReportDialog by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    if (showReportDialog) {
                         com.kelompok1.fandomhub.ui.components.ReportDialog(
                            onDismiss = { showReportDialog = false },
                            onSubmit = { reason, description -> 
                                onReport(reason, description)
                                showReportDialog = false
                            }
                        )
                    }

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                         DropdownMenuItem(
                            text = { Text("Report") },
                            onClick = { 
                                showMenu = false 
                                showReportDialog = true
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Content with Read More
            val showReadMore = review.comment.length > maxChars
            val displayComment = if (showReadMore && !expanded) {
                review.comment.take(maxChars) + "..."
            } else {
                review.comment
            }
            
            Text(displayComment, style = MaterialTheme.typography.bodyMedium)
            
            if (showReadMore) {
                Text(
                    text = if (expanded) "Show Less" else "Read More",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { expanded = !expanded }.padding(top = 4.dp)
                )
            }
            
            // Replies Section
            if (reviewWithReplies.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                reviewWithReplies.replies.forEach { reply ->
                    Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                        Divider(
                            modifier = Modifier.width(2.dp).height(24.dp).background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Seller Response", 
                                style = MaterialTheme.typography.labelSmall, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(reply.content, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Reply Action (Only Artist/Seller of this product can reply)
            if (currentUserId == productArtistId && !reviewWithReplies.replies.any { it.userId == currentUserId }) {
                if (isReplying) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Write a reply...") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isReplying = false }) { Text("Cancel") }
                        Button(
                            onClick = { 
                                if (replyText.isNotBlank()) {
                                    onReply(replyText)
                                    replyText = ""
                                    isReplying = false
                                }
                            },
                            enabled = replyText.isNotBlank()
                        ) { Text("Send") }
                    }
                } else {
                    TextButton(onClick = { isReplying = true }) {
                        Text("Reply")
                    }
                }
            }
        }
    }
}
