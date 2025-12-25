package com.kelompok1.fandomhub.data

import com.kelompok1.fandomhub.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar // For Date grouping

data class DailyStat(val date: String, val count: Int)
data class TopFan(val user: UserEntity, val interactionCount: Int)
data class OrderStatusStat(val status: String, val count: Int)

data class FandomStats(
    val totalFollowers: Int,
    val totalPosts: Int,
    val totalLikes: Int, // Post Likes
    val totalComments: Int,
    val totalRevenue: Double,
    val subscriptionRevenue: Double,
    val orderRevenue: Double,
    val totalSubscribers: Int,
    val totalOrders: Int,
    val topProducts: List<ProductEntity>,
    val topFans: List<TopFan>,
    val followerGrowth: List<DailyStat>,
    val postActivity: List<DailyStat>,
    val engagementActivity: List<DailyStat>, // Likes + Comments
    val revenueGrowth: List<DailyStat>
)

class FandomRepository(private val db: FandomDatabase) {
    val userDao = db.userDao()
    // val fandomDao = db.fandomDao() REMOVED
    val postDao = db.postDao()
    val interactionDao = db.interactionDao()
    val messageDao = db.messageDao()
    val marketDao = db.marketDao()
    val blockDao = db.blockDao()
    val notificationDao = db.notificationDao()

    // Notifications
    fun getUnreadNotificationCount(userId: Int) = notificationDao.getUnreadCount(userId)
    suspend fun markAllNotificationsRead(userId: Int) = notificationDao.markAllAsRead(userId)
    suspend fun deleteAllNotifications(userId: Int) = notificationDao.deleteAllNotifications(userId)

    data class NotificationUiModel(
        val type: String,
        val title: String, // Calculated
        val message: String, // Calculated
        val timestamp: Long,
        val avatar: String?,
        val senderId: Int, // Main sender
        val referenceId: Int // Navigation
    )

    fun getNotifications(userId: Int): Flow<List<NotificationUiModel>> {
        return notificationDao.getNotifications(userId)
            .map { list ->
                val grouped = list.groupBy { 
                    if (it.notification.type == "POST" || it.notification.type == "MERCH") {
                        // Group by Sender (Artist)
                        Pair(it.notification.type, it.notification.senderId)
                    } else {
                        // Interactions: Group by Post (Reference)
                        Pair(it.notification.type, it.notification.referenceId)
                    }
                }

                grouped.map { (key, notifications) ->
                    val type = key.first
                    // Latest notification first
                    val sorted = notifications.sortedByDescending { it.notification.timestamp }
                    val latest = sorted.first()
                    val count = sorted.size

                    // Logic for text
                    // Handle nullable sender (e.g. System notifications or deleted users)
                    val senderName = latest.sender?.fullName ?: "FandomHub"
                    val otherCount = count - 1
                    
                    val (title, message) = when (type) {
                        "POST" -> {
                            if (count > 1) {
                                Pair("New Posts", "$senderName posted $count new updates")
                            } else {
                                Pair("New Post", "$senderName posted a new update")
                            }
                        }
                        "MERCH" -> {
                            if (count > 1) {
                                Pair("New Merch", "$senderName added $count new items")
                            } else {
                                Pair("New Merch", "$senderName added a new item")
                            }
                        }
                        "LIKE_POST" -> {
                            if (otherCount > 0) {
                                Pair("Post Liked", "$senderName and $otherCount others liked your post")
                            } else {
                                Pair("Post Liked", "$senderName liked your post")
                            }
                        }
                        "COMMENT" -> {
                            if (otherCount > 0) {
                                Pair("New Comments", "$senderName and $otherCount others commented on your post")
                            } else {
                                Pair("New Comment", "$senderName commented on your post")
                            }
                        }
                        "REPLY" -> {
                             if (otherCount > 0) {
                                Pair("New Replies", "$senderName and $otherCount others replied to your comment")
                            } else {
                                Pair("New Reply", "$senderName replied to your comment")
                            }
                        }
                        "FOLLOW" -> {
                             if (otherCount > 0) {
                                Pair("New Followers", "$senderName and $otherCount others started following you")
                            } else {
                                Pair("New Follower", "$senderName started following you")
                            }
                        }
                        // System Notifications (WARNING, REPORT_RESOLVED) fall here
                        else -> Pair(latest.notification.title, latest.notification.message)
                    }

                    NotificationUiModel(
                        type = type,
                        title = title,
                        message = message,
                        timestamp = latest.notification.timestamp,
                        avatar = latest.sender?.profileImage, // Show sender avatar or null
                        senderId = latest.notification.senderId,
                        referenceId = latest.notification.referenceId
                    )
                }.sortedByDescending { it.timestamp }
            }
    }

