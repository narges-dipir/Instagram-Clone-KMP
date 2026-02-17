package de.app.instagram.feed.data.repository

import de.app.instagram.db.RemoteContentCache
import de.app.instagram.feed.data.remote.FeedApi
import de.app.instagram.feed.data.remote.FeedPostDto
import de.app.instagram.feed.data.remote.FeedPostsPageDto
import de.app.instagram.feed.domain.model.FeedMediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class DefaultFeedRepositoryTest {

    @Test
    fun getPage_mapsDtoToDomain() = runTest {
        val api = object : FeedApi {
            override suspend fun getPage(page: Int): FeedPostsPageDto {
                return FeedPostsPageDto(
                    page = page,
                    hasNext = false,
                    items = listOf(
                        FeedPostDto(
                            id = "hp_001",
                            username = "bonn.fit",
                            avatarUrl = "https://example.com/a.jpg",
                            imageUrl = "https://example.com/p.jpg",
                            mediaType = "video",
                            videoUrl = "https://example.com/v.mp4",
                            likes = 21,
                            comments = 4,
                            caption = "caption",
                        )
                    ),
                )
            }
        }

        val repository = DefaultFeedRepository(
            api = api,
            remoteContentCache = createRemoteContentCacheMock(),
            json = Json { ignoreUnknownKeys = true; isLenient = true },
        )
        val page = repository.getPage(3)

        assertEquals(3, page.page)
        assertEquals(false, page.hasNext)
        assertEquals(FeedMediaType.VIDEO, page.items.first().mediaType)
        assertEquals("https://example.com/v.mp4", page.items.first().videoUrl)
    }

    @Test
    fun getPage_readsFromCacheWhenNetworkFails() = runTest {
        val cache = createRemoteContentCacheMock()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val pageNumber = 5
        cache.write(
            key = "content_cache.feed.page.$pageNumber",
            value = json.encodeToString(
                FeedPostsPageDto.serializer(),
                FeedPostsPageDto(
                    page = pageNumber,
                    hasNext = true,
                    items = listOf(
                        FeedPostDto(
                            id = "cached_001",
                            username = "cached.user",
                            avatarUrl = "https://example.com/a.jpg",
                            imageUrl = "https://example.com/p.jpg",
                            mediaType = "image",
                            likes = 7,
                            comments = 2,
                            caption = "from cache",
                        )
                    ),
                ),
            ),
        )

        val api = object : FeedApi {
            override suspend fun getPage(page: Int): FeedPostsPageDto {
                error("network down")
            }
        }

        val repository = DefaultFeedRepository(
            api = api,
            remoteContentCache = cache,
            json = json,
        )
        val page = repository.getPage(pageNumber)

        assertEquals(pageNumber, page.page)
        assertEquals("cached_001", page.items.first().id)
        assertEquals("cached.user", page.items.first().username)
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
