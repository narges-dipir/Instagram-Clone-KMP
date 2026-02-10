package de.app.instagram.profile.domain.usecase

import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.ProfileStats
import de.app.instagram.profile.domain.repository.ProfileRepository
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GetProfileUseCaseTest {

    @Test
    fun invoke_returnsRepositoryProfile() = runTest {
        val expected = Profile(
            id = "u_001",
            username = "narges",
            fullName = "Narges Dipir",
            bio = "Android and KMP developer",
            isVerified = false,
            avatarUrl = "https://example.com/avatar.jpg",
            stats = ProfileStats(posts = 24, followers = 1280, following = 312),
            website = "https://github.com/nargesdipir",
            storyHighlights = emptyList(),
            posts = emptyList(),
        )
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = expected
        }

        val useCase = GetProfileUseCase(repository)
        val result = useCase()

        assertEquals(expected, result)
    }
}
