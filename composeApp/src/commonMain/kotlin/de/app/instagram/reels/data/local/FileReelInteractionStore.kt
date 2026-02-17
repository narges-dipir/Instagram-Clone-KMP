package de.app.instagram.reels.data.local

import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import de.app.instagram.di.createDefaultAppScope
import de.app.instagram.storage.reelInteractionsStoragePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileReelInteractionStore(
    private val json: Json,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val filePath: Path = reelInteractionsStoragePath().toPath(),
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
    private val scope: CoroutineScope = createDefaultAppScope(dispatchers),
) : ReelInteractionStore {
    private val mutex = Mutex()
    private var flushJob: Job? = null
    private var cacheLoaded = false
    private val cache = mutableMapOf<String, LocalReelInteraction>()
    private val state = MutableStateFlow<Map<String, LocalReelInteraction>>(emptyMap())

    override fun observeAll(): Flow<Map<String, LocalReelInteraction>> = state

    override suspend fun readAll(): Map<String, LocalReelInteraction> = mutex.withLock {
        ensureLoadedLocked()
        cache.toMap()
    }

    override suspend fun save(reelId: String, interaction: LocalReelInteraction) {
        mutex.withLock {
            ensureLoadedLocked()
            cache[reelId] = interaction
            state.value = cache.toMap()
            scheduleFlushLocked()
        }
    }

    private suspend fun ensureLoadedLocked() {
        if (cacheLoaded) return
        cache.clear()
        cache.putAll(readAllInternal())
        cacheLoaded = true
        state.value = cache.toMap()
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

    private suspend fun readAllInternal(): Map<String, LocalReelInteraction> = withContext(dispatchers.io) {
        if (!fileSystem.exists(filePath)) return@withContext emptyMap()
        val raw = fileSystem.read(filePath) { readUtf8() }
        if (raw.isBlank()) return@withContext emptyMap()

        runCatching {
            val payload = json.decodeFromString<LocalReelInteractionsPayload>(raw)
            payload.items.mapValues { (_, value) ->
                LocalReelInteraction(
                    isLikedByMe = value.isLikedByMe,
                    isSavedByMe = value.isSavedByMe,
                    isFollowingCreator = value.isFollowingCreator,
                    comments = value.comments,
                    localShares = value.localShares,
                )
            }
        }.getOrDefault(emptyMap())
    }

    private suspend fun writeAllInternal(data: Map<String, LocalReelInteraction>) {
        withContext(dispatchers.io) {
            filePath.parent?.let(fileSystem::createDirectories)
            val payload = LocalReelInteractionsPayload(
                items = data.mapValues { (_, value) ->
                    LocalReelInteractionDto(
                        isLikedByMe = value.isLikedByMe,
                        isSavedByMe = value.isSavedByMe,
                        isFollowingCreator = value.isFollowingCreator,
                        comments = value.comments,
                        localShares = value.localShares,
                    )
                },
            )
            val encoded = json.encodeToString(LocalReelInteractionsPayload.serializer(), payload)
            fileSystem.write(filePath) {
                writeUtf8(encoded)
            }
        }
    }
}

@Serializable
private data class LocalReelInteractionsPayload(
    val items: Map<String, LocalReelInteractionDto> = emptyMap(),
)

@Serializable
private data class LocalReelInteractionDto(
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false,
    val isFollowingCreator: Boolean = false,
    val comments: List<String> = emptyList(),
    val localShares: Int = 0,
)

private const val WRITE_DEBOUNCE_MS: Long = 750L
