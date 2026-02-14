package de.app.instagram.storage

actual fun feedInteractionsStoragePath(): String {
    val baseDir = System.getProperty("java.io.tmpdir") ?: "."
    return "$baseDir/instagram_feed_interactions.json"
}
