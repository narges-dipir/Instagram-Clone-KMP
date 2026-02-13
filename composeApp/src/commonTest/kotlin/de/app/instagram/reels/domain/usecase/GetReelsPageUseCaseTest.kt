package de.app.instagram.reels.domain.usecase

import de.app.instagram.reels.domain.model.ReelVideo
import de.app.instagram.reels.domain.model.ReelsPage
import de.app.instagram.reels.domain.repository.ReelsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GetReelsPageUseCaseTest {

    @Test
    fun invoke_delegatesToRepository() = runTest {
        var requestedPage = -1
        val repository = object : ReelsRepository {
            override suspend fun getPage(page: Int): ReelsPage {
                requestedPage = page
                return ReelsPage(
                    page = page,
                    hasNext = true,
                    items = listOf(
                        ReelVideo(
                            id = "r_001",
                            videoUrl = "https://example.com/v.mp4",
                            caption = "caption",
                            username = "u",
                            avatarUrl = "a",
                            likes = 1,
                            comments = 1,
                        )
                    ),
                )
            }
        }

        val useCase = GetReelsPageUseCase(repository)
        val result = useCase(4)

        assertEquals(4, requestedPage)
        assertEquals(4, result.page)
        assertEquals("r_001", result.items.first().id)
    }
}
