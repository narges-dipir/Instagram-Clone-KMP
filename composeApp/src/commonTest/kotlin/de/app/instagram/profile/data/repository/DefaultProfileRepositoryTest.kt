package de.app.instagram.profile.data.repository

import de.app.instagram.db.RemoteContentCache
import de.app.instagram.profile.data.remote.ProfileApi
import de.app.instagram.profile.data.remote.ProfileDto
import de.app.instagram.profile.data.remote.ProfilePostDto
import de.app.instagram.profile.data.remote.ProfileStatsDto
import de.app.instagram.profile.data.remote.StoryHighlightDto
import de.app.instagram.profile.domain.model.PostMediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

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
                            mediaType = "video",
                            videoUrl = "https://example.com/video.mp4",
                            likes = 321,
                            comments = 18,
                        )
                    ),
                )
            }
        }

        val repository = DefaultProfileRepository(
            profileApi = api,
            remoteContentCache = createRemoteContentCacheMock(),
            json = Json { ignoreUnknownKeys = true; isLenient = true },
        )
        val profile = repository.getProfile()

        assertEquals("u_001", profile.id)
        assertEquals("narges", profile.username)
        assertEquals(24, profile.stats.posts)
        assertEquals(1, profile.storyHighlights.size)
        assertEquals("p_001", profile.posts.first().id)
        assertEquals(PostMediaType.VIDEO, profile.posts.first().mediaType)
        assertEquals("https://example.com/video.mp4", profile.posts.first().videoUrl)
    }

    @Test
    fun getProfile_readsFromCacheWhenNetworkFails() = runTest {
        val cache = createRemoteContentCacheMock()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        cache.write(
            key = "content_cache.profile",
            value = json.encodeToString(
                ProfileDto.serializer(),
                ProfileDto(
                    id = "cached_u",
                    username = "cached.profile",
                    fullName = "Cached Profile",
                    bio = "cached bio",
                    isVerified = true,
                    avatarUrl = "https://example.com/avatar.jpg",
                    stats = ProfileStatsDto(posts = 9, followers = 200, following = 100),
                    website = "https://example.com",
                    storyHighlights = emptyList(),
                    posts = emptyList(),
                ),
            ),
        )

        val api = object : ProfileApi {
            override suspend fun getProfile(): ProfileDto {
                error("network down")
            }
        }

        val repository = DefaultProfileRepository(
            profileApi = api,
            remoteContentCache = cache,
            json = json,
        )
        val profile = repository.getProfile()

        assertEquals("cached_u", profile.id)
        assertEquals("cached.profile", profile.username)
        assertEquals(true, profile.isVerified)
    }

    private fun createRemoteContentCacheMock(): RemoteContentCache {
        val storage = mutableMapOf<String, MutableStateFlow<String?>>()
        return object : RemoteContentCache {
            override fun observe(key: String): Flow<String?> {
                return storage.getOrPut(key) { MutableStateFlow(null) }
            }

            override suspend fun read(key: String): String? = storage[key]?.value

            override suspend fun write(key: String, value: String) {
                storage.getOrPut(key) { MutableStateFlow(null) }.value = value
            }
        }
    }
}
