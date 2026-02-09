package de.app.instagram

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform