package de.app.instagram.di

import de.app.instagram.network.NetworkModule
import de.app.instagram.network.createPlatformHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single<NetworkConfig> { ProductionNetworkConfig }
    single<HttpClientEngine> { createPlatformHttpClientEngine() }
    single<Json> { NetworkModule.createJson() }
    single<HttpClient> {
        val networkConfig: NetworkConfig = get()
        NetworkModule.createHttpClient(
            engine = get(),
            baseUrl = networkConfig.baseUrl,
            appClientHeader = networkConfig.appClientHeader,
            json = get(),
        )
    }
}

val appModules = listOf(networkModule)
