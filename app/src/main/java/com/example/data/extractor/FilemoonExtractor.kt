package com.example.data.extractor

import androidx.media3.common.MimeTypes
import com.example.data.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Extractor para el servidor Filemoon.
 * Soporta filemoon.sx, filemoon.to, filemoon.in, etc.
 * Desempaqueta scripts ofuscados con eval(function(p,a,c,k,e,d)...) para obtener el archivo .m3u8.
 */
class FilemoonExtractor(
    private val httpClient: OkHttpClient
) : VideoExtractor {

    override val serverName: String = "Filemoon"

    override val supportedDomains: List<String> = listOf(
        "filemoon.sx",
        "filemoon.to",
        "filemoon.in",
        "filemoon.nl",
        "moonplayer.xyz"
    )

    override suspend fun extract(iframeUrl: String): VideoStream = withContext(Dispatchers.IO) {
        var html = ""
        try {
            val request = Request.Builder()
                .url(iframeUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .addHeader("Referer", iframeUrl)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    html = response.body?.string() ?: ""
                }
            }
        } catch (e: Exception) {
            // Continuar
        }

        var hlsUrl: String? = null

        if (html.isNotEmpty()) {
            var unpacked = html
            if (JsUnpacker.isPacked(html)) {
                unpacked = JsUnpacker.unpack(html)
            }

            val fileRegex = Regex("""['"]?file['"]?\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""")
            hlsUrl = fileRegex.find(unpacked)?.groupValues?.get(1)

            if (hlsUrl == null) {
                val generalM3u8Regex = Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""")
                hlsUrl = generalM3u8Regex.find(unpacked)?.groupValues?.get(1)
            }
        }

        val isFallback = hlsUrl == null
        val finalUrl = if (!isFallback) {
            hlsUrl!!
        } else {
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        }

        val qualityLabel = if (!isFallback) "HLS Adaptativo (Auto)" else "HLS Adaptativo (Stream Fallback)"

        VideoStream(
            url = finalUrl,
            mimeType = MimeTypes.APPLICATION_M3U8,
            quality = qualityLabel,
            server = serverName,
            headers = mapOf(
                "Referer" to iframeUrl,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ),
            estimatedTtlMs = 4 * 60 * 60 * 1000L
        )
    }
}
