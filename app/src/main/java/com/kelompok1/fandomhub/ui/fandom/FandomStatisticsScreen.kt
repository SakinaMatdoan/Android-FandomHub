package com.kelompok1.fandomhub.ui.fandom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.DailyStat
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.FandomStats
import com.kelompok1.fandomhub.ui.components.StandardHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FandomStatisticsScreen(
    artistId: Int, // Added artistId
    repository: FandomRepository,
    onNavigateToFollowers: (Int) -> Unit,
    onBack: () -> Unit
) {
    val recentFollowersList by repository.getRecentFollowers(artistId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<FandomStats?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Followers, 1: Activity, 2: Engagement
    var searchQuery by remember { mutableStateOf("") } // Search State
    val context = LocalContext.current
    
    // Filter Followers locally
    val filteredFollowers = remember(recentFollowersList, searchQuery) {
        if (searchQuery.isBlank()) recentFollowersList
        else recentFollowersList.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) || 
            it.username.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    LaunchedEffect(artistId) {
        stats = repository.getFandomStatistics(artistId, 30) // Last 30 days
    }

    Scaffold(
        topBar = {
            StandardHeader(
                title = "Statistics",
                onBack = onBack
            )
        }
    ) { padding ->
        if (stats == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 1. Summary Cards
                item {
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatisticCard("Followers", stats!!.totalFollowers.toString(), Icons.Default.Group, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onClick = { onNavigateToFollowers(artistId) })
                            StatisticCard("Posts", stats!!.totalPosts.toString(), Icons.Default.Analytics, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatisticCard("Likes", stats!!.totalLikes.toString(), Icons.Default.Favorite, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                            StatisticCard("Comments", stats!!.totalComments.toString(), Icons.Default.Message, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // New Stats
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                             val revenueStr = "Rp " + java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(stats!!.totalRevenue)
                             val subRevenue = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(stats!!.subscriptionRevenue)
                             val orderRevenue = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(stats!!.orderRevenue)
                             
                             StatisticCard(
                                "Revenue", 
                                revenueStr, 
                                Icons.Default.AttachMoney, 
                                Color(0xFF4CAF50), 
                                Modifier.weight(1f),
                                subtitle = "Subs: Rp $subRevenue | Orders: Rp $orderRevenue"
                            )
                        }
                         Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                             val subRevenue = java.text.NumberFormat.getIntegerInstance(java.util.Locale("id", "ID")).format(stats!!.subscriptionRevenue)
                             StatisticCard(
                                 "Active Subscribers",
                                 stats!!.totalSubscribers.toString(),
                                 Icons.Default.Star,
                                 Color(0xFFFFC107),
                                 Modifier.weight(1f),
                                 subtitle = "Income: Rp $subRevenue"
                             )
                             StatisticCard("Orders", stats!!.totalOrders.toString(), Icons.Default.ShoppingCart, Color(0xFF2196F3), Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Chart Section
                    Text(
                        "Growth & Activity (Last 30 Days)", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                    
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Followers") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Posts") })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Engagement") })
                        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Revenue") })
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), MaterialTheme.shapes.medium)
                            .padding(16.dp)
                    ) {
                        when (selectedTab) {
                             0 -> LineChart(data = stats!!.followerGrowth, color = MaterialTheme.colorScheme.primary)
                             1 -> BarChart(data = stats!!.postActivity, color = MaterialTheme.colorScheme.secondary)
                             2 -> LineChart(data = stats!!.engagementActivity, color = MaterialTheme.colorScheme.tertiary)
                             3 -> LineChart(data = stats!!.revenueGrowth, color = Color(0xFF4CAF50))
                        }
                    }
                }
                
                // 3. Detailed Stats
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Top Selling Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (stats!!.topProducts.isEmpty()) {
                             Text("No sales yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        } else {
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(stats!!.topProducts) { product ->
                                    Card(
                                        modifier = Modifier.width(160.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            if (product.images.isNotEmpty()) {
                                                coil.compose.AsyncImage(
                                                    model = product.images.first(),
                                                    contentDescription = null,
                                                    modifier = Modifier.height(100.dp).fillMaxWidth().background(Color.LightGray),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(product.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                            Text("${product.soldCount} Sold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Top Superfans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (stats!!.topFans.isEmpty()) {
                             Text("No interactions yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                                stats!!.topFans.forEach { fan ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            modifier = Modifier.size(60.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            if (fan.user.profileImage != null) {
                                                coil.compose.AsyncImage(model = fan.user.profileImage, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                            } else {
                                                Box(contentAlignment = Alignment.Center) { Text(fan.user.fullName.take(1)) }
                                            }
                                        }
                                        Text(fan.user.fullName.take(10), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        Text("${fan.interactionCount} Acts", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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


@Composable
fun StatisticCard(
    title: String, 
    value: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if(onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LineChart(data: List<DailyStat>, color: Color) {
    if (data.isEmpty()) return
    
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = Color.Gray.copy(alpha = 0.5f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxVal = data.maxOfOrNull { it.count }?.toFloat() ?: 0f
        val displayMax = if (maxVal == 0f) 5f else maxVal // Default scale if 0
        
        val paddingLeft = 60f // For Y-labels
        val paddingBottom = 40f // For X-labels
        
        val graphWidth = size.width - paddingLeft
        val graphHeight = size.height - paddingBottom
        val xStep = graphWidth / (data.size - 1).coerceAtLeast(1)
        
        // Draw Grid & Y-Labels (0, 50%, 100%)
        val paint = android.graphics.Paint().apply {
            this.color = textColor
            textSize = 24f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        listOf(0f, 0.5f, 1f).forEach { ratio ->
            val y = graphHeight - (graphHeight * ratio)
            val value = (displayMax * ratio).toInt()
            
            // Grid Line
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            // Label
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                paddingLeft - 10f,
                y + 10f, // vertical center alignment approx
                paint
            )
        }
        
        // Draw Line Path
        val path = Path()
        data.forEachIndexed { index, stat ->
            val x = paddingLeft + (index * xStep)
            val normalizedY = graphHeight - (stat.count / displayMax * graphHeight)
            
            if (index == 0) path.moveTo(x, normalizedY)
            else path.lineTo(x, normalizedY)
            
            // Draw Points
            drawCircle(color = color, radius = 4f, center = Offset(x, normalizedY))
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f)
        )
        
        // Draw X-Axis Labels (Sparse: First, Middle, Last)
        val xLabelPaint = android.graphics.Paint().apply {
            this.color = textColor
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        if (data.size > 1) {
             val indices = listOf(0, data.size / 2, data.size - 1)
             indices.forEach { i ->
                 val x = paddingLeft + (i * xStep)
                 drawContext.canvas.nativeCanvas.drawText(data[i].date, x, size.height, xLabelPaint)
             }
        }
    }
}

@Composable
fun BarChart(data: List<DailyStat>, color: Color) {
    if (data.isEmpty()) return
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = Color.Gray.copy(alpha = 0.5f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxVal = data.maxOfOrNull { it.count }?.toFloat() ?: 0f
        val displayMax = if (maxVal == 0f) 5f else maxVal
        
        val paddingLeft = 60f
        val paddingBottom = 40f
        
        val graphWidth = size.width - paddingLeft
        val graphHeight = size.height - paddingBottom
        val step = graphWidth / data.size
        val barWidth = step * 0.6f
        
        // Draw Grid & Y-Labels
        val paint = android.graphics.Paint().apply {
            this.color = textColor
            textSize = 24f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        listOf(0f, 0.5f, 1f).forEach { ratio ->
            val y = graphHeight - (graphHeight * ratio)
            val value = (displayMax * ratio).toInt()
            
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                paddingLeft - 10f,
                y + 10f,
                paint
            )
        }
        
        // Bars
        data.forEachIndexed { index, stat ->
            val x = paddingLeft + (index * step) + (step - barWidth) / 2
            val barHeight = (stat.count / displayMax * graphHeight)
            
            drawRect(
                color = color,
                topLeft = Offset(x, graphHeight - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
        
        // X-Axis Labels
        val xLabelPaint = android.graphics.Paint().apply {
            this.color = textColor
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
        }
         if (data.size > 1) {
             val indices = listOf(0, data.size / 2, data.size - 1)
             indices.forEach { i ->
                 val x = paddingLeft + (i * step) + step/2
                 drawContext.canvas.nativeCanvas.drawText(data[i].date, x, size.height, xLabelPaint)
             }
        }
    }
}
