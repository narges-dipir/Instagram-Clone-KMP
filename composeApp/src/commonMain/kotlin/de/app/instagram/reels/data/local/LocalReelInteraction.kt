package de.app.instagram.reels.data.local

data class LocalReelInteraction(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val isFollowingCreator: Boolean = false,
    val comments: List<String> = emptyList(),
    val localShares: Int = 0,
)
