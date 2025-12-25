package com.kelompok1.fandomhub.ui.fandom

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
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
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.components.StandardHeader
import com.kelompok1.fandomhub.utils.copyUriToInternalStorage
import kotlinx.coroutines.launch

@Composable
fun ManageFandomProfileScreen(
    repository: FandomRepository,
    artist: UserEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    // We bind directly to the passed 'artist' UserEntity, but we might want to refresh it?
    // Since we are editing it, local state is fine, but for persistence we need to ensure we have latest.
    // For now assuming 'artist' passed is fresh enough or we fetch it.
    // Let's fetch fresh to be safe.
    var currentArtist by remember { mutableStateOf<UserEntity?>(null) }
    var description by remember { mutableStateOf("") }
    var artistProfileImage by remember { mutableStateOf<String?>(null) }
    var bannerImage by remember { mutableStateOf<String?>(null) }
    var blockedSearchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(artist.id) {
        val fresh = repository.getUserById(artist.id)
        if (fresh != null) {
            currentArtist = fresh
            description = fresh.bio ?: ""
            artistProfileImage = fresh.profileImage
            bannerImage = fresh.coverImage
            isLoading = false
        }
    }

    var showRevertDialog by remember { mutableStateOf(false) }

    val bannerLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful && result.uriContent != null) {
            val savedUri = copyUriToInternalStorage(context, result.uriContent!!)
            if (currentArtist != null && savedUri != null) {
                scope.launch {
                    val updated = currentArtist!!.copy(coverImage = savedUri)
                    repository.updateUser(updated)
                    bannerImage = savedUri
                    currentArtist = updated
                    Toast.makeText(context, "Banner updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val profileLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful && result.uriContent != null) {
            val savedUri = copyUriToInternalStorage(context, result.uriContent!!)
            if (savedUri != null && currentArtist != null) {
                scope.launch {
                    val updated = currentArtist!!.copy(profileImage = savedUri)
                    repository.updateUser(updated)
                    artistProfileImage = savedUri
                    currentArtist = updated
                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            StandardHeader(title = "Artist Profile", onBack = onBack)
        }
    ) { padding ->
        if (isLoading || currentArtist == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val user = currentArtist!!
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Banner & Profile Image
                Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                    Box(
                        modifier = Modifier.height(150.dp).fillMaxWidth().clickable {
                                val options = CropImageContractOptions(uri = null, cropImageOptions = CropImageOptions(aspectRatioX = 3, aspectRatioY = 1, fixAspectRatio = true))
                                bannerLauncher.launch(options)
                            }
                    ) {
                        if (bannerImage != null) {
                            AsyncImage(model = bannerImage, contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.5f), CircleShape).padding(4.dp)) {
                             Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 30.dp).size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).clickable {
                             val options = CropImageContractOptions(uri = null, cropImageOptions = CropImageOptions(aspectRatioX = 1, aspectRatioY = 1, fixAspectRatio = true))
                             profileLauncher.launch(options)
                        }) {
                        if (artistProfileImage != null) {
                            AsyncImage(model = artistProfileImage, contentDescription = "Profile", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.fillMaxSize().padding(16.dp), tint = Color.Gray)
                        }
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).background(Color.Black.copy(alpha=0.5f), CircleShape).padding(4.dp)) {
                             Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("General Info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Artist Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var name by remember { mutableStateOf(user.fullName) }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Artist Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    val updated = user.copy(fullName = name, username = name)
                                    repository.updateUser(updated)
                                    currentArtist = updated
                                    Toast.makeText(context, "Artist Name updated!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save Name")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 150) description = it },
                        label = { Text("Bio / Description") },
                        supportingText = { Text("${description.length}/150") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val updated = user.copy(bio = description)
                                repository.updateUser(updated)
                                currentArtist = updated
                                Toast.makeText(context, "Bio updated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Bio")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Access Control", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Fan Interactions", style = MaterialTheme.typography.titleSmall)
                            Text("If disabled, fans can only view content (Read-Only).", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = user.isInteractionEnabled,
                            onCheckedChange = { 
                                scope.launch {
                                    val updated = user.copy(isInteractionEnabled = it)
                                    repository.updateUser(updated)
                                    currentArtist = updated
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Active Status", style = MaterialTheme.typography.titleSmall)
                            Text("If disabled, your artist profile is hidden from search.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = user.isFandomActive,
                            onCheckedChange = { 
                                scope.launch {
                                    val updated = user.copy(isFandomActive = it)
                                    repository.updateUser(updated)
                                    currentArtist = updated
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showRevertDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Revert to Fan Account", color = Color.White)
                    }
                    
                    if (showRevertDialog) {
                        AlertDialog(
                            onDismissRequest = { showRevertDialog = false },
                            title = { Text("Revert to Fan Account?") },
                            text = { Text("You will lose access to Artist Dashboard and your content will be hidden. You can apply again later.") },
                            confirmButton = {
                                TextButton(
                                    onClick = { 
                                        scope.launch {
                                            // Reset Role to FAN
                                            val updated = user.copy(role = "FAN", isFandomActive = false)
                                            repository.updateUser(updated)
                                            Toast.makeText(context, "Reverted to Fan Account.", Toast.LENGTH_SHORT).show()
                                            onBack() // This should navigate out
                                        }
                                        showRevertDialog = false 
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE91E63))
                                ) {
                                    Text("Revert")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRevertDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Blocked Fans", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    val blockedUsers: State<List<UserEntity>> = repository.getBlockedUsers(artist.id).collectAsState(initial = emptyList())
                    
                    OutlinedTextField(
                        value = blockedSearchQuery,
                        onValueChange = { blockedSearchQuery = it },
                        label = { Text("Search blocked user") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredBlockedUsers = blockedUsers.value.filter { 
                        it.fullName.contains(blockedSearchQuery, ignoreCase = true) || 
                        it.username.contains(blockedSearchQuery, ignoreCase = true) 
                    }

                    if (filteredBlockedUsers.isEmpty()) {
                        Text("No blocked users found.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        filteredBlockedUsers.forEach { blockedUser ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = blockedUser.profileImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(blockedUser.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(blockedUser.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            repository.unblockUser(artist.id, blockedUser.id)
                                            Toast.makeText(context, "User unblocked.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Unblock", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
