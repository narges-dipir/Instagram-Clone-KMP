package de.app.instagram.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver

actual fun createDatabaseDriver(): SqlDriver {
    return AndroidSqliteDriver(
        schema = InstagramCacheDatabase.Schema,
        context = AndroidAppContextHolder.requireContext(),
        name = "instagram_cache.db",
    )
}

object AndroidAppContextHolder {
    @Volatile
    var applicationContext: Context? = null

    fun requireContext(): Context {
        return checkNotNull(applicationContext) {
            "Android application context is not set. Initialize it from MainActivity before creating Koin modules."
        }
    }
}
