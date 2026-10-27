package com.example.data.extractor

import android.util.Base64
import androidx.media3.common.MimeTypes
import com.example.data.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Extractor para el servidor Voe.
 * Soporta voe.sx, voe-network.net, repackme.me, etc.
 *
 * Devuelve un stream adaptativo HLS (.m3u8).
 */
class VoeExtractor(
    private val httpClient: OkHttpClient
) : VideoExtractor {

    override val serverName: String = "Voe"

    override val supportedDomains: List<String> = listOf(
        "voe.sx",
        "voe-network.net",
        "repackme.me",
        "rovideo.cc",
        "tennistape.com"
    )

    override suspend fun extract(iframeUrl: String): VideoStream = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(iframeUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val html = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Error HTTP ${response.code} en servidor Voe")
            }
            response.body?.string() ?: throw IOException("Cuerpo de respuesta vacío")
        }

        // Caso 1: JSON o script directo con 'hls': 'https://...m3u8'
        val hlsRegex = Regex("""['"]hls['"]\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""")
        var hlsUrl = hlsRegex.find(html)?.groupValues?.get(1)

        // Caso 2: Payload en Base64 o variable sources = { ... }
        if (hlsUrl == null) {
            val base64Match = Regex("""const\s+sources\s*=\s*['"]([A-Za-z0-9+/=]+)['"]""").find(html)
            if (base64Match != null) {
                try {
                    val decodedJson = String(Base64.decode(base64Match.groupValues[1], Base64.DEFAULT))
                    val json = JSONObject(decodedJson)
                    hlsUrl = json.optString("hls")
                } catch (e: Exception) {
                    // Ignorar y continuar con fallback
                }
            }
        }

        // Caso 3: Fallback a cualquier URL .m3u8 dentro de tags <script>
        if (hlsUrl == null) {
            val generalM3u8Regex = Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""")
            hlsUrl = generalM3u8Regex.find(html)?.groupValues?.get(1)
        }

        if (hlsUrl == null) {
            throw IOException("No se encontró el manifiesto HLS .m3u8 en Voe. Es posible que el contenido haya expirado.")
        }

        VideoStream(
            url = hlsUrl,
            mimeType = MimeTypes.APPLICATION_M3U8,
            quality = "HLS Adaptativo (Auto)",
            server = serverName,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ),
            estimatedTtlMs = 12 * 60 * 60 * 1000L
        )
    }
}
