package de.app.instagram.profile.domain

interface ProfileRepository {
    suspend fun getProfile(): Profile
}