    // Auth & User
    suspend fun createUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun getUser(username: String) = userDao.getUserByUsername(username)
    suspend fun getUserByUsername(username: String) = userDao.getUserByUsername(username)
    suspend fun getUserById(userId: Int) = userDao.getUserById(userId)
    
    // Auth (Simplified)
    suspend fun getUserByEmail(email: String) = userDao.getUserByEmail(email)
    fun getUserByIdFlow(id: Int) = userDao.getUserByIdFlow(id)
    suspend fun registerUser(user: UserEntity) = userDao.insertUser(user)
    
    // Admin
    suspend fun getSystemAdmin() = userDao.getSystemAdmin()
    fun getPendingArtists() = userDao.getPendingArtists()
    suspend fun approveArtist(artist: UserEntity) {
        // 1. Update User Status
        userDao.updateUserStatus(artist.id, "ACTIVE")
        // No need to create FandomEntity anymore
    }
    suspend fun rejectArtist(artistId: Int) = userDao.updateUserStatus(artistId, "REJECTED")

    // Admin Dashboard
    fun getArtistCount() = userDao.getArtistCount()
    fun getFanCount() = userDao.getFanCount()
    fun getAllFans() = userDao.getAllFans()
    fun getAllArtistsAdmin() = userDao.getAllArtistsAdmin()

    // Artists (Replaces Fandoms) & Posts
    fun getAllArtists() = userDao.getAllActiveArtists() // Replaces getAllFandoms
    suspend fun getArtistById(id: Int) = userDao.getUserById(id) // Replaces getFandomById
    
    // Search Artists
    fun searchArtists(query: String) = userDao.searchArtists(query)
    
    fun getPostsByArtist(artistId: Int) = postDao.getPostsByArtist(artistId)
    fun getAllPosts() = postDao.getAllPosts()
    fun getPostsByAuthor(authorId: Int) = postDao.getPostsByAuthor(authorId)
    fun getFanThreads(artistId: Int) = postDao.getFanThreads(artistId)
    suspend fun getPostById(id: Int) = postDao.getPostById(id)
    fun getFeedPosts(userId: Int) = postDao.getFeedPosts(userId)
    fun searchPosts(query: String) = postDao.searchPosts(query)

