package de.app.instagram.storage

import platform.Foundation.NSTemporaryDirectory

actual fun postInteractionsStoragePath(): String {
    return NSTemporaryDirectory() + "instagram_post_interactions.json"
}
