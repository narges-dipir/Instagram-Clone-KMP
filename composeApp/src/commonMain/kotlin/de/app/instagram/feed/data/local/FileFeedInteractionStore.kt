package de.app.instagram.feed.data.local

import de.app.instagram.storage.feedInteractionsStoragePath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileFeedInteractionStore(
    private val json: Json,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val filePath: Path = feedInteractionsStoragePath().toPath(),
) : FeedInteractionStore {
    private val mutex = Mutex()

    override suspend fun readAll(): Map<String, LocalFeedInteraction> = mutex.withLock {
        readAllInternal()
    }

    override suspend fun save(postId: String, interaction: LocalFeedInteraction) {
        mutex.withLock {
            val current = readAllInternal().toMutableMap()
            current[postId] = interaction
            writeAllInternal(current)
        }
    }

    private fun readAllInternal(): Map<String, LocalFeedInteraction> {
        if (!fileSystem.exists(filePath)) return emptyMap()
        val raw = fileSystem.read(filePath) { readUtf8() }
        if (raw.isBlank()) return emptyMap()

        return runCatching {
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

    private fun writeAllInternal(data: Map<String, LocalFeedInteraction>) {
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
