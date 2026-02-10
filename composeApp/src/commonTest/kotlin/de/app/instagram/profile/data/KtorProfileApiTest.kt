package de.app.instagram.profile.data

import de.app.instagram.network.NetworkModule
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class KtorProfileApiTest {

    @Test
    fun getProfile_loadsAndParsesRemotePayload() = runTest {
        var observedPath: String? = null

        val engine = MockEngine { request ->
            observedPath = request.url.encodedPath
            respond(
                content = ByteReadChannel(
                    """
                    {
                      "id": "u_001",
                      "username": "narges",
                      "fullName": "Narges Dipir",
                      "bio": "Android and KMP developer",
                      "isVerified": false,
                      "avatarUrl": "https://example.com/avatar.jpg",
                      "stats": {
                        "posts": 24,
                        "followers": 1280,
                        "following": 312
                      },
                      "website": "https://github.com/nargesdipir",
                      "storyHighlights": [
                        {
                          "id": "h_001",
                          "title": "Travel",
                          "coverUrl": "https://example.com/h1.jpg"
                        }
                      ],
                      "posts": [
                        {
                          "id": "p_001",
                          "imageUrl": "https://example.com/p1.jpg",
                          "likes": 321,
                          "comments": 18
                        }
                      ]
                    }
                    """.trimIndent()
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = NetworkModule.createHttpClient(engine)
        val api = KtorProfileApi(client)
        val profile = api.getProfile()

        assertEquals("/Instagram-Clone-KMP/mock-api/v1/profile.json", observedPath)
        assertEquals("u_001", profile.id)
        assertEquals("narges", profile.username)
        assertEquals(24, profile.stats.posts)
        assertEquals(1, profile.storyHighlights.size)
        assertEquals(1, profile.posts.size)
        assertEquals("p_001", profile.posts.first().id)
    }
}
