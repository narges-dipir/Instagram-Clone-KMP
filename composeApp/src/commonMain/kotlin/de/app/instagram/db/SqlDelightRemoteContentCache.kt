package de.app.instagram.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import de.app.instagram.di.CoroutineDispatchers
import de.app.instagram.di.DefaultCoroutineDispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow

class SqlDelightRemoteContentCache(
    database: InstagramCacheDatabase,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers,
) : RemoteContentCache {
    private val queries = database.interactionCacheQueries

    override fun observe(key: String): Flow<String?> {
        return queries.selectMetaValue(key)
            .asFlow()
            .mapToOneOrNull(dispatchers.io)
    }

    override suspend fun read(key: String): String? = withContext(dispatchers.io) {
        queries.selectMetaValue(key).executeAsOneOrNull()
    }

    override suspend fun write(key: String, value: String) {
        withContext(dispatchers.io) {
            queries.upsertMetaValue(
                key = key,
                value_ = value,
            )
        }
    }
}
