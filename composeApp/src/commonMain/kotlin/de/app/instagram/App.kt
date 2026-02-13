package de.app.instagram

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.app.instagram.create.presentation.ui.CreateScreen
import de.app.instagram.di.appModules
import de.app.instagram.feed.domain.model.FeedMediaType
import de.app.instagram.feed.domain.model.FeedPost
import de.app.instagram.feed.presentation.state.FeedUiState
import de.app.instagram.feed.presentation.viewmodel.FeedViewModel
import de.app.instagram.profile.presentation.state.ProfileUiState
import de.app.instagram.profile.presentation.ui.ProfileScreen
import de.app.instagram.profile.presentation.viewmodel.ProfileViewModel
import de.app.instagram.reels.presentation.state.ReelsUiState
import de.app.instagram.reels.presentation.viewmodel.ReelsViewModel
import de.app.instagram.ui.PlatformBackHandler
import de.app.instagram.ui.PlatformVideoPlayer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

private const val HOME_STORIES_COUNT = 10
private val HOME_TOP_BAR_HEIGHT = 56.dp
private const val HOME_MIN_STORIES = 12
private const val HOME_STORY_DURATION_MS = 2800L

@Composable
@Preview
fun App() {
    KoinApplication(application = { modules(appModules) }) {
        MaterialTheme {
            val viewModel: ProfileViewModel = koinInject()
            val uiState by viewModel.uiState.collectAsState()
            val reelsViewModel: ReelsViewModel = koinInject()
            val reelsUiState by reelsViewModel.uiState.collectAsState()
            val feedViewModel: FeedViewModel = koinInject()
            val feedUiState by feedViewModel.uiState.collectAsState()
            var selectedTab by remember { mutableStateOf(BottomTab.Profile) }
            var showBottomBar by remember { mutableStateOf(true) }
            LaunchedEffect(selectedTab) {
                if (selectedTab != BottomTab.Home) {
                    showBottomBar = true
                }
            }

            PlatformBackHandler(
                enabled = selectedTab == BottomTab.Reels,
                onBack = { selectedTab = BottomTab.Home },
            )

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar && selectedTab != BottomTab.Reels,
                        enter = slideInVertically(
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = LinearOutSlowInEasing,
                            ),
                            initialOffsetY = { it / 2 },
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = LinearOutSlowInEasing,
                            )
                        ),
                        exit = slideOutVertically(
                            animationSpec = tween(
                                durationMillis = 170,
                                easing = FastOutLinearInEasing,
                            ),
                            targetOffsetY = { it / 2 },
                        ) + fadeOut(
                            animationSpec = tween(
                                durationMillis = 130,
                                easing = FastOutLinearInEasing,
                            )
                        ),
                    ) {
                        NavigationBar {
                            BottomTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                when (selectedTab) {
                    BottomTab.Home -> HomeTabContent(
                        uiState = feedUiState,
                        onLoadNextPage = feedViewModel::loadNextPage,
                        onToggleLike = feedViewModel::toggleLike,
                        onAddComment = feedViewModel::addComment,
                        onToggleSave = feedViewModel::toggleSave,
                        onBottomBarVisibilityChange = { visible -> showBottomBar = visible },
                        modifier = Modifier.padding(innerPadding),
                    )

                    BottomTab.Search -> SearchTabContent(
                        uiState = uiState,
                        modifier = Modifier.padding(innerPadding),
                    )

                    BottomTab.Reels -> ReelsTabContent(
                        uiState = reelsUiState,
                        onLoadNextPage = reelsViewModel::loadNextPage,
                        onToggleLike = reelsViewModel::toggleLike,
                        onAddComment = reelsViewModel::addComment,
                        onShare = reelsViewModel::share,
                        onToggleSave = reelsViewModel::toggleSave,
                        onToggleFollow = reelsViewModel::toggleFollow,
                        modifier = Modifier.padding(innerPadding),
                    )

                    BottomTab.Profile -> Box(modifier = Modifier.padding(innerPadding)) {
                        ProfileScreen(
                            uiState = uiState,
                            onRetryClick = viewModel::loadProfile,
                            onStartEditing = viewModel::startEditing,
                            onCancelEditing = viewModel::cancelEditing,
                            onSaveProfile = viewModel::saveProfileChanges,
                            onUsernameChange = viewModel::updateUsername,
                            onFullNameChange = viewModel::updateFullName,
                            onBioChange = viewModel::updateBio,
                            onWebsiteChange = viewModel::updateWebsite,
                            onPostClick = viewModel::openPost,
                            onBackFromPost = viewModel::closePost,
                            onHighlightClick = viewModel::openHighlight,
                            onBackFromHighlight = viewModel::closeHighlight,
                            onToggleLikeClick = viewModel::toggleLikeForSelectedPost,
                            onAddCommentClick = viewModel::addCommentToSelectedPost,
                            onLoadMorePosts = viewModel::loadMorePosts,
                        )
                    }

                    BottomTab.Create -> CreateScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReelsTabContent(
    uiState: ReelsUiState,
    onLoadNextPage: () -> Unit,
    onToggleLike: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onShare: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reels = uiState.items
    var commentTargetReelId by remember { mutableStateOf<String?>(null) }
    var commentDraft by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { reels.size },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.scrim),
    ) {
        if (uiState.isInitialLoading && reels.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onPrimary,
            )
            return
        }
        if (reels.isEmpty()) {
            FeaturePlaceholder(
                title = "Reels",
                subtitle = uiState.errorMessage ?: "No reels available",
                modifier = Modifier.fillMaxSize(),
            )
            return
        }

        LaunchedEffect(pagerState.currentPage, reels.size) {
            if (pagerState.currentPage >= reels.lastIndex - 1) {
                onLoadNextPage()
            }
        }

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val reel = reels[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim),
            ) {
                PlatformVideoPlayer(
                    videoUrl = reel.videoUrl,
                    isMuted = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                )
                Text(
                    text = reel.caption,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, end = 92.dp, bottom = 24.dp),
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, end = 92.dp, bottom = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = reel.avatarUrl,
                        contentDescription = "Creator avatar",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = reel.username,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    TextButton(onClick = { onToggleFollow(reel.id) }) {
                        Text(
                            text = if (reel.isFollowingCreator) "Following" else "Follow",
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ReelAction(
                        icon = if (reel.isLikedByMe) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        label = formatCompactCount(reel.likes),
                        contentDescription = "Like",
                        tint = if (reel.isLikedByMe) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        onClick = { onToggleLike(reel.id) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReelAction(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        label = formatCompactCount(reel.comments),
                        contentDescription = "Comment",
                        onClick = {
                            commentTargetReelId = reel.id
                            commentDraft = ""
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReelAction(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        label = if (reel.shares == 0) "Share" else formatCompactCount(reel.shares),
                        contentDescription = "Share",
                        onClick = { onShare(reel.id) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReelAction(
                        icon = if (reel.isSavedByMe) {
                            Icons.Filled.Bookmark
                        } else {
                            Icons.Outlined.BookmarkBorder
                        },
                        label = if (reel.isSavedByMe) "Saved" else "Save",
                        contentDescription = "Save",
                        onClick = { onToggleSave(reel.id) },
                    )
                }
            }
        }

        val commentReel = reels.firstOrNull { it.id == commentTargetReelId }
        if (commentReel != null) {
            ModalBottomSheet(
                onDismissRequest = { commentTargetReelId = null },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (commentReel.recentComments.isEmpty()) {
                        Text(
                            text = "No local comments yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(180.dp),
                        ) {
                            items(commentReel.recentComments) { comment ->
                                Text(
                                    text = comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text("Add a comment...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        TextButton(onClick = { commentTargetReelId = null }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                onAddComment(commentReel.id, commentDraft)
                                commentDraft = ""
                                commentTargetReelId = null
                            },
                        ) {
                            Text("Post")
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompactCount(value: Int): String {
    if (value >= 1000) {
        val thousands = value / 1000
        val remainderHundreds = (value % 1000) / 100
        return if (thousands >= 10 || remainderHundreds == 0) {
            "${thousands}k"
        } else {
            "${thousands}.${remainderHundreds}k"
        }
    }
    return value.toString()
}

@Composable
private fun ReelAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun HomeTabContent(
    uiState: FeedUiState,
    onLoadNextPage: () -> Unit,
    onToggleLike: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val posts = uiState.items
    val stories = remember(posts) { buildHomeStories(posts) }
    var previousScrollPosition by remember { mutableStateOf(0) }
    var selectedStoryIndex by remember { mutableStateOf<Int?>(null) }

    if (uiState.isInitialLoading && posts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    if (posts.isEmpty()) {
        FeaturePlaceholder(
            title = "Home",
            subtitle = uiState.errorMessage ?: "No posts available",
            modifier = modifier,
        )
        return
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadNextPage() }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            // Flatten index+offset into a monotonically increasing value.
            (listState.firstVisibleItemIndex * 100_000) + listState.firstVisibleItemScrollOffset
        }.collect { current ->
            val delta = current - previousScrollPosition
            previousScrollPosition = current

            if (delta > 12) {
                onBottomBarVisibilityChange(false)
            } else if (delta < -12) {
                onBottomBarVisibilityChange(true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = HOME_TOP_BAR_HEIGHT),
        ) {
            item {
                HomeStoriesRow(
                    stories = stories,
                    onStoryClick = { index -> selectedStoryIndex = index },
                )
            }
            itemsIndexed(
                items = posts,
                key = { _, post -> post.id },
            ) { _, post ->
                FeedPostCard(
                    post = post,
                    onToggleLike = onToggleLike,
                    onAddComment = onAddComment,
                    onToggleSave = onToggleSave,
                )
            }
            item {
                if (uiState.isLoadingMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
        HomeTopBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 6.dp),
        )

        val storyIndex = selectedStoryIndex
        if (storyIndex != null && stories.isNotEmpty()) {
            val safeIndex = storyIndex.coerceIn(0, stories.lastIndex)
            val story = stories[safeIndex]
            val progress = remember(safeIndex) { Animatable(0f) }

            LaunchedEffect(safeIndex, stories.size) {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = HOME_STORY_DURATION_MS.toInt(),
                        easing = LinearOutSlowInEasing,
                    ),
                )
                if (safeIndex < stories.lastIndex) {
                    selectedStoryIndex = safeIndex + 1
                } else {
                    selectedStoryIndex = null
                }
            }

            PlatformBackHandler(
                enabled = true,
                onBack = { selectedStoryIndex = null },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
            ) {
                AnimatedContent(
                    targetState = safeIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(
                                animationSpec = tween(260, easing = LinearOutSlowInEasing),
                                initialOffsetX = { it },
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(220, easing = FastOutLinearInEasing),
                                targetOffsetX = { -it },
                            )
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(260, easing = LinearOutSlowInEasing),
                                initialOffsetX = { -it },
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(220, easing = FastOutLinearInEasing),
                                targetOffsetX = { it },
                            )
                        }
                    },
                    label = "HomeStoryTransition",
                ) { index ->
                    AsyncImage(
                        model = stories[index].avatarUrl,
                        contentDescription = "${stories[index].username} story",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
                ) {
                    repeat(stories.size) { index ->
                        val segmentProgress = when {
                            index < safeIndex -> 1f
                            index > safeIndex -> 0f
                            else -> progress.value.coerceIn(0f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                                ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segmentProgress)
                                    .background(MaterialTheme.colorScheme.onPrimary),
                            )
                        }
                    }
                }
                Text(
                    text = if (story.isYourStory) "your_story" else story.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 22.dp, start = 16.dp, end = 16.dp),
                )

                Row(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                selectedStoryIndex = if (safeIndex > 0) safeIndex - 1 else 0
                            },
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                selectedStoryIndex = if (safeIndex < stories.lastIndex) {
                                    safeIndex + 1
                                } else {
                                    null
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(modifier: Modifier = Modifier) {
    var likesNotifications by remember { mutableStateOf(7) }
    var messageNotifications by remember { mutableStateOf(3) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Instagram",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        ) {
            TopBarActionIcon(
                icon = Icons.Outlined.FavoriteBorder,
                contentDescription = "Notifications",
                badgeCount = likesNotifications,
                onClick = {
                    likesNotifications = (likesNotifications - 1).coerceAtLeast(0)
                },
            )
            TopBarActionIcon(
                icon = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Messages",
                badgeCount = messageNotifications,
                onClick = {
                    messageNotifications = (messageNotifications - 1).coerceAtLeast(0)
                },
            )
        }
    }
}

@Composable
private fun TopBarActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    Box {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun HomeStoriesRow(
    stories: List<HomeStoryItem>,
    onStoryClick: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .padding(bottom = 10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(stories, key = { _, item -> item.id }) { index, story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onStoryClick(index) },
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(
                            if (story.isYourStory) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            },
                            CircleShape,
                        )
                        .let { base ->
                            if (story.isYourStory) base else {
                                base.background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFFFEDA75),
                                            androidx.compose.ui.graphics.Color(0xFFFA7E1E),
                                            androidx.compose.ui.graphics.Color(0xFFD62976),
                                        )
                                    ),
                                    shape = CircleShape,
                                )
                            }
                        }
                        .padding(2.dp)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = story.avatarUrl,
                        contentDescription = story.username,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )

                    if (story.isYourStory) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    text = if (story.isYourStory) "Your story" else story.username,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}

private data class HomeStoryItem(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val isYourStory: Boolean = false,
)

private fun buildHomeStories(posts: List<FeedPost>): List<HomeStoryItem> {
    val stories = posts
        .distinctBy { it.username }
        .map { HomeStoryItem(id = it.id, username = it.username, avatarUrl = it.avatarUrl) }
        .toMutableList()

    if (stories.size < HOME_MIN_STORIES - 1) {
        val seedAvatar = stories.firstOrNull()?.avatarUrl
            ?: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=240&q=80"
        val needed = (HOME_MIN_STORIES - 1) - stories.size
        repeat(needed) { index ->
            stories += HomeStoryItem(
                id = "generated_story_$index",
                username = "user_${index + 1}",
                avatarUrl = seedAvatar,
            )
        }
    }

    val yourStory = HomeStoryItem(
        id = "your_story",
        username = "your_story",
        avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=900&q=80",
        isYourStory = true,
    )

    return listOf(yourStory) + stories.take(maxOf(HOME_STORIES_COUNT, HOME_MIN_STORIES) - 1)
}

@Composable
private fun FeedPostCard(
    post: FeedPost,
    onToggleLike: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onToggleSave: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.avatarUrl,
                    contentDescription = post.username,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = post.username,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
            )
        }
        if (post.mediaType == FeedMediaType.VIDEO && !post.videoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Video preview ${post.id}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = "Video post",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                            shape = CircleShape,
                        )
                        .padding(6.dp)
                        .size(16.dp),
                )
            }
        } else {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = "Post ${post.id}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = if (post.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (post.isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onToggleLike(post.id) },
                )
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onAddComment(post.id) },
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Share",
                    modifier = Modifier.size(24.dp),
                )
            }
            Icon(
                imageVector = if (post.isSavedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Save",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggleSave(post.id) },
            )
        }
        Text(
            text = "${post.likes.coerceAtLeast(0)} likes",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        Text(
            text = "${post.username}  ${post.caption}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        Text(
            text = "View all ${post.comments} comments",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        Text(
            text = "2 hours ago",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun SearchTabContent(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    when (uiState) {
        ProfileUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is ProfileUiState.Error -> FeaturePlaceholder(
            title = "Search",
            subtitle = uiState.message,
            modifier = modifier,
        )

        is ProfileUiState.Success -> {
            val posts = if (query.isBlank()) {
                uiState.profile.posts
            } else {
                uiState.profile.posts.filter { post ->
                    post.id.contains(query.trim(), ignoreCase = true)
                }
            }
            Column(modifier = modifier.fillMaxSize()) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by post id") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (posts.isEmpty()) {
                    FeaturePlaceholder(
                        title = "No Results",
                        subtitle = "Try p_001, p_002 ...",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(posts) { post ->
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = "Search ${post.id}",
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePlaceholder(
    title: String,
    subtitle: String = "Coming soon",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class BottomTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Home("Home", Icons.Filled.Home),
    Search("Search", Icons.Filled.Search),
    Create("Create", Icons.Filled.AddBox),
    Reels("Reels", Icons.Filled.Movie),
    Profile("Profile", Icons.Filled.Person),
}
