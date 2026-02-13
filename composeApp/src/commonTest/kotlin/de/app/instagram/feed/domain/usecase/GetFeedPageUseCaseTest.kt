package de.app.instagram.feed.domain.usecase

import de.app.instagram.feed.domain.model.FeedMediaType
import de.app.instagram.feed.domain.model.FeedPost
import de.app.instagram.feed.domain.model.FeedPostsPage
import de.app.instagram.feed.domain.repository.FeedRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GetFeedPageUseCaseTest {

    @Test
    fun invoke_delegatesToRepository() = runTest {
        var requestedPage = -1
        val repository = object : FeedRepository {
            override suspend fun getPage(page: Int): FeedPostsPage {
                requestedPage = page
                return FeedPostsPage(
                    page = page,
                    hasNext = true,
                    items = listOf(
                        FeedPost(
                            id = "hp_001",
                            username = "u",
                            avatarUrl = "a",
                            imageUrl = "i",
                            mediaType = FeedMediaType.IMAGE,
                            videoUrl = null,
                            likes = 1,
                            comments = 0,
                            caption = "c",
                        )
                    ),
                )
            }
        }

        val useCase = GetFeedPageUseCase(repository)
        val result = useCase(5)

        assertEquals(5, requestedPage)
        assertEquals(5, result.page)
        assertEquals("hp_001", result.items.first().id)
    }
}
