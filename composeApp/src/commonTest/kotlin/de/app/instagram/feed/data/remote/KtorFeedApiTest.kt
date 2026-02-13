package de.app.instagram.feed.data.remote

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

class KtorFeedApiTest {

    @Test
    fun getPage_loadsAndParsesFeedPayload() = runTest {
        var observedPath: String? = null
        val engine = MockEngine { request ->
            observedPath = request.url.encodedPath
            respond(
                content = ByteReadChannel(
                    """
                    {
                      "page": 1,
                      "hasNext": true,
                      "items": [
                        {
                          "id": "hp_001",
                          "username": "bonn.fit",
                          "avatarUrl": "https://example.com/a.jpg",
                          "imageUrl": "https://example.com/p.jpg",
                          "mediaType": "video",
                          "videoUrl": "https://example.com/v.mp4",
                          "likes": 100,
                          "comments": 3,
                          "caption": "hello"
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
        val api = KtorFeedApi(client)
        val page = api.getPage(1)

        assertEquals("/Instagram-Clone-KMP/mock-api/v1/posts/page-1.json", observedPath)
        assertEquals(1, page.page)
        assertEquals(true, page.hasNext)
        assertEquals("hp_001", page.items.first().id)
    }
}
