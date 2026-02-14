package de.app.instagram.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
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

    val player = remember { AVPlayer() }
    val playerLayer = remember { AVPlayerLayer.playerLayerWithPlayer(player) }

    LaunchedEffect(videoUrl) {
        val item = NSURL.URLWithString(videoUrl)?.let { AVPlayerItem(uRL = it) }
        player.performSelector(
            NSSelectorFromString("replaceCurrentItemWithPlayerItem:"),
            withObject = item,
        )
        if (shouldPlay) {
            player.performSelector(NSSelectorFromString("play"))
        }
        println("PlatformVideoPlayer(iOS): loaded URL=$videoUrl itemNull=${item == null}")
    }

    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            player.performSelector(NSSelectorFromString("play"))
        } else {
            player.performSelector(NSSelectorFromString("pause"))
        }
    }

    LaunchedEffect(isMuted) {
        // setMuted/setVolume bindings are not available in this Kotlin/Native setup.
    }

    UIKitView(
        modifier = modifier,
        factory = {
            UIView(frame = CGRectZero.readValue()).apply {
                backgroundColor = UIColor.blackColor
                layer.addSublayer(playerLayer)
            }
        },
        update = { view ->
            playerLayer.frame = view.bounds
            view.setNeedsLayout()
            view.layoutIfNeeded()
        },
    )

    DisposableEffect(player) {
        val failureObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            println("PlatformVideoPlayer(iOS): failedToPlayToEnd userInfo=${notification?.userInfo}")
        }

        onDispose {
            player.performSelector(NSSelectorFromString("pause"))
            player.performSelector(
                NSSelectorFromString("replaceCurrentItemWithPlayerItem:"),
                withObject = null,
            )
            playerLayer.removeFromSuperlayer()
            NSNotificationCenter.defaultCenter.removeObserver(failureObserver)
        }
    }
}
