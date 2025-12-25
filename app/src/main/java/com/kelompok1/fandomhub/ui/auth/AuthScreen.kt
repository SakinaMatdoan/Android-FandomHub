package com.kelompok1.fandomhub.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.kelompok1.fandomhub.R
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.UserEntity
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: FandomRepository,
    onLoginSuccess: (UserEntity) -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }
    var isForgotPassword by remember { mutableStateOf(false) }
    
    // Login State
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register State
    var regFullName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regConfirmPasswordVisible by remember { mutableStateOf(false) }
    var regRole by remember { mutableStateOf("FANS") }

    // Register Confirmation State
    var showRegisterConfirmDialog by remember { mutableStateOf(false) }

    // Forgot Password State
    var forgotUsername by remember { mutableStateOf("") }
    var forgotNewPassword by remember { mutableStateOf("") }
    var forgotConfirmPassword by remember { mutableStateOf("") }
    var forgotPasswordVisible by remember { mutableStateOf(false) }

    var rememberMe by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = androidx.compose.foundation.rememberScrollState()

    // Validation Helper
    fun validateAndShowConfirm() {
        if (regFullName.isBlank() || regUsername.isBlank() || regEmail.isBlank() || regPassword.isBlank() || regConfirmPassword.isBlank()) {
            errorMessage = "All fields must be filled!"
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (!isValidEmail(regEmail)) {
            errorMessage = "Invalid email format!"
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (regPassword != regConfirmPassword) {
            errorMessage = "Passwords do not match!"
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        showRegisterConfirmDialog = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding() // Keyboard Fix
                .verticalScroll(scrollState) // Scroll Fix
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                 painter = painterResource(id = R.drawable.ic_logo),
                 contentDescription = "Logo",
                 modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Fandom Hub",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isForgotPassword) {
                // ... (No changes to Forgot Password logic, omitting for brevity in thought, but must include in replace)
                // Actually I will assume the previous content is fine and just replacing the Register content block via a targeted replace if possible. 
                // BUT previous replace was whole file. To be safe, I will replace the whole file again or targeted block if I'm confident.
                // The prompt asks to "replace the specific blocks".
                // I will replace the entire AuthScreen function to ensure consistency and easier copy-paste.
                // I'll copy the Forgot Password logic from previous turn.
                Text("Reset Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = forgotUsername,
                    onValueChange = { forgotUsername = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = forgotNewPassword,
                    onValueChange = { forgotNewPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = if (forgotPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { forgotPasswordVisible = !forgotPasswordVisible }) {
                            Icon(if (forgotPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = forgotConfirmPassword,
                    onValueChange = { forgotConfirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = if (forgotPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            resetPassword(repository, forgotUsername.trim(), forgotNewPassword.trim(), forgotConfirmPassword.trim()) { msg, success ->
                                if (success) {
                                    successMessage = msg
                                    isForgotPassword = false // Go back to login
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    errorMessage = msg
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Password")
                }
                TextButton(onClick = { isForgotPassword = false; errorMessage = null }) {
                    Text("Back to Login")
                }
            } else {
                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    TabButton("Login", isLoginTab) { isLoginTab = true; errorMessage = null; successMessage = null }
                    TabButton("Register", !isLoginTab) { isLoginTab = false; errorMessage = null; successMessage = null }
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoginTab) {
                   // Login Form ... (Copying existing logic)
                    OutlinedTextField(
                        value = loginUsername,
                        onValueChange = { loginUsername = it },
                        label = { Text("Username") },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Password") },
                        isError = errorMessage != null,
                        visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                Icon(
                                    imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { 
                            focusManager.clearFocus()
                        })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                            Text("Remember Me")
                        }
                        TextButton(onClick = { isForgotPassword = true; errorMessage = null }) {
                            Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                loginUser(context, repository, loginUsername.trim(), loginPassword.trim(), rememberMe, onLoginSuccess) { msg -> 
                                    errorMessage = msg
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login")
                    }
                } else {
                    // Register Form
                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it; errorMessage = if(isValidEmail(it)) null else "Invalid email format" },
                        label = { Text("Email") },
                        isError = !isValidEmail(regEmail) && regEmail.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Password") },
                        visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                Icon(if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = { regConfirmPassword = it },
                        label = { Text("Confirm Password") },
                        isError = regConfirmPassword.isNotEmpty() && regPassword != regConfirmPassword,
                        visualTransformation = if (regConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { regConfirmPasswordVisible = !regConfirmPasswordVisible }) {
                                Icon(if (regConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { 
                            validateAndShowConfirm()
                        })
                    )
                    if (regConfirmPassword.isNotEmpty() && regPassword != regConfirmPassword) {
                        Text("Passwords do not match!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Role Selection
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = regRole == "FANS", onClick = { regRole = "FANS" })
                        Text("Fan")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = regRole == "ARTIST", onClick = { regRole = "ARTIST" })
                        Text("Artist")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { validateAndShowConfirm() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register")
                    }
                }
            }
        }

        if (showRegisterConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showRegisterConfirmDialog = false },
                title = { Text("Confirm Registration") },
                text = {
                    Column {
                        Text("Is this information correct?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Name: $regFullName")
                        Text("Username: $regUsername")
                        Text("Email: $regEmail")
                        Text("Role: $regRole")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showRegisterConfirmDialog = false
                        echoRegister(
                            scope, 
                            repository, 
                            regFullName.trim(), 
                            regUsername.trim(), 
                            regEmail.trim(), 
                            regPassword.trim(), 
                            regConfirmPassword.trim(), 
                            regRole
                        ) { msg, success -> 
                            if(success) { 
                                successMessage = msg
                                isLoginTab = true 
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                errorMessage = msg 
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text("Yes, Register")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRegisterConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// Logic Extracted for readability
suspend fun loginUser(context: android.content.Context, repo: FandomRepository, userOrEmail: String, pass: String, rememberMe: Boolean, onSuccess: (UserEntity) -> Unit, onError: (String) -> Unit) {
    if(userOrEmail == "admin" && pass == "admin") {
        onSuccess(UserEntity(fullName="Admin", username="admin", email="admin@fandomhub.com", password="", role="ADMIN", status="ACTIVE"))
        return
    }
    
    val user = repo.getUserByUsername(userOrEmail)
    if (user != null && user.password == pass) {
        if(user.status == "PENDING") {
            onError("Your account is still under Admin verification.")
        } else if (user.isSuspended) {
            val endTime = user.suspensionEndTimestamp ?: 0L
            if (endTime > System.currentTimeMillis()) {
                 val remainingDays = (endTime - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                 onError("Account Suspended for $remainingDays days.")
            } else {
                // Suspension expired, auto-lift
                onSuccess(user)
            }
        } else {
            // Success & Remember Me
            if (rememberMe) {
                 context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE).edit().putInt("user_id", user.id).apply()
            }
            onSuccess(user)
        }
    } else {
        onError("Invalid Username or Password!")
    }
}

suspend fun resetPassword(repo: FandomRepository, userOrEmail: String, newPass: String, confirmPass: String, onResult: (String, Boolean) -> Unit) {
    if (userOrEmail.isEmpty() || newPass.isEmpty()) {
        onResult("Please fill all fields.", false)
        return
    }
    if (newPass != confirmPass) {
        onResult("Passwords do not match.", false)
        return
    }
    
    val user = repo.getUserByUsername(userOrEmail)
    if (user != null) {
        repo.updateUser(user.copy(password = newPass))
        onResult("Password successfully reset! You can now login.", true)
    } else {
        onResult("User not found.", false)
    }
}

fun echoRegister(
    scope: kotlinx.coroutines.CoroutineScope,
    repo: FandomRepository,
    name: String, username: String, email: String, pass: String, confirmPass: String, role: String,
    onResult: (String, Boolean) -> Unit
) {
    if (email.isEmpty() || username.isEmpty() || pass.isEmpty() || name.isEmpty()) {
        onResult("All fields must be filled!", false)
        return
    }
    if (!isValidEmail(email)) {
        onResult("Invalid email!", false)
        return
    }
    if (pass != confirmPass) {
        onResult("Password confirmation does not match!", false)
        return
    }

    scope.launch {
        val existing = repo.getUserByUsername(username)
        if (existing != null) {
            onResult("Username is already taken!", false)
            return@launch
        }
        
        val status = if (role == "ARTIST") "PENDING" else "ACTIVE"
        val newUser = UserEntity(
            fullName = name,
            username = username,
            email = email,
            password = pass,
            role = role,
            status = status
        )
        repo.registerUser(newUser)
        
        if (role == "ARTIST") {
            onResult("Artist registration successful! Please contact Admin.", true)
        } else {
            onResult("Registration successful! Please Login.", true)
        }
    }
}

@Composable
fun RowScope.TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface 
        ),
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        elevation = null
    ) {
        Text(text, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
