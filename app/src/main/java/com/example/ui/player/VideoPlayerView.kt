package com.example.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoStream

/**
 * Composable que envuelve AndroidView con PlayerView de Media3.
 *
 * Cumple con el Requisito 4:
 * - Ciclo de vida gestionado mediante DisposableEffect.
 * - Liberación adecuada de recursos para prevenir fugas de memoria.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    stream: VideoStream,
    onPlayerError: (PlaybackException, isExpiredToken: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Instancia recordada del PlayerManager asociada a este Composable
    val playerManager = remember {
        Media3PlayerManager(
            context = context,
            onPlaybackError = onPlayerError
        )
    }

    // Preparar el nuevo stream cada vez que cambie la URL o los headers
    LaunchedEffect(stream.url) {
        playerManager.prepareStream(stream, playWhenReady = true)
    }

    // Ciclo de vida: liberar ExoPlayer cuando el Composable abandone la composición
    DisposableEffect(Unit) {
        onDispose {
            playerManager.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .testTag("video_player_container")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("media3_player_view"),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = playerManager.getPlayer()
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setKeepContentOnPlayerReset(true)
                }
            },
            update = { playerView ->
                if (playerView.player == null) {
                    playerView.player = playerManager.getPlayer()
                }
            }
        )
    }
}
