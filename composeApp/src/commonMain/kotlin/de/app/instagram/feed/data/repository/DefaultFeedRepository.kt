package de.app.instagram.feed.data.repository

import de.app.instagram.db.RemoteContentCache
import de.app.instagram.feed.data.remote.FeedApi
import de.app.instagram.feed.data.remote.FeedPostsPageDto
import de.app.instagram.feed.domain.model.FeedMediaType
import de.app.instagram.feed.domain.model.FeedPost
import de.app.instagram.feed.domain.model.FeedPostsPage
import de.app.instagram.feed.domain.repository.FeedRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class DefaultFeedRepository(
    private val api: FeedApi,
    private val remoteContentCache: RemoteContentCache,
    private val json: Json,
) : FeedRepository {
    override suspend fun getPage(page: Int): FeedPostsPage {
        val cacheKey = cacheKey(page)
        val networkFailure = runCatching {
            val remoteDto = api.getPage(page)
            remoteContentCache.write(cacheKey, json.encodeToString(FeedPostsPageDto.serializer(), remoteDto))
        }.exceptionOrNull()

        val cachedDto = remoteContentCache.observe(cacheKey).first()?.let { payload ->
            json.decodeFromString(FeedPostsPageDto.serializer(), payload)
        }
        val dto = cachedDto ?: throw (networkFailure ?: IllegalStateException("Feed cache is empty for page=$page"))
        return FeedPostsPage(
            page = dto.page,
            hasNext = dto.hasNext,
            items = dto.items.map {
                FeedPost(
                    id = it.id,
                    username = it.username,
                    avatarUrl = it.avatarUrl,
                    imageUrl = it.imageUrl,
                    mediaType = if (it.mediaType.equals("video", ignoreCase = true)) {
                        FeedMediaType.VIDEO
                    } else {
                        FeedMediaType.IMAGE
                    },
                    videoUrl = it.videoUrl,
                    likes = it.likes,
                    comments = it.comments,
                    caption = it.caption,
                )
            },
        )
    }

    private fun cacheKey(page: Int): String = "content_cache.feed.page.$page"
}
