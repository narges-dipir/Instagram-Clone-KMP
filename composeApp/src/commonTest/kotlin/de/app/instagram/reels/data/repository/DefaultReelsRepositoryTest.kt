package de.app.instagram.reels.data.repository

import de.app.instagram.reels.data.remote.ReelVideoDto
import de.app.instagram.reels.data.remote.ReelsApi
import de.app.instagram.reels.data.remote.ReelsPageDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

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

        val repository = DefaultReelsRepository(api)
        val page = repository.getPage(2)

        assertEquals(2, page.page)
        assertEquals(false, page.hasNext)
        assertEquals("r_001", page.items.first().id)
        assertEquals("bonn.fit", page.items.first().username)
        assertEquals(10, page.items.first().likes)
    }
}
