package com.kelompok1.fandomhub.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.PostItem
import com.kelompok1.fandomhub.ui.components.StandardHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    currentUser: UserEntity,
    repository: FandomRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPost: (Int) -> Unit
) {
    val savedPosts by repository.getSavedPosts(currentUser.id).collectAsState(initial = emptyList())
    // Need to load author's followed fandoms to pass to PostItem for correct interaction logic?
    // PostItem logic check: "You must follow the fandom to like posts."
    // We need to know if currentUser follows the post's fandom.
    // We can fetch follows list or just default `isFollowing` to true if we assume saved posts are usually from followed fandoms (not always true).
    
    // Better: Fetch followed artist IDs.
    val followedArtists by repository.getFollowedArtists(currentUser.id).collectAsState(initial = emptyList())
    val followedArtistIds = remember(followedArtists) { followedArtists.map { it.id }.toSet() }
    
    Scaffold(
        topBar = {
            StandardHeader(
                title = "Saved Posts",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        if (savedPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                    contentAlignment = Alignment.Center
            ) {
                Text("No saved posts yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(savedPosts) { postWithAuthor ->
                    val isFollowing = followedArtistIds.contains(postWithAuthor.post.artistId)
                    PostItem(
                        post = postWithAuthor.post,
                        author = postWithAuthor.author,
                        currentUserId = currentUser.id,
                        repository = repository,
                        onPostClick = { onNavigateToPost(postWithAuthor.post.id) },
                        onCommentClick = { onNavigateToPost(postWithAuthor.post.id) },
                        isFollowing = isFollowing,
                        artistName = postWithAuthor.author.fullName // Changed from fandomName to artistName
                    )
                }
            }
        }
    }
}
