package de.app.instagram.profile.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryPostInteractionStore : PostInteractionStore {
    private val items = mutableMapOf<String, LocalPostInteraction>()
    private val state = MutableStateFlow<Map<String, LocalPostInteraction>>(emptyMap())

    override fun observeAll(): Flow<Map<String, LocalPostInteraction>> = state

    override suspend fun readAll(): Map<String, LocalPostInteraction> = items.toMap()

    override suspend fun save(postId: String, interaction: LocalPostInteraction) {
        items[postId] = interaction
        state.value = items.toMap()
    }
}
