package de.app.instagram.di

import de.app.instagram.network.NetworkModule
import de.app.instagram.network.createPlatformHttpClientEngine
import de.app.instagram.profile.data.KtorProfileApi
import de.app.instagram.profile.data.ProfileApi
import de.app.instagram.profile.domain.DefaultProfileRepository
import de.app.instagram.profile.domain.GetProfileUseCase
import de.app.instagram.profile.domain.ProfileRepository
import de.app.instagram.profile.presentation.ProfileViewModel
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
    single<ProfileApi> { KtorProfileApi(get()) }
    single<ProfileRepository> { DefaultProfileRepository(get()) }
    factory { GetProfileUseCase(get()) }
    single { ProfileViewModel(get()) }
}

val appModules = listOf(networkModule)
