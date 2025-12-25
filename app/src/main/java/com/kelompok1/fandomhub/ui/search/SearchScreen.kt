package com.kelompok1.fandomhub.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity 
import com.kelompok1.fandomhub.data.local.PostWithAuthor
import com.kelompok1.fandomhub.data.local.ProductEntity
import com.kelompok1.fandomhub.ui.components.PostItem
import com.kelompok1.fandomhub.utils.DateUtils
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: FandomRepository,
    currentUserId: Int,
    onBack: () -> Unit = {}, 
    onNavigateToFandom: (Int) -> Unit = {}, // Navigate to Artist Detail
    onNavigateToPost: (Int) -> Unit = {},
    onNavigateToProduct: (Int) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }
    
    // Data States
    val allArtists = repository.getAllArtists().collectAsState(initial = emptyList())
    val allPosts = repository.getAllPosts().collectAsState(initial = emptyList())
    val allProducts = repository.getAllProducts().collectAsState(initial = emptyList())
    
    // Counting States (For Sorting)
    var allFollows by remember { mutableStateOf<List<com.kelompok1.fandomhub.data.local.FollowEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        allFollows = repository.getAllFollows()
    }

    // Filtered & Sorted Results
    val results = remember(query, selectedFilter, allArtists.value, allPosts.value, allProducts.value, allFollows) {
        if (query.isBlank()) return@remember SearchResults()

        val q = query.trim()
        
        // 1. Artists (Sort by Follower Count DESC)
        // Renamed from Fandoms
        val artists = if (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ARTIST) {
            allArtists.value.filter { 
                it.fullName.contains(q, ignoreCase = true) || 
                it.username.contains(q, ignoreCase = true)
            }
                .sortedByDescending { artist -> 
                    allFollows.count { it.artistId == artist.id }
                }
        } else emptyList()

        // 2. Posts
        val posts = if (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.POST) {
            allPosts.value.filter { postWithAuthor: PostWithAuthor ->
                val artist = allArtists.value.find { it.id == postWithAuthor.post.artistId }
                val artistName = artist?.fullName ?: ""
                
                postWithAuthor.post.content.contains(q, ignoreCase = true) || 
                artistName.contains(q, ignoreCase = true)
            }
        } else emptyList<PostWithAuthor>()

        // 3. Products (Sort by Sold Count DESC)
        val products = if (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.MERCH) {
            allProducts.value.filter { product ->
                val artist = allArtists.value.find { it.id == product.artistId }
                val artistName = artist?.fullName ?: ""
                
                product.name.contains(q, ignoreCase = true) || 
                artistName.contains(q, ignoreCase = true)
            }
                .sortedByDescending { it.soldCount }
        } else emptyList()

        SearchResults(artists, posts, products)
    }

    // Limit States (Pagination)
    var artistLimit by remember { mutableIntStateOf(5) }
    var merchLimit by remember { mutableIntStateOf(5) }
    var postLimit by remember { mutableIntStateOf(5) }

    // Reset limits when query or filter changes
    LaunchedEffect(query, selectedFilter) {
        artistLimit = 5
        merchLimit = 5
        postLimit = 5
    }

    Scaffold(
        topBar = {
            Column {
                // Search Bar
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(16.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {


                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search Artist, Post, or Merch...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                
                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (query.isBlank()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Start searching...", color = Color.Gray)
                        }
                    }
                }
            } else {
                // Artist Results
                if (results.artists.isNotEmpty()) {
                    val showAll = selectedFilter == SearchFilter.ARTIST
                    val displayItems = if (showAll) results.artists else results.artists.take(artistLimit)
                    
                    item { SectionHeader("Artists") }
                    items(displayItems) { artist ->
                        ArtistSearchResultItem(artist, repository, currentUserId, onNavigateToFandom)
                    }
                    if (!showAll && results.artists.size > artistLimit) {
                         item {
                             LoadMoreButton { artistLimit += 5 }
                         }
                    }
                }

                // Product Results
                if (results.products.isNotEmpty()) {
                    val showAll = selectedFilter == SearchFilter.MERCH
                    val displayItems = if (showAll) results.products else results.products.take(merchLimit)

                    item { SectionHeader("Merch (Best Sellers)") }
                    items(displayItems) { product ->
                        val artist = allArtists.value.find { it.id == product.artistId }
                        val artistName = artist?.fullName ?: "Unknown Artist"
                        
                        ProductSearchResultItem(product, artistName, onNavigateToProduct)
                    }
                    if (!showAll && results.products.size > merchLimit) {
                         item {
                             LoadMoreButton { merchLimit += 5 }
                         }
                    }
                }

                // Post Results
                if (results.posts.isNotEmpty()) {
                    val showAll = selectedFilter == SearchFilter.POST
                    val displayItems = if (showAll) results.posts else results.posts.take(postLimit)

                    item { SectionHeader("Posts") }
                    items(displayItems) { post ->
                        val artist = allArtists.value.find { it.id == post.post.artistId }
                        val artistName = artist?.fullName ?: ""
                        
                        // Show label only for Fan Posts (Threads)
                        if (post.post.isThread && artistName.isNotEmpty()) {
                            Text(
                                text = "from $artistName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                            )
                        }

                        val isFollowing = allFollows.any { it.followerId == currentUserId && it.artistId == post.post.artistId }
                        PostItem(
                            post = post.post,
                            author = post.author,
                            currentUserId = currentUserId,
                            repository = repository,
                            onPostClick = { onNavigateToPost(post.post.id) },
                            onCommentClick = { onNavigateToPost(post.post.id) },
                            isFollowing = isFollowing
                        )
                    }
                    if (!showAll && results.posts.size > postLimit) {
                         item {
                             LoadMoreButton { postLimit += 5 }
                         }
                    }
                }
                
                if (results.isEmpty()) {
                     item {
                        Box(modifier = Modifier.fillParentMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results found.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadMoreButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick) {
            Text("See More", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ArtistSearchResultItem(
    artist: UserEntity, 
    repository: FandomRepository,
    currentUserId: Int,
    onClick: (Int) -> Unit
) {
    val followerCount = repository.getFollowerCount(artist.id).collectAsState(initial = 0)
    val isFollowing = repository.isFollowing(currentUserId, artist.id).collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    
    Card(
        onClick = { onClick(artist.id) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Pic
            if (artist.profileImage != null) {
                AsyncImage(
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
                    contentAlignment = Alignment.Center
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artist.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    com.kelompok1.fandomhub.ui.components.ArtistBadge(visible = true)
                }
                Text(
                    text = "${followerCount.value} Followers",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (artist.bio != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist.bio,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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


@Composable
fun ProductSearchResultItem(
    product: ProductEntity, 
    artistName: String, 
    onClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick(product.id) },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = if (product.images.isNotEmpty()) product.images.first() else null,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "from $artistName", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    com.kelompok1.fandomhub.ui.components.ArtistBadge(visible = true)
                }
                Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Rp ${java.text.NumberFormat.getIntegerInstance().format(product.price)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD700))
                    Text(" ${product.rating} | Terjual ${product.soldCount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

enum class SearchFilter(val label: String) {
    ALL("All"),
    ARTIST("Artist"),
    MERCH("Merch"),
    POST("Post")
}

data class SearchResults(
    val artists: List<UserEntity> = emptyList(),
    val posts: List<PostWithAuthor> = emptyList(),
    val products: List<ProductEntity> = emptyList()
) {
    fun isEmpty() = artists.isEmpty() && posts.isEmpty() && products.isEmpty()
}
