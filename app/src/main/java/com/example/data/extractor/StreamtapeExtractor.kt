package com.example.data.extractor

import androidx.media3.common.MimeTypes
import com.example.data.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Extractor para el servidor Streamtape.
 * Soporta streamtape.com, streamtape.to, streamtape.net, streamta.pe, etc.
 *
 * El servidor genera enlaces MP4 directos con tokens temporales (expiran en ~4 a 24 horas).
 * Requiere el header 'Referer: https://streamtape.com/' en ExoPlayer para evitar HTTP 403 Forbidden.
 */
class StreamtapeExtractor(
    private val httpClient: OkHttpClient
) : VideoExtractor {

    override val serverName: String = "Streamtape"

    override val supportedDomains: List<String> = listOf(
        "streamtape.com",
        "streamtape.to",
        "streamtape.net",
        "streamta.pe",
        "streamadblocker.com",
        "tapecontent.net"
    )

    override suspend fun extract(iframeUrl: String): VideoStream = withContext(Dispatchers.IO) {
        // Normalizar la URL para asegurar formato de embed
        val embedUrl = if (iframeUrl.contains("/v/")) {
            iframeUrl.replace("/v/", "/e/")
        } else {
            iframeUrl
        }

        val request = Request.Builder()
            .url(embedUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", "https://streamtape.com/")
            .build()

        val html = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Error HTTP ${response.code} al consultar Streamtape")
            }
            response.body?.string() ?: throw IOException("Cuerpo de respuesta vacío")
        }

        // Buscar el token generado dinámicamente en el DOM
        // Patrón común: document.getElementById('robotlink' o 'idealdvl').innerHTML = '//streamtape.com/get_video?id=...' + ('&token=...');
        val pattern = Regex("""id=['"](?:robotlink|idealdvl)['"][^>]*>([^<]+)""")
        val scriptTokenPattern = Regex("""document\.getElementById\(['"](?:robotlink|idealdvl)['"]\)\.innerHTML\s*=\s*['"]([^'"]+)['"]\s*\+\s*\(['"]([^'"]+)['"]""")

        var videoUrl: String? = null

        val scriptMatch = scriptTokenPattern.find(html)
        if (scriptMatch != null) {
            val part1 = scriptMatch.groupValues[1]
            val part2 = scriptMatch.groupValues[2]
            videoUrl = "https:$part1$part2"
        } else {
            val simpleMatch = pattern.find(html)
            if (simpleMatch != null) {
                val raw = simpleMatch.groupValues[1]
                videoUrl = if (raw.startsWith("//")) "https:$raw" else raw
            }
        }

        if (videoUrl == null || !videoUrl.contains("get_video")) {
            throw IOException("No se encontró el enlace 'get_video' en el HTML de Streamtape. Es posible que el video haya sido eliminado o requiera verificación.")
        }

        VideoStream(
            url = videoUrl,
            mimeType = MimeTypes.VIDEO_MP4,
            quality = "1080p MP4",
            server = serverName,
            headers = mapOf(
                "Referer" to "https://streamtape.com/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ),
            estimatedTtlMs = 6 * 60 * 60 * 1000L // Streamtape suele expirar en 6 horas
        )
    }
}
