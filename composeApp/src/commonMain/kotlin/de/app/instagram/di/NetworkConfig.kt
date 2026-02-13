package de.app.instagram.di

import de.app.instagram.config.NetworkEnvironment
import de.app.instagram.network.NetworkModule

interface NetworkConfig {
    val baseUrl: String
    val appClientHeader: String
}

data class DefaultNetworkConfig(
    override val baseUrl: String,
    override val appClientHeader: String = NetworkModule.APP_CLIENT_HEADER,
) : NetworkConfig

object ProductionNetworkConfig : NetworkConfig {
    override val baseUrl: String = NetworkEnvironment.BASE_URL_PRODUCTION
    override val appClientHeader: String = NetworkModule.APP_CLIENT_HEADER
}

object StagingNetworkConfig : NetworkConfig {
    override val baseUrl: String = NetworkEnvironment.BASE_URL_STAGING
    override val appClientHeader: String = NetworkModule.APP_CLIENT_HEADER
}

object TestNetworkConfig : NetworkConfig {
    override val baseUrl: String = NetworkEnvironment.BASE_URL_TEST
    override val appClientHeader: String = NetworkModule.APP_CLIENT_HEADER
}
