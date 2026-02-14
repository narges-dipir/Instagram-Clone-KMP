package de.app.instagram.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual fun createDatabaseDriver(): SqlDriver {
    return NativeSqliteDriver(
        schema = InstagramCacheDatabase.Schema,
        name = "instagram_cache.db",
    )
}
