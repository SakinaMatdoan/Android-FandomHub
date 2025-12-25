package com.kelompok1.fandomhub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FandomDiscoveryScreen(
    repository: FandomRepository,
    currentUserId: Int,
    onNavigateToDetail: (Int) -> Unit,
    onBack: () -> Unit
) {
    // Fetch Artists instead of Fandoms
    val allArtists = repository.getAllArtists().collectAsState(initial = emptyList())
    
    // Search & Sort State
    var searchQuery by remember { mutableStateOf("") }
    var sortByMostFollowers by remember { mutableStateOf(true) } // true = Most Followers, false = Newest

    // Refresh & Loading State
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState() // Persist scroll position

    LaunchedEffect(Unit) {
        delay(1000)
        isLoading = false
    }

    
    Scaffold(
        topBar = {
            StandardHeader(
                title = "Discover Fandoms",
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
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
            // Search & Filter Section
            var active by remember { mutableStateOf(false) }

            // Search Bar Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .zIndex(10f)
            ) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it 
                            active = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search Artists...") },
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
                                      else allArtists.value.filter { 
                                          it.fullName.contains(searchQuery, ignoreCase = true) 
                                      }.map { it.fullName }.distinct().take(5)
                                      
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
            }
                
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Sort Options
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    FilterChip(
                        selected = sortByMostFollowers,
                        onClick = { sortByMostFollowers = true },
                        label = { Text("Most Followers") }
                    )
                    FilterChip(
                        selected = !sortByMostFollowers,
                        onClick = { sortByMostFollowers = false },
                        label = { Text("Newest") }
                    )
                }
            }

            // List Content
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = listState
            ) {
                // Filter Logic
                val filteredList = allArtists.value.filter { 
                    it.fullName.contains(searchQuery, ignoreCase = true) || (it.bio?.contains(searchQuery, ignoreCase = true) ?: false)
                }
                
                val sortedList = if (!sortByMostFollowers) {
                    filteredList.sortedByDescending { it.id } // Newest
                } else {
                    filteredList // Default (Ideally sort by followers count, but that requires async fetch or pre-fetch. Keep basic for now or modify if needed)
                }

                items(sortedList) { artist ->
                     FandomDiscoveryItem(
                         artist = artist,
                         repository = repository,
                         currentUserId = currentUserId,
                         onClick = { onNavigateToDetail(artist.id) }
                     )
                }
            }
        }
        }
        }
        }
    }
}

@Composable
fun FandomDiscoveryItem(
    artist: UserEntity,
    repository: FandomRepository,
    currentUserId: Int,
    onClick: () -> Unit
) {
    val followerCount = repository.getFollowerCount(artist.id).collectAsState(initial = 0)
    val isFollowing = repository.isFollowing(currentUserId, artist.id).collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Profile Pic
            if (artist.profileImage != null) {
                coil.compose.AsyncImage(
                    model = artist.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        artist.fullName.take(1),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = artist.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    com.kelompok1.fandomhub.ui.components.ArtistBadge(visible = true)
                }
                Text(
                    text = "${followerCount.value} Followers",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (artist.bio != null) {
                    Text(
                        text = artist.bio,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Follow Button
            Button(
                onClick = { 
                    scope.launch {
                        repository.toggleFollow(currentUserId, artist.id, isFollowing.value)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing.value) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (isFollowing.value) "Following" else "Follow",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
