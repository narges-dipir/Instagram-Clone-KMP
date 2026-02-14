package de.app.instagram.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.io.File
import okhttp3.Cache

private const val HTTP_CACHE_DIR_NAME: String = "http_json_cache"
private const val HTTP_CACHE_SIZE_BYTES: Long = 50L * 1024L * 1024L

actual fun createPlatformHttpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        val rootPath = AndroidCachePaths.appCacheDirPath ?: System.getProperty("java.io.tmpdir")
        val cacheDir = File(rootPath, HTTP_CACHE_DIR_NAME).apply { mkdirs() }
        cache(Cache(cacheDir, HTTP_CACHE_SIZE_BYTES))
        addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.header("Cache-Control").isNullOrBlank()) {
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .build()
            } else {
                response
            }
        }
    }
}
