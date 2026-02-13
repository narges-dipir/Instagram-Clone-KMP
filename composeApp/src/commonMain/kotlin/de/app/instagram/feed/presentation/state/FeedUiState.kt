package de.app.instagram.feed.presentation.state

import de.app.instagram.feed.domain.model.FeedPost

data class FeedUiState(
    val items: List<FeedPost> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
)
