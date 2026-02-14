package de.app.instagram.feed.data.local

import de.app.instagram.db.InstagramCacheDatabase
import de.app.instagram.db.LegacyJsonCacheMigrator
import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import kotlinx.coroutines.withContext

class SqlDelightFeedInteractionStore(
    database: InstagramCacheDatabase,
    private val migrator: LegacyJsonCacheMigrator,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
) : FeedInteractionStore {
    private val queries = database.interactionCacheQueries

    override suspend fun readAll(): Map<String, LocalFeedInteraction> {
        migrator.ensureMigrated()
        return withContext(dispatchers.io) {
            queries.selectAllFeed().executeAsList().associate { row ->
                row.post_id to LocalFeedInteraction(
                    isLikedByMe = row.is_liked_by_me != 0L,
                    isSavedByMe = row.is_saved_by_me != 0L,
                    localComments = row.local_comments.toInt(),
                )
            }
        }
    }

    override suspend fun save(postId: String, interaction: LocalFeedInteraction) {
        migrator.ensureMigrated()
        withContext(dispatchers.io) {
            queries.upsertFeed(
                post_id = postId,
                is_liked_by_me = if (interaction.isLikedByMe) 1L else 0L,
                is_saved_by_me = if (interaction.isSavedByMe) 1L else 0L,
                local_comments = interaction.localComments.toLong(),
            )
        }
    }
}
