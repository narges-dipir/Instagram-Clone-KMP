package de.app.instagram.di

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class NetworkModuleKoinTest {

    @Test
    fun koin_resolvesHttpClient_withInjectedBaseUrlAndHeader() = runTest {
        var observedPath: String? = null
        var observedHeader: String? = null

        val mockEngine = MockEngine { request ->
            observedPath = request.url.encodedPath
            observedHeader = request.headers["X-App-Client"]
            respond(
                content = ByteReadChannel("""{"value":"ok"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val testOverrides = module {
            single<HttpClientEngine> { mockEngine }
            single<NetworkConfig> {
                DefaultNetworkConfig(
                    baseUrl = "https://example.com/api/",
                    appClientHeader = "TestClient",
                )
            }
        }

        val app = startKoin { modules(appModules + testOverrides) }
        try {
            val client: HttpClient = app.koin.get()
            val payload = client.get("profile").body<SimplePayload>()

            assertEquals("/api/profile", observedPath)
            assertEquals("TestClient", observedHeader)
            assertEquals("ok", payload.value)
        } finally {
            stopKoin()
        }
    }
}

@Serializable
private data class SimplePayload(val value: String)
