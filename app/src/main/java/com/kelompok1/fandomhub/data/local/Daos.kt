package com.kelompok1.fandomhub.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kelompok1.fandomhub.data.DailyStat
import kotlinx.coroutines.flow.Flow

data class SubscriberWithStatus(
    @androidx.room.Embedded val user: UserEntity,
    val validUntil: Long
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE UPPER(role) = 'ADMIN' LIMIT 1")
    suspend fun getSystemAdmin(): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'ARTIST' AND status = 'PENDING'")
    fun getPendingArtists(): Flow<List<UserEntity>>

    // Get All Active Artists (Replaces getAllFandoms)
    @Query("SELECT * FROM users WHERE role = 'ARTIST' AND status = 'ACTIVE' AND isFandomActive = 1 AND isSuspended = 0")
    fun getAllActiveArtists(): Flow<List<UserEntity>>
    
    // For search
    @Query("SELECT * FROM users WHERE role = 'ARTIST' AND status = 'ACTIVE' AND isFandomActive = 1 AND isSuspended = 0 AND (fullName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%')")
    fun searchArtists(query: String): Flow<List<UserEntity>>

    // Admin Stats & Lists
    @Query("SELECT COUNT(*) FROM users WHERE role = 'ARTIST'")
    fun getArtistCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'FAN' OR role = 'FANS'")
    fun getFanCount(): Flow<Int>

    @Query("SELECT * FROM users WHERE role = 'FAN' OR role = 'FANS'")
    fun getAllFans(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE role = 'ARTIST'")
    fun getAllArtistsAdmin(): Flow<List<UserEntity>>

    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Int, status: String)

    @Query("UPDATE users SET isSuspended = :isSuspended, suspensionEndTimestamp = :endTimestamp WHERE id = :userId")
    suspend fun suspendUser(userId: Int, isSuspended: Boolean, endTimestamp: Long?)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Int)
}

// FandomDao REMOVED

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @androidx.room.Transaction
    @Query("SELECT * FROM posts WHERE artistId = :artistId ORDER BY timestamp DESC")
    fun getPostsByArtist(artistId: Int): Flow<List<PostWithAuthor>>

    @androidx.room.Transaction
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostWithAuthor>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: Int): PostEntity?

    @androidx.room.Transaction
    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY timestamp DESC")
    fun getPostsByAuthor(authorId: Int): Flow<List<PostWithAuthor>>

    @androidx.room.Transaction
    @Query("SELECT * FROM posts WHERE artistId = :artistId AND isThread = 1 ORDER BY timestamp DESC")
    fun getFanThreads(artistId: Int): Flow<List<PostWithAuthor>>

    @Query("SELECT * FROM posts WHERE artistId = :artistId AND timestamp >= :timestamp ORDER BY timestamp ASC")
    suspend fun getPostsAfter(artistId: Int, timestamp: Long): List<PostEntity>

    // Count Artist's OWN posts on their wall
    @Query("SELECT COUNT(*) FROM posts WHERE artistId = :artistId AND authorId = :artistId")
    suspend fun countPostsByArtist(artistId: Int): Int
    
    // Count Posts by Artist (with explicit author check)
    // Note: Parameter naming might shadow.
    // Fixed:
    @Query("SELECT COUNT(*) FROM posts WHERE artistId = :artistId AND authorId = :authorId")
    suspend fun countPostsByAuthorInFandom(artistId: Int, authorId: Int): Int

    @androidx.room.Transaction
    @Query("SELECT posts.* FROM posts INNER JOIN follows ON posts.artistId = follows.artistId INNER JOIN users ON posts.artistId = users.id WHERE follows.followerId = :userId AND posts.isThread = 0 AND users.isFandomActive = 1 AND users.isSuspended = 0 ORDER BY posts.timestamp DESC")
    fun getFeedPosts(userId: Int): Flow<List<PostWithAuthor>>

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: Int)

    @Query("UPDATE posts SET content = :content, images = :images, isEdited = :isEdited WHERE id = :postId")
    suspend fun updatePostContent(postId: Int, content: String, images: List<String>, isEdited: Boolean = true)
    
    // Search Posts (Filter Suspended Authors)
    @androidx.room.Transaction
    @Query("SELECT posts.* FROM posts INNER JOIN users ON posts.authorId = users.id WHERE users.isSuspended = 0 AND posts.content LIKE '%' || :query || '%' ORDER BY posts.timestamp DESC")
    fun searchPosts(query: String): Flow<List<PostWithAuthor>>
}

