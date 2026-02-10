package de.app.instagram.profile.data.repository

import de.app.instagram.profile.data.remote.ProfileApi
import de.app.instagram.profile.data.remote.ProfileDto
import de.app.instagram.profile.data.remote.ProfilePostDto
import de.app.instagram.profile.data.remote.ProfileStatsDto
import de.app.instagram.profile.data.remote.StoryHighlightDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class DefaultProfileRepositoryTest {

    @Test
    fun getProfile_mapsApiDtoToDomainModel() = runTest {
        val api = object : ProfileApi {
            override suspend fun getProfile(): ProfileDto {
                return ProfileDto(
                    id = "u_001",
                    username = "narges",
                    fullName = "Narges Dipir",
                    bio = "Android and KMP developer",
                    isVerified = false,
                    avatarUrl = "https://example.com/avatar.jpg",
                    stats = ProfileStatsDto(posts = 24, followers = 1280, following = 312),
                    website = "https://github.com/nargesdipir",
                    storyHighlights = listOf(
                        StoryHighlightDto(
                            id = "h_001",
                            title = "Travel",
                            coverUrl = "https://example.com/h1.jpg",
                        )
                    ),
                    posts = listOf(
                        ProfilePostDto(
                            id = "p_001",
                            imageUrl = "https://example.com/p1.jpg",
                            likes = 321,
                            comments = 18,
                        )
                    ),
                )
            }
        }

        val repository = DefaultProfileRepository(api)
        val profile = repository.getProfile()

        assertEquals("u_001", profile.id)
        assertEquals("narges", profile.username)
        assertEquals(24, profile.stats.posts)
        assertEquals(1, profile.storyHighlights.size)
        assertEquals("p_001", profile.posts.first().id)
    }
}
