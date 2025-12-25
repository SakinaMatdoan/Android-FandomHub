package com.kelompok1.fandomhub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kelompok1.fandomhub.data.FandomRepository
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.kelompok1.fandomhub.ui.search.SearchScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.kelompok1.fandomhub.data.local.UserEntity
import com.kelompok1.fandomhub.ui.auth.AuthScreen
import com.kelompok1.fandomhub.ui.home.HomeScreen
import com.kelompok1.fandomhub.ui.message.MessageListScreen
import com.kelompok1.fandomhub.ui.notification.NotificationScreen
import com.kelompok1.fandomhub.ui.profile.ProfileScreen
import com.kelompok1.fandomhub.ui.admin.AdminUserManagementScreen
import com.kelompok1.fandomhub.ui.fandom.FandomDetailScreen
import com.kelompok1.fandomhub.ui.post.PostDetailScreen
import com.kelompok1.fandomhub.ui.splash.SplashScreen
import com.kelompok1.fandomhub.ui.navigation.Screen
import androidx.navigation.NavType
import androidx.navigation.navArgument

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch

@Composable
fun FandomHubApp(repository: FandomRepository) {
    val navController = rememberNavController()
    var currentUser by remember { mutableStateOf<UserEntity?>(null) } // Simple Session Management
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Restore Session
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        val savedUserId = prefs.getInt("user_id", -1)
        if (savedUserId != -1) {
            val user = repository.getUserById(savedUserId)
            if (user != null && !user.isSuspended) {
                 currentUser = user
            }
        }
    }

    // Seed System Admin
    LaunchedEffect(Unit) {
        val admin = repository.getSystemAdmin()
        if (admin == null) {
            val sysAdmin = UserEntity(
                fullName = "FandomHub Support",
                username = "admin",
                email = "admin@fandomhub.com",
                password = "admin", // Default password
                role = "ADMIN",
                profileImage = null,
                coverImage = null,
                bio = "Official System Administrator",
                location = "FandomHub HQ",
                isSuspended = false,
                status = "ACTIVE"
            )
            repository.registerUser(sysAdmin)
        }
    }
    
    // Logout Helper
    fun doLogout() {
        context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        currentUser = null
        navController.navigate(Screen.Auth.route) {
            popUpTo(0)
        }
    }
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Define main screens that should show bottom navigation
            val mainScreenRoutes = when (currentUser?.role) {
                "ARTIST" -> listOf(
                    Screen.ArtistDashboard.route,
                    Screen.FandomManagement.route,
                    Screen.MessageList.route,
                    Screen.Notifications.route,
                    Screen.More.route
                )
                "ADMIN" -> emptyList() // Admin has no bottom bar

                else -> listOf( // "FANS"
                    Screen.Home.route,
                    Screen.Search.route,
                    Screen.MessageList.route,
                    Screen.Notifications.route,
                    Screen.More.route
                )
            }
            
            // Show bottom bar only if user is logged in AND on a main screen
            if (currentUser != null && currentRoute in mainScreenRoutes) {
                NavigationBar(
                    modifier = Modifier,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    
                    val items = when (currentUser!!.role) {
                        "ARTIST" -> listOf(
                            Screen.ArtistDashboard,
                            Screen.FandomManagement,
                            Screen.MessageList,
                            Screen.Notifications,
                            Screen.More
                        )
                        "ADMIN" -> listOf(
                            Screen.Home,
                             Screen.Search, // Admin might want to search
                            Screen.More
                        )
                        else -> listOf( // "FANS"
                            Screen.Home,
                            Screen.Search,
                            Screen.MessageList,
                            Screen.Notifications,
                            Screen.More
                        )
                    }
                    
                    items.forEach { screen ->
                        val icon = when(screen) {
                            Screen.Home -> Icons.Default.Home
                            Screen.Search -> Icons.Default.Search
                            Screen.MessageList -> Icons.Default.Email
                            Screen.Notifications -> Icons.Default.Notifications
                            Screen.More -> Icons.Default.Menu
                            Screen.ArtistDashboard -> Icons.Default.Home // Artist Home
                            Screen.FandomManagement -> Icons.Default.Settings
                            Screen.AdminUserManagement -> Icons.Default.SupportAgent // Admin Icon
                            Screen.AdminReports -> Icons.Default.Notifications // Using Notifications icon for Reports
                            else -> Icons.Default.Home
                        }
                        val label = when(screen) {
                            Screen.Home -> "Home"
                            Screen.Search -> "Search"
                            Screen.MessageList -> "DM"
                            Screen.Notifications -> "Notifikasi"
                            Screen.More -> "More"
                            Screen.ArtistDashboard -> "My Fandom"
                            Screen.FandomManagement -> "Manage"
                            Screen.AdminUserManagement -> "Users"
                            Screen.AdminReports -> "Reports"
                            else -> ""
                        }
                        
                        val unreadCountState = when (screen) {
                            Screen.MessageList -> repository.getTotalUnreadMessageCount(currentUser!!.id).collectAsState(initial = 0)
                            Screen.Notifications -> repository.getUnreadNotificationCount(currentUser!!.id).collectAsState(initial = 0)
                            else -> null
                        }
                        
                        NavigationBarItem(
                            icon = { 
                                if (unreadCountState != null && unreadCountState.value > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                val count = unreadCountState.value
                                                val countText = if (count > 99) "99+" else count.toString()
                                                Text(countText)
                                            }
                                        }
                                    ) {
                                        Icon(icon, contentDescription = label)
                                    }
                                } else {
                                    Icon(icon, contentDescription = label)
                                }
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen {
                    if (currentUser != null) {
                         val route = if (currentUser!!.role == "ARTIST") Screen.ArtistDashboard.route else Screen.Home.route
                         navController.navigate(route) {
                             popUpTo(Screen.Splash.route) { inclusive = true }
                         }
                    } else {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            }
            composable(Screen.Auth.route) {
                AuthScreen(repository = repository) { user ->
                    currentUser = user
                    if (user.role == "ARTIST") {
                        navController.navigate(Screen.ArtistDashboard.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                }
            }
            composable(Screen.Home.route) {
                if(currentUser != null) {
                    if (currentUser!!.role == "ADMIN") {
                        com.kelompok1.fandomhub.ui.admin.AdminDashboardScreen(
                            repository = repository,
                            onNavigateToArtistList = { navController.navigate(Screen.AdminArtistList.route) },
                            onNavigateToFanList = { navController.navigate(Screen.AdminFanList.route) },
                            onNavigateToApproveArtists = { navController.navigate(Screen.AdminUserManagement.route) },
                            onNavigateToReports = { navController.navigate(Screen.AdminReports.route) },
                            onLogout = { doLogout() }
                        )
                    } else {
                        HomeScreen(
                            repository = repository, 
                            currentUser = currentUser!!,
                            onNavigateToFandom = { artistId ->
                                navController.navigate(Screen.FandomDetail.createRoute(artistId))
                            },
                            onNavigateToPost = { postId ->
                                navController.navigate(Screen.PostDetail.createRoute(postId))
                            },
                            onNavigateToDiscovery = {
                                navController.navigate(Screen.FandomDiscovery.route)
                            },
                            onNavigateToAllMerch = {
                                navController.navigate(Screen.FollowedMerch.route)
                            },
                            onNavigateToNotifications = {
                                navController.navigate(Screen.Notifications.route)
                            },
                            onNavigateToProduct = { productId ->
                                navController.navigate(Screen.ProductDetail.createRoute(productId))
                            }
                        )
                    }
                }
            }

            composable(Screen.AdminArtistList.route) {
                if (currentUser?.role == "ADMIN") {
                    com.kelompok1.fandomhub.ui.admin.AdminUserListScreen(
                        userType = "ARTIST",
                        repository = repository,
                        onNavigateToDetail = { userId ->
                            // Navigate to Fandom Detail (Artist Profile)
                            navController.navigate(Screen.FandomDetail.createRoute(userId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.AdminFanList.route) {
                if (currentUser?.role == "ADMIN") {
                    com.kelompok1.fandomhub.ui.admin.AdminUserListScreen(
                        userType = "FAN",
                        repository = repository,
                        onNavigateToDetail = { userId ->
                            // Navigate to Fan Detail
                            navController.navigate(Screen.FanDetail.createRoute(userId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Screen.FanDetail.route,
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId")
                if (userId != null && currentUser != null) {
                    // Reusing ProfileScreen for Admin View of Fan
                    com.kelompok1.fandomhub.ui.profile.ProfileScreen(
                        repository = repository,
                        currentUser = currentUser!!,
                        userIdToDisplay = userId,
                        onBack = { navController.popBackStack() },
                        // Actions below are hidden in ProfileScreen when !isSelf, but must be passed
                        onLogout = {}, 
                        onNavigateToAdmin = {},
                        onNavigateToArtistDashboard = {},
                        onNavigateToFandom = { artistId -> navController.navigate(Screen.FandomDetail.createRoute(artistId)) }, // Following list might use this
                        onNavigateToSavedPosts = {},
                        onNavigateToSubscriptions = {}
                    )
                }
            }

            composable(Screen.FandomDiscovery.route) {
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.home.FandomDiscoveryScreen(
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        onNavigateToDetail = { artistId ->
                            navController.navigate(Screen.FandomDetail.createRoute(artistId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.FollowedMerch.route) {
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.home.FollowedMerchScreen(
                        repository = repository,
                        currentUser = currentUser!!,
                        onNavigateToProduct = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.Search.route) {
                if (currentUser != null) {
                    SearchScreen(
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        onBack = { navController.popBackStack() },
                        onNavigateToFandom = { artistId ->
                            navController.navigate(Screen.FandomDetail.createRoute(artistId))
                        },
                        onNavigateToPost = { postId ->
                            navController.navigate(Screen.PostDetail.createRoute(postId))
                        },
                        onNavigateToProduct = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        }
                    )
                }
            }
            composable(
                route = Screen.ProductDetail.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(300)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(300)) }
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull()
                if (productId != null) {
                    com.kelompok1.fandomhub.ui.market.ProductDetailScreen(
                        productId = productId,
                        repository = repository,
                        navController = navController,
                        currentUserId = currentUser?.id ?: 0
                    )
                }
            }
            composable(Screen.Cart.route) {
                com.kelompok1.fandomhub.ui.market.CartScreen(
                    repository = repository,
                    navController = navController,
                    currentUserId = currentUser?.id ?: 0
                )
            }
            composable(Screen.Checkout.route) {
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.market.CheckoutScreen(
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        onBack = { navController.popBackStack() },
                        onPaymentSuccess = {
                            navController.navigate(Screen.Marketplace.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                }
            }
            composable(
                route = Screen.MessageList.route,
                arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "SOCIAL" })
            ) { backStackEntry ->
                val user = currentUser
                val tabInfo = backStackEntry.arguments?.getString("tab") ?: "SOCIAL"
                if(user != null) {
                    com.kelompok1.fandomhub.ui.message.MessageListScreen(
                        repository, 
                        user,
                        initialTab = tabInfo,
                        onNavigateToChat = { userId, type ->
                            navController.navigate(Screen.ChatDetail.createRoute(userId, type))
                        },
                        onManageSubscription = {
                            navController.navigate(Screen.ManageSubscription.route)
                        }
                    )
                }
            }
            
            composable(Screen.ManageSubscription.route) {
                val user = currentUser
                if (user != null && user.role == "ARTIST") {
                    com.kelompok1.fandomhub.ui.subscription.ManageSubscriptionScreen(
                        repository = repository,
                        currentUserId = user.id,
                        onNavigateToChat = { userId ->
                            navController.navigate(Screen.ChatDetail.createRoute(userId, "SOCIAL"))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.Notifications.route) {
                if (currentUser != null) {
                    NotificationScreen(
                        repository = repository,
                        currentUser = currentUser!!,
                        onNavigateToPost = { postId ->
                            navController.navigate(Screen.PostDetail.createRoute(postId))
                        },
                        onNavigateToMerch = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        },
                        onNavigateToFollowers = { artistId ->
                            navController.navigate(Screen.FandomFollowers.createRoute(artistId))
                        }
                    )
                }
            }
            composable(Screen.More.route) {
                val scope = rememberCoroutineScope()
                com.kelompok1.fandomhub.ui.more.MoreScreen(
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToMarketplace = { navController.navigate(Screen.Marketplace.route) }
                )
            }
            composable(Screen.Marketplace.route) {
                com.kelompok1.fandomhub.ui.market.MarketplaceScreen(
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                    onNavigateToOrderHistory = { navController.navigate(Screen.OrderHistory.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.OrderHistory.route) {
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.market.OrderHistoryScreen(
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        onBack = { navController.popBackStack() },
                        onNavigateToOrderDetail = { orderId ->
                            navController.navigate(Screen.OrderDetail.createRoute(orderId))
                        },
                        onNavigateToChat = { userId ->
                            navController.navigate(Screen.ChatDetail.createRoute(userId, "MARKET"))
                        }
                    )

                }
            }
            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(navArgument("orderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getInt("orderId")
                if (orderId != null) {
                    com.kelompok1.fandomhub.ui.market.OrderDetailScreen(
                        repository = repository,
                        orderId = orderId,
                        onBack = { navController.popBackStack() },
                        onNavigateToChat = { userId ->
                            navController.navigate(Screen.ChatDetail.createRoute(userId, "MARKET"))
                        }
                    )
                }
            }
            composable(Screen.Profile.route) {
                if(currentUser != null) {
                    ProfileScreen(
                        repository = repository, 
                        currentUser = currentUser!!, 
                        onLogout = { doLogout() }, 
                        onNavigateToAdmin = {
                             navController.navigate(Screen.AdminUserManagement.route)
                        },
                        onNavigateToArtistDashboard = {
                             navController.navigate(Screen.ArtistDashboard.route)
                        },
                        onNavigateToFandom = { artistId ->
                            navController.navigate(Screen.FandomDetail.createRoute(artistId))
                        },
                        onNavigateToSavedPosts = {
                            navController.navigate(Screen.SavedPosts.route)
                        },
                        onNavigateToSubscriptions = {
                            navController.navigate(Screen.MySubscriptions.route)
                        },
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0)
                                }
                            }
                        }
                    )
                }
            }
            
            composable(Screen.ArtistDashboard.route) {
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.home.ArtistDashboardScreen(
                        repository = repository,
                        artist = currentUser!!,
                         onNavigateToPost = { postId ->
                            navController.navigate(Screen.PostDetail.createRoute(postId))
                        }
                    )
                }
            }
            
            composable(Screen.FandomManagement.route) {
                if (currentUser != null && currentUser!!.role == "ARTIST") {
                    com.kelompok1.fandomhub.ui.fandom.FandomManagementScreen(
                        repository = repository,
                        artist = currentUser!!,
                        onNavigateToMessageList = { tab ->
                             navController.navigate(Screen.MessageList.createRoute(tab))
                        },
                        onNavigateTo = { destination ->
                            when(destination) {
                                "profile" -> navController.navigate(Screen.ManageFandomProfile.route)
                                "products" -> navController.navigate(Screen.ManageProducts.route)
                                "orders" -> navController.navigate(Screen.ManageOrders.route)
                                "subscription" -> navController.navigate(Screen.ManageSubscription.route)
                                "inquiries" -> navController.navigate(Screen.MarketChats.route)
                                "statistics" -> navController.navigate(Screen.FandomStatistics.route)
                            }
                        }
                    )
                }
            }
            
            composable(Screen.ManageFandomProfile.route) {
                if (currentUser != null && currentUser!!.role == "ARTIST") {
                     com.kelompok1.fandomhub.ui.fandom.ManageFandomProfileScreen(
                         repository = repository,
                         artist = currentUser!!,
                         onBack = { navController.popBackStack() }
                     )
                }
            }

            composable(Screen.ManageProducts.route) {
                if (currentUser != null && currentUser!!.role == "ARTIST") {
                    com.kelompok1.fandomhub.ui.fandom.ManageProductsScreen(
                        repository = repository,
                        artistId = currentUser!!.id,
                        onBack = { navController.popBackStack() },
                        onNavigateToProductForm = { productId ->
                             navController.navigate(Screen.ProductForm.createRoute(productId))
                        },
                        onNavigateToMonitoring = { productId ->
                            navController.navigate(Screen.ProductMonitoring.createRoute(productId))
                        }
                    )
                }
            }

            composable(
                route = Screen.ProductMonitoring.route,
                arguments = listOf(navArgument("productId") { type = NavType.IntType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getInt("productId")
                if (productId != null) {
                    com.kelompok1.fandomhub.ui.fandom.ProductMonitoringScreen(
                        repository = repository,
                        productId = productId,
                        onBack = { navController.popBackStack() },
                        onNavigateToEdit = {
                            navController.navigate(Screen.ProductForm.createRoute(productId))
                        }
                    )
                }
            }

            composable(Screen.ManageOrders.route) {
                if (currentUser != null && currentUser!!.role == "ARTIST") {
                     com.kelompok1.fandomhub.ui.fandom.ManageOrdersScreen(
                         repository = repository,
                         artistId = currentUser!!.id,
                         onBack = { navController.popBackStack() },
                         onNavigateToChat = { userId ->
                             navController.navigate(Screen.ChatDetail.createRoute(userId, "MARKET"))
                         }
                     )
                }
            }

            composable(
                route = Screen.ProductForm.route,
                arguments = listOf(navArgument("productId") { type = NavType.IntType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getInt("productId").takeIf { it != -1 }
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.profile.ProductFormScreen(
                        repository = repository,
                        artistId = currentUser!!.id,
                        productId = productId,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
            }
            
            composable(Screen.AdminUserManagement.route) {
                if (currentUser?.role == "ADMIN") {
                    AdminUserManagementScreen(repository = repository) {
                        navController.popBackStack()
                    }
                }
            }
            composable(Screen.AdminReports.route) {
                if (currentUser?.role == "ADMIN") {
                    com.kelompok1.fandomhub.ui.admin.AdminReportListScreen(
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onNavigateToProduct = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                        onNavigateToFandom = { id -> navController.navigate(Screen.FandomDetail.createRoute(id)) },

                        onNavigateToPost = { id -> navController.navigate(Screen.PostDetail.createRoute(id)) },
                        onNavigateToChat = { u1, u2 -> navController.navigate(Screen.AdminChatReview.createRoute(u1, u2)) }
                    )
                }
            }

            composable(Screen.AdminSupportChatList.route) {
                if (currentUser?.role == "ADMIN") {
                    com.kelompok1.fandomhub.ui.admin.AdminSupportChatListScreen(
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        onNavigateToChat = { userId ->
                            navController.navigate(Screen.ChatDetail.createRoute(userId, "SUPPORT"))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            
            composable(
                route = Screen.FandomDetail.route,
                arguments = listOf(navArgument("artistId") { type = NavType.IntType })
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getInt("artistId")
                if (artistId != null && currentUser != null) {
                    FandomDetailScreen(
                        repository = repository,
                        artistId = artistId,
                        currentUserId = currentUser!!.id,
                        onBack = { navController.popBackStack() },
                        onNavigateToPost = { postId ->
                            navController.navigate(Screen.PostDetail.createRoute(postId))
                        },
                        onNavigateToProduct = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        },
                        onNavigateToCart = {
                            navController.navigate(Screen.Cart.route)
                        },
                        onNavigateToChat = { userId ->
                            navController.navigate(Screen.ChatDetail.createRoute(userId))
                        },
                        onNavigateToCheckout = { aId ->
                             navController.navigate(Screen.SubscriptionCheckout.createRoute(aId))
                        }
                    )
                }
            }
            
            composable(
                route = Screen.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.IntType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId")
                if (postId != null && currentUser != null) {
                    PostDetailScreen(
                        postId = postId,
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            
            composable(
                route = Screen.ChatDetail.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("type") { type = NavType.StringType; defaultValue = "SOCIAL" }
                )
            ) { backStackEntry ->
                val otherUserId = backStackEntry.arguments?.getInt("userId")
                val chatType = backStackEntry.arguments?.getString("type") ?: "SOCIAL"
                
                if (otherUserId != null && currentUser != null) {
                    com.kelompok1.fandomhub.ui.chat.ChatScreen(
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        otherUserId = otherUserId,
                        chatType = chatType,
                        onBack = { navController.popBackStack() },
                        onNavigateToCheckout = { artistId ->
                            navController.navigate(Screen.SubscriptionCheckout.createRoute(artistId))
                        }
                    )
                }
            }

            composable(Screen.SubscriptionCheckout.route) {
                val artistId = it.arguments?.getString("artistId")?.toInt()
                if (artistId != null && currentUser != null) {
                   com.kelompok1.fandomhub.ui.subscription.SubscriptionCheckoutScreen(
                        repository = repository,
                        currentUserId = currentUser!!.id,
                        artistId = artistId,
                        onBack = { navController.popBackStack() },
                        onSuccess = {
                            navController.popBackStack()
                        }
                   )
                }
            }

            composable(Screen.SavedPosts.route) {
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.profile.SavedPostsScreen(
                        currentUser = currentUser!!,
                        repository = repository,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPost = { postId ->
                            navController.navigate(Screen.PostDetail.createRoute(postId))
                        }
                    )
                }
            }

            composable(Screen.MySubscriptions.route) {
                if (currentUser != null) {
                    com.kelompok1.fandomhub.ui.profile.MySubscriptionsScreen(
                        currentUser = currentUser!!,
                        repository = repository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            
            composable(Screen.FandomStatistics.route) {
                if (currentUser != null && currentUser!!.role == "ARTIST") {
                    val artistId = currentUser!!.id
                    com.kelompok1.fandomhub.ui.fandom.FandomStatisticsScreen(
                        artistId = artistId, // No more fandomId
                        repository = repository,
                        onNavigateToFollowers = { aId ->
                            navController.navigate(Screen.FandomFollowers.createRoute(aId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            
            composable(Screen.MarketChats.route) {
                if (currentUser != null && currentUser!!.role == "ARTIST") {
                    com.kelompok1.fandomhub.ui.message.MarketChatsScreen(
                        repository = repository,
                        currentUser = currentUser!!,
                        onBack = { navController.popBackStack() },
                        onNavigateToChat = { userId ->
                            navController.navigate(Screen.ChatDetail.createRoute(userId, "MARKET"))
                        }
                    )
                }
            }

            composable(
                route = Screen.FandomFollowers.route,
                arguments = listOf(navArgument("artistId") { type = NavType.IntType })
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getInt("artistId")
                if (artistId != null) {
                    com.kelompok1.fandomhub.ui.fandom.FandomFollowersScreen(
                        artistId = artistId,
                        repository = repository,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Screen.AdminChatReview.route,
                arguments = listOf(
                    navArgument("user1Id") { type = NavType.IntType },
                    navArgument("user2Id") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val user1Id = backStackEntry.arguments?.getInt("user1Id")
                val user2Id = backStackEntry.arguments?.getInt("user2Id")
                if (user1Id != null && user2Id != null) {
                    com.kelompok1.fandomhub.ui.admin.AdminChatReviewScreen(
                        repository = repository,
                        user1Id = user1Id,
                        user2Id = user2Id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
