package de.app.instagram.db

import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import de.app.instagram.storage.feedInteractionsStoragePath
import de.app.instagram.storage.postInteractionsStoragePath
import de.app.instagram.storage.reelInteractionsStoragePath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class LegacyJsonCacheMigrator(
    private val database: InstagramCacheDatabase,
    private val json: Json,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
    private val feedLegacyPath: Path = feedInteractionsStoragePath().toPath(),
    private val reelLegacyPath: Path = reelInteractionsStoragePath().toPath(),
    private val postLegacyPath: Path = postInteractionsStoragePath().toPath(),
) {
    private val mutex = Mutex()
    private val queries = database.interactionCacheQueries

    suspend fun ensureMigrated() {
        mutex.withLock {
            withContext(dispatchers.io) {
                if (queries.selectMetaValue(META_KEY).executeAsOneOrNull() == META_DONE) return@withContext

                database.transaction {
                    if (queries.countFeed().executeAsOne() == 0L) {
                        readLegacyFeed().forEach { (postId, item) ->
                            queries.upsertFeed(
                                post_id = postId,
                                is_liked_by_me = if (item.isLikedByMe) 1L else 0L,
                                is_saved_by_me = if (item.isSavedByMe) 1L else 0L,
                                local_comments = item.localComments.toLong(),
                            )
                        }
                    }

                    if (queries.countReels().executeAsOne() == 0L) {
                        readLegacyReels().forEach { (reelId, item) ->
                            queries.upsertReel(
                                reel_id = reelId,
                                is_liked_by_me = if (item.isLikedByMe) 1L else 0L,
                                is_saved_by_me = if (item.isSavedByMe) 1L else 0L,
                                is_following_creator = if (item.isFollowingCreator) 1L else 0L,
                                comments_json = json.encodeToString(stringListSerializer, item.comments),
                                local_shares = item.localShares.toLong(),
                            )
                        }
                    }

                    if (queries.countPosts().executeAsOne() == 0L) {
                        readLegacyPosts().forEach { (postId, item) ->
                            queries.upsertPost(
                                post_id = postId,
                                is_liked_by_me = if (item.isLikedByMe) 1L else 0L,
                                comments_json = json.encodeToString(stringListSerializer, item.comments),
                            )
                        }
                    }

                    queries.upsertMetaValue(
                        key = META_KEY,
                        value_ = META_DONE,
                    )
                }

                deleteLegacyFile(feedLegacyPath)
                deleteLegacyFile(reelLegacyPath)
                deleteLegacyFile(postLegacyPath)
            }
        }
    }

    private fun readLegacyFeed(): Map<String, LegacyFeedDto> {
        val raw = readFile(feedLegacyPath) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(LegacyFeedPayload.serializer(), raw).items
        }.getOrDefault(emptyMap())
    }

    private fun readLegacyReels(): Map<String, LegacyReelDto> {
        val raw = readFile(reelLegacyPath) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(LegacyReelPayload.serializer(), raw).items
        }.getOrDefault(emptyMap())
    }

    private fun readLegacyPosts(): Map<String, LegacyPostDto> {
        val raw = readFile(postLegacyPath) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(LegacyPostPayload.serializer(), raw).items
        }.getOrDefault(emptyMap())
    }

    private fun readFile(path: Path): String? {
        if (!fileSystem.exists(path)) return null
        val raw = runCatching { fileSystem.read(path) { readUtf8() } }.getOrNull() ?: return null
        return raw.takeIf { it.isNotBlank() }
    }

    private fun deleteLegacyFile(path: Path) {
        if (!fileSystem.exists(path)) return
        runCatching { fileSystem.delete(path) }
    }
}

@Serializable
private data class LegacyFeedPayload(
    val items: Map<String, LegacyFeedDto> = emptyMap(),
)

@Serializable
private data class LegacyFeedDto(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val localComments: Int = 0,
)

@Serializable
private data class LegacyReelPayload(
    val items: Map<String, LegacyReelDto> = emptyMap(),
)

@Serializable
private data class LegacyReelDto(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val isFollowingCreator: Boolean = false,
    val comments: List<String> = emptyList(),
    val localShares: Int = 0,
)

@Serializable
private data class LegacyPostPayload(
    val items: Map<String, LegacyPostDto> = emptyMap(),
)

@Serializable
private data class LegacyPostDto(
    val isLikedByMe: Boolean = false,
    val comments: List<String> = emptyList(),
)

private const val META_KEY = "legacy_json_migrated_v1"
private const val META_DONE = "1"
private val stringListSerializer = ListSerializer(String.serializer())
