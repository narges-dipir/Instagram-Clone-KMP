package de.app.instagram.network

import de.app.instagram.config.NetworkEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkModule {
    const val APP_CLIENT_HEADER: String = "InstagramCloneKMP"

    fun createJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun createHttpClient(
        engine: HttpClientEngine,
        baseUrl: String = NetworkEnvironment.BASE_URL_PRODUCTION,
        appClientHeader: String = APP_CLIENT_HEADER,
        json: Json = createJson(),
    ): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                url(baseUrl)
                headers.append("X-App-Client", appClientHeader)
            }
        }
    }
}
