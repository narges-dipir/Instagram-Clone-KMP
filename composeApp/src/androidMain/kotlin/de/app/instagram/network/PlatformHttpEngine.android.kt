package de.app.instagram.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPlatformHttpClientEngine(): HttpClientEngine = OkHttp.create()
