package de.app.instagram.reels.domain.model

data class ReelVideo(
    val id: String,
    val videoUrl: String,
    val caption: String,
    val username: String,
    val avatarUrl: String,
    val likes: Int,
    val comments: Int,
)

data class ReelsPage(
    val page: Int,
    val hasNext: Boolean,
    val items: List<ReelVideo>,
)
