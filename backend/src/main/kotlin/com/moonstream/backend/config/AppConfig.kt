package com.moonstream.backend.config

/**
 * Configuración centralizada de la aplicación extraída de variables de entorno.
 */
object AppConfig {
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host: String = System.getenv("HOST") ?: "0.0.0.0"

    // User-Agent configurable por variable de entorno para evitar bloqueos
    val userAgent: String = System.getenv("SCRAPER_USER_AGENT")
        ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    val targetBaseUrl: String = System.getenv("SCRAPER_BASE_URL")
        ?: "https://example-movies-site.test"

    val rateLimitMs: Long = System.getenv("RATE_LIMIT_MS")?.toLongOrNull() ?: 1000L
}
