package de.app.instagram.feed.presentation.viewmodel

import de.app.instagram.feed.data.local.FeedInteractionStore
import de.app.instagram.feed.data.local.LocalFeedInteraction
import de.app.instagram.di.createDefaultAppScope
import de.app.instagram.feed.domain.model.FeedPost
import de.app.instagram.feed.domain.usecase.GetFeedPageUseCase
import de.app.instagram.feed.presentation.state.FeedUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(
    private val getFeedPageUseCase: GetFeedPageUseCase,
    private val feedInteractionStore: FeedInteractionStore,
    private val scope: CoroutineScope = createDefaultAppScope(),
) {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private val localInteractions = MutableStateFlow<Map<String, LocalFeedInteraction>>(emptyMap())

    private var nextPage = 1
    private var loopRound = 0

    init {
        scope.launch {
            feedInteractionStore.observeAll().collect { snapshot ->
                localInteractions.value = snapshot
            }
        }
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
                    val localInteractionsSnapshot = localInteractions.value
                    val isLoopStart = !pageData.hasNext
                    val pageItemsWithRound = pageData.items.map { post ->
                        val local = localInteractionsSnapshot[basePostId(post.id)]
                        post.copy(
                            id = "${post.id}_r$loopRound",
                            likes = post.likes + if (local?.isLikedByMe == true) 1 else 0,
                            comments = post.comments + (local?.localComments ?: 0),
                            isLikedByMe = local?.isLikedByMe == true,
                            isSavedByMe = local?.isSavedByMe == true,
                        )
                    }

                    _uiState.update {
                        it.copy(
                            items = it.items + pageItemsWithRound,
                            isInitialLoading = false,
                            isLoadingMore = false,
                            errorMessage = null,
                        )
                    }
                    seedMissingInteractions(pageData.items.map { it.id }, localInteractionsSnapshot)

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

    fun toggleLike(postId: String) = updatePost(postId) { post ->
        val updated = if (post.isLikedByMe) {
            post.copy(
                isLikedByMe = false,
                likes = (post.likes - 1).coerceAtLeast(0),
            )
        } else {
            post.copy(
                isLikedByMe = true,
                likes = post.likes + 1,
            )
        }

        scope.launch {
            val baseId = basePostId(postId)
            val current = localInteractions.value[baseId] ?: LocalFeedInteraction()
            val updatedInteraction = current.copy(isLikedByMe = updated.isLikedByMe)
            localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
            feedInteractionStore.save(
                postId = baseId,
                interaction = updatedInteraction,
            )
        }
        updated
    }

    fun toggleSave(postId: String) = updatePost(postId) { post ->
        val updated = post.copy(isSavedByMe = !post.isSavedByMe)
        scope.launch {
            val baseId = basePostId(postId)
            val current = localInteractions.value[baseId] ?: LocalFeedInteraction()
            val updatedInteraction = current.copy(isSavedByMe = updated.isSavedByMe)
            localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
            feedInteractionStore.save(
                postId = baseId,
                interaction = updatedInteraction,
            )
        }
        updated
    }

    fun addComment(postId: String) = updatePost(postId) { post ->
        val updated = post.copy(comments = post.comments + 1)
        scope.launch {
            val baseId = basePostId(postId)
            val current = localInteractions.value[baseId] ?: LocalFeedInteraction()
            val updatedInteraction = current.copy(localComments = current.localComments + 1)
            localInteractions.value = localInteractions.value + (baseId to updatedInteraction)
            feedInteractionStore.save(
                postId = baseId,
                interaction = updatedInteraction,
            )
        }
        updated
    }

    private fun updatePost(
        postId: String,
        transform: (FeedPost) -> FeedPost,
    ) {
        val target = _uiState.value.items.firstOrNull { it.id == postId } ?: return
        val updated = transform(target)
        _uiState.update { state ->
            state.copy(
                items = state.items.map { post ->
                    if (post.id == postId) updated else post
                }
            )
        }
    }

    private fun basePostId(postId: String): String = postId.substringBefore("_r")

    private suspend fun seedMissingInteractions(
        postIds: List<String>,
        existing: Map<String, LocalFeedInteraction>,
    ) {
        val missing = postIds.map(::basePostId).distinct().filterNot(existing::containsKey)
        if (missing.isEmpty()) return
        missing.forEach { id ->
            localInteractions.value = localInteractions.value + (id to LocalFeedInteraction())
            feedInteractionStore.save(postId = id, interaction = LocalFeedInteraction())
        }
    }
}
