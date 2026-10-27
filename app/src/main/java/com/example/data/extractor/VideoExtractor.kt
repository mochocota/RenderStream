package com.example.data.extractor

import com.example.data.model.VideoStream

/**
 * Interfaz base para cualquier extractor de video.
 *
 * Permite cumplir con el Requisito 6 (registrar extractores personalizados)
 * y desacoplar la lógica de scraping de la capa de presentación y reproducción.
 */
interface VideoExtractor {
    /** Nombre legible del servidor (e.g. "Streamtape", "Voe", "Filemoon") */
    val serverName: String

    /** Dominios asociados a este servidor de streaming */
    val supportedDomains: List<String>

    /**
     * Determina si esta clase puede manejar la URL o iframe proporcionado.
     */
    fun canHandle(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return supportedDomains.any { domain -> lowerUrl.contains(domain) }
    }

    /**
     * Ejecuta el proceso de extracción para resolver el enlace directo (.m3u8 o .mp4).
     * Se ejecuta dentro de un contexto de corrutinas (Dispatchers.IO).
     */
    suspend fun extract(iframeUrl: String): VideoStream
}
