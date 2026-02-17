package de.app.instagram.profile.data.local

import kotlinx.coroutines.flow.Flow

interface PostInteractionStore {
    fun observeAll(): Flow<Map<String, LocalPostInteraction>>
    suspend fun readAll(): Map<String, LocalPostInteraction>
    suspend fun save(postId: String, interaction: LocalPostInteraction)
}
