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
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.data.model.VideoStream
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Gestor del reproductor Media3 / ExoPlayer.
 *
 * Cumple con el Requisito 4:
 * 1. Forzar MIME types: MimeTypes.APPLICATION_M3U8 para HLS, MimeTypes.VIDEO_MP4 para MP4.
 * 2. Inyectar headers requeridos (Referer, User-Agent) mediante OkHttpDataSource.Factory.
 * 3. Manejo de ciclo de vida (release).
 * 4. Manejo de errores con Player.Listener y detección de tokens expirados (HTTP 403/410).
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
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
                // Detección de token expirado o enlace caducado (común en servidores como Streamtape / Voe tras unas horas)
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
     * Configura el MediaSource con OkHttp DataSource para soportar headers personalizados (Referer).
     */
    fun prepareStream(stream: VideoStream, playWhenReady: Boolean = true) {
        val player = getPlayer()

        // 1. Configurar DataSource con User-Agent y Headers (Referer fundamental en hosts como Streamtape)
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(httpClient)
            .setUserAgent(stream.headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setDefaultRequestProperties(stream.headers)

        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

        // 2. Configurar MediaItem forzando el MIME Type exacto (Requisito 4)
        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .setMimeType(stream.mimeType)
            .build()

        // 3. Crear MediaSource específico (HLS vs Progresivo/MP4)
        val mediaSource: MediaSource = if (stream.mimeType == MimeTypes.APPLICATION_M3U8) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }

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
