package de.app.instagram.reels.data.repository

import de.app.instagram.reels.data.remote.ReelsApi
import de.app.instagram.reels.domain.model.ReelVideo
import de.app.instagram.reels.domain.model.ReelsPage
import de.app.instagram.reels.domain.repository.ReelsRepository

class DefaultReelsRepository(
    private val api: ReelsApi,
) : ReelsRepository {
    override suspend fun getPage(page: Int): ReelsPage {
        val dto = api.getPage(page)
        return ReelsPage(
            page = dto.page,
            hasNext = dto.hasNext,
            items = dto.items.map {
                ReelVideo(
                    id = it.id,
                    videoUrl = it.videoUrl,
                    caption = it.caption,
                )
            },
        )
    }
}
