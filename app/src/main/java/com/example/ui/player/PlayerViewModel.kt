package com.example.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import com.example.data.extractor.ExtractorRegistry
import com.example.data.extractor.MimeTypeDetector
import com.example.data.model.ExtractionErrorType
import com.example.data.model.ExtractorResult
import com.example.data.model.MovieItem
import com.example.data.model.ServerOption
import com.example.data.model.VideoStream
import com.example.data.remote.BackendApiClient
import com.example.data.repository.VideoExtractorRepository
import com.example.data.repository.VideoExtractorRepositoryImpl
import com.example.domain.usecase.ExtractVideoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Estados de la interfaz de usuario para la pantalla del reproductor.
 */
sealed interface PlayerUiState {
    data object Idle : PlayerUiState

    data class Extracting(
        val url: String,
        val stepMessage: String = "Resolviendo enlace del servidor...",
        val serverName: String? = null
    ) : PlayerUiState

    data class Playing(
        val stream: VideoStream,
        val sourceIframeUrl: String,
        val extractionMs: Long,
        val isAutoReExtraction: Boolean = false
    ) : PlayerUiState

    data class Error(
        val message: String,
        val errorType: ExtractionErrorType,
        val canRetry: Boolean = true,
        val sourceIframeUrl: String? = null,
        val isPlaybackError: Boolean = false
    ) : PlayerUiState
}

/**
 * ViewModel que conecta la UI de Compose con el backend de scraping Ktor,
 * el caso de uso de extracción y el reproductor Media3 / ExoPlayer.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val mimeTypeDetector = MimeTypeDetector(httpClient)
    private val extractorRegistry = ExtractorRegistry(httpClient, mimeTypeDetector)

    private val repository: VideoExtractorRepository = VideoExtractorRepositoryImpl(
        context = application.applicationContext,
        extractorRegistry = extractorRegistry
    )

    private val extractVideoUseCase = ExtractVideoUseCase(repository)
    val backendApiClient = BackendApiClient(httpClient)

    // Estados de reproducción
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Estados de búsqueda en el Backend Ktor
    private val _searchResults = MutableStateFlow<List<MovieItem>>(emptyList())
    val searchResults: StateFlow<List<MovieItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedMovieServers = MutableStateFlow<List<ServerOption>>(emptyList())
    val selectedMovieServers: StateFlow<List<ServerOption>> = _selectedMovieServers.asStateFlow()

    private val _isLoadingServers = MutableStateFlow(false)
    val isLoadingServers: StateFlow<Boolean> = _isLoadingServers.asStateFlow()

    private val _activeMovieTitle = MutableStateFlow<String?>(null)
    val activeMovieTitle: StateFlow<String?> = _activeMovieTitle.asStateFlow()

    // Mantenemos la URL del iframe fuente en memoria para re-extracciones (NO se cachea la URL final que caduca)
    private var lastSourceIframeUrl: String = ""

    init {
        // Carga inicial de catálogo popular para demostración inmediata
        searchMovies("Inception")
    }

    /**
     * Busca películas a través del endpoint GET /search?q={query} del backend Ktor.
     */
    fun searchMovies(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = backendApiClient.searchMovies(trimmed)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Consulta los servidores e iframes de video a través del endpoint GET /servers?imdb={id}.
     */
    fun fetchServersForMovie(movie: MovieItem) {
        _activeMovieTitle.value = movie.title
        _isLoadingServers.value = true
        viewModelScope.launch {
            try {
                val servers = backendApiClient.getServers(movie.id)
                _selectedMovieServers.value = servers
            } catch (e: Exception) {
                _selectedMovieServers.value = emptyList()
            } finally {
                _isLoadingServers.value = false
            }
        }
    }

    fun clearSelectedServers() {
        _selectedMovieServers.value = emptyList()
        _activeMovieTitle.value = null
    }

    /**
     * Inicia la extracción y posterior reproducción de video a partir de un iframe.
     */
    fun extractAndPlay(iframeUrl: String) {
        val trimmed = iframeUrl.trim()
        if (trimmed.isEmpty()) return

        lastSourceIframeUrl = trimmed
        _uiState.value = PlayerUiState.Extracting(
            url = trimmed,
            stepMessage = "Analizando proveedor y extrayendo stream..."
        )

        viewModelScope.launch {
            when (val result = extractVideoUseCase(trimmed)) {
                is ExtractorResult.Success -> {
                    _uiState.value = PlayerUiState.Playing(
                        stream = result.stream,
                        sourceIframeUrl = result.sourceIframeUrl,
                        extractionMs = result.durationMs
                    )
                }
                is ExtractorResult.Error -> {
                    _uiState.value = PlayerUiState.Error(
                        message = result.message,
                        errorType = result.errorType,
                        canRetry = result.canRetry,
                        sourceIframeUrl = result.sourceIframeUrl
                    )
                }
                is ExtractorResult.Loading -> {
                    _uiState.value = PlayerUiState.Extracting(
                        url = trimmed,
                        stepMessage = result.stepMessage,
                        serverName = result.serverName
                    )
                }
                is ExtractorResult.Idle -> {
                    _uiState.value = PlayerUiState.Idle
                }
            }
        }
    }

    /**
     * Maneja errores reportados por Player.Listener de ExoPlayer.
     * Si el error es HTTP 403 Forbidden o token expirado, desencadena una re-extracción automática.
     */
    fun handlePlayerError(exception: PlaybackException, isExpiredToken: Boolean) {
        if (isExpiredToken && lastSourceIframeUrl.isNotEmpty()) {
            _uiState.value = PlayerUiState.Extracting(
                url = lastSourceIframeUrl,
                stepMessage = "Enlace caducado (HTTP 403). Re-extrayendo nuevo token..."
            )
            viewModelScope.launch {
                when (val result = extractVideoUseCase.reExtract(lastSourceIframeUrl)) {
                    is ExtractorResult.Success -> {
                        _uiState.value = PlayerUiState.Playing(
                            stream = result.stream,
                            sourceIframeUrl = result.sourceIframeUrl,
                            extractionMs = result.durationMs,
                            isAutoReExtraction = true
                        )
                    }
                    is ExtractorResult.Error -> {
                        _uiState.value = PlayerUiState.Error(
                            message = "No se pudo renovar el enlace expirado: ${result.message}",
                            errorType = ExtractionErrorType.EXPIRED_LINK,
                            canRetry = true,
                            sourceIframeUrl = lastSourceIframeUrl,
                            isPlaybackError = true
                        )
                    }
                    else -> Unit
                }
            }
        } else {
            _uiState.value = PlayerUiState.Error(
                message = "Error de reproducción Media3: ${exception.message ?: "Fallo en decodificador o red"}",
                errorType = ExtractionErrorType.PARSING_FAILED,
                canRetry = true,
                sourceIframeUrl = lastSourceIframeUrl,
                isPlaybackError = true
            )
        }
    }

    fun retryLastExtraction() {
        if (lastSourceIframeUrl.isNotEmpty()) {
            extractAndPlay(lastSourceIframeUrl)
        }
    }

    fun getSupportedServers(): List<String> {
        return extractVideoUseCase.getSupportedServers()
    }

    fun registerCustomRegexExtractor(
        name: String,
        domain: String,
        regexPattern: String,
        mimeType: String
    ) {
        extractorRegistry.registerRegexExtractor(
            name = name,
            domains = listOf(domain),
            videoUrlRegex = Regex(regexPattern),
            mimeType = mimeType
        )
    }

    fun clearState() {
        _uiState.value = PlayerUiState.Idle
    }
}
