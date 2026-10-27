package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.URLUtil
import com.example.data.extractor.ExtractorRegistry
import com.example.data.extractor.VideoExtractor
import com.example.data.model.ExtractionErrorType
import com.example.data.model.ExtractorResult
import com.example.data.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Interfaz de la capa de abstracción del repositorio de extracción de video.
 *
 * Cumple con el Requisito 2:
 * - Aísla la implementación de scraping o librerías externas (como MoonGetter).
 * - Se ejecuta en Dispatchers.IO.
 * - Devuelve el sealed class ExtractorResult.
 * - Maneja timeouts y excepciones específicas.
 */
interface VideoExtractorRepository {
    suspend fun extractVideo(iframeUrl: String): ExtractorResult
    suspend fun reExtractExpired(sourceIframeUrl: String): ExtractorResult
    fun getRegisteredServers(): List<String>
    fun registerCustomExtractor(extractor: VideoExtractor)
}

/**
 * Implementación robusta del repositorio con validación de red, timeouts,
 * soporte de re-extracción y política de NO-caching de URLs de video temporales.
 */
class VideoExtractorRepositoryImpl(
    private val context: Context,
    private val extractorRegistry: ExtractorRegistry,
    private val timeoutMillis: Long = 15_000L,
    private val networkChecker: (() -> Boolean)? = null
) : VideoExtractorRepository {

    override suspend fun extractVideo(iframeUrl: String): ExtractorResult = withContext(Dispatchers.IO) {
        val trimmedUrl = iframeUrl.trim()
        val startTime = System.currentTimeMillis()

        // 1. Caso Borde: URL malformada o vacía
        if (trimmedUrl.isEmpty() || (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://"))) {
            return@withContext ExtractorResult.Error(
                message = "La URL del iframe no es válida. Debe comenzar con http:// o https://",
                errorType = ExtractionErrorType.MALFORMED_URL,
                canRetry = false,
                sourceIframeUrl = iframeUrl
            )
        }

        if (!URLUtil.isValidUrl(trimmedUrl)) {
            return@withContext ExtractorResult.Error(
                message = "Formato de URL inválido. Comprueba que no tenga caracteres erróneos o espacios.",
                errorType = ExtractionErrorType.MALFORMED_URL,
                canRetry = false,
                sourceIframeUrl = iframeUrl
            )
        }

        // 2. Caso Borde: Sin conexión a internet
        if (!isNetworkAvailable()) {
            return@withContext ExtractorResult.Error(
                message = "No hay conexión a Internet. Verifica tu conexión Wi-Fi o datos móviles.",
                errorType = ExtractionErrorType.NO_INTERNET,
                canRetry = true,
                sourceIframeUrl = iframeUrl
            )
        }

        // 3. Caso Borde: Servidor no soportado
        val extractor = extractorRegistry.findExtractorFor(trimmedUrl)
        if (extractor == null) {
            val supported = extractorRegistry.getAllRegisteredServers().joinToString(", ")
            return@withContext ExtractorResult.Error(
                message = "El servidor de esta URL no está soportado.\nServidores disponibles: $supported",
                errorType = ExtractionErrorType.UNSUPPORTED_SERVER,
                canRetry = false,
                sourceIframeUrl = iframeUrl
            )
        }

        // 4. Ejecución de la extracción con timeout controlado
        try {
            val videoStream: VideoStream = withTimeout(timeoutMillis) {
                extractor.extract(trimmedUrl)
            }

            val elapsedMs = System.currentTimeMillis() - startTime
            ExtractorResult.Success(
                stream = videoStream,
                sourceIframeUrl = trimmedUrl,
                durationMs = elapsedMs
            )
        } catch (e: TimeoutCancellationException) {
            ExtractorResult.Error(
                message = "Tiempo de espera agotado (${timeoutMillis / 1000}s) al contactar al servidor ${extractor.serverName}.",
                errorType = ExtractionErrorType.TIMEOUT,
                canRetry = true,
                sourceIframeUrl = trimmedUrl,
                technicalDetails = e.message
            )
        } catch (e: SocketTimeoutException) {
            ExtractorResult.Error(
                message = "Timeout de socket con el servidor ${extractor.serverName}. El host podría estar sobrecargado.",
                errorType = ExtractionErrorType.TIMEOUT,
                canRetry = true,
                sourceIframeUrl = trimmedUrl,
                technicalDetails = e.message
            )
        } catch (e: UnknownHostException) {
            ExtractorResult.Error(
                message = "No se pudo resolver el host del servidor. Puede que el dominio haya cambiado o esté caído.",
                errorType = ExtractionErrorType.NO_INTERNET,
                canRetry = true,
                sourceIframeUrl = trimmedUrl,
                technicalDetails = e.message
            )
        } catch (e: IOException) {
            val isForbidden = e.message?.contains("403") == true
            val errorType = if (isForbidden) {
                ExtractionErrorType.HTTP_FORBIDDEN
            } else {
                ExtractionErrorType.PARSING_FAILED
            }

            ExtractorResult.Error(
                message = e.message ?: "Error de comunicación con el servidor ${extractor.serverName}",
                errorType = errorType,
                canRetry = true,
                sourceIframeUrl = trimmedUrl,
                technicalDetails = e.stackTraceToString()
            )
        } catch (e: Exception) {
            ExtractorResult.Error(
                message = "Error inesperado al extraer: ${e.localizedMessage}",
                errorType = ExtractionErrorType.PARSING_FAILED,
                canRetry = true,
                sourceIframeUrl = trimmedUrl,
                technicalDetails = e.stackTraceToString()
            )
        }
    }

    /**
     * Estrategia de reintento cuando un enlace expira (HTTP 403 o token caducado).
     * No almacena en caché enlaces finales: consulta de nuevo el iframe original
     * para obtener un token fresco.
     */
    override suspend fun reExtractExpired(sourceIframeUrl: String): ExtractorResult {
        return extractVideo(sourceIframeUrl)
    }

    override fun getRegisteredServers(): List<String> {
        return extractorRegistry.getAllRegisteredServers()
    }

    override fun registerCustomExtractor(extractor: VideoExtractor) {
        extractorRegistry.register(extractor)
    }

    /**
     * Comprobación real del estado de red en Android.
     */
    private fun isNetworkAvailable(): Boolean {
        if (networkChecker != null) return networkChecker.invoke()
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val activeNetwork = cm.activeNetwork ?: return true
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return true
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // Fallback permisivo si falla la inspección del sistema
        }
    }
}
