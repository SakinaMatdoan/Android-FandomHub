package com.kelompok1.fandomhub.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class PostWithAuthor(
    @androidx.room.Embedded val post: PostEntity,
    @androidx.room.Relation(
        parentColumn = "authorId",
        entityColumn = "id"
    )
    val author: UserEntity,

    @androidx.room.Relation(
        parentColumn = "artistId",
        entityColumn = "id"
    )
    val artist: UserEntity? // Replaces Fandom
)

data class ReviewWithReplies(
    @androidx.room.Embedded val review: ReviewEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "reviewId"
    )
    val replies: List<ReviewReplyEntity>
)
