package de.app.instagram.reels.data.local

import kotlinx.coroutines.flow.Flow

interface ReelInteractionStore {
    fun observeAll(): Flow<Map<String, LocalReelInteraction>>
    suspend fun readAll(): Map<String, LocalReelInteraction>
    suspend fun save(reelId: String, interaction: LocalReelInteraction)
}
