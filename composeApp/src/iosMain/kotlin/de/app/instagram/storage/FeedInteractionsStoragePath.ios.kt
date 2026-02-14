package de.app.instagram.storage

import platform.Foundation.NSTemporaryDirectory

actual fun feedInteractionsStoragePath(): String {
    return NSTemporaryDirectory() + "instagram_feed_interactions.json"
}
