package de.app.instagram.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClientEngine(): HttpClientEngine = Darwin.create()
