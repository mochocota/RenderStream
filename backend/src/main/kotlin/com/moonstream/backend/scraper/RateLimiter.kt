package com.moonstream.backend.scraper

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Limitador de tasa de peticiones concurrente para scraping ético.
 *
 * Garantiza que entre el inicio de dos peticiones consecutivas al sitio destino
 * transcurra al menos [minIntervalMs] (por defecto 1000ms = 1 petición/segundo).
 */
class RateLimiter(
    private val minIntervalMs: Long = 1000L
) {
    private val mutex = Mutex()
    private var lastRequestTime: Long = 0L

    suspend fun <T> withLimit(action: suspend () -> T): T = mutex.withLock {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < minIntervalMs) {
            val waitTime = minIntervalMs - elapsed
            delay(waitTime)
        }
        try {
            action()
        } finally {
            lastRequestTime = System.currentTimeMillis()
        }
    }
}
