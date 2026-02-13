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
    modifier: Modifier,
) {
    val player = remember(videoUrl) {
        NSURL.URLWithString(videoUrl)?.let { url ->
            AVPlayer(uRL = url)
        }
    }
    val controller = remember(player) {
        AVPlayerViewController().apply {
            this.player = player
            this.showsPlaybackControls = true
        }
    }

    UIKitView(
        modifier = modifier,
        factory = {
            controller.view
        },
        update = { view: UIView ->
            view.setNeedsLayout()
        },
    )

    DisposableEffect(player) {
        onDispose {
            // AVPlayerViewController handles player lifecycle with its view.
        }
    }
}
