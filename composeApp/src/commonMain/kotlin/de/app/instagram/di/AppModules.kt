package de.app.instagram.di

import de.app.instagram.network.NetworkModule
import de.app.instagram.network.createPlatformHttpClientEngine
import de.app.instagram.feed.data.remote.FeedApi
import de.app.instagram.feed.data.remote.KtorFeedApi
import de.app.instagram.feed.data.repository.DefaultFeedRepository
import de.app.instagram.feed.data.local.FeedInteractionStore
import de.app.instagram.feed.data.local.FileFeedInteractionStore
import de.app.instagram.feed.domain.repository.FeedRepository
import de.app.instagram.feed.domain.usecase.GetFeedPageUseCase
import de.app.instagram.feed.presentation.viewmodel.FeedViewModel
import de.app.instagram.profile.data.local.FilePostInteractionStore
import de.app.instagram.profile.data.local.PostInteractionStore
import de.app.instagram.profile.data.remote.KtorProfileApi
import de.app.instagram.profile.data.remote.ProfileApi
import de.app.instagram.profile.data.repository.DefaultProfileRepository
import de.app.instagram.profile.domain.repository.ProfileRepository
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import de.app.instagram.profile.presentation.viewmodel.ProfileViewModel
import de.app.instagram.reels.data.remote.KtorReelsApi
import de.app.instagram.reels.data.remote.ReelsApi
import de.app.instagram.reels.data.repository.DefaultReelsRepository
import de.app.instagram.reels.data.local.FileReelInteractionStore
import de.app.instagram.reels.data.local.ReelInteractionStore
import de.app.instagram.reels.domain.repository.ReelsRepository
import de.app.instagram.reels.domain.usecase.GetReelsPageUseCase
import de.app.instagram.reels.presentation.viewmodel.ReelsViewModel
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
    single<ReelsApi> { KtorReelsApi(get()) }
    single<FeedApi> { KtorFeedApi(get()) }
    single<ProfileRepository> { DefaultProfileRepository(get()) }
    single<ReelsRepository> { DefaultReelsRepository(get()) }
    single<FeedRepository> { DefaultFeedRepository(get()) }
    single<PostInteractionStore> { FilePostInteractionStore(get()) }
    single<ReelInteractionStore> { FileReelInteractionStore(get()) }
    single<FeedInteractionStore> { FileFeedInteractionStore(get()) }
    factory { GetProfileUseCase(get()) }
    factory { GetReelsPageUseCase(get()) }
    factory { GetFeedPageUseCase(get()) }
    single { ProfileViewModel(get(), get(), get()) }
    single { ReelsViewModel(get(), get()) }
    single { FeedViewModel(get(), get()) }
}

val appModules = listOf(networkModule)
