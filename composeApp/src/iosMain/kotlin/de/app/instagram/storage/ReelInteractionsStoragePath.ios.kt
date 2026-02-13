package de.app.instagram.storage

import platform.Foundation.NSTemporaryDirectory

actual fun reelInteractionsStoragePath(): String {
    return NSTemporaryDirectory() + "instagram_reel_interactions.json"
}
