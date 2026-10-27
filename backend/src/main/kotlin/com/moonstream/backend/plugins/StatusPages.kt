package com.moonstream.backend.plugins

import com.moonstream.backend.models.BadRequestException
import com.moonstream.backend.models.ErrorResponse
import com.moonstream.backend.models.ExternalScraperException
import com.moonstream.backend.models.ResourceNotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("StatusPages")

    install(StatusPages) {
        // Error 400: Parámetros inválidos o faltantes
        exception<BadRequestException> { call, cause ->
            logger.warn("Petición inválida: ${cause.message}")
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorResponse(
                    status = HttpStatusCode.BadRequest.value,
                    error = "Bad Request",
                    message = cause.message ?: "Parámetros de consulta incorrectos"
                )
            )
        }

        // Error 404: Contenido no encontrado en el proveedor
        exception<ResourceNotFoundException> { call, cause ->
            logger.info("Recurso no encontrado: ${cause.message}")
            call.respond(
                status = HttpStatusCode.NotFound,
                message = ErrorResponse(
                    status = HttpStatusCode.NotFound.value,
                    error = "Not Found",
                    message = cause.message ?: "Recurso no encontrado"
                )
            )
        }

        // Error 502: Fallo al comunicarse con el sitio externo o bloqueo por Cloudflare/Anti-Bot
        exception<ExternalScraperException> { call, cause ->
            logger.error("Error en sitio externo (502 Bad Gateway): ${cause.message}", cause)
            call.respond(
                status = HttpStatusCode.BadGateway,
                message = ErrorResponse(
                    status = HttpStatusCode.BadGateway.value,
                    error = "Bad Gateway",
                    message = cause.message ?: "Error al consultar el proveedor externo de películas"
                )
            )
        }

        // Error 500: Errores no controlados del servidor
        exception<Throwable> { call, cause ->
            logger.error("Error no controlado: ${cause.message}", cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse(
                    status = HttpStatusCode.InternalServerError.value,
                    error = "Internal Server Error",
                    message = "Ocurrió un error inesperado en el servidor backend"
                )
            )
        }
    }
}
