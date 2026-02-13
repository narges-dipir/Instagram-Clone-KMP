package de.app.instagram.feed.domain.model

data class FeedPost(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val imageUrl: String,
    val mediaType: FeedMediaType,
    val videoUrl: String?,
    val likes: Int,
    val comments: Int,
    val caption: String,
)

enum class FeedMediaType {
    IMAGE,
    VIDEO,
}

data class FeedPostsPage(
    val page: Int,
    val hasNext: Boolean,
    val items: List<FeedPost>,
)
