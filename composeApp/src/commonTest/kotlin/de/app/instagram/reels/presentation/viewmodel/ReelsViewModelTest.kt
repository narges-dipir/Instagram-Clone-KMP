package de.app.instagram.reels.presentation.viewmodel

import de.app.instagram.reels.data.local.InMemoryReelInteractionStore
import de.app.instagram.reels.data.local.LocalReelInteraction
import de.app.instagram.reels.domain.model.ReelVideo
import de.app.instagram.reels.domain.model.ReelsPage
import de.app.instagram.reels.domain.repository.ReelsRepository
import de.app.instagram.reels.domain.usecase.GetReelsPageUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ReelsViewModelTest {

    @Test
    fun init_loadsFirstPage_andAppliesStoredLocalState() = runTest {
        val store = InMemoryReelInteractionStore().apply {
            save(
                reelId = "r_001",
                interaction = LocalReelInteraction(
                    isLikedByMe = true,
                    isSavedByMe = true,
                    isFollowingCreator = true,
                    comments = listOf("local comment"),
                    localShares = 3,
                ),
            )
        }
        val viewModel = ReelsViewModel(
            getReelsPageUseCase = testUseCase(),
            reelInteractionStore = store,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
        )

        advanceUntilIdle()

        val reel = viewModel.uiState.value.items.first()
        assertEquals("r_001_r0", reel.id)
        assertTrue(reel.isLikedByMe)
        assertTrue(reel.isSavedByMe)
        assertTrue(reel.isFollowingCreator)
        assertEquals(11, reel.likes)
        assertEquals(3, reel.comments)
        assertEquals(3, reel.shares)
        assertEquals(listOf("local comment"), reel.recentComments)
    }

    @Test
    fun toggleLike_andAddComment_persistToStore() = runTest {
        val store = InMemoryReelInteractionStore()
        val viewModel = ReelsViewModel(
            getReelsPageUseCase = testUseCase(),
            reelInteractionStore = store,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
        )
        advanceUntilIdle()

        val reelId = viewModel.uiState.value.items.first().id
        viewModel.toggleLike(reelId)
        viewModel.addComment(reelId, "awesome")
        advanceUntilIdle()

        val updated = viewModel.uiState.value.items.first()
        assertTrue(updated.isLikedByMe)
        assertEquals(11, updated.likes)
        assertEquals(3, updated.comments)
        assertEquals(listOf("awesome"), updated.recentComments)

        val stored = store.readAll().getValue("r_001")
        assertTrue(stored.isLikedByMe)
        assertEquals(listOf("awesome"), stored.comments)
    }

    @Test
    fun share_save_follow_updateUiAndPersistence() = runTest {
        val store = InMemoryReelInteractionStore()
        val viewModel = ReelsViewModel(
            getReelsPageUseCase = testUseCase(),
            reelInteractionStore = store,
            scope = TestScope(StandardTestDispatcher(testScheduler)),
        )
        advanceUntilIdle()

        val reelId = viewModel.uiState.value.items.first().id
        viewModel.share(reelId)
        viewModel.toggleSave(reelId)
        viewModel.toggleFollow(reelId)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.items.first()
        assertEquals(1, updated.shares)
        assertTrue(updated.isSavedByMe)
        assertTrue(updated.isFollowingCreator)

        val stored = store.readAll().getValue("r_001")
        assertEquals(1, stored.localShares)
        assertTrue(stored.isSavedByMe)
        assertTrue(stored.isFollowingCreator)
    }

    private fun testUseCase(): GetReelsPageUseCase {
        val repository = object : ReelsRepository {
            override suspend fun getPage(page: Int): ReelsPage {
                return ReelsPage(
                    page = page,
                    hasNext = false,
                    items = listOf(
                        ReelVideo(
                            id = "r_001",
                            videoUrl = "https://example.com/video.mp4",
                            caption = "caption",
                            username = "bonn.fit",
                            avatarUrl = "https://example.com/avatar.jpg",
                            likes = 10,
                            comments = 2,
                        )
                    ),
                )
            }
        }
        return GetReelsPageUseCase(repository)
    }
}
