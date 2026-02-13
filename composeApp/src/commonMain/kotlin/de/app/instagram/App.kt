package de.app.instagram

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import de.app.instagram.ui.PlatformVideoPlayer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

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

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
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
                },
            ) { innerPadding ->
                when (selectedTab) {
                    BottomTab.Home -> HomeTabContent(
                        uiState = feedUiState,
                        onLoadNextPage = feedViewModel::loadNextPage,
                        modifier = Modifier.padding(innerPadding),
                    )

                    BottomTab.Search -> SearchTabContent(
                        uiState = uiState,
                        modifier = Modifier.padding(innerPadding),
                    )

                    BottomTab.Reels -> ReelsTabContent(
                        uiState = reelsUiState,
                        onLoadNextPage = reelsViewModel::loadNextPage,
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

                    BottomTab.Create -> FeaturePlaceholder(
                        title = selectedTab.label,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelsTabContent(
    uiState: ReelsUiState,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reels = uiState.items
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
                        .padding(start = 14.dp, end = 14.dp, bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    uiState: FeedUiState,
    onLoadNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val posts = uiState.items

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

    LaunchedEffect(listState, posts.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { it >= posts.lastIndex - 2 }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadNextPage() }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        itemsIndexed(
            items = posts,
            key = { _, post -> post.id },
        ) { _, post ->
            FeedPostCard(post = post)
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
}

@Composable
private fun FeedPostCard(
    post: FeedPost,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = post.avatarUrl,
                contentDescription = post.username,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
            Text(
                text = post.username,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        if (post.mediaType == FeedMediaType.VIDEO && !post.videoUrl.isNullOrBlank()) {
            PlatformVideoPlayer(
                videoUrl = post.videoUrl,
                isMuted = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        } else {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = "Post ${post.id}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        }
        Text(
            text = "${post.likes} likes",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Text(
            text = post.caption,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        Text(
            text = "${post.comments} comments",
            style = MaterialTheme.typography.bodySmall,
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
