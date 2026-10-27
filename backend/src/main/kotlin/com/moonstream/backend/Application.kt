package com.moonstream.backend

import com.moonstream.backend.config.AppConfig
import com.moonstream.backend.plugins.configureHTTP
import com.moonstream.backend.plugins.configureRouting
import com.moonstream.backend.plugins.configureSerialization
import com.moonstream.backend.plugins.configureStatusPages
import com.moonstream.backend.scraper.ExampleMovieScraper
import com.moonstream.backend.service.ScraperService
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

fun main() {
    val logger = LoggerFactory.getLogger("ApplicationMain")
    val port = AppConfig.port
    val host = AppConfig.host

    logger.info("Iniciando MoonStream Backend en $host:$port...")

    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Configuración del cliente OkHttp compartido
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Inyección de dependencias modular
    val scraper = ExampleMovieScraper(httpClient)
    val scraperService = ScraperService(scraper)

    // Configuración de Plugins Ktor
    configureSerialization()
    configureHTTP()
    configureStatusPages()
    configureRouting(scraperService)
}
