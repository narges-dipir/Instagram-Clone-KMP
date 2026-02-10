package de.app.instagram.profile.presentation

import de.app.instagram.profile.domain.Profile

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: Profile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}
