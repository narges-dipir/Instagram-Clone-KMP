package de.app.instagram.feed.presentation.viewmodel

import de.app.instagram.feed.domain.usecase.GetFeedPageUseCase
import de.app.instagram.feed.presentation.state.FeedUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(
    private val getFeedPageUseCase: GetFeedPageUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var nextPage = 1
    private var loopRound = 0

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore) return

        _uiState.update {
            it.copy(
                isInitialLoading = it.items.isEmpty(),
                isLoadingMore = true,
                errorMessage = null,
            )
        }

        scope.launch {
            runCatching { getFeedPageUseCase(nextPage) }
                .onSuccess { pageData ->
                    val isLoopStart = !pageData.hasNext
                    val pageItemsWithRound = pageData.items.map { post ->
                        post.copy(id = "${post.id}_r$loopRound")
                    }

                    _uiState.update {
                        it.copy(
                            items = it.items + pageItemsWithRound,
                            isInitialLoading = false,
                            isLoadingMore = false,
                            errorMessage = null,
                        )
                    }

                    if (isLoopStart) {
                        nextPage = 1
                        loopRound += 1
                    } else {
                        nextPage = pageData.page + 1
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            isLoadingMore = false,
                            errorMessage = throwable.message ?: "Could not load posts",
                        )
                    }
                }
        }
    }
}
