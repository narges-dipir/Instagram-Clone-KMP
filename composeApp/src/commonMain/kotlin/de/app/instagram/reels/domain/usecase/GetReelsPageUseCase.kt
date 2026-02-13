package de.app.instagram.reels.domain.usecase

import de.app.instagram.reels.domain.model.ReelsPage
import de.app.instagram.reels.domain.repository.ReelsRepository

class GetReelsPageUseCase(
    private val repository: ReelsRepository,
) {
    suspend operator fun invoke(page: Int): ReelsPage = repository.getPage(page)
}
