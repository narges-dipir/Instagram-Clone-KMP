package de.app.instagram.reels.presentation.state

import de.app.instagram.reels.domain.model.ReelVideo

data class ReelsUiState(
    val items: List<ReelVideo> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
)
