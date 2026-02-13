package de.app.instagram.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVKit.AVPlayerViewController
import platform.AVFoundation.AVPlayer
import platform.Foundation.NSURL
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformVideoPlayer(
    videoUrl: String,
    isMuted: Boolean,
    modifier: Modifier,
) {
    val muteState = isMuted
    val player = remember(videoUrl) {
        NSURL.URLWithString(videoUrl)?.let { url ->
            AVPlayer(uRL = url)
        }
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
