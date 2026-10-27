package com.moonstream.backend.plugins

import com.moonstream.backend.models.ApiStatusResponse
import com.moonstream.backend.models.HealthResponse
import com.moonstream.backend.service.ScraperService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(scraperService: ScraperService) {
    routing {
        // Health check y status endpoints con modelos @Serializable tipados
        get("/") {
            call.respond(HttpStatusCode.OK, ApiStatusResponse())
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse())
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
