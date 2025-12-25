package com.kelompok1.fandomhub.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.ui.components.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: FandomRepository,
    currentUser: UserEntity,
    onNavigateToFandom: (Int) -> Unit, // Navigate to Artist
    onNavigateToPost: (Int) -> Unit,
    onNavigateToDiscovery: () -> Unit,
    onNavigateToAllMerch: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProduct: (Int) -> Unit
) {
    if (currentUser.role == "ARTIST") {
        ArtistDashboardScreen(repository, currentUser, onNavigateToPost)
    } else {
        FanHomeScreen(
            repository, 
            currentUser, 
            onNavigateToFandom, 
            onNavigateToPost, 
            onNavigateToDiscovery,
            onNavigateToAllMerch,
            onNavigateToNotifications,
            onNavigateToProduct
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanHomeScreen(
    repository: FandomRepository,
    currentUser: UserEntity,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToPost: (Int) -> Unit,
    onNavigateToDiscovery: () -> Unit,
    onNavigateToAllMerch: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProduct: (Int) -> Unit
) {
    val followedArtists by repository.getFollowedArtists(currentUser.id).collectAsState(initial = emptyList())
    val feedPosts by repository.getFeedPosts(currentUser.id).collectAsState(initial = emptyList())
    val feedProducts by repository.getFeedProducts(currentUser.id).collectAsState(initial = emptyList())
    val unreadCount = 0 
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val followedArtistsState = rememberLazyListState()
    val newMerchState = rememberLazyListState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        StandardHeader(
            title = "FandomHub"
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(1500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                // 1. Followed Artists + Discovery
                item {
                    Column {
                        Text(
                            text = "Artists you follow",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            state = followedArtistsState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                        ) {
                            // Followed Artists
                            items(followedArtists) { artist ->
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onNavigateToDetail(artist.id) }
                                        .width(72.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                            .padding(4.dp)
                                    ) {
                                        AsyncImage(
                                            model = artist.profileImage,
                                            contentDescription = artist.fullName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(androidx.compose.ui.graphics.Color.LightGray),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = artist.fullName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    com.kelompok1.fandomhub.ui.components.ArtistBadge(visible = true)
                                }
                            }

                            // Add (+) Button
                            item {
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onNavigateToDiscovery() }
                                        .width(72.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .border(
                                                width = 1.dp,
                                                color = androidx.compose.ui.graphics.Color.Gray,
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                            .padding(4.dp),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                            contentDescription = "Discover",
                                            tint = androidx.compose.ui.graphics.Color.Gray,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                     androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                     Text(
                                            text = "Add",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color.Gray,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
                
                // 2. New Recommended Products
                item {
                    if (feedProducts.isNotEmpty()) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New Merch from Your Artists",
                                style = MaterialTheme.typography.titleMedium
                            )
                            androidx.compose.material3.TextButton(onClick = onNavigateToAllMerch) {
                                Text("Show All")
                            }
                        }
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            state = newMerchState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                        ) {
                            items(feedProducts.take(5)) { product ->
                                val ownerName = followedArtists.find { it.id == product.artistId }?.fullName
                                
                                Box(modifier = Modifier.width(160.dp)) {
                                    com.kelompok1.fandomhub.ui.components.ProductItem(
                                        product = product,
                                        ownerName = ownerName,
                                        onClick = { onNavigateToProduct(product.id) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }

                // 3. Feed (Posts)
                if (feedPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Latest Updates",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }
                    items(feedPosts) { postWithAuthor ->
                        PostItem(
                            post = postWithAuthor.post,
                            author = postWithAuthor.author,
                            currentUserId = currentUser.id,
                            repository = repository,
                            onCommentClick = { onNavigateToPost(postWithAuthor.post.id) },
                            onPostClick = { onNavigateToPost(postWithAuthor.post.id) },
                            isFollowing = true, 
                            artistName = postWithAuthor.artist?.fullName, // Use artistName
                            onAuthorClick = { 
                                if (postWithAuthor.artist != null) {
                                    onNavigateToDetail(postWithAuthor.artist.id) // Navigate to Artist
                                }
                            },
                            artist = postWithAuthor.artist // Pass artist entity
                        )
                    }
                } else if (followedArtists.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("No recent updates from your artists.", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.Gray)
                        }
                    }
                } else {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                             Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text("Your Feed is Empty", style = MaterialTheme.typography.bodyLarge, color = androidx.compose.ui.graphics.Color.Gray)
                                Text("Follow artists to see updates here!", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
