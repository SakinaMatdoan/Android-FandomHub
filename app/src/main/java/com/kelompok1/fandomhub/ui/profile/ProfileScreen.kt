package com.kelompok1.fandomhub.ui.profile

import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.ui.components.ExpandableText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import kotlinx.coroutines.launch
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import com.kelompok1.fandomhub.utils.copyUriToInternalStorage
import com.kelompok1.fandomhub.ui.components.PostItem
import com.kelompok1.fandomhub.data.local.PostWithAuthor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape 
import androidx.compose.material3.TextButton 

import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ConfirmationNumber

@Composable
fun ProfileScreen(
    repository: FandomRepository,
    currentUser: UserEntity,
    userIdToDisplay: Int? = null, // Optional: If provided, view this user. Else view self.
    onLogout: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToArtistDashboard: () -> Unit,
    onNavigateToFandom: (Int) -> Unit, // Navigate to Artist Profile
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onBack: () -> Unit
) {
    val targetUserId = userIdToDisplay ?: currentUser.id
    val isSelf = targetUserId == currentUser.id
    val isAdmin = currentUser.role == "ADMIN"
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    // Initialize with currentUser if self, else null (wait for fetch)
    var user by remember { mutableStateOf<UserEntity?>(if (isSelf) currentUser else null) }
    
    val scope = rememberCoroutineScope()
    val scrollState = androidx.compose.foundation.rememberScrollState() // Scroll state
    var showMenu by remember { mutableStateOf(false) } // Menu State

    var showWarnDialog by remember { mutableStateOf(false) }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }

    // Reload user data to ensure freshness or fetch target user
    LaunchedEffect(targetUserId) {
        val fetched = repository.getUserById(targetUserId)
        if (fetched != null) {
            user = fetched
        }
    }

    if (showEditDialog && isSelf && user != null) {
        EditProfileDialog(
            user = user!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedUser ->
                scope.launch {
                    repository.updateUser(updatedUser)
                    user = updatedUser
                    showEditDialog = false
                }
            }
        )
    }

    if (showChangePasswordDialog && isSelf && user != null) {
        ChangePasswordDialog(
            user = user!!,
            onDismiss = { showChangePasswordDialog = false },
            onSave = { newPassword ->
                scope.launch {
                    val updatedUser = user!!.copy(password = newPassword)
                    repository.updateUser(updatedUser)
                    user = updatedUser
                    showChangePasswordDialog = false
                }
            }
        )
    } 
    
    // Admin Dialogs
    if (showWarnDialog && isAdmin && user != null) {
        WarnUserDialog(
            onDismiss = { showWarnDialog = false },
            onConfirm = { reason ->
                scope.launch {
                    repository.warnUserDirect(user!!.id, currentUser.id, reason)
                    showWarnDialog = false
                }
            }
        )
    }

    if (showSuspendDialog && isAdmin && user != null) {
        SuspendUserDialog(
            onDismiss = { showSuspendDialog = false },
            onConfirm = { durationMillis ->
                scope.launch {
                    repository.suspendUserDirect(user!!.id, durationMillis, currentUser.id)
                    user = user!!.copy(isSuspended = true) // Update local state immediately
                    showSuspendDialog = false
                }
            }
        )
    }

    if (showBanDialog && isAdmin && user != null) {
        AlertDialog(
            onDismissRequest = { showBanDialog = false },
            title = { Text("Ban User") },
            text = { Text("Are you sure you want to permanently delete this user? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteUserDirect(user!!.id, currentUser.id)
                            showBanDialog = false
                            onBack() // Exit profile as user is gone
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Loading State if user is null
    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Force non-null for subsequent usage
    val displayedUser = user!!
    
    // Check Suspension Status
    if (displayedUser.isSuspended) {
        if (!isAdmin && !isSelf) {
            // Case C: Visitor viewing suspended user -> "Account Deactivated"
             Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Account Deactivated", 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This account is no longer active.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
            return
        } else if (isSelf) {
            // Case B: User viewing their own suspended account -> "Account Suspended"
             Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Account Suspended", 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your account has been suspended due to violations of our Community Guidelines.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                     Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Logout")
                    }
                }
            }
            return
        }
    }

    Scaffold(
        topBar = {
            StandardHeader(
                title = if (isSelf) "Profile" else "Fan Profile",
                isArtist = displayedUser.role == "ARTIST",
                onBack = onBack,
                actions = {
                    // Only show Menu if IT IS SELF
                    if (isSelf) {
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (displayedUser.role == "FANS" || displayedUser.role == "FAN") {
                                    DropdownMenuItem(
                                        text = { Text("Saved Posts") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToSavedPosts()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
                                    )
                                    HorizontalDivider()
                                }

                                DropdownMenuItem(
                                    text = { Text("Edit Profile") },
                                    onClick = {
                                        showMenu = false
                                        showEditDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )

                                if (displayedUser.role == "ARTIST") {
                                    DropdownMenuItem(
                                        text = { Text("Artist Dashboard") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToArtistDashboard()
                                        },
                                        leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Change Password") },
                                    onClick = {
                                        showMenu = false
                                        showChangePasswordDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                )

                                if (displayedUser.role == "ADMIN") {
                                    DropdownMenuItem(
                                        text = { Text("Admin Panel") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToAdmin()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                                    )
                                }

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Logout", color = Color(0xFFE91E63)) },
                                    onClick = {
                                        showMenu = false
                                        onLogout()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    } else if (isAdmin && displayedUser.role != "ADMIN") {
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Admin Actions")
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Warn User") },
                                    onClick = {
                                        showMenu = false
                                        showWarnDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) } // Placeholder icon
                                )
                                
                                DropdownMenuItem(
                                    text = { Text("Suspend User") },
                                    onClick = {
                                        showMenu = false
                                        showSuspendDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                )
                                
                                HorizontalDivider()
                                
                                DropdownMenuItem(
                                    text = { Text("Ban User", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showBanDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Content Wrapper for Valid Paddings
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Admin Warning Context
                if (displayedUser.isSuspended) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("This account is currently SUSPENDED.", fontWeight = FontWeight.Bold)
                                Text("Visible only to Admins.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Profile Header
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayedUser.profileImage != null) {
                        AsyncImage(
                            model = displayedUser.profileImage,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(displayedUser.fullName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("@${displayedUser.username}", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                
                // Show Email if Self or Admin
                if (isSelf || isAdmin) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(displayedUser.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Show Bio only for Artists and Admins (Hide for Fans) - OR if we want to reveal it to Admin?
                // Logic: "Hide Bio for Fans" meant "Don't show it on Fan profile".
                // If displayedUser is Artist, show bio.
                // If displayedUser is Fan, hide bio (unless Admin wants to see empty bio?).
                // Let's stick to existing logic for now: Hide for FANS role.
                if (displayedUser.role != "FANS" && displayedUser.role != "FAN" && !displayedUser.bio.isNullOrEmpty()) {
                    ExpandableText(
                        text = displayedUser.bio!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (displayedUser.role != "FANS" && displayedUser.role != "FAN" && !displayedUser.location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(displayedUser.location!!, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                
                 Spacer(modifier = Modifier.height(24.dp))

                // Stats (Following) - Only for Fans
                if (displayedUser.role == "FANS" || displayedUser.role == "FAN") {
                    // Update to getFollowedArtists
                    val followedArtists by repository.getFollowedArtists(displayedUser.id).collectAsState(initial = emptyList())
                    val followedCount = followedArtists.size
                    var showFollowedDialog by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFollowedDialog = true }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Following: $followedCount Artists",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (showFollowedDialog) {
                        AlertDialog(
                            onDismissRequest = { showFollowedDialog = false },
                            title = { Text("Followed Artists") },
                            text = {
                                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                    items(followedArtists) { artist ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    onNavigateToFandom(artist.id) // Still use same callback for now
                                                    showFollowedDialog = false
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (artist.profileImage != null) {
                                                AsyncImage(
                                                    model = artist.profileImage,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(artist.fullName.first().toString(), color = Color.White)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(artist.fullName, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showFollowedDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
            } // End of Padded Column
            
            // Full Width Section for Posts (No Horizontal Padding)
            if (displayedUser.role == "FANS" || displayedUser.role == "FAN") {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSelf) "My Community Posts" else "Community Posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp) // Maintain padding for Title
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val myPosts by repository.getPostsByAuthor(displayedUser.id).collectAsState(initial = emptyList())
                
                if (myPosts.isEmpty()) {
                    Text(
                        "No posts yet.", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        myPosts.forEach { postWithAuthor ->
                            PostItem(
                                post = postWithAuthor.post,
                                author = postWithAuthor.author,
                                currentUserId = currentUser.id, // Interaction is done by CURRENT user
                                repository = repository,
                                onCommentClick = { /* No-op or navigate */ },
                                onPostClick = { /* Navigate to detail */ },
                                isFollowing = true, // Viewing user's posts
                                artistName = postWithAuthor.artist?.fullName
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
    }
}
}

@Composable
fun EditProfileDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    var location by remember { mutableStateOf(user.location ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    
    val imageCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            selectedImageUri = result.uriContent
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Image Picker Area
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val options = CropImageContractOptions(
                                uri = null, 
                                cropImageOptions = CropImageOptions(
                                    imageSourceIncludeGallery = true,
                                    imageSourceIncludeCamera = true,
                                    fixAspectRatio = true,
                                    aspectRatioX = 1,
                                    aspectRatioY = 1,
                                    cropShape = CropImageView.CropShape.RECTANGLE
                                )
                            ) 
                            imageCropLauncher.launch(options)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val imageToShow = selectedImageUri ?: (if (user.profileImage != null) Uri.parse(user.profileImage) else null)
                    
                    if (imageToShow != null) {
                        AsyncImage(
                            model = imageToShow,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp))
                    }
                    
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (user.role != "FANS" && user.role != "FAN") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                if (user.role != "FANS" && user.role != "FAN") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalImageString = if (selectedImageUri != null) {
                    copyUriToInternalStorage(context, selectedImageUri!!)
                } else {
                    user.profileImage
                }
                
                onSave(user.copy(
                    fullName = fullName.trim(), 
                    bio = bio.trim(), 
                    location = location.trim(),
                    profileImage = finalImageString
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Old Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (oldPassword != user.password) {
                    error = "Old password incorrect"
                } else if (newPassword.length < 6) {
                    error = "Password must be at least 6 characters"
                } else if (newPassword != confirmPassword) {
                    error = "Passwords do not match"
                } else {
                    onSave(newPassword)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WarnUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Warn User") },
        text = {
            Column {
                Text("Send an official warning to this user.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.ifBlank { "Violation of Community Guidelines" }) }
            ) {
                Text("Send Warning")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SuspendUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val options = listOf(
        "3 Days" to 3 * 24 * 60 * 60 * 1000L,
        "7 Days" to 7 * 24 * 60 * 60 * 1000L,
        "30 Days" to 30 * 24 * 60 * 60 * 1000L,
        "Permanent" to 36500L * 24 * 60 * 60 * 1000L // ~100 Years
    )
    var selectedOption by remember { mutableStateOf(options[1]) } // Default 7 days
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suspend User") },
        text = {
            Column {
                Text("Select suspension duration:")
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = { selectedOption = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.first)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedOption.second) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Suspend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
