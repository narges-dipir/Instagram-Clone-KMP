package de.app.instagram.feed.data.local

import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import de.app.instagram.di.createDefaultAppScope
import de.app.instagram.storage.feedInteractionsStoragePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileFeedInteractionStore(
    private val json: Json,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val filePath: Path = feedInteractionsStoragePath().toPath(),
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
    private val scope: CoroutineScope = createDefaultAppScope(dispatchers),
) : FeedInteractionStore {
    private val mutex = Mutex()
    private var flushJob: Job? = null
    private var cacheLoaded = false
    private val cache = mutableMapOf<String, LocalFeedInteraction>()

    override suspend fun readAll(): Map<String, LocalFeedInteraction> = mutex.withLock {
        ensureLoadedLocked()
        cache.toMap()
    }

    override suspend fun save(postId: String, interaction: LocalFeedInteraction) {
        mutex.withLock {
            ensureLoadedLocked()
            cache[postId] = interaction
            scheduleFlushLocked()
        }
    }

    private suspend fun ensureLoadedLocked() {
        if (cacheLoaded) return
        cache.clear()
        cache.putAll(readAllInternal())
        cacheLoaded = true
    }

    private fun scheduleFlushLocked() {
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(WRITE_DEBOUNCE_MS)
            flushToDisk()
        }
    }

    private suspend fun flushToDisk() {
        val snapshot = mutex.withLock {
            if (!cacheLoaded) return
            cache.toMap()
        }
        writeAllInternal(snapshot)
    }

    private suspend fun readAllInternal(): Map<String, LocalFeedInteraction> = withContext(dispatchers.io) {
        if (!fileSystem.exists(filePath)) return@withContext emptyMap()
        val raw = fileSystem.read(filePath) { readUtf8() }
        if (raw.isBlank()) return@withContext emptyMap()

        runCatching {
            val payload = json.decodeFromString<LocalFeedInteractionsPayload>(raw)
            payload.items.mapValues { (_, value) ->
                LocalFeedInteraction(
                    isLikedByMe = value.isLikedByMe,
                    isSavedByMe = value.isSavedByMe,
                    localComments = value.localComments,
                )
            }
        }.getOrDefault(emptyMap())
    }

    private suspend fun writeAllInternal(data: Map<String, LocalFeedInteraction>) {
        withContext(dispatchers.io) {
            filePath.parent?.let(fileSystem::createDirectories)
            val payload = LocalFeedInteractionsPayload(
                items = data.mapValues { (_, value) ->
                    LocalFeedInteractionDto(
                        isLikedByMe = value.isLikedByMe,
                        isSavedByMe = value.isSavedByMe,
                        localComments = value.localComments,
                    )
                },
            )
            val encoded = json.encodeToString(LocalFeedInteractionsPayload.serializer(), payload)
            fileSystem.write(filePath) {
                writeUtf8(encoded)
            }
        }
    }
}

@Serializable
private data class LocalFeedInteractionsPayload(
    val items: Map<String, LocalFeedInteractionDto> = emptyMap(),
)

@Serializable
private data class LocalFeedInteractionDto(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val localComments: Int = 0,
)

private const val WRITE_DEBOUNCE_MS: Long = 750L
