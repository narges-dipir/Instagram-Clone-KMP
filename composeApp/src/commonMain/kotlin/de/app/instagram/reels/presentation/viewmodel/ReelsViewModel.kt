package de.app.instagram.reels.presentation.viewmodel

import de.app.instagram.reels.domain.usecase.GetReelsPageUseCase
import de.app.instagram.reels.data.local.InMemoryReelInteractionStore
import de.app.instagram.reels.data.local.LocalReelInteraction
import de.app.instagram.reels.data.local.ReelInteractionStore
import de.app.instagram.reels.domain.model.ReelVideo
import de.app.instagram.reels.presentation.state.ReelsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReelsViewModel(
    private val getReelsPageUseCase: GetReelsPageUseCase,
    private val reelInteractionStore: ReelInteractionStore = InMemoryReelInteractionStore(),
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
                val localInteractions = reelInteractionStore.readAll()
                val normalizedItems = pageData.items.map { item ->
                    val baseId = item.id
                    val local = localInteractions[baseId]
                    item.copy(
                        id = "${item.id}_r${loopRound}",
                        likes = item.likes + if (local?.isLikedByMe == true) 1 else 0,
                        comments = item.comments + (local?.comments?.size ?: 0),
                        shares = local?.localShares ?: 0,
                        isLikedByMe = local?.isLikedByMe == true,
                        isSavedByMe = local?.isSavedByMe == true,
                        isFollowingCreator = local?.isFollowingCreator == true,
                        recentComments = local?.comments.orEmpty(),
                    )
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

    fun toggleLike(reelId: String) = updateReel(reelId) { reel ->
        val updated = if (reel.isLikedByMe) {
            reel.copy(
                isLikedByMe = false,
                likes = (reel.likes - 1).coerceAtLeast(0),
            )
        } else {
            reel.copy(
                isLikedByMe = true,
                likes = reel.likes + 1,
            )
        }
        scope.launch {
            val baseId = baseReelId(reelId)
            val current = reelInteractionStore.readAll()[baseId] ?: LocalReelInteraction()
            reelInteractionStore.save(
                reelId = baseId,
                interaction = current.copy(isLikedByMe = updated.isLikedByMe),
            )
        }
        updated
    }

    fun addComment(reelId: String, comment: String) {
        val trimmed = comment.trim()
        if (trimmed.isEmpty()) return
        updateReel(reelId) { reel ->
            val updated = reel.copy(
                comments = reel.comments + 1,
                recentComments = reel.recentComments + trimmed,
            )
            scope.launch {
                val baseId = baseReelId(reelId)
                val current = reelInteractionStore.readAll()[baseId] ?: LocalReelInteraction()
                reelInteractionStore.save(
                    reelId = baseId,
                    interaction = current.copy(comments = current.comments + trimmed),
                )
            }
            updated
        }
    }

    fun share(reelId: String) = updateReel(reelId) { reel ->
        val updated = reel.copy(shares = reel.shares + 1)
        scope.launch {
            val baseId = baseReelId(reelId)
            val current = reelInteractionStore.readAll()[baseId] ?: LocalReelInteraction()
            reelInteractionStore.save(
                reelId = baseId,
                interaction = current.copy(localShares = current.localShares + 1),
            )
        }
        updated
    }

    fun toggleSave(reelId: String) = updateReel(reelId) { reel ->
        val updated = reel.copy(isSavedByMe = !reel.isSavedByMe)
        scope.launch {
            val baseId = baseReelId(reelId)
            val current = reelInteractionStore.readAll()[baseId] ?: LocalReelInteraction()
            reelInteractionStore.save(
                reelId = baseId,
                interaction = current.copy(isSavedByMe = updated.isSavedByMe),
            )
        }
        updated
    }

    fun toggleFollow(reelId: String) = updateReel(reelId) { reel ->
        val updated = reel.copy(isFollowingCreator = !reel.isFollowingCreator)
        scope.launch {
            val baseId = baseReelId(reelId)
            val current = reelInteractionStore.readAll()[baseId] ?: LocalReelInteraction()
            reelInteractionStore.save(
                reelId = baseId,
                interaction = current.copy(isFollowingCreator = updated.isFollowingCreator),
            )
        }
        updated
    }

    private fun updateReel(
        reelId: String,
        transform: (ReelVideo) -> ReelVideo,
    ) {
        val currentState = _uiState.value
        val target = currentState.items.firstOrNull { it.id == reelId } ?: return
        val updated = transform(target)
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == reelId) updated else item
                }
            )
        }
    }

    private fun baseReelId(reelId: String): String = reelId.substringBefore("_r")
}
