package de.app.instagram.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVKit.AVPlayerViewController
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSURL
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformVideoPlayer(
    videoUrl: String,
    isMuted: Boolean,
    isActive: Boolean,
    shouldPlay: Boolean,
    modifier: Modifier,
) {
    if (!isActive) {
        Box(modifier = modifier.background(Color.Black))
        return
    }

    val muteState = isMuted
    val playerItem = remember(videoUrl) {
        NSURL.URLWithString(videoUrl)?.let { url ->
            val asset = AVURLAsset(uRL = url, options = null)
            AVPlayerItem(asset = asset)
        }
    }
    val player = remember(playerItem) {
        playerItem?.let { AVPlayer(playerItem = it) }
    }
    val controller = remember(player) {
        AVPlayerViewController().apply {
            this.player = player
            this.showsPlaybackControls = false
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            controller.view
        },
        update = { view: UIView ->
            if (muteState) {
                // Mute control is not currently exposed in this AVPlayer interop surface.
            }
            view.setNeedsLayout()
        },
    )

    DisposableEffect(player) {
        onDispose {
            // AVPlayerViewController handles player lifecycle with its view.
        }
    }
}
