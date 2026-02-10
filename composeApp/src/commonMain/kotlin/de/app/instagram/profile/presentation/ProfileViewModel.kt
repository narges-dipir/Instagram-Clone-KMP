package de.app.instagram.profile.presentation

import de.app.instagram.profile.domain.GetProfileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
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
                ProfileUiState.Success(getProfileUseCase())
            } catch (throwable: Throwable) {
                ProfileUiState.Error(throwable.message ?: "Failed to load profile")
            }
        }
    }
}
