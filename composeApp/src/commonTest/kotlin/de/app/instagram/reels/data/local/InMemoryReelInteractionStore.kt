package de.app.instagram.reels.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryReelInteractionStore : ReelInteractionStore {
    private val items = mutableMapOf<String, LocalReelInteraction>()
    private val state = MutableStateFlow<Map<String, LocalReelInteraction>>(emptyMap())

    override fun observeAll(): Flow<Map<String, LocalReelInteraction>> = state

    override suspend fun readAll(): Map<String, LocalReelInteraction> = items.toMap()

    override suspend fun save(reelId: String, interaction: LocalReelInteraction) {
        items[reelId] = interaction
        state.value = items.toMap()
    }
}
