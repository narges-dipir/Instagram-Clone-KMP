package de.app.instagram.reels.presentation.viewmodel

import de.app.instagram.reels.domain.usecase.GetReelsPageUseCase
import de.app.instagram.reels.data.local.LocalReelInteraction
import de.app.instagram.reels.data.local.ReelInteractionStore
import de.app.instagram.di.createDefaultAppScope
import de.app.instagram.reels.domain.model.ReelVideo
import de.app.instagram.reels.presentation.state.ReelsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReelsViewModel(
    private val getReelsPageUseCase: GetReelsPageUseCase,
    private val reelInteractionStore: ReelInteractionStore,
    private val scope: CoroutineScope = createDefaultAppScope(),
) {
    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()
    private val localInteractions = MutableStateFlow<Map<String, LocalReelInteraction>>(emptyMap())

    private var nextPage: Int = 1
    private var loopRound: Int = 0

    init {
        scope.launch {
            reelInteractionStore.observeAll().collect { snapshot ->
                localInteractions.value = snapshot
            }
        }
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
                val localInteractionsSnapshot = localInteractions.value
                val normalizedItems = pageData.items.map { item ->
                    val baseId = item.id
                    val local = localInteractionsSnapshot[baseId]
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
                seedMissingInteractions(pageData.items.map { it.id }, localInteractionsSnapshot)
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
            val current = localInteractions.value[baseId] ?: LocalReelInteraction()
            val updatedInteraction = current.copy(isLikedByMe = updated.isLikedByMe)
            localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
            reelInteractionStore.save(
                reelId = baseId,
                interaction = updatedInteraction,
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
                val current = localInteractions.value[baseId] ?: LocalReelInteraction()
                val updatedInteraction = current.copy(comments = current.comments + trimmed)
                localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
                reelInteractionStore.save(
                    reelId = baseId,
                    interaction = updatedInteraction,
                )
            }
            updated
        }
    }

    fun share(reelId: String) = updateReel(reelId) { reel ->
        val updated = reel.copy(shares = reel.shares + 1)
        scope.launch {
            val baseId = baseReelId(reelId)
            val current = localInteractions.value[baseId] ?: LocalReelInteraction()
            val updatedInteraction = current.copy(localShares = current.localShares + 1)
            localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
            reelInteractionStore.save(
                reelId = baseId,
                interaction = updatedInteraction,
            )
        }
        updated
    }

    fun toggleSave(reelId: String) = updateReel(reelId) { reel ->
        val updated = reel.copy(isSavedByMe = !reel.isSavedByMe)
        scope.launch {
            val baseId = baseReelId(reelId)
            val current = localInteractions.value[baseId] ?: LocalReelInteraction()
            val updatedInteraction = current.copy(isSavedByMe = updated.isSavedByMe)
            localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
            reelInteractionStore.save(
                reelId = baseId,
                interaction = updatedInteraction,
            )
        }
        updated
    }

    fun toggleFollow(reelId: String) = updateReel(reelId) { reel ->
        val updated = reel.copy(isFollowingCreator = !reel.isFollowingCreator)
        scope.launch {
            val baseId = baseReelId(reelId)
            val current = localInteractions.value[baseId] ?: LocalReelInteraction()
            val updatedInteraction = current.copy(isFollowingCreator = updated.isFollowingCreator)
            localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
            reelInteractionStore.save(
                reelId = baseId,
                interaction = updatedInteraction,
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

    private suspend fun seedMissingInteractions(
        reelIds: List<String>,
        existing: Map<String, LocalReelInteraction>,
    ) {
        val missing = reelIds.map(::baseReelId).distinct().filterNot(existing::containsKey)
        if (missing.isEmpty()) return
        missing.forEach { id ->
            localInteractions.value = localInteractions.value + (id to LocalReelInteraction())
            reelInteractionStore.save(reelId = id, interaction = LocalReelInteraction())
        }
    }
}
