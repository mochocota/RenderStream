package com.example.data.extractor

import androidx.media3.common.MimeTypes
import com.example.data.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

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
        val request = Request.Builder()
            .url(iframeUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", iframeUrl)
            .build()

        val html = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Error HTTP ${response.code} en servidor Filemoon")
            }
            response.body?.string() ?: throw IOException("Cuerpo de respuesta vacío")
        }

        var unpacked = html
        if (JsUnpacker.isPacked(html)) {
            unpacked = JsUnpacker.unpack(html)
        }

        // Buscar el archivo m3u8 en el código desempaquetado: {file: "https://...master.m3u8"} o sources: [...]
        val fileRegex = Regex("""['"]?file['"]?\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""")
        var hlsUrl = fileRegex.find(unpacked)?.groupValues?.get(1)

        if (hlsUrl == null) {
            val generalM3u8Regex = Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""")
            hlsUrl = generalM3u8Regex.find(unpacked)?.groupValues?.get(1)
        }

        if (hlsUrl == null) {
            throw IOException("No se encontró el enlace .m3u8 en Filemoon. Posible captcha o enlace expirado.")
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
