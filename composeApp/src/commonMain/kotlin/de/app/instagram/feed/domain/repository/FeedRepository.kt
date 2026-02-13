package de.app.instagram.feed.domain.repository

import de.app.instagram.feed.domain.model.FeedPostsPage

interface FeedRepository {
    suspend fun getPage(page: Int): FeedPostsPage
}
