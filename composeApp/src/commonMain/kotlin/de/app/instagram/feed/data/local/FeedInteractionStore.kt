package de.app.instagram.feed.data.local

import kotlinx.coroutines.flow.Flow

interface FeedInteractionStore {
    fun observeAll(): Flow<Map<String, LocalFeedInteraction>>
    suspend fun readAll(): Map<String, LocalFeedInteraction>
    suspend fun save(postId: String, interaction: LocalFeedInteraction)
}
