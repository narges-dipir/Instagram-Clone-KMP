package de.app.instagram.reels.presentation.viewmodel

import de.app.instagram.reels.domain.usecase.GetReelsPageUseCase
import de.app.instagram.reels.presentation.state.ReelsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReelsViewModel(
    private val getReelsPageUseCase: GetReelsPageUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    private var nextPage: Int = 1
    private var loopRound: Int = 0

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore) return

        val loadingInitial = state.items.isEmpty()
        _uiState.value = state.copy(
            isInitialLoading = loadingInitial,
            isLoadingMore = !loadingInitial,
            errorMessage = null,
        )

        scope.launch {
            val previousState = _uiState.value
            try {
                val pageData = getReelsPageUseCase(nextPage)
                val normalizedItems = pageData.items.map { item ->
                    item.copy(id = "${item.id}_r${loopRound}")
                }
                val mergedItems = previousState.items + normalizedItems

                if (pageData.hasNext) {
                    nextPage = pageData.page + 1
                } else {
                    nextPage = 1
                    loopRound += 1
                }

                _uiState.value = previousState.copy(
                    items = mergedItems,
                    isInitialLoading = false,
                    isLoadingMore = false,
                    errorMessage = null,
                )
            } catch (t: Throwable) {
                _uiState.value = previousState.copy(
                    isInitialLoading = false,
                    isLoadingMore = false,
                    errorMessage = t.message ?: "Failed to load reels",
                )
            }
        }
    }
}
