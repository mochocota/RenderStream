package com.moonstream.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class MovieSearchResult(
    val id: String,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val detailPath: String,
    val rating: Double? = null
)

@Serializable
data class SearchResponse(
    val query: String,
    val totalResults: Int,
    val results: List<MovieSearchResult>
)

@Serializable
data class VideoServerInfo(
    val serverName: String,
    val iframeUrl: String,
    val quality: String = "1080p",
    val language: String = "Latino",
    val headersRequired: Map<String, String> = emptyMap()
)

@Serializable
data class MovieServersResponse(
    val imdbId: String,
    val title: String,
    val serversCount: Int,
    val servers: List<VideoServerInfo>
)

@Serializable
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ApiStatusResponse(
    val status: String = "online",
    val service: String = "MoonStream Movie Scraper Backend",
    val version: String = "1.0.0",
    val endpoints: List<String> = listOf(
        "GET /search?q={titulo}",
        "GET /servers?imdb={id}"
    )
)

@Serializable
data class HealthResponse(
    val status: String = "UP"
)

// Excepciones tipadas de dominio
class ResourceNotFoundException(message: String) : RuntimeException(message)
class ExternalScraperException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class BadRequestException(message: String) : RuntimeException(message)
