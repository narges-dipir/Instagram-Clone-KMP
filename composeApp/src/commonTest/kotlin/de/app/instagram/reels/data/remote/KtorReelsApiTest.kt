package de.app.instagram.reels.data.remote

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

class KtorReelsApiTest {

    @Test
    fun getPage_loadsAndParsesReelsPayload() = runTest {
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
                          "id": "r_001",
                          "videoUrl": "https://example.com/video.mp4",
                          "caption": "caption",
                          "username": "bonn.fit",
                          "avatarUrl": "https://example.com/avatar.jpg",
                          "likes": 100,
                          "comments": 10
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
        val api = KtorReelsApi(client)
        val page = api.getPage(1)

        assertEquals("/Instagram-Clone-KMP/mock-api/v1/reels/page-1.json", observedPath)
        assertEquals(1, page.page)
        assertEquals("r_001", page.items.first().id)
        assertEquals("bonn.fit", page.items.first().username)
    }
}
