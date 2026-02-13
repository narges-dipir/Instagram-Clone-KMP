package de.app.instagram.profile.data.local

import de.app.instagram.storage.postInteractionsStoragePath
import kotlin.collections.toMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FilePostInteractionStore(
    private val json: Json,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val filePath: Path = postInteractionsStoragePath().toPath(),
) : PostInteractionStore {
    private val mutex = Mutex()

    override suspend fun readAll(): Map<String, LocalPostInteraction> = mutex.withLock {
        readAllInternal().toMap()
    }

    override suspend fun save(postId: String, interaction: LocalPostInteraction) {
        mutex.withLock {
            val current = readAllInternal().toMutableMap()
            current[postId] = interaction
            writeAllInternal(current)
        }
    }

    private fun readAllInternal(): Map<String, LocalPostInteraction> {
        if (!fileSystem.exists(filePath)) return emptyMap()
        val raw = fileSystem.read(filePath) { readUtf8() }
        if (raw.isBlank()) return emptyMap()

        return runCatching {
            val payload = json.decodeFromString<LocalPostInteractionsPayload>(raw)
            payload.items.mapValues { (_, value) ->
                LocalPostInteraction(
                    isLikedByMe = value.isLikedByMe,
                    comments = value.comments,
                )
            }
        }.getOrDefault(emptyMap())
    }

    private fun writeAllInternal(data: Map<String, LocalPostInteraction>) {
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

@Serializable
private data class LocalPostInteractionsPayload(
    val items: Map<String, LocalPostInteractionDto> = emptyMap(),
)

@Serializable
private data class LocalPostInteractionDto(
    val isLikedByMe: Boolean = false,
    val comments: List<String> = emptyList(),
)
