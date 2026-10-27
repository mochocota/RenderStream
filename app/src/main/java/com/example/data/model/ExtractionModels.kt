package com.example.data.model

/**
 * Modelo de datos que representa el stream directo extraído y listo para Media3/ExoPlayer.
 */
data class VideoStream(
    val url: String,
    val mimeType: String,
    val quality: String = "Auto",
    val server: String,
    val headers: Map<String, String> = emptyMap(),
    val extractedAt: Long = System.currentTimeMillis(),
    val estimatedTtlMs: Long = 4 * 60 * 60 * 1000L // Estimación de expiración (~4h a 24h)
) {
    val isExpired: Boolean
        get() = (System.currentTimeMillis() - extractedAt) > estimatedTtlMs
}

/**
 * Categorías de errores en la extracción de video para guiar al usuario
 * y decidir la estrategia de recuperación automática o manual.
 */
enum class ExtractionErrorType(val userTitle: String) {
    NO_INTERNET("Sin conexión a Internet"),
    MALFORMED_URL("URL inválida o malformada"),
    UNSUPPORTED_SERVER("Servidor no soportado"),
    EXPIRED_LINK("Enlace de streaming expirado"),
    HTTP_FORBIDDEN("Acceso denegado (HTTP 403 / Token Inválido)"),
    TIMEOUT("Tiempo de espera agotado"),
    PARSING_FAILED("Fallo al decodificar la fuente del video"),
    VIDEO_DELETED("Video eliminado o no disponible en el servidor"),
    CAPTCHA_OR_CLOUDFLARE("Bloqueo por protección anti-bot / Cloudflare")
}

/**
 * Estado sellado devuelto por la capa de abstracción del extractor.
 */
sealed class ExtractorResult {
    data object Idle : ExtractorResult()

    data class Loading(
        val stepMessage: String = "Extrayendo enlace de video...",
        val serverName: String? = null
    ) : ExtractorResult()

    data class Success(
        val stream: VideoStream,
        val sourceIframeUrl: String,
        val durationMs: Long = 0L
    ) : ExtractorResult()

    data class Error(
        val message: String,
        val errorType: ExtractionErrorType,
        val canRetry: Boolean = true,
        val sourceIframeUrl: String? = null,
        val technicalDetails: String? = null
    ) : ExtractorResult()
}
