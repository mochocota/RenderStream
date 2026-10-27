package com.example.data.extractor

import okhttp3.OkHttpClient
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registro de extractores de video (Extractor Registry).
 *
 * Cumple con el Requisito 6:
 * Permite registrar extractores personalizados dinámicamente si un servidor
 * específico no viene soportado por defecto o cambia de dominio/lógica.
 */
class ExtractorRegistry(
    private val httpClient: OkHttpClient,
    private val mimeTypeDetector: MimeTypeDetector
) {
    private val extractors = CopyOnWriteArrayList<VideoExtractor>()

    init {
        // Registrar extractores integrados por defecto
        register(StreamtapeExtractor(httpClient))
        register(VoeExtractor(httpClient))
        register(FilemoonExtractor(httpClient))
        register(StreamWishExtractor(httpClient))
        register(DirectMediaExtractor(mimeTypeDetector))
    }

    /**
     * Registra un extractor personalizado en tiempo de ejecución.
     * Los extractores registrados más recientemente tienen mayor prioridad.
     */
    fun register(extractor: VideoExtractor) {
        // Añadir al inicio para permitir sobrescribir extractores por defecto si es necesario
        extractors.add(0, extractor)
    }

    /**
     * Desregistra un extractor por nombre de servidor.
     */
    fun unregister(serverName: String) {
        extractors.removeAll { it.serverName.equals(serverName, ignoreCase = true) }
    }

    /**
     * Busca el extractor adecuado para la URL dada.
     */
    fun findExtractorFor(url: String): VideoExtractor? {
        return extractors.firstOrNull { it.canHandle(url) }
    }

    /**
     * Lista todos los servidores registrados actualmente.
     */
    fun getAllRegisteredServers(): List<String> {
        return extractors.map { it.serverName }.distinct()
    }

    /**
     * Ejemplo de registro de extractor genérico basado en Regex personalizado.
     */
    fun registerRegexExtractor(
        name: String,
        domains: List<String>,
        videoUrlRegex: Regex,
        mimeType: String,
        headers: Map<String, String> = emptyMap()
    ) {
        register(object : VideoExtractor {
            override val serverName: String = name
            override val supportedDomains: List<String> = domains

            override suspend fun extract(iframeUrl: String): com.example.data.model.VideoStream {
                val request = okhttp3.Request.Builder()
                    .url(iframeUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val html = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}")
                    response.body?.string() ?: ""
                }

                val directUrl = videoUrlRegex.find(html)?.groupValues?.getOrNull(1)
                    ?: throw java.io.IOException("No se encontró coincidencia con el regex personalizado para $name")

                return com.example.data.model.VideoStream(
                    url = directUrl,
                    mimeType = mimeType,
                    server = name,
                    headers = headers
                )
            }
        })
    }
}
