package com.example.domain.usecase

import com.example.data.model.ExtractorResult
import com.example.data.repository.VideoExtractorRepository

/**
 * Caso de uso para la extracción de video.
 * Encapsula la llamada al repositorio y la orquestación para la UI.
 */
class ExtractVideoUseCase(
    private val repository: VideoExtractorRepository
) {
    suspend operator fun invoke(iframeUrl: String): ExtractorResult {
        return repository.extractVideo(iframeUrl)
    }

    suspend fun reExtract(sourceIframeUrl: String): ExtractorResult {
        return repository.reExtractExpired(sourceIframeUrl)
    }

    fun getSupportedServers(): List<String> {
        return repository.getRegisteredServers()
    }
}
