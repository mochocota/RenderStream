package com.moonstream.backend.plugins

import com.moonstream.backend.service.ScraperService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(scraperService: ScraperService) {
    routing {
        // Health check endpoint
        get("/") {
            call.respond(
                mapOf(
                    "status" to "online",
                    "service" to "MoonStream Movie Scraper Backend",
                    "version" to "1.0.0",
                    "endpoints" to listOf(
                        "GET /search?q={titulo}",
                        "GET /servers?imdb={id}"
                    )
                )
            )
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }

        // Requisito 1: Endpoint GET /search?q={titulo}
        get("/search") {
            val query = call.request.queryParameters["q"]
            val response = scraperService.search(query)
            call.respond(HttpStatusCode.OK, response)
        }

        // Requisito 2: Endpoint GET /servers?imdb={id}
        get("/servers") {
            val imdbId = call.request.queryParameters["imdb"]
            val response = scraperService.getServersForImdb(imdbId)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
