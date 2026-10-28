package com.example.data.extractor

import com.example.data.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extractor para enlaces directos .m3u8 (HLS) y .mp4, útil para pruebas y CDNs directos.
 */
class DirectMediaExtractor(
    private val mimeTypeDetector: MimeTypeDetector
) : VideoExtractor {

    override val serverName: String = "Direct Stream"

    override val supportedDomains: List<String> = listOf(
        ".m3u8",
        ".mp4",
        "mux.dev",
        "akamaized.net",
        "akamaihd.net",
        "zencdn.net",
        "w3.org",
        "cloudfront.net"
    )

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.endsWith(".m3u8") ||
               lower.endsWith(".mp4") ||
               lower.contains(".m3u8?") ||
               lower.contains(".mp4?") ||
               supportedDomains.any { lower.contains(it) }
    }

    override suspend fun extract(iframeUrl: String): VideoStream = withContext(Dispatchers.IO) {
        val detectedMime = mimeTypeDetector.detectMimeType(iframeUrl)

        VideoStream(
            url = iframeUrl,
            mimeType = detectedMime,
            quality = if (detectedMime.contains("mpegurl")) "HLS Adaptativo" else "MP4 Directo",
            server = serverName,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ),
            estimatedTtlMs = 24 * 60 * 60 * 1000L
        )
    }
}
