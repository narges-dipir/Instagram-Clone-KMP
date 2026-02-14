package de.app.instagram.ui

import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color as ComposeColor
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
    isActive: Boolean,
    shouldPlay: Boolean,
    modifier: Modifier,
) {
    if (!isActive) {
        Box(modifier = modifier.background(ComposeColor.Black))
        return
    }

    val context = LocalContext.current
    val player = remember(videoUrl) {
        ReelsPlayerPool.acquire(context.applicationContext, videoUrl)
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
            player.playWhenReady = shouldPlay
        },
    )

    DisposableEffect(player) {
        onDispose {
            ReelsPlayerPool.release(videoUrl)
        }
    }
}

private object ReelsPlayerPool {
    private const val MAX_POOL_SIZE = 3
    private val entries: LinkedHashMap<String, PlayerEntry> = linkedMapOf()

    @Synchronized
    fun acquire(context: android.content.Context, videoUrl: String): ExoPlayer {
        entries[videoUrl]?.let { entry ->
            entry.refCount += 1
            entry.lastAccessMs = SystemClock.elapsedRealtime()
            return entry.player
        }

        val mediaSource = ProgressiveMediaSource.Factory(
            AndroidVideoCache.createDataSourceFactory(context),
        ).createMediaSource(MediaItem.fromUri(videoUrl))

        val player = ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }

        entries[videoUrl] = PlayerEntry(
            player = player,
            refCount = 1,
            lastAccessMs = SystemClock.elapsedRealtime(),
        )
        evictIdlePlayersIfNeeded()
        return player
    }

    @Synchronized
    fun release(videoUrl: String) {
        val entry = entries[videoUrl] ?: return
        entry.refCount = (entry.refCount - 1).coerceAtLeast(0)
        entry.player.playWhenReady = false
        entry.lastAccessMs = SystemClock.elapsedRealtime()
        evictIdlePlayersIfNeeded()
    }

    private fun evictIdlePlayersIfNeeded() {
        if (entries.size <= MAX_POOL_SIZE) return

        val idleCandidates = entries
            .filterValues { it.refCount == 0 }
            .entries
            .sortedBy { it.value.lastAccessMs }

        val removeCount = entries.size - MAX_POOL_SIZE
        idleCandidates.take(removeCount).forEach { candidate ->
            entries.remove(candidate.key)?.player?.release()
        }
    }
}

private data class PlayerEntry(
    val player: ExoPlayer,
    var refCount: Int,
    var lastAccessMs: Long,
)
