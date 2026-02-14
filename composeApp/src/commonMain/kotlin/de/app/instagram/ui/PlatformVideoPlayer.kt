package de.app.instagram.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformVideoPlayer(
    videoUrl: String,
    isMuted: Boolean,
    isActive: Boolean = true,
    shouldPlay: Boolean = true,
    modifier: Modifier = Modifier,
)
