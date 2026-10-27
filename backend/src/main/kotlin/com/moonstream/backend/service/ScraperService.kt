package com.moonstream.backend.service

import com.moonstream.backend.models.BadRequestException
import com.moonstream.backend.models.MovieServersResponse
import com.moonstream.backend.models.ResourceNotFoundException
import com.moonstream.backend.models.SearchResponse
import com.moonstream.backend.scraper.MovieScraper

/**
 * Capa de servicio que orquesta la búsqueda y extracción de servidores.
 */
class ScraperService(
    private val scraper: MovieScraper
) {
    suspend fun search(query: String?): SearchResponse {
        val trimmed = query?.trim()
        if (trimmed.isNullOrBlank()) {
            throw BadRequestException("El parámetro de consulta 'q' no puede estar vacío.")
        }
        if (trimmed.length < 2) {
            throw BadRequestException("El parámetro 'q' debe contener al menos 2 caracteres.")
        }

        val results = scraper.search(trimmed)
        return SearchResponse(
            query = trimmed,
            totalResults = results.size,
            results = results
        )
    }

    suspend fun getServersForImdb(imdbId: String?): MovieServersResponse {
        val trimmed = imdbId?.trim()
        if (trimmed.isNullOrBlank()) {
            throw BadRequestException("El parámetro 'imdb' es obligatorio (e.g. tt1375666).")
        }
        if (!trimmed.startsWith("tt") || trimmed.length < 5) {
            throw BadRequestException("Formato de ID de IMDb inválido. Ejemplo esperado: tt1375666")
        }

        val servers = scraper.getServersByImdb(trimmed)
        if (servers.isEmpty()) {
            throw ResourceNotFoundException("No se encontraron servidores de video disponibles para IMDb ID '$trimmed'.")
        }

        return MovieServersResponse(
            imdbId = trimmed,
            title = "Título asociado a $trimmed",
            serversCount = servers.size,
            servers = servers
        )
    }
}
