package com.example.data.extractor

import android.net.Uri
import androidx.media3.common.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

/**
 * Detector de tipo MIME para Media3 / ExoPlayer.
 *
 * Cumple con el Requisito 5:
 * 1. Análisis estático rápido a partir de la extensión o parámetros de la URL.
 * 2. Si la URL carece de extensión explícita (e.g. rutas REST con tokens temporales),
 *    realiza una petición HEAD HTTP en segundo plano para leer el header Content-Type.
 */
class MimeTypeDetector(
    private val httpClient: OkHttpClient
) {
    /**
     * Resuelve el MIME Type adecuado para Media3.
     */
    suspend fun detectMimeType(
        streamUrl: String,
        customHeaders: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        // 1. Intento rápido vía URL / Query Params
        val staticMime = detectFromUrl(streamUrl)
        if (staticMime != null) {
            return@withContext staticMime
        }

        // 2. Si no es concluyente, inspeccionar los headers del servidor HTTP
        detectFromHttpHeaders(streamUrl, customHeaders)
    }

    /**
     * Detección estática basada en la ruta del archivo o parámetros HLS/MP4.
     */
    fun detectFromUrl(url: String): String? {
        val cleanUrl = url.lowercase(Locale.ROOT).trim()

        return when {
            cleanUrl.contains(".m3u8") ||
            cleanUrl.contains("format=m3u8") ||
            cleanUrl.contains("format=hls") ||
            cleanUrl.contains("/hls/") ||
            cleanUrl.contains("playlist.m3u8") ||
            cleanUrl.contains("master.m3u8") -> MimeTypes.APPLICATION_M3U8

            cleanUrl.contains(".mp4") ||
            cleanUrl.contains("format=mp4") ||
            cleanUrl.contains("/mp4/") -> MimeTypes.VIDEO_MP4

            cleanUrl.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            cleanUrl.contains(".webm") -> MimeTypes.VIDEO_WEBM
            cleanUrl.contains(".mkv") -> MimeTypes.VIDEO_MATROSKA
            else -> null
        }
    }

    /**
     * Realiza una consulta HEAD (o Range GET mínima) para extraer el Content-Type real
     * del CDN/servidor de streaming.
     */
    private fun detectFromHttpHeaders(
        url: String,
        customHeaders: Map<String, String>
    ): String {
        return try {
            val requestBuilder = Request.Builder()
                .url(url)
                .head()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

            customHeaders.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val contentType = response.header("Content-Type")?.lowercase(Locale.ROOT) ?: ""
                when {
                    contentType.contains("application/x-mpegurl") ||
                    contentType.contains("application/vnd.apple.mpegurl") ||
                    contentType.contains("audio/x-mpegurl") -> MimeTypes.APPLICATION_M3U8

                    contentType.contains("video/mp4") -> MimeTypes.VIDEO_MP4
                    contentType.contains("application/dash+xml") -> MimeTypes.APPLICATION_MPD
                    contentType.contains("video/webm") -> MimeTypes.VIDEO_WEBM
                    contentType.contains("video/x-matroska") -> MimeTypes.VIDEO_MATROSKA
                    else -> MimeTypes.APPLICATION_M3U8 // Default a HLS para streams adaptativos modernos
                }
            }
        } catch (e: Exception) {
            // Si la petición HEAD falla (algunos CDNs bloquean HEAD con 405 Method Not Allowed),
            // usar heurística por defecto HLS
            MimeTypes.APPLICATION_M3U8
        }
    }
}
