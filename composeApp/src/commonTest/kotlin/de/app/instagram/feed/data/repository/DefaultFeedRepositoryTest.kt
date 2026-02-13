package de.app.instagram.feed.data.repository

import de.app.instagram.feed.data.remote.FeedApi
import de.app.instagram.feed.data.remote.FeedPostDto
import de.app.instagram.feed.data.remote.FeedPostsPageDto
import de.app.instagram.feed.domain.model.FeedMediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

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

        val repository = DefaultFeedRepository(api)
        val page = repository.getPage(3)

        assertEquals(3, page.page)
        assertEquals(false, page.hasNext)
        assertEquals(FeedMediaType.VIDEO, page.items.first().mediaType)
        assertEquals("https://example.com/v.mp4", page.items.first().videoUrl)
    }
}
