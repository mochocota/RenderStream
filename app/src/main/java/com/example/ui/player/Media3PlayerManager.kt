package com.example.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.example.data.model.VideoStream
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Gestor del reproductor Media3 / ExoPlayer de alta resiliencia.
 *
 * Mejoras aplicadas:
 * 1. Usa DefaultMediaSourceFactory para auto-detectar e instanciar HLS (.m3u8), MP4, MKV o DASH
 *    sin riesgo de excepciones por comparación estricta de cadenas de MIME types.
 * 2. Limpieza inteligente de headers: el header 'Referer' sólo se inyecta cuando la URL del
 *    video pertenece al dominio del host, evitando rechazos HTTP 403 en CDNs públicas.
 * 3. Configuración robusta de timeouts y seguimiento de redirecciones en OkHttp.
 */
@OptIn(UnstableApi::class)
class Media3PlayerManager(
    private val context: Context,
    private val onPlaybackError: (PlaybackException, isExpiredToken: Boolean) -> Unit = { _, _ -> },
    private val onPlaybackStateChanged: (Int) -> Unit = {}
) {
    private var exoPlayer: ExoPlayer? = null

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val isExpiredToken = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                        error.message?.contains("403") == true ||
                        error.message?.contains("410") == true ||
                        error.message?.contains("Response code: 403") == true

                onPlaybackError(error, isExpiredToken)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                this@Media3PlayerManager.onPlaybackStateChanged(playbackState)
            }
        })

        return player
    }

    /**
     * Prepara y reproduce el videoStream extraído.
     */
    fun prepareStream(stream: VideoStream, playWhenReady: Boolean = true) {
        val player = getPlayer()

        // Filtrar headers para evitar que un Referer ajeno bloquee CDNs como Akamai, Mux o Cloudflare
        val cleanHeaders = mutableMapOf<String, String>()
        val userAgent = stream.headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        cleanHeaders["User-Agent"] = userAgent

        stream.headers["Referer"]?.let { referer ->
            val urlLower = stream.url.lowercase()
            // Inyectar Referer solo si el stream pertenece al dominio del host original
            if (urlLower.contains("streamtape") || urlLower.contains("tapecontent") ||
                urlLower.contains("voe") || urlLower.contains("filemoon") || urlLower.contains("streamwish")) {
                cleanHeaders["Referer"] = referer
            }
        }

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(httpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(cleanHeaders)

        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

        // Resolver MIME Type canónico
        val effectiveMime = when {
            stream.mimeType.contains("mpegurl", ignoreCase = true) ||
            stream.mimeType.contains("m3u8", ignoreCase = true) ||
            stream.url.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8

            stream.url.contains(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
            else -> stream.mimeType
        }

        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .setMimeType(effectiveMime)
            .build()

        // DefaultMediaSourceFactory delega automáticamente a HlsMediaSource o ProgressiveMediaSource
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val mediaSource: MediaSource = mediaSourceFactory.createMediaSource(mediaItem)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun play() {
        exoPlayer?.play()
    }

    fun release() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        exoPlayer?.release()
        exoPlayer = null
    }
}
