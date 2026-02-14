package de.app.instagram.profile.data.local

import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import de.app.instagram.di.createDefaultAppScope
import de.app.instagram.storage.postInteractionsStoragePath
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

class FilePostInteractionStore(
    private val json: Json,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val filePath: Path = postInteractionsStoragePath().toPath(),
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
    private val scope: CoroutineScope = createDefaultAppScope(dispatchers),
) : PostInteractionStore {
    private val mutex = Mutex()
    private var flushJob: Job? = null
    private var cacheLoaded = false
    private val cache = mutableMapOf<String, LocalPostInteraction>()

    override suspend fun readAll(): Map<String, LocalPostInteraction> = mutex.withLock {
        ensureLoadedLocked()
        cache.toMap()
    }

    override suspend fun save(postId: String, interaction: LocalPostInteraction) {
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

    private suspend fun readAllInternal(): Map<String, LocalPostInteraction> = withContext(dispatchers.io) {
        if (!fileSystem.exists(filePath)) return@withContext emptyMap()
        val raw = fileSystem.read(filePath) { readUtf8() }
        if (raw.isBlank()) return@withContext emptyMap()

        runCatching {
            val payload = json.decodeFromString<LocalPostInteractionsPayload>(raw)
            payload.items.mapValues { (_, value) ->
                LocalPostInteraction(
                    isLikedByMe = value.isLikedByMe,
                    comments = value.comments,
                )
            }
        }.getOrDefault(emptyMap())
    }

    private suspend fun writeAllInternal(data: Map<String, LocalPostInteraction>) {
        withContext(dispatchers.io) {
            filePath.parent?.let(fileSystem::createDirectories)
            val payload = LocalPostInteractionsPayload(
                items = data.mapValues { (_, value) ->
                    LocalPostInteractionDto(
                        isLikedByMe = value.isLikedByMe,
                        comments = value.comments,
                    )
                }
            )
            val encoded = json.encodeToString(LocalPostInteractionsPayload.serializer(), payload)
            fileSystem.write(filePath) {
                writeUtf8(encoded)
            }
        }
    }
}

@Serializable
private data class LocalPostInteractionsPayload(
    val items: Map<String, LocalPostInteractionDto> = emptyMap(),
)

@Serializable
private data class LocalPostInteractionDto(
    val isLikedByMe: Boolean = false,
    val comments: List<String> = emptyList(),
)

private const val WRITE_DEBOUNCE_MS: Long = 750L
