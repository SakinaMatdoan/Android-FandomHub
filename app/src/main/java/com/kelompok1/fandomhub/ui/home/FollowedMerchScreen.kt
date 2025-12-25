package com.kelompok1.fandomhub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.zIndex
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.ui.components.ProductItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedMerchScreen(
    repository: FandomRepository,
    currentUser: UserEntity,
    onNavigateToProduct: (Int) -> Unit,
    onBack: () -> Unit
) {
    val feedProducts = repository.getFeedProducts(currentUser.id).collectAsState(initial = emptyList())
    val followedArtists = repository.getFollowedArtists(currentUser.id).collectAsState(initial = emptyList())
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    var selectedArtistId by remember { mutableStateOf<Int?>(null) } // null = All
    var sortByMostSold by remember { mutableStateOf(true) } // true = Most Sold, false = Newest
    
    // Refresh & Loading State
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) } // Initial loading
    val scope = rememberCoroutineScope()
    
    // Simulate initial data load
    LaunchedEffect(Unit) {
        delay(1000)
        isLoading = false
    }

    
    Scaffold(
        topBar = {
            StandardHeader(
                title = "Merch from Your Artists",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(1500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                 var active by remember { mutableStateOf(false) }

                // Content Layer
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                Column(modifier = Modifier.fillMaxSize()) {
                     Spacer(modifier = Modifier.height(80.dp)) // Reserve space for Search Bar
                     
                     Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Artist Filter Chips
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedArtistId == null,
                                    onClick = { selectedArtistId = null },
                                    label = { Text("All") }
                                )
                            }
                            items(followedArtists.value) { artist ->
                                FilterChip(
                                    selected = selectedArtistId == artist.id,
                                    onClick = { selectedArtistId = artist.id },
                                    label = { Text(artist.fullName) }
                                )
                            }
                        }
                        
                         // Sort Options
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            FilterChip(
                                selected = sortByMostSold,
                                onClick = { sortByMostSold = true },
                                label = { Text("Best Selling") },
                                leadingIcon = if (sortByMostSold) {
                                    { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = !sortByMostSold,
                                onClick = { sortByMostSold = false },
                                label = { Text("Newest") },
                                 leadingIcon = if (!sortByMostSold) {
                                    { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                     }
        
                    // Product Grid
                    val filteredProducts = feedProducts.value.filter { product ->
                        val matchesArtist = selectedArtistId == null || product.artistId == selectedArtistId
                        val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) || 
                                           product.description.contains(searchQuery, ignoreCase = true)
                        matchesArtist && matchesSearch
                    }
                    
                    val sortedProducts = filteredProducts.sortedWith(
                        compareByDescending<com.kelompok1.fandomhub.data.local.ProductEntity> { it.stock > 0 }
                            .thenByDescending { 
                                if (sortByMostSold) it.soldCount else it.id 
                            }
                    )
    
                    if (sortedProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                if (searchQuery.isNotEmpty() || selectedArtistId != null) "No products match your filter." 
                                else "No products found from followed artists.",
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sortedProducts) { product ->
                                val ownerName = followedArtists.value.find { it.id == product.artistId }?.fullName
                                ProductItem(
                                    product = product,
                                    ownerName = ownerName,
                                    onClick = { onNavigateToProduct(product.id) }
                                )
                            }
                        }
                    }
                } // End Content Column
                }

                // Search Bar & Suggestions Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it 
                            active = true
                        },
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background), 
                        placeholder = { Text("Search products...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = ""; active = false }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
    
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    
                    // Suggestions Overlay
                    val suggestions = if (searchQuery.isBlank()) emptyList() 
                                      else feedProducts.value.filter { 
                                          it.name.contains(searchQuery, ignoreCase = true) 
                                      }.map { it.name }.distinct().take(5)
                                      
                    if (active && suggestions.isNotEmpty()) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                        ) {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(suggestions) { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchQuery = suggestion
                                                active = false 
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = suggestion, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            } // End Box
        }
    } // End Scaffold
} // End Screen
