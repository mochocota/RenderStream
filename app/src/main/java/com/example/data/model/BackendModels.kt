package com.example.data.model

data class MovieItem(
    val id: String,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val detailPath: String = "",
    val rating: Double? = null
)

data class ServerOption(
    val serverName: String,
    val iframeUrl: String,
    val quality: String = "1080p",
    val language: String = "Latino",
    val headersRequired: Map<String, String> = emptyMap()
)
