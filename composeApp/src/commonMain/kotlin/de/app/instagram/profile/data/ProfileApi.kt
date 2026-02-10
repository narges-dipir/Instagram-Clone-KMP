package de.app.instagram.profile.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ProfileApi {
    suspend fun getProfile(): ProfileDto
}

class KtorProfileApi(
    private val httpClient: HttpClient,
) : ProfileApi {
    override suspend fun getProfile(): ProfileDto {
        return httpClient.get("mock-api/v1/profile.json").body()
    }
}

@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
    val fullName: String,
    val bio: String,
    val isVerified: Boolean,
    val avatarUrl: String,
    val stats: ProfileStatsDto,
    val website: String,
    val storyHighlights: List<StoryHighlightDto>,
    val posts: List<ProfilePostDto>,
)

@Serializable
data class ProfileStatsDto(
    val posts: Int,
    val followers: Int,
    val following: Int,
)

@Serializable
data class StoryHighlightDto(
    val id: String,
    val title: String,
    val coverUrl: String,
)

@Serializable
data class ProfilePostDto(
    val id: String,
    val imageUrl: String,
    val likes: Int,
    val comments: Int,
)
