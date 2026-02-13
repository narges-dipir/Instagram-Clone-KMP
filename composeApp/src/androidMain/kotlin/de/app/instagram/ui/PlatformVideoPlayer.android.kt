package de.app.instagram.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
actual fun PlatformVideoPlayer(
    videoUrl: String,
    isMuted: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val player = remember(videoUrl) {
        val mediaSource = ProgressiveMediaSource.Factory(
            AndroidVideoCache.createDataSourceFactory(context),
        ).createMediaSource(MediaItem.fromUri(videoUrl))

        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = if (isMuted) 0f else 1f
            prepare()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(Color.BLACK)
                this.player = player
            }
        },
        update = { view ->
            view.player = player
            player.volume = if (isMuted) 0f else 1f
        },
    )

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }
}
