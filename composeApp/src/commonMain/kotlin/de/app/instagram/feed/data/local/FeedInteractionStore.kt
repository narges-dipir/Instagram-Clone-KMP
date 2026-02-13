package de.app.instagram.feed.data.local

interface FeedInteractionStore {
    suspend fun readAll(): Map<String, LocalFeedInteraction>
    suspend fun save(postId: String, interaction: LocalFeedInteraction)
}

data class LocalFeedInteraction(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val localComments: Int = 0,
)

class InMemoryFeedInteractionStore : FeedInteractionStore {
    private val items = mutableMapOf<String, LocalFeedInteraction>()

    override suspend fun readAll(): Map<String, LocalFeedInteraction> = items.toMap()

    override suspend fun save(postId: String, interaction: LocalFeedInteraction) {
        items[postId] = interaction
    }
}
