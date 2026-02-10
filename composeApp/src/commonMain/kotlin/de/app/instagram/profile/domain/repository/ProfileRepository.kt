package de.app.instagram.profile.domain.repository

import de.app.instagram.profile.domain.model.Profile

interface ProfileRepository {
    suspend fun getProfile(): Profile
}
