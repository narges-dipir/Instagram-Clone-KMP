package de.app.instagram.profile.presentation.state

import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.StoryHighlight

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val profile: Profile,
        val isEditing: Boolean,
        val editDraft: EditProfileDraft,
        val editError: String?,
        val selectedPost: ProfilePost?,
        val selectedHighlight: StoryHighlight?,
        val isLoadingMorePosts: Boolean = false,
        val postsErrorMessage: String? = null,
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}
