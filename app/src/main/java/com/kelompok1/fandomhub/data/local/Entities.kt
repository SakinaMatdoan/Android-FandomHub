package com.kelompok1.fandomhub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

// 1. User Entity (Artist = Fandom)
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val username: String,
    val email: String,
    val password: String, // Plain text for demo/MVP
    val bio: String = "",
    val coverImage: String? = null, // URL or Path
    val isSuspended: Boolean = false,
    val suspensionEndTimestamp: Long? = null, // Null if permanent or not suspended
    val role: String = "FAN", // FAN, ARTIST, ADMIN"
    val status: String, // "ACTIVE", "PENDING"
    val profileImage: String? = null, // Uri string
    val location: String? = null,
    
    // Fandom/Artist Profile Fields
    val subscriptionPrice: Double? = null,
    val subscriptionDuration: Int? = 30, // Days
    val subscriptionBenefits: String? = null,
    val isDmActive: Boolean = false,
    val isFandomActive: Boolean = true, // Controls visibility (Deactivation)
    val isInteractionEnabled: Boolean = true // Read-Only mode
)

// FandomEntity removed.

// 3. Post Entity (Thread or Official Post)
@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["authorId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["artistId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["authorId"]),
        androidx.room.Index(value = ["artistId"])
    ]
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: Int, // The creator (Fan or Artist)
    val artistId: Int, // The "Wall" or Context (The Artist)
    val content: String,
    val images: List<String> = emptyList(), 
    val timestamp: Long,
    val isThread: Boolean = false, // true = Fan Thread, false = Official Post
    val isEdited: Boolean = false
)

// 4. Interaction: Comments
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PostEntity::class, parentColumns = ["id"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["postId"])
    ]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val userId: Int,
    val content: String,
    val timestamp: Long,
    val parentId: Int? = null, // For nested replies
    val likeCount: Int = 0,
    val isEdited: Boolean = false
)

// 4b. Comment Likes
@Entity(tableName = "comment_likes")
data class CommentLikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commentId: Int,
    val userId: Int
)

// 5. Interaction: Likes
@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val userId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// 6. Interaction: Saved Posts
@Entity(tableName = "saved_posts", indices = [androidx.room.Index(value = ["postId", "userId"], unique = true)])
data class SavedPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val userId: Int
)

// 7. Interaction: Follow (Fan follows Artist)
@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val followerId: Int, // Fan
    val artistId: Int, // The Artist being followed
    val timestamp: Long = System.currentTimeMillis()
)

// 8. Messages (DM)
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: String = "SOCIAL" // SOCIAL, MARKET
)

// 9. Product (Market Item)
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["artistId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["artistId"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val artistId: Int,
    // fandomId removed
    val name: String,
    val description: String,
    val price: Double,
    val stock: Int,
    val images: List<String> = emptyList(), // JSON Converter
    val rating: Float = 0f,
    val soldCount: Int = 0
)

// 10. Product Review
@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["productId"]),
        androidx.room.Index(value = ["userId"])
    ]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val userId: Int,
    val rating: Int,
    val comment: String,
    val timestamp: Long
)

// 11. Review Reply
@Entity(
    tableName = "review_replies",
    foreignKeys = [
        ForeignKey(entity = ReviewEntity::class, parentColumns = ["id"], childColumns = ["reviewId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["reviewId"]),
        androidx.room.Index(value = ["userId"])
    ]
)
data class ReviewReplyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reviewId: Int,
    val userId: Int, // The replier (usually Artist)
    val content: String,
    val timestamp: Long
)

// 11. Cart Item
@Entity(
    tableName = "cart_items",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["productId"])
    ]
)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val productId: Int,
    val quantity: Int
)

data class CartItemWithProduct(
    @androidx.room.Embedded val cartItem: CartItemEntity,
    @androidx.room.Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: ProductEntity
)

// 12. Order (Checkout)
@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        androidx.room.Index(value = ["userId"])
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val artistId: Int, // Already existed, refers to UserEntity(Artist)
    val totalAmount: Double,
    val status: String, // PENDING, PROCESSED, SHIPPED, DELIVERED
    val shippingAddress: String,
    val paymentMethod: String,
    val itemsJson: String, // JSON string of List<OrderItem>
    val timestamp: Long
)

// Helper Data Class for Order Items (Not an Entity)
data class OrderItem(
    val productId: Int,
    val productName: String,
    val productImage: String?,
    val price: Double,
    val quantity: Int
)

// 13. Subscription (Fan subscribes to Artist)
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val artistId: Int, // Renamed from fandomId
    val startDate: Long,
    val validUntil: Long,
    val isCancelled: Boolean = false 
)

// 6. Block Entity
@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val blockerId: Int,
    val blockedId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// 7. Report Entity
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reporterId: Int,
    val reportedId: Int? = null, 
    val type: String = "USER", // USER, POST, COMMENT, PRODUCT, FANDOM (Maps to Artist)
    val referenceId: Int? = null, // userId, postId, commentId, productId, artistId
    val reason: String,
    val description: String = "",
    val contentSnapshot: String? = null, 
    val status: String = "PENDING", 
    val adminAction: String? = null, // SUSPEND, DELETE, WARNING, NONE
    val timestamp: Long = System.currentTimeMillis()
)

// 15. Notification Entity
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int, // Recipient
    val senderId: Int, // Actor
    val type: String, // POST, LIKE_POST, COMMENT, REPLY, MERCH, FOLLOW
    val referenceId: Int, // postId, productId, artistId
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
