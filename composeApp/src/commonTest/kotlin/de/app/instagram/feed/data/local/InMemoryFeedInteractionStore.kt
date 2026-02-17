package de.app.instagram.feed.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryFeedInteractionStore : FeedInteractionStore {
    private val items = mutableMapOf<String, LocalFeedInteraction>()
    private val state = MutableStateFlow<Map<String, LocalFeedInteraction>>(emptyMap())

    override fun observeAll(): Flow<Map<String, LocalFeedInteraction>> = state

    override suspend fun readAll(): Map<String, LocalFeedInteraction> = items.toMap()

    override suspend fun save(postId: String, interaction: LocalFeedInteraction) {
        items[postId] = interaction
        state.value = items.toMap()
    }
}
