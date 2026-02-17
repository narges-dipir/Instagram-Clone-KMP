package de.app.instagram.profile.data.local

data class LocalPostInteraction(
    val isLikedByMe: Boolean = false,
    val comments: List<String> = emptyList(),
)
