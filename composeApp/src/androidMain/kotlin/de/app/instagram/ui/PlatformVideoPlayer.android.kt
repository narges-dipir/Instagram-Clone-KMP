package de.app.instagram.ui

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun PlatformVideoPlayer(
    videoUrl: String,
    modifier: Modifier,
) {
    val source = remember(videoUrl) { videoUrl }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                setVideoPath(source)
                setMediaController(MediaController(context).also { controller ->
                    controller.setAnchorView(this)
                })
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    start()
                }
            }
        },
        update = { view ->
            view.setVideoPath(source)
            view.start()
        },
    )

    DisposableEffect(source) {
        onDispose {
            // VideoView is owned by AndroidView lifecycle; no explicit release API.
        }
    }
}