    suspend fun createPost(post: PostEntity) {
        val id = postDao.insertPost(post)
        
        // Notify Followers of Artist
        // post.artistId is the "Fandom" context (Artist Wall)
        val followers = interactionDao.getFollowersList(post.artistId)
        followers.forEach { follower ->
             if (follower.id != post.authorId) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        userId = follower.id,
                        senderId = post.authorId,
                        type = "POST",
                        referenceId = id.toInt(),
                        title = "New Post",
                        message = "New update from your artist",
                        timestamp = System.currentTimeMillis()
                    )
                )
             }
        }
    }
    
    suspend fun deletePost(postId: Int) = postDao.deletePost(postId)
    
    
    suspend fun updatePost(postId: Int, content: String, images: List<String> = emptyList()) = postDao.updatePostContent(postId, content, images, isEdited = true)

    // Statistics
    suspend fun getFandomStatistics(artistId: Int, days: Int = 30): FandomStats {
         val calendar = Calendar.getInstance()
         calendar.add(Calendar.DAY_OF_YEAR, -days)
         val startTime = calendar.timeInMillis
         
         val artist = userDao.getUserById(artistId)
         // If artist is null, return empty or crash (shouldn't happen in valid flow)
         
         // 1. Fetch Raw Data since startTime (For Graphs)
         val follows = interactionDao.getFollowsAfter(artistId, startTime)
         val posts = postDao.getPostsAfter(artistId, startTime)
         val likes = interactionDao.getLikesAfter(artistId, startTime)
         val comments = interactionDao.getCommentsAfter(artistId, startTime)
         val orders = marketDao.getOrdersAfter(artistId, 0) // Fetch ALL orders for total revenue logic if needed, or filter.
         // Actually RevenueGrowth uses orders. Let's use orders since start time for growth, but total for summary.
         
         // 2. Fetch Totals
         val totalFollowers = interactionDao.getFollowerCount(artistId).firstOrNull() ?: 0
         
         // Scoped Counts (Artist posts only for counts, usually)
         // But "totalPosts" on dashboard usually means "All posts in my fandom" or "My posts"?
         // Typically Artist Dashboard shows "My Posts". But Fandom Stats might show community activity.
         // Let's assume "Community Activity" for graphs, but explicit counts:
         val totalPosts = postDao.countPostsByArtist(artistId) // Own posts
         val totalLikes = interactionDao.getAllLikesOnArtistPosts(artistId).size
         val totalComments = interactionDao.getAllCommentsOnArtistPosts(artistId).size
         
         // Market & Subscription Revenue
         val allSubscriptionsCount = interactionDao.getAllSubscriptionsCount(artistId)
         val subscriptionPrice = artist?.subscriptionPrice ?: 0.0
         val subscriptionRevenue = allSubscriptionsCount * subscriptionPrice
         
         val totalOrders = orders.size
         val orderRevenue = orders.sumOf { it.totalAmount }
         
         val totalRevenue = subscriptionRevenue + orderRevenue
         
         val activeSubs = interactionDao.getActiveSubscriptions(artistId, System.currentTimeMillis())
         val totalSubscribers = activeSubs.size

         // 3. Detailed Lists
         // Top Products
         val topProducts = marketDao.getTopSellingProducts(artistId, 5).firstOrNull() ?: emptyList()
         
         // Top Fans
         val artistPostLikes = interactionDao.getAllLikesOnArtistPosts(artistId)
         val artistPostComments = interactionDao.getAllCommentsOnArtistPosts(artistId)
         
         val fanInteractions = mutableMapOf<Int, Int>()
         artistPostLikes.forEach { fanInteractions[it.userId] = (fanInteractions[it.userId] ?: 0) + 1 }
         artistPostComments.forEach { fanInteractions[it.userId] = (fanInteractions[it.userId] ?: 0) + 2 } 
         
         val topFanIds = fanInteractions.filterKeys { it != artistId }.entries.sortedByDescending { it.value }.take(5).map { it.key }
         val topFans = topFanIds.mapNotNull { id ->
             val user = userDao.getUserById(id)
             if (user != null) TopFan(user, fanInteractions[id] ?: 0) else null
         }
         
         // 4. Helper to Group by Day
         fun getLastXDaysStats(timestamps: List<Long>): List<DailyStat> {
             val result = mutableListOf<DailyStat>()
             val map = mutableMapOf<String, Int>()
             timestamps.forEach { ts ->
                val key =  java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ts))
                map[key] = (map[key] ?: 0) + 1
             }
             for (i in (days - 1) downTo 0) {
                 val d = java.util.Date(System.currentTimeMillis() - (i * 86400000L))
                 val key = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(d)
                 result.add(DailyStat(key, map[key] ?: 0))
             }
             return result
         }
         
         // Revenue Growth
         fun getRevenueGrowth(ordersList: List<OrderEntity>): List<DailyStat> {
             val result = mutableListOf<DailyStat>()
             val map = mutableMapOf<String, Int>() 
             ordersList.forEach { order ->
                if (order.timestamp >= startTime) { 
                    val key =  java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp))
                    map[key] = (map[key] ?: 0) + order.totalAmount.toInt()
                }
             }
             for (i in (days - 1) downTo 0) {
                 val d = java.util.Date(System.currentTimeMillis() - (i * 86400000L))
                 val key = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(d)
                 result.add(DailyStat(key, map[key] ?: 0))
             }
             return result
         }

         return FandomStats(
             totalFollowers = totalFollowers,
             totalPosts = totalPosts,
             totalLikes = totalLikes,
             totalComments = totalComments,
             totalRevenue = totalRevenue,
             subscriptionRevenue = subscriptionRevenue,
             orderRevenue = orderRevenue,
             totalSubscribers = totalSubscribers,
             totalOrders = totalOrders,
             topProducts = topProducts,
             topFans = topFans,
             followerGrowth = getLastXDaysStats(follows.map { it.timestamp }),
             postActivity = getLastXDaysStats(posts.map { it.timestamp }), 
             engagementActivity = getLastXDaysStats(likes.map { it.timestamp } + comments.map { it.timestamp }),
             revenueGrowth = getRevenueGrowth(orders)
         )
    }

    // Interactions
    fun isLiked(postId: Int, userId: Int) = interactionDao.isLiked(postId, userId)
    fun getAllLikes() = interactionDao.getAllLikes()
    fun getLikeCount(postId: Int) = interactionDao.getLikeCount(postId)
    
    suspend fun toggleLike(postId: Int, userId: Int, isLiked: Boolean) {
        if (isLiked) interactionDao.deleteLike(postId, userId)
        else {
            interactionDao.insertLike(LikeEntity(postId = postId, userId = userId))
            // Create Notification
            val post = postDao.getPostById(postId)
            if (post != null && post.authorId != userId) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        userId = post.authorId,
                        senderId = userId,
                        type = "LIKE_POST",
                        referenceId = postId,
                        title = "New Like",
                        message = "Someone liked your post",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun getComments(postId: Int) = interactionDao.getComments(postId)
    fun getCommentCount(postId: Int) = interactionDao.getCommentCount(postId)
    suspend fun getCommentById(commentId: Int) = interactionDao.getCommentById(commentId)
    
    suspend fun addComment(comment: CommentEntity) {
        interactionDao.insertComment(comment)
        
        val post = postDao.getPostById(comment.postId) ?: return
        
        // 1. Notify Post Author
        if (post.authorId != comment.userId) {
             notificationDao.insertNotification(
                NotificationEntity(
                    userId = post.authorId,
                    senderId = comment.userId,
                    type = "COMMENT",
                    referenceId = comment.postId,
                    title = "New Comment",
                    message = "Someone commented on your post",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // 2. Notify Parent Comment Author
        if (comment.parentId != null) {
            val parent = interactionDao.getCommentById(comment.parentId)
            if (parent != null && parent.userId != comment.userId && parent.userId != post.authorId) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        userId = parent.userId,
                        senderId = comment.userId,
                        type = "REPLY",
                        referenceId = comment.postId,
                        title = "New Reply",
                        message = "Someone replied to your comment",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    suspend fun deleteComment(commentId: Int) = interactionDao.deleteComment(commentId)

    suspend fun updateComment(commentId: Int, content: String) = interactionDao.updateCommentText(commentId, content, isEdited = true)
    
    fun isCommentLiked(commentId: Int, userId: Int) = interactionDao.isCommentLiked(commentId, userId)
    
    @androidx.room.Transaction
    suspend fun toggleCommentLike(commentId: Int, userId: Int, isLiked: Boolean) {
        if (isLiked) {
            interactionDao.deleteCommentLike(commentId, userId)
            interactionDao.decrementCommentLike(commentId)
        } else {
            interactionDao.insertCommentLike(CommentLikeEntity(commentId = commentId, userId = userId))
            interactionDao.incrementCommentLike(commentId)
        }
    }

    fun isSaved(postId: Int, userId: Int): Flow<Boolean> = interactionDao.isSaved(postId, userId)
    fun getSavedCount(postId: Int): Flow<Int> = interactionDao.getSavedCount(postId)
    suspend fun toggleSave(postId: Int, userId: Int, isSaved: Boolean) {
        if (isSaved) interactionDao.deleteSavedPost(postId, userId)
        else interactionDao.insertSavedPost(SavedPostEntity(postId = postId, userId = userId))
    }

    fun isFollowing(userId: Int, artistId: Int) = interactionDao.isFollowing(userId, artistId)
    suspend fun toggleFollow(userId: Int, artistId: Int, isFollowing: Boolean) {
        if (isFollowing) {
            interactionDao.deleteFollow(userId, artistId)
        } else {
            interactionDao.insertFollow(FollowEntity(followerId = userId, artistId = artistId))
            // Notify Artist
            // Logic: artistId IS the User ID of the artist
            if (artistId != userId) {
                 notificationDao.insertNotification(
                    NotificationEntity(
                        userId = artistId,
                        senderId = userId,
                        type = "FOLLOW",
                        referenceId = artistId,
                        title = "New Follower",
                        message = "started following you",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun getFollowedArtists(userId: Int): Flow<List<UserEntity>> = interactionDao.getFollowedArtists(userId)
    suspend fun getAllFollows() = interactionDao.getAllFollows()
    fun getFollowerCount(artistId: Int) = interactionDao.getFollowerCount(artistId)
    
    fun getPostLikers(postId: Int) = interactionDao.getPostLikers(postId)
    fun getCommentLikers(commentId: Int) = interactionDao.getCommentLikers(commentId)
    
    fun getSavedPosts(userId: Int): kotlinx.coroutines.flow.Flow<List<PostWithAuthor>> = interactionDao.getSavedPosts(userId) 
    fun getMySubscriptions(userId: Int): kotlinx.coroutines.flow.Flow<List<SubscriptionEntity>> = kotlinx.coroutines.flow.flowOf(emptyList()) // Placeholder
    
    fun getFollowers(artistId: Int): kotlinx.coroutines.flow.Flow<List<UserEntity>> = interactionDao.getFollowers(artistId)
    fun getRecentFollowers(artistId: Int): kotlinx.coroutines.flow.Flow<List<UserEntity>> = interactionDao.getRecentFollowers(artistId)

    // Block System
    suspend fun reportUser(report: ReportEntity): Boolean {
        // Check for duplicate
        val exists = blockDao.hasExistingReport(report.reporterId, report.type, report.referenceId ?: 0)
        if (exists) return false // Already reported
        blockDao.insertReport(report)
        return true
    }
    
    fun getPendingReports(): Flow<List<ReportEntity>> = blockDao.getPendingReports()
    fun getResolvedReports(): Flow<List<ReportEntity>> = blockDao.getResolvedReports()
    fun getAllReports(): Flow<List<ReportEntity>> = blockDao.getAllReports()
    suspend fun hasExistingReport(reporterId: Int, type: String, referenceId: Int) = blockDao.hasExistingReport(reporterId, type, referenceId)
    
    suspend fun resolveReport(reportId: Int, action: String, adminNotice: String) {
        blockDao.updateReportStatus(reportId, "RESOLVED", action)
    }
    
    suspend fun suspendUser(reportId: Int, userId: Int, durationMillis: Long, reporterId: Int) {
        userDao.suspendUser(userId, true, System.currentTimeMillis() + durationMillis)
        blockDao.updateReportStatus(reportId, "RESOLVED", "SUSPEND")
        
        // Notify reporter
        val days = durationMillis / (24 * 60 * 60 * 1000)
        notificationDao.insertNotification(
            NotificationEntity(
                userId = reporterId,
                senderId = 0, // System
                type = "REPORT_RESOLVED",
                referenceId = reportId,
                title = "Report Resolved",
                message = "Your report has been reviewed. The user has been suspended for $days days.",
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun warnUser(reportId: Int, userId: Int, reporterId: Int) {
        blockDao.updateReportStatus(reportId, "RESOLVED", "WARNING")
        
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                senderId = 0,
                type = "WARNING",
                referenceId = reportId,
                title = "Account Warning",
                message = "You have received a warning due to reported content.",
                timestamp = System.currentTimeMillis()
            )
        )
        
        notificationDao.insertNotification(
            NotificationEntity(
                userId = reporterId,
                senderId = 0,
                type = "REPORT_RESOLVED",
                referenceId = reportId,
                title = "Report Resolved",
                message = "Your report has been reviewed. A warning has been issued.",
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun deleteUser(reportId: Int, userId: Int, reporterId: Int) {
        notificationDao.insertNotification(
            NotificationEntity(
                userId = reporterId,
                senderId = 0,
                type = "REPORT_RESOLVED",
                referenceId = reportId,
                title = "Report Resolved",
                message = "Your report has been reviewed. The user has been banned.",
                timestamp = System.currentTimeMillis()
            )
        )
        userDao.deleteUser(userId)
        blockDao.updateReportStatus(reportId, "RESOLVED", "DELETE")
    }

    // Direct Admin Actions (No Report)
    suspend fun suspendUserDirect(userId: Int, durationMillis: Long, adminId: Int) {
        userDao.suspendUser(userId, true, System.currentTimeMillis() + durationMillis)
        
        val days = durationMillis / (24 * 60 * 60 * 1000)
        // Notify User
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                senderId = 0, // System
                type = "WARNING", // Use WARNING type for generic system messages or similar
                referenceId = 0,
                title = "Account Suspended",
                message = "Your account has been suspended by an Administrator for $days days.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun warnUserDirect(userId: Int, adminId: Int, reason: String = "Policy Violation") {
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                senderId = 0,
                type = "WARNING",
                referenceId = 0,
                title = "Official Warning",
                message = "You have received an official warning from an Administrator: $reason",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun banUser(userId: Int) {
        userDao.suspendUser(userId, true, null) // Permanent
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                senderId = 0,
                type = "WARNING",
                referenceId = 0,
                title = "Account Banned",
                message = "Your account has been permanently banned by an Administrator.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun unsuspendUser(userId: Int) {
        userDao.suspendUser(userId, false, null)
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                senderId = 0,
                type = "WARNING", // Or SYSTEM
                referenceId = 0,
                title = "Account Restored",
                message = "Your account suspension has been lifted.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteUser(userId: Int) {
        userDao.deleteUser(userId)
    }

    suspend fun deleteUserDirect(userId: Int, adminId: Int) {
        userDao.deleteUser(userId)
    }
    
    suspend fun dismissReport(reportId: Int, reporterId: Int) {
        blockDao.updateReportStatus(reportId, "DISMISSED", null)
        notificationDao.insertNotification(
            NotificationEntity(
                userId = reporterId,
                senderId = 0,
                type = "REPORT_RESOLVED",
                referenceId = reportId,
                title = "Report Reviewed",
                message = "Your report has been reviewed and dismissed.",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Messages
    fun getChatHistory(user1: Int, user2: Int, type: String = "SOCIAL") = messageDao.getChatHistory(user1, user2, type)
    fun getSupportChatHistory(userId: Int) = messageDao.getSupportChatHistory(userId)
    suspend fun sendMessage(msg: MessageEntity) = messageDao.insertMessage(msg)
    
    fun getChatPartners(userId: Int, type: String = "SOCIAL"): Flow<List<Int>> {
        return messageDao.getChatPartnerIds(userId, type)
    }
    
    fun getChatUnreadCount(userId: Int, senderId: Int, type: String = "SOCIAL") = messageDao.getUnreadCount(userId, senderId, type)
    fun getTotalUnreadMessageCount(userId: Int) = messageDao.getTotalUnreadCount(userId) 
    fun getTotalUnreadCountByType(userId: Int, type: String) = messageDao.getTotalUnreadCountByType(userId, type)
    
    suspend fun getDebugAllMessages() = messageDao.getDebugAllMessages()

    fun getAllSupportChatPartners(currentAdminId: Int): Flow<List<Int>> {
        return messageDao.getAllSupportChatParticipants().map { allIds ->
            allIds.filter { it != currentAdminId }
        }
    }
    
    fun getGlobalSupportUnreadCount() = messageDao.getGlobalSupportUnreadCount()
    fun getSupportUnreadCountFromUser(userId: Int) = messageDao.getSupportUnreadCountFromUser(userId)

    suspend fun markChatAsRead(userId: Int, senderId: Int, type: String) = messageDao.markAsRead(userId, senderId, type)
    suspend fun deleteChat(user1: Int, user2: Int, type: String = "SOCIAL") = messageDao.deleteChat(user1, user2, type)

    // Subscriptions
    fun getSubscribedArtists(userId: Int) = interactionDao.getSubscribedArtists(userId, System.currentTimeMillis())
    fun getSubscribers(artistId: Int) = interactionDao.getSubscribers(artistId, System.currentTimeMillis())
    fun getSubscribersWithStatus(artistId: Int) = interactionDao.getSubscribersWithStatus(artistId, System.currentTimeMillis())
    suspend fun getSubscription(artistId: Int, userId: Int): SubscriptionEntity? = interactionDao.getSubscription(artistId, userId)
    suspend fun subscribe(userId: Int, artistId: Int, durationMillis: Long) {
        val validUntil = System.currentTimeMillis() + durationMillis
        val sub = SubscriptionEntity(
            userId = userId, 
            artistId = artistId, 
            startDate = System.currentTimeMillis(),
            validUntil = validUntil,
            isCancelled = false
        )
        interactionDao.insertSubscription(sub)
    }
    suspend fun cancelSubscription(userId: Int, artistId: Int) = interactionDao.cancelSubscription(artistId, userId)

    
    // Market & E-commerce
    fun getProductsByArtist(artistId: Int) = marketDao.getProductsByArtist(artistId) // Replaces getProductsByFandom
    fun getAllProducts() = marketDao.getAllProducts()
    fun getFeedProducts(userId: Int) = marketDao.getFeedProducts(userId)
    suspend fun getProductById(productId: Int) = marketDao.getProductById(productId)
    fun searchProducts(query: String) = marketDao.searchProducts(query)
    
    suspend fun addProduct(product: ProductEntity) {
        val id = marketDao.insertProduct(product)
        
        // Notify Followers
        val followers = interactionDao.getFollowersList(product.artistId)
        followers.forEach { follower ->
             if (follower.id != product.artistId) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        userId = follower.id,
                        senderId = product.artistId,
                        type = "MERCH",
                        referenceId = id.toInt(),
                        title = "New Merch",
                        message = "${product.name} is now available!",
                        timestamp = System.currentTimeMillis()
                    )
                )
             }
        }
    }

    suspend fun updateProduct(product: ProductEntity) = marketDao.updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = marketDao.deleteProduct(product)
    
    // Cart
    fun getCartItems(userId: Int): Flow<List<CartItemWithProduct>> = marketDao.getCartItems(userId)
    suspend fun addToCart(userId: Int, productId: Int, quantity: Int = 1) {
        val product = marketDao.getProductById(productId) ?: throw Exception("Product not found")
        val existingItem = marketDao.getCartItem(userId, productId)
        
        val currentQty = existingItem?.quantity ?: 0
        if (currentQty + quantity > product.stock) {
            throw Exception("Insufficient stock. Only ${product.stock} available.")
        }

        if (existingItem != null) {
            marketDao.updateCartItem(existingItem.copy(quantity = existingItem.quantity + quantity))
        } else {
            marketDao.insertCartItem(CartItemEntity(userId = userId, productId = productId, quantity = quantity))
        }
    }
    suspend fun updateCartItem(item: CartItemEntity) {
        val product = marketDao.getProductById(item.productId) ?: return
        if (item.quantity > product.stock) {
            throw Exception("Insufficient stock.")
        }
        marketDao.updateCartItem(item)
    }

    suspend fun removeCartItem(item: CartItemEntity) = marketDao.deleteCartItem(item)
    suspend fun clearCart(userId: Int) = marketDao.clearCart(userId)

    // Orders
    suspend fun createOrder(order: OrderEntity) {
        // 1. Decrement Stock
        val cartItems = marketDao.getCartItemsList(order.userId)
        cartItems.forEach { item ->
            val product = marketDao.getProductById(item.product.id) ?: throw Exception("Product not found")
            if (product.stock < item.cartItem.quantity) {
                 throw Exception("Insufficient stock for ${product.name}")
            }
        }

        // Commit Pass
        cartItems.forEach { item ->
            val product = marketDao.getProductById(item.product.id)!!
            val newStock = product.stock - item.cartItem.quantity
            marketDao.updateProduct(product.copy(stock = newStock))
        }
    
        marketDao.insertOrder(order)
        marketDao.clearCart(order.userId) 
    }
    fun getOrdersByUser(userId: Int) = marketDao.getOrdersByUser(userId)
    suspend fun getOrderById(orderId: Int) = marketDao.getOrderById(orderId)
    fun getOrdersByArtist(artistId: Int) = marketDao.getOrdersByArtist(artistId)
    
    suspend fun updateOrderStatus(orderId: Int, status: String) {
        marketDao.updateOrderStatus(orderId, status)
        
        // 2. Increment Sold Count if DELIVERED
        if (status == "DELIVERED") {
            val order = marketDao.getOrderById(orderId)
            if (order != null) {
                try {
                     // Very simple JSON parsing
                     val itemType = object : com.google.gson.reflect.TypeToken<List<com.kelompok1.fandomhub.ui.market.OrderItemSnapshot>>() {}.type
                     val items: List<com.kelompok1.fandomhub.ui.market.OrderItemSnapshot> = com.google.gson.Gson().fromJson(order.itemsJson, itemType)
                     
                     items.forEach { item ->
                         val product = marketDao.getProductById(item.productId)
                         if (product != null) {
                             val newSold = product.soldCount + item.quantity
                             marketDao.updateProduct(product.copy(soldCount = newSold))
                         }
                     }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Reviews
    suspend fun addReview(review: ReviewEntity) {
        marketDao.insertReview(review)
        val reviews = marketDao.getReviewsList(review.productId)
        if (reviews.isNotEmpty()) {
             val avg = reviews.map { it.rating }.average()
             val rounded = kotlin.math.round(avg * 10) / 10.0
             val product = marketDao.getProductById(review.productId)
             product?.let { marketDao.updateProduct(it.copy(rating = rounded.toFloat())) }
        }
    }
    fun getReviewsByProduct(productId: Int) = marketDao.getReviewsByProduct(productId)
    suspend fun addReviewReply(reply: ReviewReplyEntity) = marketDao.insertReviewReply(reply)

    // Blocking Logic
    suspend fun blockUser(blockerId: Int, blockedId: Int) {
        if (blockerId == blockedId) return
        val existing = blockDao.isBlocked(blockerId, blockedId)
        if (!existing) {
            blockDao.blockUser(BlockEntity(blockerId = blockerId, blockedId = blockedId))
            // Force unfollow
            // Logic: Is there a "Follow" from blocked to blocker?
            // "getFollowedArtists" logic
            val isFollowing = isFollowing(blockedId, blockerId).firstOrNull() ?: false
            if(isFollowing) {
                 toggleFollow(blockedId, blockerId, true) 
            }
        }
    }

    suspend fun unblockUser(blockerId: Int, blockedId: Int) {
         blockDao.unblockUser(blockerId, blockedId)
    }
    fun getBlockedUsers(blockerId: Int): Flow<List<UserEntity>> {
        return blockDao.getBlockedUsers(blockerId)
    }

    suspend fun isBlocked(userId: Int, blockedBy: Int): Boolean {
        return blockDao.isBlocked(blockedBy, userId)
    }
}