@Dao
interface InteractionDao {
    // Likes
    @Insert
    suspend fun insertLike(like: LikeEntity)
    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteLike(postId: Int, userId: Int)
    @Query("SELECT COUNT(*) FROM likes WHERE postId = :postId")
    fun getLikeCount(postId: Int): Flow<Int>
    @Query("SELECT EXISTS(SELECT * FROM likes WHERE postId = :postId AND userId = :userId)")
    fun isLiked(postId: Int, userId: Int): Flow<Boolean>

    @Query("SELECT likes.* FROM likes INNER JOIN posts ON likes.postId = posts.id WHERE posts.artistId = :artistId AND likes.timestamp >= :timestamp ORDER BY likes.timestamp ASC")
    suspend fun getLikesAfter(artistId: Int, timestamp: Long): List<LikeEntity>

    // Follows
    @Insert
    suspend fun insertFollow(follow: FollowEntity)
    @Query("DELETE FROM follows WHERE followerId = :userId AND artistId = :artistId")
    suspend fun deleteFollow(userId: Int, artistId: Int)
    @Query("SELECT EXISTS(SELECT * FROM follows WHERE followerId = :userId AND artistId = :artistId)")
    fun isFollowing(userId: Int, artistId: Int): Flow<Boolean>

    @Query("SELECT * FROM follows WHERE artistId = :artistId AND timestamp >= :timestamp ORDER BY timestamp ASC")
    suspend fun getFollowsAfter(artistId: Int, timestamp: Long): List<FollowEntity>
    
    // Replaces getFollowedFandoms, direct to User
    @Query("SELECT users.* FROM follows INNER JOIN users ON follows.artistId = users.id WHERE followerId = :userId")
    fun getFollowedArtists(userId: Int): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM follows WHERE followerId = :userId")
    fun getFollowedArtistCount(userId: Int): Flow<Int>

    @Query("SELECT * FROM follows")
    suspend fun getAllFollows(): List<FollowEntity>

    @Query("SELECT likes.* FROM likes INNER JOIN posts ON likes.postId = posts.id WHERE posts.artistId = :artistId")
    suspend fun getAllArtistLikes(artistId: Int): List<LikeEntity>

    @Query("SELECT * FROM likes WHERE postId IN (SELECT id FROM posts WHERE artistId = :artistId AND authorId = :artistId)")
    suspend fun getAllLikesOnArtistPosts(artistId: Int): List<LikeEntity>

    @Query("SELECT c.* FROM comments c INNER JOIN posts p ON c.postId = p.id WHERE p.artistId = :artistId")
    suspend fun getAllArtistComments(artistId: Int): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE postId IN (SELECT id FROM posts WHERE artistId = :artistId AND authorId = :artistId)")
    suspend fun getAllCommentsOnArtistPosts(artistId: Int): List<CommentEntity>
    
    @Query("SELECT COUNT(*) FROM subscriptions WHERE artistId = :artistId")
    suspend fun getAllSubscriptionsCount(artistId: Int): Int

    @Query("SELECT * FROM likes")
    fun getAllLikes(): Flow<List<LikeEntity>>

    @Query("SELECT COUNT(*) FROM follows WHERE artistId = :artistId")
    fun getFollowerCount(artistId: Int): Flow<Int>

    @Query("SELECT DISTINCT users.* FROM follows INNER JOIN users ON follows.followerId = users.id WHERE follows.artistId = :artistId")
    fun getFollowers(artistId: Int): Flow<List<UserEntity>>

