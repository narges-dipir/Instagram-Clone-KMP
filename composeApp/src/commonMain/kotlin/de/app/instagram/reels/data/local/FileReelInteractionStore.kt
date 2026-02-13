package de.app.instagram.reels.data.local

import de.app.instagram.storage.reelInteractionsStoragePath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FileReelInteractionStore(
    private val json: Json,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val filePath: Path = reelInteractionsStoragePath().toPath(),
) : ReelInteractionStore {
    private val mutex = Mutex()

    override suspend fun readAll(): Map<String, LocalReelInteraction> = mutex.withLock {
        readAllInternal()
    }

    override suspend fun save(reelId: String, interaction: LocalReelInteraction) {
        mutex.withLock {
            val current = readAllInternal().toMutableMap()
            current[reelId] = interaction
            writeAllInternal(current)
        }
    }

    private fun readAllInternal(): Map<String, LocalReelInteraction> {
        if (!fileSystem.exists(filePath)) return emptyMap()
        val raw = fileSystem.read(filePath) { readUtf8() }
        if (raw.isBlank()) return emptyMap()

        return runCatching {
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

    private fun writeAllInternal(data: Map<String, LocalReelInteraction>) {
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
