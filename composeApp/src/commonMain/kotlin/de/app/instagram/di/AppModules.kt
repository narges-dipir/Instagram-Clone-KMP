package de.app.instagram.di

import de.app.instagram.db.InstagramCacheDatabase
import de.app.instagram.db.LegacyJsonCacheMigrator
import de.app.instagram.db.createDatabaseDriver
import de.app.instagram.network.NetworkModule
import de.app.instagram.network.createPlatformHttpClientEngine
import de.app.instagram.feed.data.remote.FeedApi
import de.app.instagram.feed.data.remote.KtorFeedApi
import de.app.instagram.feed.data.repository.DefaultFeedRepository
import de.app.instagram.feed.data.local.FeedInteractionStore
import de.app.instagram.feed.data.local.SqlDelightFeedInteractionStore
import de.app.instagram.feed.domain.repository.FeedRepository
import de.app.instagram.feed.domain.usecase.GetFeedPageUseCase
import de.app.instagram.feed.presentation.viewmodel.FeedViewModel
import de.app.instagram.profile.data.local.PostInteractionStore
import de.app.instagram.profile.data.local.SqlDelightPostInteractionStore
import de.app.instagram.profile.data.remote.KtorProfileApi
import de.app.instagram.profile.data.remote.ProfileApi
import de.app.instagram.profile.data.repository.DefaultProfileRepository
import de.app.instagram.profile.domain.repository.ProfileRepository
import de.app.instagram.profile.domain.usecase.GetProfileUseCase
import de.app.instagram.profile.presentation.viewmodel.ProfileViewModel
import de.app.instagram.reels.data.remote.KtorReelsApi
import de.app.instagram.reels.data.remote.ReelsApi
import de.app.instagram.reels.data.repository.DefaultReelsRepository
import de.app.instagram.reels.data.local.ReelInteractionStore
import de.app.instagram.reels.data.local.SqlDelightReelInteractionStore
import de.app.instagram.reels.domain.repository.ReelsRepository
import de.app.instagram.reels.domain.usecase.GetReelsPageUseCase
import de.app.instagram.reels.presentation.viewmodel.ReelsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val APP_SCOPE_QUALIFIER = "app_scope"

val networkModule = module {
    single<CoroutineDispatchers> { DefaultCoroutineDispatchers }
    single(named(APP_SCOPE_QUALIFIER)) { createDefaultAppScope(get()) }
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
    single { InstagramCacheDatabase(createDatabaseDriver()) }
    single { LegacyJsonCacheMigrator(database = get(), json = get(), dispatchers = get()) }
    single<ProfileApi> { KtorProfileApi(get()) }
    single<ReelsApi> { KtorReelsApi(get()) }
    single<FeedApi> { KtorFeedApi(get()) }
    single<ProfileRepository> { DefaultProfileRepository(get()) }
    single<ReelsRepository> { DefaultReelsRepository(get()) }
    single<FeedRepository> { DefaultFeedRepository(get()) }
    single<PostInteractionStore> {
        SqlDelightPostInteractionStore(
            database = get(),
            migrator = get(),
            json = get(),
            dispatchers = get(),
        )
    }
    single<ReelInteractionStore> {
        SqlDelightReelInteractionStore(
            database = get(),
            migrator = get(),
            json = get(),
            dispatchers = get(),
        )
    }
    single<FeedInteractionStore> {
        SqlDelightFeedInteractionStore(
            database = get(),
            migrator = get(),
            dispatchers = get(),
        )
    }
    factory { GetProfileUseCase(get()) }
    factory { GetReelsPageUseCase(get()) }
    factory { GetFeedPageUseCase(get()) }
    single { ProfileViewModel(get(), get(), get(), get(named(APP_SCOPE_QUALIFIER))) }
    single { ReelsViewModel(get(), get(), get(named(APP_SCOPE_QUALIFIER))) }
    single { FeedViewModel(get(), get(), get(named(APP_SCOPE_QUALIFIER))) }
}

val appModules = listOf(networkModule)
