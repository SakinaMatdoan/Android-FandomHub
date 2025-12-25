package com.kelompok1.fandomhub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        // FandomEntity::class, // REMOVED
        PostEntity::class,
        CommentEntity::class,
        LikeEntity::class,
        CommentLikeEntity::class,
        SavedPostEntity::class,

        FollowEntity::class,
        MessageEntity::class,
        ProductEntity::class,
        ReviewEntity::class,
        ReviewReplyEntity::class,
        OrderEntity::class,
        CartItemEntity::class,
        BlockEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        SubscriptionEntity::class
    ],
    version = 29,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class FandomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    // abstract fun fandomDao(): FandomDao // REMOVED
    abstract fun postDao(): PostDao
    abstract fun interactionDao(): InteractionDao
    abstract fun messageDao(): MessageDao
    abstract fun marketDao(): MarketDao
    abstract fun blockDao(): BlockDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: FandomDatabase? = null

        val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE posts ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE comments ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN type TEXT NOT NULL DEFAULT 'SOCIAL'")
            }
        }

        val MIGRATION_26_27 = object : androidx.room.migration.Migration(26, 27) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE fandoms ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE fandoms ADD COLUMN isInteractionEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_27_28 = object : androidx.room.migration.Migration(27, 28) {
             override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                 // Add timestamp to likes and follows, defaulting to current time for existing data
                 val currentTime = System.currentTimeMillis()
                 database.execSQL("ALTER TABLE likes ADD COLUMN timestamp INTEGER NOT NULL DEFAULT $currentTime")
                 database.execSQL("ALTER TABLE follows ADD COLUMN timestamp INTEGER NOT NULL DEFAULT $currentTime")
             }
        }

        fun getDatabase(context: Context): FandomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FandomDatabase::class.java,
                    "fandom_hub_db"
                )
                .addMigrations(MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28)
                .fallbackToDestructiveMigration() // For dev phase
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
