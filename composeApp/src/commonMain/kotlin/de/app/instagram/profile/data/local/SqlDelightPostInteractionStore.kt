package de.app.instagram.profile.data.local

import de.app.instagram.db.InstagramCacheDatabase
import de.app.instagram.db.LegacyJsonCacheMigrator
import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class SqlDelightPostInteractionStore(
    database: InstagramCacheDatabase,
    private val migrator: LegacyJsonCacheMigrator,
    private val json: Json,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
) : PostInteractionStore {
    private val queries = database.interactionCacheQueries
    private val commentsSerializer = ListSerializer(String.serializer())

    override suspend fun readAll(): Map<String, LocalPostInteraction> {
        migrator.ensureMigrated()
        return withContext(dispatchers.io) {
            queries.selectAllPosts().executeAsList().associate { row ->
                row.post_id to LocalPostInteraction(
                    isLikedByMe = row.is_liked_by_me != 0L,
                    comments = runCatching { json.decodeFromString(commentsSerializer, row.comments_json) }
                        .getOrDefault(emptyList()),
                )
            }
        }
    }

    override suspend fun save(postId: String, interaction: LocalPostInteraction) {
        migrator.ensureMigrated()
        withContext(dispatchers.io) {
            queries.upsertPost(
                post_id = postId,
                is_liked_by_me = if (interaction.isLikedByMe) 1L else 0L,
                comments_json = json.encodeToString(commentsSerializer, interaction.comments),
            )
        }
    }
}
