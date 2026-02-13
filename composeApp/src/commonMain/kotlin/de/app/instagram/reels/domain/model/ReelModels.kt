package de.app.instagram.reels.domain.model

data class ReelVideo(
    val id: String,
    val videoUrl: String,
    val caption: String,
    val username: String,
    val avatarUrl: String,
    val likes: Int,
    val comments: Int,
    val shares: Int = 0,
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val isFollowingCreator: Boolean = false,
    val recentComments: List<String> = emptyList(),
)

data class ReelsPage(
    val page: Int,
    val hasNext: Boolean,
    val items: List<ReelVideo>,
)
