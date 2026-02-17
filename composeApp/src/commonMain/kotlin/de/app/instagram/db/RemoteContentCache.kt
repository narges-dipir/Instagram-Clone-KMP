package de.app.instagram.db

import kotlinx.coroutines.flow.Flow

interface RemoteContentCache {
    fun observe(key: String): Flow<String?>
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
}
