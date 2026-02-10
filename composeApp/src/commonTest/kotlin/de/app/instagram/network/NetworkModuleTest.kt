package de.app.instagram.network

import de.app.instagram.config.NetworkEnvironment
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NetworkModuleTest {

    @Test
    fun baseUrl_pointsToGithubPages() {
        assertEquals(
            "https://narges-dipir.github.io/Instagram-Clone-KMP/",
            NetworkEnvironment.BASE_URL_PRODUCTION
        )
    }

    @Test
    fun serializer_isConfiguredForRemotePayloadChanges() {
        val json = NetworkModule.createJson()

        assertTrue(json.configuration.ignoreUnknownKeys)
        assertTrue(json.configuration.isLenient)
    }

    @Test
    fun httpClient_usesBaseUrl_andAddsDefaultHeader() = runTest {
        var observedPath: String? = null
        var observedHeader: String? = null

        val engine = MockEngine { request ->
            observedPath = request.url.encodedPath
            observedHeader = request.headers["X-App-Client"]

            val body = """
                {
                  "id": "u_001",
                  "username": "narges",
                  "unknownFieldFromServer": "safe to ignore"
                }
            """.trimIndent()

            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = NetworkModule.createHttpClient(engine)
        val profile = client.get("mock-api/v1/profile.json").body<ProfileNetworkDto>()

        assertEquals("/Instagram-Clone-KMP/mock-api/v1/profile.json", observedPath)
        assertEquals("InstagramCloneKMP", observedHeader)
        assertEquals("u_001", profile.id)
        assertEquals("narges", profile.username)
    }
}

@Serializable
private data class ProfileNetworkDto(
    @SerialName("id")
    val id: String,
    @SerialName("username")
    val username: String
)
