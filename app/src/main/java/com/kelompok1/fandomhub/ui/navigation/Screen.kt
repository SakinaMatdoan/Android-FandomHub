package com.kelompok1.fandomhub.ui.navigation

sealed class Screen(val route: String) {
    object AdminChatReview : Screen("admin/chat_review/{user1Id}/{user2Id}") {
        fun createRoute(user1Id: Int, user2Id: Int) = "admin/chat_review/$user1Id/$user2Id"
    }
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Main : Screen("main") // Holds BottomBar
    
    // Bottom Tabs
    object Home : Screen("home")
    object Search : Screen("search")
    object MessageList : Screen("message_list?tab={tab}") {
        fun createRoute(tab: String = "SOCIAL") = "message_list?tab=$tab"
    }
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    
    // Details
    object ChatDetail : Screen("chat/{userId}?type={type}") {
        fun createRoute(userId: Int, type: String = "SOCIAL") = "chat/$userId?type=$type"
    }
    object FandomDetail : Screen("fandom/{artistId}") {
        fun createRoute(artistId: Int) = "fandom/$artistId"
    }
    object MerchStore : Screen("merch/{artistId}") {
        fun createRoute(artistId: Int) = "merch/$artistId"
    }
    object AdminUserManagement : Screen("admin_users")
    object AdminReports : Screen("admin_reports")
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: Int) = "post_detail/$postId"
    }
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: Int) = "product/$productId"
    }
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object More : Screen("more")
    object Marketplace : Screen("marketplace") // Menu for Cart & Orders
    object OrderHistory : Screen("order_history")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: Int) = "order_detail/$orderId"
    }
    object ArtistDashboard : Screen("artist_dashboard")
    object FandomDiscovery : Screen("fandom_discovery")
    object FandomManagement : Screen("fandom_management")
    object ManageFandomProfile : Screen("manage_fandom_profile")
    object ManageProducts : Screen("manage_products")
    object ManageOrders : Screen("manage_orders")
    object FollowedMerch : Screen("followed_merch")
    object ProductForm : Screen("product_form?productId={productId}") {
        fun createRoute(productId: Int? = null) = "product_form?productId=${productId ?: -1}"
    }
    object SubscriptionCheckout : Screen("subscription_checkout/{artistId}") {
        fun createRoute(artistId: Int) = "subscription_checkout/$artistId"
    }
    object ManageSubscription : Screen("manage_subscription")
    object SavedPosts : Screen("saved_posts")
    object MySubscriptions : Screen("my_subscriptions")
    object FandomStatistics : Screen("fandom_statistics")
    object FandomFollowers : Screen("fandom_followers/{artistId}") {
        fun createRoute(artistId: Int) = "fandom_followers/$artistId"
    }
    object ProductMonitoring : Screen("product_monitoring/{productId}") {
        fun createRoute(productId: Int) = "product_monitoring/$productId"
    }
    object MarketChats : Screen("market_chats")
    object Support : Screen("support")
    
    // Admin Routes
    object AdminArtistList : Screen("admin_artist_list")
    object AdminFanList : Screen("admin_fan_list")
    object FanDetail : Screen("fan_detail/{userId}") {
        fun createRoute(userId: Int) = "fan_detail/$userId"
    }
    object AdminSupportChatList : Screen("admin_support_chat_list")
}
