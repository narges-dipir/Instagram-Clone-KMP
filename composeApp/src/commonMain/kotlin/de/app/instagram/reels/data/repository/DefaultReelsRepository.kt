package de.app.instagram.reels.data.repository

import de.app.instagram.db.RemoteContentCache
import de.app.instagram.reels.data.remote.ReelsApi
import de.app.instagram.reels.data.remote.ReelsPageDto
import de.app.instagram.reels.domain.model.ReelVideo
import de.app.instagram.reels.domain.model.ReelsPage
import de.app.instagram.reels.domain.repository.ReelsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class DefaultReelsRepository(
    private val api: ReelsApi,
    private val remoteContentCache: RemoteContentCache,
    private val json: Json,
) : ReelsRepository {
    private val fallbackUrls = listOf(
        "https://download.samplelib.com/mp4/sample-5s.mp4",
        "https://download.samplelib.com/mp4/sample-10s.mp4",
        "https://download.samplelib.com/mp4/sample-5s.mp4",
        "https://download.samplelib.com/mp4/sample-10s.mp4",
        "https://download.samplelib.com/mp4/sample-5s.mp4",
        "https://download.samplelib.com/mp4/sample-10s.mp4",
    )

    override suspend fun getPage(page: Int): ReelsPage {
        val cacheKey = cacheKey(page)
        val networkFailure = runCatching {
            val remoteDto = api.getPage(page)
            remoteContentCache.write(cacheKey, json.encodeToString(ReelsPageDto.serializer(), remoteDto))
        }.exceptionOrNull()

        val cachedDto = remoteContentCache.observe(cacheKey).first()?.let { payload ->
            json.decodeFromString(ReelsPageDto.serializer(), payload)
        }
        val dto = cachedDto ?: throw (networkFailure ?: IllegalStateException("Reels cache is empty for page=$page"))
        return ReelsPage(
            page = dto.page,
            hasNext = dto.hasNext,
            items = dto.items.map {
                ReelVideo(
                    id = it.id,
                    videoUrl = resolvePlayableVideoUrl(it.id, it.videoUrl),
                    caption = it.caption,
                    username = it.username,
                    avatarUrl = it.avatarUrl,
                    likes = it.likes,
                    comments = it.comments,
                )
            },
        )
    }

    private fun resolvePlayableVideoUrl(id: String, url: String): String {
        val normalized = url.trim()
        if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            return normalized
        }
        val index = (id.hashCode() and Int.MAX_VALUE) % fallbackUrls.size
        return fallbackUrls[index]
    }

    private fun cacheKey(page: Int): String = "content_cache.reels.page.$page"
}
