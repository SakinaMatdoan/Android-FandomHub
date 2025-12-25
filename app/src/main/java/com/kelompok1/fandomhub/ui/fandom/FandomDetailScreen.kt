 package com.kelompok1.fandomhub.ui.fandom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.kelompok1.fandomhub.ui.components.ExpandableText
import com.kelompok1.fandomhub.ui.components.ArtistBadge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.utils.DateUtils
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.saveable.rememberSaveable

import kotlinx.coroutines.launch
import com.kelompok1.fandomhub.ui.components.CreatePostDialog
import com.kelompok1.fandomhub.utils.copyUriToInternalStorage
import com.kelompok1.fandomhub.data.local.PostEntity
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.kelompok1.fandomhub.ui.components.PostItem
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import com.kelompok1.fandomhub.ui.components.StandardHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FandomDetailScreen(
    repository: FandomRepository,
    artistId: Int, // Renamed from fandomId
    currentUserId: Int,
    onBack: () -> Unit,
    onNavigateToPost: (Int) -> Unit,
    onNavigateToProduct: (Int) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToChat: (Int) -> Unit,
    onNavigateToCheckout: (Int) -> Unit
) {
    var artist by remember { mutableStateOf<UserEntity?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Official", "Community", "Market")
    
    var showCreateThreadDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Follow State
    val isFollowingState = repository.isFollowing(currentUserId, artistId).collectAsState(initial = false)
    val followerCount = repository.getFollowerCount(artistId).collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState() // Persist scroll position
    val newArrivalsState = rememberLazyListState() // Persist horizontal scroll position

    LaunchedEffect(artistId) {
        artist = repository.getUserById(artistId)
    }

    // Check Block Status
    var isBlocked by remember { mutableStateOf(false) }
    LaunchedEffect(artistId) {
        isBlocked = repository.isBlocked(currentUserId, artistId)
    }

    if (artist == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (isBlocked) {
        Scaffold(
            topBar = { StandardHeader(title = "Fandom", onBack = onBack) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFE91E63))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Access Denied", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("You have been blocked by this artist.", color = Color.Gray)
                }
            }
        }
        return
    }

    // Check Active Status (isFandomActive field in UserEntity)
    if (!artist!!.isFandomActive && artist!!.id != currentUserId) {
         Scaffold(
            topBar = { StandardHeader(title = "Fandom", onBack = onBack) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fandom Not Available", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("This fandom has been deactivated by the artist.", color = Color.Gray)
                }
            }
        }
        return
    }


    if (showCreateThreadDialog) {
        CreatePostDialog(
            title = "Start a Thread",
            onDismiss = { showCreateThreadDialog = false },
            onConfirm = { content, imageStrings ->
                scope.launch {
                    val finalImages = imageStrings.mapNotNull { uriString ->
                         val uri = Uri.parse(uriString)
                         copyUriToInternalStorage(context, uri)
                    }
                    val newPost = PostEntity(
                        authorId = currentUserId,
                        artistId = artistId, // Context is the Artist
                        content = content.trim(),
                        images = finalImages,
                        timestamp = System.currentTimeMillis(),
                        isThread = true
                    )
                    repository.createPost(newPost)
                    showCreateThreadDialog = false
                    Toast.makeText(context, "Thread posted!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 2) {
                ExtendedFloatingActionButton(
                    text = { Text("New Thread", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = { 
                        if (!artist!!.isInteractionEnabled && artist!!.id != currentUserId) {
                             Toast.makeText(context, "Interactions are currently disabled for this fandom.", Toast.LENGTH_SHORT).show()
                             return@ExtendedFloatingActionButton
                        }
                        if (isFollowingState.value) {
                            showCreateThreadDialog = true 
                        } else {
                            Toast.makeText(context, "You must follow the fandom to create a thread.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(40.dp),
                    containerColor = if (isFollowingState.value && (artist!!.isInteractionEnabled || artist!!.id == currentUserId)) MaterialTheme.colorScheme.primary else Color.Gray
                )
            } else if (selectedTab == 3) {
                 FloatingActionButton(
                    onClick = { onNavigateToCart() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                }
            }
        },
        topBar = {
            StandardHeader(
                title = artist?.username ?: "Fandom", 
                onBack = onBack,
                actions = {
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
                                            reportedId = artistId, 
                                            type = "FANDOM", // Treating Artist Profile as Fandom Report
                                            referenceId = artistId,
                                            reason = reason,
                                            description = description,
                                            contentSnapshot = "Fandom: ${artist!!.fullName}"
                                        )
                                        val success = repository.reportUser(report)
                                        showReportDialog = false
                                        if (success) {
                                            Toast.makeText(context, "Fandom reported.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "You have already reported this fandom.", Toast.LENGTH_SHORT).show()
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
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out this fandom: https://fandomhub.com/fandom/${artistId}")
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report") },
                                onClick = { 
                                    showMenu = false
                                    if (isFollowingState.value) {
                                        showReportDialog = true
                                    } else {
                                        Toast.makeText(context, "You must follow the fandom to report.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    kotlinx.coroutines.delay(1500)
                    isRefreshing = false
                    Toast.makeText(context, "Content refreshed!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
            // Collapsible Header Info
            item {
                FandomHeaderInfo(
                    artist = artist!!,
                    isFollowing = isFollowingState.value,
                    followerCount = followerCount.value,
                    onToggleFollow = {
                        scope.launch {
                            repository.toggleFollow(currentUserId, artistId, isFollowingState.value)
                        }
                    },
                    onDmClick = {
                        if (artist!!.id != currentUserId) {
                             if (!artist!!.isDmActive) {
                                  // Wait, isDmActive is on UserEntity now.
                                 // But wait, if Artist disabled DMs, does the button even show?
                                 // We will check isDmActive in button visibility too.
                            }
                            if (!isFollowingState.value) {
                                 Toast.makeText(context, "You must follow the fandom first!", Toast.LENGTH_SHORT).show()
                                 return@FandomHeaderInfo
                            }
                        }
                        // Check Subscription
                         scope.launch {
                             val subscription = repository.getSubscription(artistId, currentUserId)
                             val isActive = subscription != null && subscription.validUntil > System.currentTimeMillis() && !subscription.isCancelled
                             
                             if (isActive) {
                                 onNavigateToChat(artistId) 
                             } else {
                                 onNavigateToCheckout(artistId)
                             }
                         }
                    }
                )
            }
            
            // Sticky Tabs
            stickyHeader {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { 
                                    if (selectedTab == index) {
                                        if (!isRefreshing) {
                                            isRefreshing = true
                                            scope.launch {
                                                kotlinx.coroutines.delay(1500) 
                                                isRefreshing = false
                                                Toast.makeText(context, "$title refreshed!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        selectedTab = index 
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                    if (isRefreshing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> item { 
                    FandomFeedContent(
                        repository = repository, 
                        artistId = artistId, 
                        currentUserId = currentUserId,
                        onNavigateToPost = onNavigateToPost,
                        onNavigateToProduct = onNavigateToProduct,
                        onTabChange = { newTab -> selectedTab = newTab },
                        isFollowing = isFollowingState.value,
                        newArrivalsState = newArrivalsState
                    ) 
                }
                1 -> item { 
                    OfficialPostsContent(
                        repository = repository, 
                        artistId = artistId, 
                        currentUserId = currentUserId,
                        onNavigateToPost = onNavigateToPost,
                        isFollowing = isFollowingState.value
                    ) 
                }
                2 -> item { 
                    CommunityContent(
                        repository = repository, 
                        artistId = artistId, 
                        currentUserId = currentUserId,
                        onNavigateToPost = onNavigateToPost,
                        isFollowing = isFollowingState.value
                    ) 
                }
                3 -> item { MarketContent(repository, artistId, onNavigateToProduct) }
            }
            }
        }
    }
}

@Composable
fun FandomHeaderInfo(
    artist: UserEntity,
    isFollowing: Boolean,
    followerCount: Int,
    onToggleFollow: () -> Unit,
    onDmClick: () -> Unit
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
                    contentDescription = "Profile",
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
                    contentDescription = "Profile",
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
                ExpandableText(
                    text = artist.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Followers & Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$followerCount Followers", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    // DM Button
                    if (artist.isDmActive) {
                        IconButton(onClick = onDmClick) {
                            Icon(Icons.Default.Email, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Follow Button
                    Button(
                        onClick = onToggleFollow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color.Gray else MaterialTheme.colorScheme.primary
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50) 
                    ) {
                        Text(if (isFollowing) "Following" else "Follow")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FandomFeedContent(
    repository: FandomRepository, 
    artistId: Int, 
    currentUserId: Int,
    onNavigateToPost: (Int) -> Unit,
    onNavigateToProduct: (Int) -> Unit,
    onTabChange: (Int) -> Unit,
    isFollowing: Boolean,
    newArrivalsState: androidx.compose.foundation.lazy.LazyListState
) {
    val officialPosts = repository.getPostsByAuthor(artistId).collectAsState(initial = emptyList())
    val newArrivals = repository.getProductsByArtist(artistId).collectAsState(initial = emptyList()) // Updated
    val communityThreads = repository.getFanThreads(artistId).collectAsState(initial = emptyList()) // Updated
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Latest Updates
        if (officialPosts.value.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Latest Updates", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onTabChange(1) }) { 
                    Text("See All")
                }
            }

            val latestPostItem = officialPosts.value.first()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                 PostItem(
                    post = latestPostItem.post,
                    author = latestPostItem.author,
                    currentUserId = currentUserId,
                    repository = repository,
                    onCommentClick = { onNavigateToPost(latestPostItem.post.id) },
                    onPostClick = { onNavigateToPost(latestPostItem.post.id) },
                    isFollowing = isFollowing,
                    artist = latestPostItem.artist // Replaces fandom
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // New Arrivals (New Merch)
        if (newArrivals.value.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "New Arrivals", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onTabChange(3) }) {
                    Text("See All")
                }
            }
            
            LazyRow(
                state = newArrivalsState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(newArrivals.value.take(5)) { product ->
                     Box(modifier = Modifier.width(160.dp)) {
                        com.kelompok1.fandomhub.ui.components.ProductItem(
                            product = product,
                            onClick = { onNavigateToProduct(product.id) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Trending Threads
        if (communityThreads.value.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Trending in Fandom", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onTabChange(2) }) {
                    Text("See All")
                }
            }
            
            Column {
                communityThreads.value.take(3).forEach { threadItem ->
                PostItem(
                    post = threadItem.post,
                    author = threadItem.author,
                    currentUserId = currentUserId,
                    repository = repository,
                    onCommentClick = { onNavigateToPost(threadItem.post.id) },
                    onPostClick = { onNavigateToPost(threadItem.post.id) },
                    isFollowing = isFollowing,
                    artist = threadItem.artist
                )
                }
            }
        } else {
             Text(
                "No trending community activity yet.", 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun OfficialPostsContent(
    repository: FandomRepository, 
    artistId: Int, 
    currentUserId: Int,
    onNavigateToPost: (Int) -> Unit,
    isFollowing: Boolean
) {
    val posts = repository.getPostsByAuthor(artistId).collectAsState(initial = emptyList())
    val allLikes = repository.getAllLikes().collectAsState(initial = emptyList())
    var sortByLikes by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Sort Options
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

        val sortedPosts = if (sortByLikes) {
            posts.value.sortedByDescending { post -> 
                allLikes.value.filter { it.postId == post.post.id }.size
            }
        } else {
            posts.value.sortedByDescending { it.post.timestamp }
        }

        if (sortedPosts.isEmpty()) {
            Text("No official updates from the Artist.", modifier = Modifier.padding(16.dp))
        } else {
            sortedPosts.forEach { postItem ->
                PostItem(
                    post = postItem.post,
                    author = postItem.author,
                    currentUserId = currentUserId,
                    repository = repository,
                    onCommentClick = { onNavigateToPost(postItem.post.id) },
                    onPostClick = { onNavigateToPost(postItem.post.id) },
                    isFollowing = isFollowing,
                    artist = postItem.artist
                )
            }
        }
    }
}

@Composable
fun CommunityContent(
    repository: FandomRepository, 
    artistId: Int,
    currentUserId: Int,
    onNavigateToPost: (Int) -> Unit,
    isFollowing: Boolean
) {
    val threads = repository.getFanThreads(artistId).collectAsState(initial = emptyList())
    val allLikes = repository.getAllLikes().collectAsState(initial = emptyList())
    var sortByLikes by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Sort Options
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

        val sortedThreads = if (sortByLikes) {
            threads.value.sortedByDescending { post -> 
                allLikes.value.filter { it.postId == post.post.id }.size
            }
        } else {
            threads.value.sortedByDescending { it.post.timestamp }
        }

        if (sortedThreads.isEmpty()) {
            Text("No threads yet. Be the first!", modifier = Modifier.padding(16.dp))
        } else {
            sortedThreads.forEach { threadItem ->
                 PostItem(
                    post = threadItem.post,
                    author = threadItem.author,
                    currentUserId = currentUserId,
                    repository = repository,
                    onCommentClick = { onNavigateToPost(threadItem.post.id) },
                    onPostClick = { onNavigateToPost(threadItem.post.id) },
                    isFollowing = isFollowing,
                    artist = threadItem.artist
                )
            }
        }
    }
}

@Composable
fun MarketContent(repository: FandomRepository, artistId: Int, onNavigateToProduct: (Int) -> Unit) {
    val products = repository.getProductsByArtist(artistId).collectAsState(initial = emptyList())
    var sortByBestSelling by remember { mutableStateOf(true) }
    
    // Suggestion Logic for Market
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        
        // Search & Suggestions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(10f) 
        ) {
            var active by remember { mutableStateOf(false) }

            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        active = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search Merch...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = ""; active = false }) { Icon(Icons.Default.Clear, "Clear") } }
                    } else null,
                    singleLine = true
                )
                
                val suggestions = if (searchQuery.isBlank()) emptyList() 
                                  else products.value.filter { 
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
        }
        
        // Filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FilterChip(
                selected = sortByBestSelling,
                onClick = { sortByBestSelling = true },
                label = { Text("Best Selling") }
            )
            FilterChip(
                selected = !sortByBestSelling,
                onClick = { sortByBestSelling = false },
                label = { Text("Newest") }
            )
        }
    
        val filteredProducts = products.value.filter { 
            it.name.contains(searchQuery, ignoreCase = true)
        }

        val sortedProducts = if (sortByBestSelling) {
            filteredProducts.sortedByDescending { it.soldCount }
        } else {
            filteredProducts.sortedByDescending { it.id }
        }
        
        // Stock Priority: In Stock first
        val finalProducts = sortedProducts.sortedByDescending { it.stock > 0 }

        if (finalProducts.isEmpty()) {
            Text("No merchandise available yet.", modifier = Modifier.padding(16.dp))
        } else {
            // Grid Layout (Using FlowRow or nested Rows)
            // Simplified: Vertical List of Rows (2 items)
            val chunked = finalProducts.chunked(2)
            chunked.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { product ->
                        Box(modifier = Modifier.weight(1f)) {
                            com.kelompok1.fandomhub.ui.components.ProductItem(
                                product = product,
                                onClick = { onNavigateToProduct(product.id) }
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
