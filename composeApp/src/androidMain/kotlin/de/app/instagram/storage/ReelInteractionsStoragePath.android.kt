package de.app.instagram.storage

actual fun reelInteractionsStoragePath(): String {
    val baseDir = System.getProperty("java.io.tmpdir") ?: "."
    return "$baseDir/instagram_reel_interactions.json"
}
