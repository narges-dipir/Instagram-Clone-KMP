package de.app.instagram.profile.presentation.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.activity.ComponentActivity
import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.ProfileStats
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.presentation.state.EditProfileDraft
import de.app.instagram.profile.presentation.state.ProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun profileScreen_rendersCoreSections() {
        composeRule.setContent {
            ProfileScreen(
                uiState = successState(),
                onRetryClick = {},
                onStartEditing = {},
                onCancelEditing = {},
                onSaveProfile = {},
                onUsernameChange = {},
                onFullNameChange = {},
                onBioChange = {},
                onWebsiteChange = {},
                onPostClick = {},
                onBackFromPost = {},
                onHighlightClick = {},
                onBackFromHighlight = {},
                onToggleLikeClick = {},
                onAddCommentClick = {},
                onLoadMorePosts = {},
            )
        }

        composeRule.onNodeWithText("@narges").fetchSemanticsNode()
        composeRule.onNodeWithText("Highlights").fetchSemanticsNode()
        composeRule.onNodeWithText("Posts").fetchSemanticsNode()
        composeRule.onNodeWithText("Edit profile").fetchSemanticsNode()
    }

    @Test
    fun clickingEditProfile_invokesCallback() {
        var editClicks = 0

        composeRule.setContent {
            ProfileScreen(
                uiState = successState(),
                onRetryClick = {},
                onStartEditing = { editClicks += 1 },
                onCancelEditing = {},
                onSaveProfile = {},
                onUsernameChange = {},
                onFullNameChange = {},
                onBioChange = {},
                onWebsiteChange = {},
                onPostClick = {},
                onBackFromPost = {},
                onHighlightClick = {},
                onBackFromHighlight = {},
                onToggleLikeClick = {},
                onAddCommentClick = {},
                onLoadMorePosts = {},
            )
        }

        composeRule.onNodeWithText("Edit profile").performClick()
        assertEquals(1, editClicks)
    }

    @Test
    fun clickingPost_invokesPostCallbackWithCorrectId() {
        var selectedPostId: String? = null

        composeRule.setContent {
            ProfileScreen(
                uiState = successState(),
                onRetryClick = {},
                onStartEditing = {},
                onCancelEditing = {},
                onSaveProfile = {},
                onUsernameChange = {},
                onFullNameChange = {},
                onBioChange = {},
                onWebsiteChange = {},
                onPostClick = { selectedPostId = it.id },
                onBackFromPost = {},
                onHighlightClick = {},
                onBackFromHighlight = {},
                onToggleLikeClick = {},
                onAddCommentClick = {},
                onLoadMorePosts = {},
            )
        }

        composeRule.onNodeWithContentDescription("Post p_001").performClick()
        assertEquals("p_001", selectedPostId)
    }

    private fun successState(): ProfileUiState.Success {
        val profile = Profile(
            id = "u_001",
            username = "narges",
            fullName = "Narges Dipir",
            bio = "Android and KMP developer",
            isVerified = false,
            avatarUrl = "https://example.com/avatar.jpg",
            stats = ProfileStats(posts = 1, followers = 100, following = 50),
            website = "https://example.com",
            storyHighlights = listOf(
                StoryHighlight(
                    id = "h_001",
                    title = "Travel",
                    coverUrl = "https://example.com/highlight.jpg",
                    mediaUrls = listOf("https://example.com/highlight.jpg"),
                )
            ),
            posts = listOf(
                ProfilePost(
                    id = "p_001",
                    imageUrl = "https://example.com/post.jpg",
                    likes = 10,
                    comments = 2,
                )
            ),
        )

        return ProfileUiState.Success(
            profile = profile,
            isEditing = false,
            editDraft = EditProfileDraft.fromProfile(profile),
            editError = null,
            selectedPost = null,
            selectedHighlight = null,
            isLoadingMorePosts = false,
            postsErrorMessage = null,
        )
    }
}
