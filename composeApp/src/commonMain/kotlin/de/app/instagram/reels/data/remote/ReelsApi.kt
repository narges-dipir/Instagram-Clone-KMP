package de.app.instagram.reels.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

interface ReelsApi {
    suspend fun getPage(page: Int): ReelsPageDto
}

class KtorReelsApi(
    private val httpClient: HttpClient,
) : ReelsApi {
    override suspend fun getPage(page: Int): ReelsPageDto {
        return httpClient.get("mock-api/v1/reels/page-$page.json").body()
    }
}

@Serializable
data class ReelsPageDto(
    val page: Int,
    val hasNext: Boolean,
    val items: List<ReelVideoDto>,
)

@Serializable
data class ReelVideoDto(
    val id: String,
    val videoUrl: String,
    val caption: String,
    val username: String,
    val avatarUrl: String,
    val likes: Int,
    val comments: Int,
)
