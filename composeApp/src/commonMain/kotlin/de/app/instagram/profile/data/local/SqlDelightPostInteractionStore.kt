package de.app.instagram.profile.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import de.app.instagram.db.InstagramCacheDatabase
import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class SqlDelightPostInteractionStore(
    database: InstagramCacheDatabase,
    private val json: Json,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
) : PostInteractionStore {
    private val queries = database.interactionCacheQueries
    private val commentsSerializer = ListSerializer(String.serializer())

    override fun observeAll(): Flow<Map<String, LocalPostInteraction>> {
        return queries.selectAllPosts()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { rows ->
                rows.associate { row ->
                    row.post_id to LocalPostInteraction(
                        isLikedByMe = row.is_liked_by_me != 0L,
                        comments = json.decodeFromString(commentsSerializer, row.comments_json),
                    )
                }
            }
    }

    override suspend fun readAll(): Map<String, LocalPostInteraction> {
        return withContext(dispatchers.io) {
            queries.selectAllPosts().executeAsList().associate { row ->
                row.post_id to LocalPostInteraction(
                    isLikedByMe = row.is_liked_by_me != 0L,
                    comments = json.decodeFromString(commentsSerializer, row.comments_json),
                )
            }
        }
    }

    override suspend fun save(postId: String, interaction: LocalPostInteraction) {
        withContext(dispatchers.io) {
            queries.upsertPost(
                post_id = postId,
                is_liked_by_me = if (interaction.isLikedByMe) 1L else 0L,
                comments_json = json.encodeToString(commentsSerializer, interaction.comments),
            )
        }
    }
}
