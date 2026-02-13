package de.app.instagram.reels.data.local

interface ReelInteractionStore {
    suspend fun readAll(): Map<String, LocalReelInteraction>
    suspend fun save(reelId: String, interaction: LocalReelInteraction)
}

data class LocalReelInteraction(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val isFollowingCreator: Boolean = false,
    val comments: List<String> = emptyList(),
    val localShares: Int = 0,
)

class InMemoryReelInteractionStore : ReelInteractionStore {
    private val items = mutableMapOf<String, LocalReelInteraction>()

    override suspend fun readAll(): Map<String, LocalReelInteraction> = items.toMap()

    override suspend fun save(reelId: String, interaction: LocalReelInteraction) {
        items[reelId] = interaction
    }
}
