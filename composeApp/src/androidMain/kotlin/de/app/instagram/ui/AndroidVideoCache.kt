package de.app.instagram.ui

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

private const val VIDEO_CACHE_DIR_NAME: String = "video_cache"
private const val VIDEO_CACHE_SIZE_BYTES: Long = 300L * 1024L * 1024L

object AndroidVideoCache {
    @Volatile
    private var cache: Cache? = null

    fun get(context: Context): Cache {
        cache?.let { return it }
        return synchronized(this) {
            cache?.let { return@synchronized it }
            val cacheDir = File(context.cacheDir, VIDEO_CACHE_DIR_NAME).apply { mkdirs() }
            val created = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_SIZE_BYTES),
                StandaloneDatabaseProvider(context),
            )
            cache = created
            created
        }
    }

    fun createDataSourceFactory(context: Context): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(androidx.media3.datasource.DefaultDataSource.Factory(context))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
