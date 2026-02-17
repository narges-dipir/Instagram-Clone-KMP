package de.app.instagram.feed.data.local

data class LocalFeedInteraction(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val localComments: Int = 0,
)
