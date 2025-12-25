package com.kelompok1.fandomhub.ui.home

import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kelompok1.fandomhub.ui.components.PostInteractionRow
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.ui.components.CommentBottomSheet
import com.kelompok1.fandomhub.ui.components.ArtistBadge
import com.kelompok1.fandomhub.ui.components.CreatePostDialog
import com.kelompok1.fandomhub.ui.components.AddProductDialog
import com.kelompok1.fandomhub.ui.components.PostItem
import com.kelompok1.fandomhub.utils.copyUriToInternalStorage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlinx.coroutines.delay
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.PostEntity
import com.kelompok1.fandomhub.data.local.ProductEntity
import com.kelompok1.fandomhub.data.local.UserEntity

import kotlinx.coroutines.launch
import java.util.Date
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.kelompok1.fandomhub.utils.DateUtils
import com.kelompok1.fandomhub.data.local.PostWithAuthor
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilterChip
import androidx.compose.ui.zIndex

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistDashboardScreen(
    repository: FandomRepository,
    artist: UserEntity,
    onNavigateToPost: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    
    // Sorting & Search State
    val allLikes = repository.getAllLikes().collectAsState(initial = emptyList())
    var sortByLikes by remember { mutableStateOf(false) }
    var merchSearchQuery by remember { mutableStateOf("") }
    var merchSortByMostSold by remember { mutableStateOf(true) }
    var merchSearchActive by remember { mutableStateOf(false) }
    
    val tabs = listOf("My Posts", "Community", "My Merch")
    
    val followerCount = repository.getFollowerCount(artist.id).collectAsState(initial = 0)

    Scaffold(
        topBar = {
            StandardHeader(title = "FandomHub")
        },
        floatingActionButton = {
            if (selectedTab == 0) { 
                FloatingActionButton(
                    onClick = {
                        showCreatePostDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Post")
                }
            }
        }
    ) { padding ->
        if (showCreatePostDialog) {
            CreatePostDialog(
                onDismiss = { showCreatePostDialog = false },
                title = if (selectedTab == 1) "Start a Community Thread" else "Create New Post",
                onConfirm = { content, imageUris ->
                    scope.launch {
                        val savedImagePaths = imageUris.mapNotNull { uriString ->
                            val uri = Uri.parse(uriString)
                            copyUriToInternalStorage(context, uri)
                        }
                        
                        val isThreadPost = selectedTab == 1

                        repository.createPost(
                            PostEntity(
                                authorId = artist.id,
                                artistId = artist.id, // Context is self
                                content = content,
                                images = savedImagePaths,
                                timestamp = System.currentTimeMillis(),
                                isThread = isThreadPost
                            )
                        )
                        Toast.makeText(context, if (isThreadPost) "Thread started!" else "Post created!", Toast.LENGTH_SHORT).show()
                        showCreatePostDialog = false
                    }
                }
            )
        }
        
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
             // Hoist Data Collection
             val artistPosts = repository.getPostsByAuthor(artist.id).collectAsState(initial = emptyList())
             val fanThreads = repository.getFanThreads(artist.id).collectAsState(initial = emptyList())
             val artistProducts = repository.getProductsByArtist(artist.id).collectAsState(initial = emptyList())
             
             LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Header
                item {
                    ArtistDashboardHeader(
                        artist = artist,
                        followerCount = followerCount.value
                    )
                }

                // Sticky Tabs
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            indicator = { tabPositions ->
                                if (selectedTab < tabPositions.size) {
                                    TabRowDefaults.Indicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }
                }

                // Feed Content
                when (selectedTab) {
                    0 -> {
                        // My Posts (Official)
                        // Sort Options
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                FilterChip(
                                    selected = !sortByLikes,
                                    onClick = { sortByLikes = false },
                                    label = { Text("Most Recent") },
                                    leadingIcon = if (!sortByLikes) {
                                        { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = sortByLikes,
                                    onClick = { sortByLikes = true },
                                    label = { Text("Most Likes") },
                                     leadingIcon = if (sortByLikes) {
                                        { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        
                        val posts = artistPosts.value
                        val sortedPosts = if (sortByLikes) {
                            posts.sortedByDescending { post -> 
                                allLikes.value.filter { it.postId == post.post.id }.size
                            }
                        } else {
                            posts.sortedByDescending { it.post.timestamp }
                        }
                        
                        if (sortedPosts.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("You haven't posted anything yet.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(sortedPosts) { postItem ->
                                 PostItem(
                                    post = postItem.post,
                                    author = postItem.author,
                                    currentUserId = artist.id,
                                    repository = repository,
                                    onCommentClick = { onNavigateToPost(postItem.post.id) },
                                    onPostClick = { onNavigateToPost(postItem.post.id) },
                                    artist = artist
                                )
                            }
                        }
                    }
                    1 -> {
                        // Community
                        // Sort Options
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                FilterChip(
                                    selected = !sortByLikes,
                                    onClick = { sortByLikes = false },
                                    label = { Text("Most Recent") },
                                    leadingIcon = if (!sortByLikes) {
                                        { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = sortByLikes,
                                    onClick = { sortByLikes = true },
                                    label = { Text("Most Likes") },
                                     leadingIcon = if (sortByLikes) {
                                        { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        
                         val threads = fanThreads.value
                         val sortedThreads = if (sortByLikes) {
                            threads.sortedByDescending { post -> 
                                allLikes.value.filter { it.postId == post.post.id }.size
                            }
                        } else {
                            threads.sortedByDescending { it.post.timestamp }
                        }

                         if (sortedThreads.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No community threads yet.", color = Color.Gray)
                                }
                            }
                         } else {
                             items(sortedThreads) { postItem ->
                                 PostItem(
                                    post = postItem.post,
                                    author = postItem.author,
                                    currentUserId = artist.id,
                                    repository = repository,
                                    onCommentClick = { onNavigateToPost(postItem.post.id) },
                                    onPostClick = { onNavigateToPost(postItem.post.id) },
                                    artist = artist
                                )
                            }
                         }
                    }
                    2 -> {
                        // Merch
                         val products = artistProducts.value
                         
                         // Search Bar
                         item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .zIndex(10f)
                            ) {
                                Column {
                                    OutlinedTextField(
                                        value = merchSearchQuery,
                                        onValueChange = { 
                                            merchSearchQuery = it 
                                            merchSearchActive = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Search products...") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        trailingIcon = if (merchSearchQuery.isNotEmpty()) {
                                            {
                                                IconButton(onClick = { merchSearchQuery = ""; merchSearchActive = false }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    
                                                }
                                            }
                                        } else null,
                                        singleLine = true,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                    
                                    // Suggestions Overlay
                                    val suggestions = if (merchSearchQuery.isBlank()) emptyList() 
                                                      else products.filter { 
                                                          it.name.contains(merchSearchQuery, ignoreCase = true) 
                                                      }.map { it.name }.distinct().take(5)
                                                      
                                    if (merchSearchActive && suggestions.isNotEmpty()) {
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
                                                                merchSearchQuery = suggestion
                                                                merchSearchActive = false 
                                                            }
                                                            .padding(16.dp),
                                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(text = suggestion, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                         }
                         
                         item {
                             // Sort Options
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                FilterChip(
                                    selected = merchSortByMostSold,
                                    onClick = { merchSortByMostSold = true },
                                    label = { Text("Best Selling") },
                                    leadingIcon = if (merchSortByMostSold) {
                                        { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = !merchSortByMostSold,
                                    onClick = { merchSortByMostSold = false },
                                    label = { Text("Newest") },
                                     leadingIcon = if (!merchSortByMostSold) {
                                        { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            } 
                         }

                         val filteredProducts = products.filter { it.name.contains(merchSearchQuery, ignoreCase = true) }
                    
                         // Sort Logic: Stock Priority > Criteria
                         val sortedProducts = filteredProducts.sortedWith(
                             compareByDescending<com.kelompok1.fandomhub.data.local.ProductEntity> { it.stock > 0 }
                                 .thenByDescending { 
                                     if (merchSortByMostSold) it.soldCount else it.id
                                 }
                         )

                         if (sortedProducts.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillParentMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(if (merchSearchQuery.isNotEmpty()) "No products found." else "No products yet. Add some!", color = Color.Gray)
                                }
                            }
                         } else {
                              val chunkedProducts = sortedProducts.chunked(2)
                              items(chunkedProducts) { rowItems ->
                                  Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                      rowItems.forEach { product ->
                                          Box(modifier = Modifier.weight(1f)) {
                                              com.kelompok1.fandomhub.ui.components.ProductItem(
                                                  product = product,
                                                  onClick = { },
                                                  onEdit = null // Disable editing as per request
                                              )
                                          }
                                      }
                                      if (rowItems.size < 2) {
                                          Spacer(modifier = Modifier.weight(1f))
                                      }
                                  }
                                  Spacer(modifier = Modifier.height(8.dp))
                              }
                         }
                    }
                }
             }
        }
    }
}

@Composable
fun ArtistDashboardHeader(
    artist: UserEntity,
    followerCount: Int
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // 1. Banner Image (Fixed Height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (artist.coverImage != null) {
                AsyncImage(
                    model = artist.coverImage,
                    contentDescription = "Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                 Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
            }
        }

        // 2. Info Section (Overlapping Banner)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 110.dp) // Cluster starts here
                .padding(horizontal = 16.dp)
        ) {
            // Profile Image (Circle)
            // It will overlap the banner bottom edge (150dp)
            if (artist.profileImage != null) {
                AsyncImage(
                    model = artist.profileImage,
                    contentDescription = "Artist Profile Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface) // Border effect
                        .padding(4.dp)
                        .clip(CircleShape), // Clip again for the image
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                     tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Name & Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = artist.fullName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                ArtistBadge(visible = true)
            }
            
            if (artist.bio != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Row (Followers only for now, maybe add more stats later)
            Text(
                "$followerCount Followers", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
