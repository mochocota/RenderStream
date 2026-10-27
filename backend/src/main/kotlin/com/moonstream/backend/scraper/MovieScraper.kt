package com.moonstream.backend.scraper

import com.moonstream.backend.models.MovieSearchResult
import com.moonstream.backend.models.VideoServerInfo

/**
 * Contrato de proveedor de scraping de películas.
 * Permite añadir fácilmente nuevos sitios (e.g. Cuevana, Pelisplus, etc.)
 * manteniendo desacoplada la capa HTTP y la lógica de enrutamiento.
 */
interface MovieScraper {
    val siteName: String

    suspend fun search(query: String): List<MovieSearchResult>
    suspend fun getServersByImdb(imdbId: String): List<VideoServerInfo>
}
