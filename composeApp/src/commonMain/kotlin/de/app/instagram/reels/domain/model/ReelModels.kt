package de.app.instagram.reels.domain.model

data class ReelVideo(
    val id: String,
    val videoUrl: String,
    val caption: String,
)

data class ReelsPage(
    val page: Int,
    val hasNext: Boolean,
    val items: List<ReelVideo>,
)
