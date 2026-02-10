package de.app.instagram.di

import de.app.instagram.network.NetworkModule
import de.app.instagram.network.createPlatformHttpClientEngine
import de.app.instagram.profile.data.remote.KtorProfileApi
import de.app.instagram.profile.data.remote.ProfileApi
import de.app.instagram.profile.data.repository.DefaultProfileRepository
import de.app.instagram.profile.domain.repository.ProfileRepository
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import de.app.instagram.profile.presentation.viewmodel.ProfileViewModel
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
