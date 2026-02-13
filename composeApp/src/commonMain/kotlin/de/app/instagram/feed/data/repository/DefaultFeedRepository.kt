package de.app.instagram.feed.data.repository

import de.app.instagram.feed.data.remote.FeedApi
import de.app.instagram.feed.domain.model.FeedMediaType
import de.app.instagram.feed.domain.model.FeedPost
import de.app.instagram.feed.domain.model.FeedPostsPage
import de.app.instagram.feed.domain.repository.FeedRepository

class DefaultFeedRepository(
    private val api: FeedApi,
) : FeedRepository {
    override suspend fun getPage(page: Int): FeedPostsPage {
        val dto = api.getPage(page)
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
}