    @Query("SELECT DISTINCT users.* FROM follows INNER JOIN users ON follows.followerId = users.id WHERE follows.artistId = :artistId")
    suspend fun getFollowersList(artistId: Int): List<UserEntity>

    @Query("SELECT DISTINCT users.* FROM subscriptions INNER JOIN users ON subscriptions.userId = users.id WHERE subscriptions.artistId = :artistId AND subscriptions.isCancelled = 0 AND subscriptions.validUntil > :now")
    fun getSubscribers(artistId: Int, now: Long): Flow<List<UserEntity>>

    @Query("SELECT DISTINCT users.* FROM follows INNER JOIN users ON follows.followerId = users.id WHERE follows.artistId = :artistId ORDER BY follows.timestamp DESC")
    fun getRecentFollowers(artistId: Int): Flow<List<UserEntity>>

    @Query("""
        SELECT DISTINCT users.* FROM subscriptions 
        INNER JOIN users ON subscriptions.artistId = users.id 
        WHERE subscriptions.userId = :userId 
        AND subscriptions.validUntil > :currentTime 
        AND subscriptions.isCancelled = 0
        AND users.isDmActive = 1
    """)
    fun getSubscribedArtists(userId: Int, currentTime: Long): Flow<List<UserEntity>>
    
    @Query("SELECT users.*, MAX(subscriptions.validUntil) as validUntil FROM subscriptions INNER JOIN users ON subscriptions.userId = users.id WHERE subscriptions.artistId = :artistId AND subscriptions.isCancelled = 0 AND subscriptions.validUntil > :now GROUP BY users.id")
    fun getSubscribersWithStatus(artistId: Int, now: Long): Flow<List<SubscriberWithStatus>>

    // Like Lists
    @Query("SELECT users.* FROM likes INNER JOIN users ON likes.userId = users.id WHERE likes.postId = :postId")
    fun getPostLikers(postId: Int): Flow<List<UserEntity>>

    @Query("SELECT users.* FROM comment_likes INNER JOIN users ON comment_likes.userId = users.id WHERE comment_likes.commentId = :commentId")
    fun getCommentLikers(commentId: Int): Flow<List<UserEntity>>

    // Saved Posts
    @Transaction
    @Query("SELECT posts.* FROM posts INNER JOIN saved_posts ON posts.id = saved_posts.postId WHERE saved_posts.userId = :userId ORDER BY saved_posts.id DESC")
    fun getSavedPosts(userId: Int): Flow<List<PostWithAuthor>>

    // Comments
    @Insert
    suspend fun insertComment(comment: CommentEntity)
    
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY likeCount DESC, timestamp ASC")
    fun getComments(postId: Int): Flow<List<CommentEntity>>
    
    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    fun getCommentCount(postId: Int): Flow<Int>
    
    @Query("SELECT * FROM comments WHERE id = :commentId")
    suspend fun getCommentById(commentId: Int): CommentEntity?

    @Query("SELECT comments.* FROM comments INNER JOIN posts ON comments.postId = posts.id WHERE posts.artistId = :artistId AND comments.timestamp >= :timestamp ORDER BY comments.timestamp ASC")
    suspend fun getCommentsAfter(artistId: Int, timestamp: Long): List<CommentEntity>

    // Comment Likes
    @Insert
    suspend fun insertCommentLike(like: CommentLikeEntity)
    
    @Query("DELETE FROM comment_likes WHERE commentId = :commentId AND userId = :userId")
    suspend fun deleteCommentLike(commentId: Int, userId: Int)
    
    @Query("SELECT EXISTS(SELECT * FROM comment_likes WHERE commentId = :commentId AND userId = :userId)")
    fun isCommentLiked(commentId: Int, userId: Int): Flow<Boolean>

    @Query("UPDATE comments SET likeCount = likeCount + 1 WHERE id = :commentId")
    suspend fun incrementCommentLike(commentId: Int)

