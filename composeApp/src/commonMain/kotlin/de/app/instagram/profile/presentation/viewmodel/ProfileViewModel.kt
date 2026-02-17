package de.app.instagram.profile.presentation.viewmodel

import de.app.instagram.profile.data.local.LocalPostInteraction
import de.app.instagram.profile.data.local.PostInteractionStore
import de.app.instagram.di.createDefaultAppScope
import de.app.instagram.feed.domain.model.FeedMediaType
import de.app.instagram.feed.domain.model.FeedPost
import de.app.instagram.feed.domain.usecase.GetFeedPageUseCase
import de.app.instagram.profile.domain.model.PostMediaType
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.presentation.state.EditProfileDraft
import de.app.instagram.profile.presentation.state.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val getFeedPageUseCase: GetFeedPageUseCase,
    private val postInteractionStore: PostInteractionStore,
    private val scope: CoroutineScope = createDefaultAppScope(),
) {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val localInteractions = MutableStateFlow<Map<String, LocalPostInteraction>>(emptyMap())
    private var nextPostsPage = 1
    private var postsLoopRound = 0

    init {
        scope.launch {
            postInteractionStore.observeAll().collect { snapshot ->
                localInteractions.value = snapshot
            }
        }
        loadProfile()
    }

    fun loadProfile() {
        scope.launch {
            _uiState.value = ProfileUiState.Loading
            _uiState.value = try {
                nextPostsPage = 1
                postsLoopRound = 0
                val profile = getProfileUseCase()
                val initialPosts = runCatching { getFeedPageUseCase(nextPostsPage) }.getOrNull()
                val mappedInitialPosts = initialPosts?.items.orEmpty().mapToProfilePosts(postsLoopRound)
                if (initialPosts != null) {
                    if (initialPosts.hasNext) {
                        nextPostsPage = initialPosts.page + 1
                    } else {
                        nextPostsPage = 1
                        postsLoopRound = 1
                    }
                }
                val localInteractionsSnapshot = localInteractions.value
                ProfileUiState.Success(
                    profile = profile.copy(
                        posts = mappedInitialPosts.map { post ->
                            val local = localInteractionsSnapshot[basePostId(post.id)]
                            if (local == null) {
                                post
                            } else {
                                post.copy(
                                    likes = post.likes + if (local.isLikedByMe) 1 else 0,
                                    comments = post.comments + local.comments.size,
                                    isLikedByMe = local.isLikedByMe,
                                    recentComments = local.comments,
                                )
                            }
                        },
                        stats = profile.stats.copy(posts = mappedInitialPosts.size),
                    ),
                    isEditing = false,
                    editDraft = EditProfileDraft.fromProfile(profile),
                    editError = null,
                    selectedPost = null,
                    selectedHighlight = null,
                    postsErrorMessage = if (initialPosts == null) {
                        "Could not load posts."
                    } else {
                        null
                    },
                )
            } catch (throwable: Throwable) {
                ProfileUiState.Error(throwable.message ?: "Failed to load profile")
            }
            val latest = _uiState.value as? ProfileUiState.Success
            if (latest != null) {
                seedMissingInteractions(
                    postIds = latest.profile.posts.map { it.id },
                    existing = localInteractions.value,
                )
            }
        }
    }

    fun startEditing() {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = successState.copy(
            isEditing = true,
            editDraft = EditProfileDraft.fromProfile(successState.profile),
            editError = null,
            selectedPost = null,
            selectedHighlight = null,
        )
    }

    fun cancelEditing() {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = successState.copy(
            isEditing = false,
            editDraft = EditProfileDraft.fromProfile(successState.profile),
            editError = null,
            selectedPost = null,
            selectedHighlight = null,
        )
    }

    fun updateUsername(value: String) = updateDraft { copy(username = value) }

    fun updateFullName(value: String) = updateDraft { copy(fullName = value) }

    fun updateBio(value: String) = updateDraft { copy(bio = value) }

    fun updateWebsite(value: String) = updateDraft { copy(website = value) }

    fun saveProfileChanges() {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        val draft = successState.editDraft
        val validationMessage = validateDraft(draft)

        if (validationMessage != null) {
            _uiState.value = successState.copy(editError = validationMessage)
            return
        }

        val updatedProfile = successState.profile.copy(
            username = draft.username.trim(),
            fullName = draft.fullName.trim(),
            bio = draft.bio.trim(),
            website = draft.website.trim(),
        )
        _uiState.value = successState.copy(
            profile = updatedProfile,
            isEditing = false,
            editDraft = EditProfileDraft.fromProfile(updatedProfile),
            editError = null,
            selectedPost = null,
            selectedHighlight = null,
        )
    }

    fun openPost(post: ProfilePost) {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = successState.copy(
            isEditing = false,
            editError = null,
            selectedPost = post,
            selectedHighlight = null,
        )
    }

    fun closePost() {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = successState.copy(selectedPost = null)
    }

    fun openHighlight(highlight: StoryHighlight) {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = successState.copy(
            isEditing = false,
            editError = null,
            selectedPost = null,
            selectedHighlight = highlight,
        )
    }

    fun closeHighlight() {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = successState.copy(selectedHighlight = null)
    }

    fun loadMorePosts() {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        if (successState.isLoadingMorePosts) return

        _uiState.value = successState.copy(
            isLoadingMorePosts = true,
            postsErrorMessage = null,
        )

        scope.launch {
            runCatching { getFeedPageUseCase(nextPostsPage) }
                .onSuccess { pageData ->
                    val localInteractionsSnapshot = localInteractions.value
                    val loadedPosts = pageData.items
                        .mapToProfilePosts(postsLoopRound)
                        .map { post ->
                            val local = localInteractionsSnapshot[basePostId(post.id)]
                            if (local == null) {
                                post
                            } else {
                                post.copy(
                                    likes = post.likes + if (local.isLikedByMe) 1 else 0,
                                    comments = post.comments + local.comments.size,
                                    isLikedByMe = local.isLikedByMe,
                                    recentComments = local.comments,
                                )
                            }
                        }

                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onSuccess
                    val updatedPosts = latest.profile.posts + loadedPosts
                    _uiState.value = latest.copy(
                        profile = latest.profile.copy(
                            posts = updatedPosts,
                            stats = latest.profile.stats.copy(posts = updatedPosts.size),
                        ),
                        isLoadingMorePosts = false,
                        postsErrorMessage = null,
                    )
                    seedMissingInteractions(
                        postIds = loadedPosts.map { it.id },
                        existing = localInteractionsSnapshot,
                    )

                    if (pageData.hasNext) {
                        nextPostsPage = pageData.page + 1
                    } else {
                        nextPostsPage = 1
                        postsLoopRound += 1
                    }
                }
                .onFailure { throwable ->
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(
                        isLoadingMorePosts = false,
                        postsErrorMessage = throwable.message ?: "Could not load more posts",
                    )
                }
        }
    }

    fun toggleLikeForSelectedPost() {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        val selected = successState.selectedPost ?: return
        val updatedSelected = if (selected.isLikedByMe) {
            selected.copy(
                isLikedByMe = false,
                likes = (selected.likes - 1).coerceAtLeast(0),
            )
        } else {
            selected.copy(
                isLikedByMe = true,
                likes = selected.likes + 1,
            )
        }
        applyPostUpdate(updatedSelected)
    }

    fun addCommentToSelectedPost(comment: String) {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        val selected = successState.selectedPost ?: return
        val trimmed = comment.trim()
        if (trimmed.isEmpty()) return

        val updatedSelected = selected.copy(
            comments = selected.comments + 1,
            recentComments = selected.recentComments + trimmed,
        )
        applyPostUpdate(updatedSelected)
    }

    private fun updateDraft(transform: EditProfileDraft.() -> EditProfileDraft) {
        val successState = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = successState.copy(
            editDraft = successState.editDraft.transform(),
            editError = null,
        )
    }

    private fun validateDraft(draft: EditProfileDraft): String? {
        val username = draft.username.trim()
        val fullName = draft.fullName.trim()
        val bio = draft.bio.trim()
        val website = draft.website.trim()

        if (username.length < 3) return "Username must have at least 3 characters."
        if (username.length > 20) return "Username cannot exceed 20 characters."
        if (!username.all { it.isLetterOrDigit() || it == '_' || it == '.' }) {
            return "Username can only use letters, numbers, '_' and '.'."
        }
        if (fullName.isEmpty()) return "Full name is required."
        if (fullName.length > 40) return "Full name cannot exceed 40 characters."
        if (bio.length > 160) return "Bio cannot exceed 160 characters."
        if (website.isNotEmpty() && !isValidWebsite(website)) {
            return "Website must start with http:// or https://."
        }
        return null
    }

    private fun isValidWebsite(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun applyPostUpdate(updatedPost: ProfilePost) {
        val successState = _uiState.value as? ProfileUiState.Success ?: return

        val updatedPosts = successState.profile.posts.map { post ->
            if (post.id == updatedPost.id) updatedPost else post
        }
        _uiState.value = successState.copy(
            profile = successState.profile.copy(posts = updatedPosts),
            selectedPost = updatedPost,
        )

        scope.launch {
            localInteractions.value = localInteractions.value + (
                basePostId(updatedPost.id) to LocalPostInteraction(
                    isLikedByMe = updatedPost.isLikedByMe,
                    comments = updatedPost.recentComments,
                )
            )
            postInteractionStore.save(
                postId = basePostId(updatedPost.id),
                interaction = LocalPostInteraction(
                    isLikedByMe = updatedPost.isLikedByMe,
                    comments = updatedPost.recentComments,
                ),
            )
        }
    }

    private fun List<FeedPost>.mapToProfilePosts(round: Int): List<ProfilePost> {
        return map { feedPost ->
            ProfilePost(
                id = "${feedPost.id}_r$round",
                imageUrl = feedPost.imageUrl,
                mediaType = if (feedPost.mediaType == FeedMediaType.VIDEO) {
                    PostMediaType.VIDEO
                } else {
                    PostMediaType.IMAGE
                },
                videoUrl = feedPost.videoUrl,
                likes = feedPost.likes,
                comments = feedPost.comments,
            )
        }
    }

    private fun basePostId(postId: String): String = postId.substringBefore("_r")

    private suspend fun seedMissingInteractions(
        postIds: List<String>,
        existing: Map<String, LocalPostInteraction>,
    ) {
        val missing = postIds.map(::basePostId).distinct().filterNot(existing::containsKey)
        if (missing.isEmpty()) return
        missing.forEach { id ->
            localInteractions.value = localInteractions.value + (id to LocalPostInteraction())
            postInteractionStore.save(postId = id, interaction = LocalPostInteraction())
        }
    }
}
