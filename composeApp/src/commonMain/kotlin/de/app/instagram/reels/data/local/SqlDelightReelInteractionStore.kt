package de.app.instagram.reels.data.local

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

class SqlDelightReelInteractionStore(
    database: InstagramCacheDatabase,
    private val json: Json,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
) : ReelInteractionStore {
    private val queries = database.interactionCacheQueries
    private val commentsSerializer = ListSerializer(String.serializer())

    override fun observeAll(): Flow<Map<String, LocalReelInteraction>> {
        return queries.selectAllReels()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { rows ->
                rows.associate { row ->
                    row.reel_id to LocalReelInteraction(
                        isLikedByMe = row.is_liked_by_me != 0L,
                        isSavedByMe = row.is_saved_by_me != 0L,
                        isFollowingCreator = row.is_following_creator != 0L,
                        comments = json.decodeFromString(commentsSerializer, row.comments_json),
                        localShares = row.local_shares.toInt(),
                    )
                }
            }
    }

    override suspend fun readAll(): Map<String, LocalReelInteraction> {
        return withContext(dispatchers.io) {
            queries.selectAllReels().executeAsList().associate { row ->
                row.reel_id to LocalReelInteraction(
                    isLikedByMe = row.is_liked_by_me != 0L,
                    isSavedByMe = row.is_saved_by_me != 0L,
                    isFollowingCreator = row.is_following_creator != 0L,
                    comments = json.decodeFromString(commentsSerializer, row.comments_json),
                    localShares = row.local_shares.toInt(),
                )
            }
        }
    }

    override suspend fun save(reelId: String, interaction: LocalReelInteraction) {
        withContext(dispatchers.io) {
            queries.upsertReel(
                reel_id = reelId,
                is_liked_by_me = if (interaction.isLikedByMe) 1L else 0L,
                is_saved_by_me = if (interaction.isSavedByMe) 1L else 0L,
                is_following_creator = if (interaction.isFollowingCreator) 1L else 0L,
                comments_json = json.encodeToString(commentsSerializer, interaction.comments),
                local_shares = interaction.localShares.toLong(),
            )
        }
    }
}
