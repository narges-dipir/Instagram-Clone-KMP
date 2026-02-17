package de.app.instagram.reels.data.repository

import de.app.instagram.db.RemoteContentCache
import de.app.instagram.reels.data.remote.ReelVideoDto
import de.app.instagram.reels.data.remote.ReelsApi
import de.app.instagram.reels.data.remote.ReelsPageDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class DefaultReelsRepositoryTest {

    @Test
    fun getPage_mapsDtoToDomain() = runTest {
        val api = object : ReelsApi {
            override suspend fun getPage(page: Int): ReelsPageDto {
                return ReelsPageDto(
                    page = page,
                    hasNext = false,
                    items = listOf(
                        ReelVideoDto(
                            id = "r_001",
                            videoUrl = "https://example.com/video.mp4",
                            caption = "caption",
                            username = "bonn.fit",
                            avatarUrl = "https://example.com/avatar.jpg",
                            likes = 10,
                            comments = 3,
                        )
                    ),
                )
            }
        }

        val repository = DefaultReelsRepository(
            api = api,
            remoteContentCache = createRemoteContentCacheMock(),
            json = Json { ignoreUnknownKeys = true; isLenient = true },
        )
        val page = repository.getPage(2)

        assertEquals(2, page.page)
        assertEquals(false, page.hasNext)
        assertEquals("r_001", page.items.first().id)
        assertEquals("bonn.fit", page.items.first().username)
        assertEquals(10, page.items.first().likes)
    }

    @Test
    fun getPage_readsFromCacheWhenNetworkFails() = runTest {
        val cache = createRemoteContentCacheMock()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val pageNumber = 4
        cache.write(
            key = "content_cache.reels.page.$pageNumber",
            value = json.encodeToString(
                ReelsPageDto.serializer(),
                ReelsPageDto(
                    page = pageNumber,
                    hasNext = false,
                    items = listOf(
                        ReelVideoDto(
                            id = "cached_reel_1",
                            videoUrl = "https://example.com/video.mp4",
                            caption = "cached",
                            username = "cached.reel",
                            avatarUrl = "https://example.com/avatar.jpg",
                            likes = 99,
                            comments = 12,
                        )
                    ),
                ),
            ),
        )

        val api = object : ReelsApi {
            override suspend fun getPage(page: Int): ReelsPageDto {
                error("network down")
            }
        }

        val repository = DefaultReelsRepository(
            api = api,
            remoteContentCache = cache,
            json = json,
        )
        val page = repository.getPage(pageNumber)

        assertEquals(pageNumber, page.page)
        assertEquals("cached_reel_1", page.items.first().id)
        assertEquals("cached.reel", page.items.first().username)
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
