package de.app.instagram.reels.domain.repository

import de.app.instagram.reels.domain.model.ReelsPage

interface ReelsRepository {
    suspend fun getPage(page: Int): ReelsPage
}
