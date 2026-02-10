package de.app.instagram.profile.presentation.viewmodel

import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.ProfileStats
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.domain.repository.ProfileRepository
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import de.app.instagram.profile.presentation.state.ProfileUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun init_loadsProfile_andEmitsSuccess() = runTest {
        val expected = testProfile()
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = expected
        }
        val useCase = GetProfileUseCase(repository)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            scope = scope,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(state)
        assertEquals(expected.username, state.profile.username)
    }

    @Test
    fun init_whenFailure_emitsError() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile {
                error("network failed")
            }
        }
        val useCase = GetProfileUseCase(repository)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            scope = scope,
        )

        advanceUntilIdle()

        assertIs<ProfileUiState.Error>(viewModel.uiState.value)
    }
}

private fun testProfile(): Profile {
    return Profile(
        id = "u_001",
        username = "narges",
        fullName = "Narges Dipir",
        bio = "Android and KMP developer",
        isVerified = false,
        avatarUrl = "https://example.com/avatar.jpg",
        stats = ProfileStats(posts = 24, followers = 1280, following = 312),
        website = "https://github.com/nargesdipir",
        storyHighlights = listOf(
            StoryHighlight(
                id = "h_001",
                title = "Travel",
                coverUrl = "https://example.com/h1.jpg",
            )
        ),
        posts = listOf(
            ProfilePost(
                id = "p_001",
                imageUrl = "https://example.com/p1.jpg",
                likes = 321,
                comments = 18,
            )
        ),
    )
}
