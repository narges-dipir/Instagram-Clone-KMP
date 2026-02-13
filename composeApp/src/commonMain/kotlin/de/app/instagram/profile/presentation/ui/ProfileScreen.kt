package de.app.instagram.profile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.app.instagram.profile.domain.model.Profile
import de.app.instagram.profile.domain.model.PostMediaType
import de.app.instagram.profile.domain.model.ProfilePost
import de.app.instagram.profile.domain.model.StoryHighlight
import de.app.instagram.profile.presentation.state.EditProfileDraft
import de.app.instagram.profile.presentation.state.ProfileUiState
import de.app.instagram.ui.PlatformBackHandler
import de.app.instagram.ui.PlatformVideoPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private const val HIGHLIGHT_PAGE_DURATION_MS = 3200

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onRetryClick: () -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onPostClick: (ProfilePost) -> Unit,
    onBackFromPost: () -> Unit,
    onHighlightClick: (StoryHighlight) -> Unit,
    onBackFromHighlight: () -> Unit,
    onToggleLikeClick: () -> Unit,
    onAddCommentClick: (String) -> Unit,
    onLoadMorePosts: () -> Unit,
) {
    when (uiState) {
        ProfileUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading profile...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        is ProfileUiState.Success -> {
            val selectedPost = uiState.selectedPost
            val selectedHighlight = uiState.selectedHighlight
            if (selectedHighlight != null) {
                HighlightDetailScreen(
                    highlight = selectedHighlight,
                    onBack = onBackFromHighlight,
                )
            } else if (selectedPost != null) {
                PostDetailScreen(
                    post = selectedPost,
                    onBack = onBackFromPost,
                    onToggleLike = onToggleLikeClick,
                    onAddComment = onAddCommentClick,
                )
            } else {
                ProfileContent(
                    profile = uiState.profile,
                    isEditing = uiState.isEditing,
                    editDraft = uiState.editDraft,
                    editError = uiState.editError,
                    onStartEditing = onStartEditing,
                    onCancelEditing = onCancelEditing,
                    onSaveProfile = onSaveProfile,
                    onUsernameChange = onUsernameChange,
                    onFullNameChange = onFullNameChange,
                    onBioChange = onBioChange,
                    onWebsiteChange = onWebsiteChange,
                    onPostClick = onPostClick,
                    onHighlightClick = onHighlightClick,
                    isLoadingMorePosts = uiState.isLoadingMorePosts,
                    postsErrorMessage = uiState.postsErrorMessage,
                    onLoadMorePosts = onLoadMorePosts,
                )
            }
        }

        is ProfileUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = uiState.message, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    isEditing: Boolean,
    editDraft: EditProfileDraft,
    editError: String?,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onPostClick: (ProfilePost) -> Unit,
    onHighlightClick: (StoryHighlight) -> Unit,
    isLoadingMorePosts: Boolean,
    postsErrorMessage: String?,
    onLoadMorePosts: () -> Unit,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState, profile.posts.size, isLoadingMorePosts) {
        snapshotFlow {
            val threshold = (scrollState.maxValue - 220).coerceAtLeast(0)
            scrollState.value >= threshold
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (!isLoadingMorePosts) {
                    onLoadMorePosts()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${profile.username}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (profile.isVerified) {
                    Text(
                        text = "Verified",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (!isEditing) {
                OutlinedButton(onClick = onStartEditing) {
                    Text("Edit profile")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Profile avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(label = "Posts", value = profile.stats.posts.toString())
                StatItem(label = "Followers", value = profile.stats.followers.toString())
                StatItem(label = "Following", value = profile.stats.following.toString())
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isEditing) {
            EditProfileSection(
                draft = editDraft,
                error = editError,
                onUsernameChange = onUsernameChange,
                onFullNameChange = onFullNameChange,
                onBioChange = onBioChange,
                onWebsiteChange = onWebsiteChange,
                onCancelEditing = onCancelEditing,
                onSaveProfile = onSaveProfile,
            )
        } else {
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = profile.website,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Highlights",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HighlightsStrip(
            highlights = profile.storyHighlights,
            onHighlightClick = onHighlightClick,
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Text(
            text = "Posts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )
        PostsGrid(
            posts = profile.posts,
            onPostClick = onPostClick,
        )
        if (isLoadingMorePosts) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        if (!postsErrorMessage.isNullOrBlank()) {
            Text(
                text = postsErrorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun EditProfileSection(
    draft: EditProfileDraft,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = draft.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = draft.fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
        )
        OutlinedTextField(
            value = draft.bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            minLines = 3,
            maxLines = 4,
        )
        OutlinedTextField(
            value = draft.website,
            onValueChange = onWebsiteChange,
            label = { Text("Website") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCancelEditing,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSaveProfile,
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PostsGrid(
    posts: List<ProfilePost>,
    onPostClick: (ProfilePost) -> Unit,
) {
    val rows = posts.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { rowPosts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowPosts.forEach { post ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onPostClick(post) },
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "Post ${post.id}",
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (post.mediaType == PostMediaType.VIDEO) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                                        shape = CircleShape,
                                    )
                                    .padding(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Video post",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Text(
                            text = "${post.likes} likes",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                repeat(3 - rowPosts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HighlightsStrip(
    highlights: List<StoryHighlight>,
    onHighlightClick: (StoryHighlight) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        highlights.forEach { highlight ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(78.dp)
                    .clickable { onHighlightClick(highlight) },
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFEDA75),
                                    Color(0xFFFA7E1E),
                                    Color(0xFFD62976),
                                    Color(0xFF962FBF),
                                    Color(0xFF4F5BD5),
                                    Color(0xFFFEDA75),
                                )
                            ),
                            shape = CircleShape,
                        )
                        .padding(3.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = highlight.coverUrl,
                        contentDescription = highlight.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                }
                Text(
                    text = highlight.title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun HighlightDetailScreen(
    highlight: StoryHighlight,
    onBack: () -> Unit,
) {
    val mediaUrls = if (highlight.mediaUrls.isNotEmpty()) {
        highlight.mediaUrls
    } else {
        listOf(highlight.coverUrl)
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { mediaUrls.size },
    )
    val scope = rememberCoroutineScope()
    val pageProgress = remember(highlight.id) { Animatable(0f) }

    LaunchedEffect(pagerState.currentPage, mediaUrls.size) {
        pageProgress.snapTo(0f)
        if (pagerState.currentPage < mediaUrls.lastIndex) {
            pageProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HIGHLIGHT_PAGE_DURATION_MS,
                    easing = LinearEasing,
                ),
            )
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        } else {
            pageProgress.snapTo(1f)
        }
    }

    PlatformBackHandler(
        enabled = true,
        onBack = onBack,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }
        Text(
            text = highlight.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                AsyncImage(
                    model = mediaUrls[page],
                    contentDescription = "${highlight.title} ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            val previousPage = pagerState.currentPage - 1
                            if (previousPage >= 0) {
                                scope.launch { pagerState.animateScrollToPage(previousPage) }
                            }
                        },
                )
                Spacer(modifier = Modifier.weight(0.56f))
                Box(
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            val nextPage = pagerState.currentPage + 1
                            if (nextPage < mediaUrls.size) {
                                scope.launch { pagerState.animateScrollToPage(nextPage) }
                            }
                        },
                )
            }
        }

        if (mediaUrls.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(mediaUrls.size) { index ->
                    val segmentProgress = when {
                        index < pagerState.currentPage -> 1f
                        index > pagerState.currentPage -> 0f
                        else -> pageProgress.value.coerceIn(0f, 1f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(segmentProgress)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${mediaUrls.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PostDetailScreen(
    post: ProfilePost,
    onBack: () -> Unit,
    onToggleLike: () -> Unit,
    onAddComment: (String) -> Unit,
) {
    var draftComment by remember { mutableStateOf("") }
    var isMuted by remember(post.id) { mutableStateOf(true) }

    PlatformBackHandler(
        enabled = true,
        onBack = onBack,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (post.mediaType == PostMediaType.VIDEO && !post.videoUrl.isNullOrBlank()) {
                PlatformVideoPlayer(
                    videoUrl = post.videoUrl,
                    isMuted = isMuted,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { isMuted = !isMuted },
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute video" else "Mute video",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post ${post.id}",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (post.isLikedByMe) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (post.isLikedByMe) "Unlike post" else "Like post",
                    tint = if (post.isLikedByMe) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                )
            }
        }

        Text(
            text = "${post.likes} likes",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = "${post.comments} comments",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp),
                ),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draftComment,
                    onValueChange = { draftComment = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (draftComment.isBlank()) {
                            Text(
                                text = "Add a comment...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )
                TextButton(
                    onClick = {
                        onAddComment(draftComment)
                        draftComment = ""
                    },
                ) {
                    Text("Post")
                }
            }
        }

        if (post.recentComments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Local comments",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            post.recentComments.forEach { comment ->
                Text(
                    text = "- $comment",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            Text(
                text = "No local comments yet.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
