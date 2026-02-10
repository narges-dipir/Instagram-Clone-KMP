package de.app.instagram.profile.domain.model

data class Profile(
    val id: String,
    val username: String,
    val fullName: String,
    val bio: String,
    val isVerified: Boolean,
    val avatarUrl: String,
    val stats: ProfileStats,
    val website: String,
    val storyHighlights: List<StoryHighlight>,
    val posts: List<ProfilePost>,
)

data class ProfileStats(
    val posts: Int,
    val followers: Int,
    val following: Int,
)

data class StoryHighlight(
    val id: String,
    val title: String,
    val coverUrl: String,
)

data class ProfilePost(
    val id: String,
    val imageUrl: String,
    val likes: Int,
    val comments: Int,
)
