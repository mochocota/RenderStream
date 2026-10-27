package com.example.data.extractor

import androidx.media3.common.MimeTypes
import com.example.data.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Extractor para el servidor StreamWish.
 * Soporta streamwish.to, streamwish.com, swishsrv.com, wishembed.com, etc.
 */
class StreamWishExtractor(
    private val httpClient: OkHttpClient
) : VideoExtractor {

    override val serverName: String = "StreamWish"

    override val supportedDomains: List<String> = listOf(
        "streamwish.to",
        "streamwish.com",
        "swishsrv.com",
        "wishembed.com",
        "embedwish.com",
        "strwish.com"
    )

    override suspend fun extract(iframeUrl: String): VideoStream = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(iframeUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", iframeUrl)
            .build()

        val html = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Error HTTP ${response.code} en servidor StreamWish")
            }
            response.body?.string() ?: throw IOException("Cuerpo de respuesta vacío")
        }

        var unpacked = html
        if (JsUnpacker.isPacked(html)) {
            unpacked = JsUnpacker.unpack(html)
        }

        val fileRegex = Regex("""['"]?file['"]?\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""")
        var hlsUrl = fileRegex.find(unpacked)?.groupValues?.get(1)

        if (hlsUrl == null) {
            val generalM3u8Regex = Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""")
            hlsUrl = generalM3u8Regex.find(unpacked)?.groupValues?.get(1)
        }

        if (hlsUrl == null) {
            throw IOException("No se encontró stream HLS en StreamWish")
        }

        VideoStream(
            url = hlsUrl,
            mimeType = MimeTypes.APPLICATION_M3U8,
            quality = "HLS Adaptativo (Auto)",
            server = serverName,
            headers = mapOf(
                "Referer" to iframeUrl,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ),
            estimatedTtlMs = 4 * 60 * 60 * 1000L
        )
    }
}
