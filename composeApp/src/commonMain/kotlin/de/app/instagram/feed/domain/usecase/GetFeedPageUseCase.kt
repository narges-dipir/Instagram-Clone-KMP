package de.app.instagram.feed.domain.usecase

import de.app.instagram.feed.domain.model.FeedPostsPage
import de.app.instagram.feed.domain.repository.FeedRepository

class GetFeedPageUseCase(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke(page: Int): FeedPostsPage = repository.getPage(page)
}
