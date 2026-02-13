package de.app.instagram.storage

actual fun postInteractionsStoragePath(): String {
    val baseDir = System.getProperty("java.io.tmpdir") ?: "."
    return "$baseDir/instagram_post_interactions.json"
}