    @Query("UPDATE comments SET likeCount = likeCount - 1 WHERE id = :commentId")
    suspend fun decrementCommentLike(commentId: Int)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Int)

    @Query("UPDATE comments SET content = :content, isEdited = :isEdited WHERE id = :commentId")
    suspend fun updateCommentText(commentId: Int, content: String, isEdited: Boolean = true)

    // Saved Posts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPost(savedPost: SavedPostEntity)
    @Query("DELETE FROM saved_posts WHERE postId = :postId AND userId = :userId")
    suspend fun deleteSavedPost(postId: Int, userId: Int)
    @Query("SELECT EXISTS(SELECT * FROM saved_posts WHERE postId = :postId AND userId = :userId)")
    fun isSaved(postId: Int, userId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM saved_posts WHERE postId = :postId")
    fun getSavedCount(postId: Int): Flow<Int>

    // Subscriptions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Query("SELECT EXISTS(SELECT * FROM subscriptions WHERE artistId = :artistId AND userId = :userId AND validUntil > :currentTime AND isCancelled = 0)")
    fun isSubscribed(artistId: Int, userId: Int, currentTime: Long): Flow<Boolean>

    @Query("SELECT * FROM subscriptions WHERE artistId = :artistId AND startDate >= :timestamp")
    suspend fun getSubscriptionsAfter(artistId: Int, timestamp: Long): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE artistId = :artistId AND validUntil > :now AND isCancelled = 0")
    suspend fun getActiveSubscriptions(artistId: Int, now: Long): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE artistId = :artistId AND userId = :userId ORDER BY validUntil DESC LIMIT 1")
    suspend fun getSubscription(artistId: Int, userId: Int): SubscriptionEntity?

    @Query("UPDATE subscriptions SET isCancelled = 1 WHERE artistId = :artistId AND userId = :userId")
    suspend fun cancelSubscription(artistId: Int, userId: Int)
}

@Dao
interface BlockDao {
    @Insert
    suspend fun blockUser(block: BlockEntity)

    @Query("DELETE FROM blocks WHERE blockerId = :blockerId AND blockedId = :blockedId")
    suspend fun unblockUser(blockerId: Int, blockedId: Int)

    @Query("SELECT EXISTS(SELECT * FROM blocks WHERE blockerId = :blockerId AND blockedId = :blockedId)")
    suspend fun isBlocked(blockerId: Int, blockedId: Int): Boolean

    @Query("SELECT users.* FROM blocks INNER JOIN users ON blocks.blockedId = users.id WHERE blocks.blockerId = :blockerId")
    fun getBlockedUsers(blockerId: Int): Flow<List<UserEntity>>

    @Insert
    suspend fun insertReport(report: ReportEntity)

    @Query("SELECT * FROM reports WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingReports(): Flow<List<ReportEntity>>
    
    @Query("SELECT * FROM reports WHERE status != 'PENDING' ORDER BY timestamp DESC")
    fun getResolvedReports(): Flow<List<ReportEntity>>
    
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>
    
    @Query("SELECT EXISTS(SELECT * FROM reports WHERE reporterId = :reporterId AND type = :type AND referenceId = :referenceId AND status = 'PENDING')")
    suspend fun hasExistingReport(reporterId: Int, type: String, referenceId: Int): Boolean

    @Query("UPDATE reports SET status = :status, adminAction = :action WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: Int, status: String, action: String?)
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insertNotification(notification: NotificationEntity)

    @androidx.room.Transaction
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotifications(userId: Int): Flow<List<NotificationWithSender>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: Int): Flow<Int>

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllNotifications(userId: Int)
} 

data class NotificationWithSender(
    @androidx.room.Embedded val notification: NotificationEntity,
    @androidx.room.Relation(
        parentColumn = "senderId",
        entityColumn = "id"
    )
    val sender: UserEntity?
)

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(msg: MessageEntity)

