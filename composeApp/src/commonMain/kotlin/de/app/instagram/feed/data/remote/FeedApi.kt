package de.app.instagram.feed.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

interface FeedApi {
    suspend fun getPage(page: Int): FeedPostsPageDto
}

class KtorFeedApi(
    private val httpClient: HttpClient,
) : FeedApi {
    override suspend fun getPage(page: Int): FeedPostsPageDto {
        return httpClient.get("mock-api/v1/posts/page-$page.json").body()
    }
}

@Serializable
data class FeedPostsPageDto(
    val page: Int,
    val hasNext: Boolean,
    val items: List<FeedPostDto>,
)

@Serializable
data class FeedPostDto(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val imageUrl: String,
    val mediaType: String,
    val videoUrl: String? = null,
    val likes: Int,
    val comments: Int,
    val caption: String,
)
