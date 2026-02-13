package de.app.instagram.profile.data.local

interface PostInteractionStore {
    suspend fun readAll(): Map<String, LocalPostInteraction>
    suspend fun save(postId: String, interaction: LocalPostInteraction)
}

data class LocalPostInteraction(
    val isLikedByMe: Boolean = false,
    val comments: List<String> = emptyList(),
)

class InMemoryPostInteractionStore : PostInteractionStore {
    private val items = mutableMapOf<String, LocalPostInteraction>()

    override suspend fun readAll(): Map<String, LocalPostInteraction> = items.toMap()

    override suspend fun save(postId: String, interaction: LocalPostInteraction) {
        items[postId] = interaction
    }
}
