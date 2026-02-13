package de.app.instagram.profile.presentation.viewmodel

import de.app.instagram.profile.data.local.InMemoryPostInteractionStore
import de.app.instagram.profile.data.local.LocalPostInteraction
import de.app.instagram.profile.data.local.PostInteractionStore
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.presentation.state.EditProfileDraft
import de.app.instagram.profile.presentation.state.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val postInteractionStore: PostInteractionStore = InMemoryPostInteractionStore(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        scope.launch {
            _uiState.value = ProfileUiState.Loading
            _uiState.value = try {
                val profile = getProfileUseCase()
                val localInteractions = postInteractionStore.readAll()
                ProfileUiState.Success(
                    profile = profile.copy(
                        posts = profile.posts.map { post ->
                            val local = localInteractions[post.id]
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
                    ),
                    isEditing = false,
                    editDraft = EditProfileDraft.fromProfile(profile),
                    editError = null,
                    selectedPost = null,
                    selectedHighlight = null,
                )
            } catch (throwable: Throwable) {
                ProfileUiState.Error(throwable.message ?: "Failed to load profile")
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
            postInteractionStore.save(
                postId = updatedPost.id,
                interaction = LocalPostInteraction(
                    isLikedByMe = updatedPost.isLikedByMe,
                    comments = updatedPost.recentComments,
                ),
            )
        }
    }
}
