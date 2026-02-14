package de.app.instagram.reels.data.repository

import de.app.instagram.reels.data.remote.ReelsApi
import de.app.instagram.reels.domain.model.ReelVideo
import de.app.instagram.reels.domain.model.ReelsPage
import de.app.instagram.reels.domain.repository.ReelsRepository

class DefaultReelsRepository(
    private val api: ReelsApi,
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
        val dto = api.getPage(page)
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
}
