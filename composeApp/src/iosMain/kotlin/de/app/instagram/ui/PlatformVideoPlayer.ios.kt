package de.app.instagram.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
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
    val shouldPlayState = rememberUpdatedState(shouldPlay)
    val videoUrlState = rememberUpdatedState(videoUrl)
    var currentItem by remember { mutableStateOf<AVPlayerItem?>(null) }

    LaunchedEffect(videoUrl) {
        val item = NSURL.URLWithString(videoUrl)?.let { AVPlayerItem(uRL = it) }
        currentItem = item
        player.performSelector(
            NSSelectorFromString("replaceCurrentItemWithPlayerItem:"),
            withObject = item,
        )
        if (shouldPlay) {
            player.performSelector(NSSelectorFromString("play"))
        } else {
            player.performSelector(NSSelectorFromString("pause"))
        }
    }

    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            player.performSelector(NSSelectorFromString("play"))
        } else {
            player.performSelector(NSSelectorFromString("pause"))
        }
    }

    LaunchedEffect(isMuted) {
        player.performSelector(
            NSSelectorFromString("setMuted:"),
            withObject = isMuted,
        )
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

    DisposableEffect(currentItem) {
        val item = currentItem
        val failureObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = item,
            queue = null,
        ) { _ -> }
        val endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = null,
        ) { _ ->
            val loopItem = NSURL.URLWithString(videoUrlState.value)?.let { AVPlayerItem(uRL = it) }
            currentItem = loopItem
            player.performSelector(
                NSSelectorFromString("replaceCurrentItemWithPlayerItem:"),
                withObject = loopItem,
            )
            if (shouldPlayState.value) {
                player.performSelector(NSSelectorFromString("play"))
            } else {
                player.performSelector(NSSelectorFromString("pause"))
            }
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(failureObserver)
            NSNotificationCenter.defaultCenter.removeObserver(endObserver)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.performSelector(NSSelectorFromString("pause"))
            player.performSelector(
                NSSelectorFromString("replaceCurrentItemWithPlayerItem:"),
                withObject = null,
            )
            playerLayer.removeFromSuperlayer()
        }
    }
}