    @Query("SELECT * FROM messages WHERE ((senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1)) AND type = :type ORDER BY timestamp ASC")
    fun getChatHistory(user1: Int, user2: Int, type: String): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND senderId = :senderId AND isRead = 0 AND type = :type")
    fun getUnreadCount(userId: Int, senderId: Int, type: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0 AND type != 'SUPPORT'")
    fun getTotalUnreadCount(userId: Int): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0 AND type = :type")
    fun getTotalUnreadCountByType(userId: Int, type: String): Flow<Int>

    @Query("UPDATE messages SET isRead = 1 WHERE receiverId = :userId AND senderId = :senderId AND type = :type")
    suspend fun markAsRead(userId: Int, senderId: Int, type: String)

    @Query("DELETE FROM messages WHERE ((senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1)) AND type = :type")
    suspend fun deleteChat(user1: Int, user2: Int, type: String)

    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND type = :type ORDER BY timestamp DESC")
    fun getMessagesForUserByType(userId: Int, type: String): Flow<List<MessageEntity>> 

    @Query("SELECT DISTINCT CASE WHEN senderId = :userId THEN receiverId ELSE senderId END FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND type = :type")
    fun getChatPartnerIds(userId: Int, type: String): Flow<List<Int>>

    // DEBUG ONLY
    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT 20")
    suspend fun getDebugAllMessages(): List<MessageEntity>

    @Query("SELECT senderId FROM messages WHERE type = 'SUPPORT' UNION SELECT receiverId FROM messages WHERE type = 'SUPPORT'")
    fun getAllSupportChatParticipants(): Flow<List<Int>>

    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) AND type = 'SUPPORT' ORDER BY timestamp ASC")
    fun getSupportChatHistory(userId: Int): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE type = 'SUPPORT' AND isRead = 0 AND senderId NOT IN (SELECT id FROM users WHERE role = 'ADMIN')")
    fun getGlobalSupportUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE type = 'SUPPORT' AND isRead = 0 AND senderId = :userId")
    fun getSupportUnreadCountFromUser(userId: Int): Flow<Int>
}

@Dao
interface MarketDao {
    // --- Products ---
    @Query("SELECT * FROM products WHERE artistId = :artistId ORDER BY id DESC")
    fun getProductsByArtist(artistId: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT products.* FROM products INNER JOIN follows ON products.artistId = follows.artistId INNER JOIN users ON products.artistId = users.id WHERE follows.followerId = :userId AND users.isFandomActive = 1 AND users.isSuspended = 0 ORDER BY products.id DESC")
    fun getFeedProducts(userId: Int): Flow<List<ProductEntity>>
    
    // Search Products (Filter Suspended Artists)
    @Query("SELECT products.* FROM products INNER JOIN users ON products.artistId = users.id WHERE users.isSuspended = 0 AND products.name LIKE '%' || :query || '%' ORDER BY products.id DESC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long
    
    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // --- Cart ---
    @androidx.room.Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItems(userId: Int): Flow<List<CartItemWithProduct>>

    @androidx.room.Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartItemsList(userId: Int): List<CartItemWithProduct>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun getCartItem(userId: Int, productId: Int): CartItemEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)
    
    @Update
    suspend fun updateCartItem(cartItem: CartItemEntity)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Int)

    // --- Orders ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long
    
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY timestamp DESC")
    fun getOrdersByUser(userId: Int): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): OrderEntity?
    
    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, status: String)

    @Query("SELECT * FROM orders WHERE artistId = :artistId ORDER BY timestamp DESC")
    fun getOrdersByArtist(artistId: Int): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE artistId = :artistId AND timestamp >= :timestamp")
    suspend fun getOrdersAfter(artistId: Int, timestamp: Long): List<OrderEntity>

    // --- Reviews ---
    @Insert
    suspend fun insertReview(review: ReviewEntity)
    
    @androidx.room.Transaction
    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY timestamp DESC")
    fun getReviewsByProduct(productId: Int): Flow<List<ReviewWithReplies>>

    @Query("SELECT * FROM reviews WHERE productId = :productId")
    suspend fun getReviewsList(productId: Int): List<ReviewEntity>

    @Query("SELECT * FROM products WHERE artistId = :artistId ORDER BY soldCount DESC LIMIT :limit")
    fun getTopSellingProducts(artistId: Int, limit: Int = 5): Flow<List<ProductEntity>>

    @Insert
    suspend fun insertReviewReply(reply: ReviewReplyEntity)
}
