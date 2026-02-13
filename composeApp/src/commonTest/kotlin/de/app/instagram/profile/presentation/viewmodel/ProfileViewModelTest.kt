package de.app.instagram.profile.presentation.viewmodel

import de.app.instagram.profile.data.local.InMemoryPostInteractionStore
import de.app.instagram.feed.domain.model.FeedMediaType
import de.app.instagram.feed.domain.model.FeedPost
import de.app.instagram.feed.domain.model.FeedPostsPage
import de.app.instagram.feed.domain.repository.FeedRepository
import de.app.instagram.feed.domain.usecase.GetFeedPageUseCase
import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.ProfileStats
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.domain.repository.ProfileRepository
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import de.app.instagram.profile.presentation.state.ProfileUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun init_loadsProfile_andEmitsSuccess() = runTest {
        val expected = testProfile()
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = expected
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(state)
        assertEquals(expected.username, state.profile.username)
        assertFalse(state.isEditing)
        assertNull(state.selectedPost)
        assertNull(state.selectedHighlight)
    }

    @Test
    fun init_whenFailure_emitsError() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile {
                error("network failed")
            }
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )

        advanceUntilIdle()

        assertIs<ProfileUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun editAndSave_updatesProfile_andLeavesEditMode() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = testProfile()
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )
        advanceUntilIdle()

        viewModel.startEditing()
        viewModel.updateUsername("new_name")
        viewModel.updateFullName("New Name")
        viewModel.updateBio("Updated bio")
        viewModel.updateWebsite("https://example.com/new")
        viewModel.saveProfileChanges()

        val state = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(state)
        assertFalse(state.isEditing)
        assertNull(state.editError)
        assertNull(state.selectedPost)
        assertNull(state.selectedHighlight)
        assertEquals("new_name", state.profile.username)
        assertEquals("New Name", state.profile.fullName)
        assertEquals("Updated bio", state.profile.bio)
        assertEquals("https://example.com/new", state.profile.website)
    }

    @Test
    fun saveProfileChanges_withInvalidWebsite_setsValidationError() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = testProfile()
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )
        advanceUntilIdle()

        viewModel.startEditing()
        viewModel.updateWebsite("example.com/no-scheme")
        viewModel.saveProfileChanges()

        val state = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(state)
        assertTrue(state.isEditing)
        assertEquals("Website must start with http:// or https://.", state.editError)
    }

    @Test
    fun openAndClosePost_updatesSelectedPost() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = testProfile()
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )
        advanceUntilIdle()

        val post = testProfile().posts.first()
        viewModel.openPost(post)

        val selectedState = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(selectedState)
        assertEquals(post.id, selectedState.selectedPost?.id)

        viewModel.closePost()

        val closedState = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(closedState)
        assertNull(closedState.selectedPost)
    }

    @Test
    fun openAndCloseHighlight_updatesSelectedHighlight() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = testProfile()
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )
        advanceUntilIdle()

        val highlight = testProfile().storyHighlights.first()
        viewModel.openHighlight(highlight)

        val selectedState = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(selectedState)
        assertEquals(highlight.id, selectedState.selectedHighlight?.id)

        viewModel.closeHighlight()

        val closedState = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(closedState)
        assertNull(closedState.selectedHighlight)
    }

    @Test
    fun toggleLikeForSelectedPost_updatesLikeState() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = testProfile()
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )
        advanceUntilIdle()

        val post = testProfile().posts.first()
        viewModel.openPost(post)
        viewModel.toggleLikeForSelectedPost()

        val likedState = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(likedState)
        assertEquals(true, likedState.selectedPost?.isLikedByMe)
        assertEquals(post.likes + 1, likedState.selectedPost?.likes)
    }

    @Test
    fun addCommentToSelectedPost_incrementsCount_andStoresComment() = runTest {
        val repository = object : ProfileRepository {
            override suspend fun getProfile(): Profile = testProfile()
        }
        val useCase = GetProfileUseCase(repository)
        val feedUseCase = testFeedUseCase()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val viewModel = ProfileViewModel(
            getProfileUseCase = useCase,
            getFeedPageUseCase = feedUseCase,
            postInteractionStore = InMemoryPostInteractionStore(),
            scope = scope,
        )
        advanceUntilIdle()

        val post = testProfile().posts.first()
        viewModel.openPost(post)
        viewModel.addCommentToSelectedPost("nice shot")

        val state = viewModel.uiState.value
        assertIs<ProfileUiState.Success>(state)
        assertEquals(post.comments + 1, state.selectedPost?.comments)
        assertEquals(listOf("nice shot"), state.selectedPost?.recentComments)
    }
}

private fun testProfile(): Profile {
    return Profile(
        id = "u_001",
        username = "narges",
        fullName = "Narges Dipir",
        bio = "Android and KMP developer",
        isVerified = false,
        avatarUrl = "https://example.com/avatar.jpg",
        stats = ProfileStats(posts = 24, followers = 1280, following = 312),
        website = "https://github.com/nargesdipir",
        storyHighlights = listOf(
            StoryHighlight(
                id = "h_001",
                title = "Travel",
                coverUrl = "https://example.com/h1.jpg",
            )
        ),
        posts = listOf(
            ProfilePost(
                id = "p_001",
                imageUrl = "https://example.com/p1.jpg",
                likes = 321,
                comments = 18,
            )
        ),
    )
}

private fun testFeedUseCase(): GetFeedPageUseCase {
    val repository = object : FeedRepository {
        override suspend fun getPage(page: Int): FeedPostsPage {
            return FeedPostsPage(
                page = page,
                hasNext = true,
                items = listOf(
                    FeedPost(
                        id = "feed_001",
                        username = "feed.user",
                        avatarUrl = "https://example.com/avatar.jpg",
                        imageUrl = "https://example.com/post.jpg",
                        mediaType = FeedMediaType.IMAGE,
                        videoUrl = null,
                        likes = 10,
                        comments = 2,
                        caption = "test caption",
                    )
                ),
            )
        }
    }
    return GetFeedPageUseCase(repository)
}
