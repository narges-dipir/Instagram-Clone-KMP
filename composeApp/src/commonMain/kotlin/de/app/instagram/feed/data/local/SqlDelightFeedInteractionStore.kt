package de.app.instagram.feed.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import de.app.instagram.db.InstagramCacheDatabase
import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDelightFeedInteractionStore(
    database: InstagramCacheDatabase,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
) : FeedInteractionStore {
    private val queries = database.interactionCacheQueries

    override fun observeAll(): Flow<Map<String, LocalFeedInteraction>> {
        return queries.selectAllFeed()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { rows ->
                rows.associate { row ->
                    row.post_id to LocalFeedInteraction(
                        isLikedByMe = row.is_liked_by_me != 0L,
                        isSavedByMe = row.is_saved_by_me != 0L,
                        localComments = row.local_comments.toInt(),
                    )
                }
            }
    }

    override suspend fun readAll(): Map<String, LocalFeedInteraction> {
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
