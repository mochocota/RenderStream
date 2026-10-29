package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MovieItem
import com.example.data.model.ServerOption
import com.example.ui.player.PlayerUiState
import com.example.ui.player.PlayerViewModel
import com.example.ui.player.VideoPlayerView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamExtractorScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val selectedServers by viewModel.selectedMovieServers.collectAsStateWithLifecycle()
    val isLoadingServers by viewModel.isLoadingServers.collectAsStateWithLifecycle()
    val activeMovieTitle by viewModel.activeMovieTitle.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Scraper Películas, 1: Extractor Directo
    var searchQuery by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8") }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showServersDialog by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showBackendUrlDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DeepViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MoonStream",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Backend Ktor • MoonGetter • Media3",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBackendUrlDialog = true },
                        modifier = Modifier.testTag("backend_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Configurar Backend Ktor",
                            tint = NeonCyan
                        )
                    }
                    IconButton(
                        onClick = { showServersDialog = true },
                        modifier = Modifier.testTag("servers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Servidores soportados",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Guía y arquitectura",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Pestañas: 1. Scraper de Películas Backend | 2. Extractor Directo
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonCyan
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scraper Películas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("URL Iframe Directo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vista según la pestaña activa
            if (selectedTabIndex == 0) {
                // Modo 1: Búsqueda en backend Ktor
                MovieScraperSection(
                    searchQuery = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        viewModel.searchMovies(it)
                    },
                    onSearch = { viewModel.searchMovies(searchQuery) },
                    isSearching = isSearching,
                    searchResults = searchResults,
                    onSelectMovie = { movie ->
                        viewModel.fetchServersForMovie(movie)
                    }
                )
            } else {
                // Modo 2: Extractor directo por URL
                PresetsRow(
                    onSelectPreset = { presetUrl ->
                        inputUrl = presetUrl
                        viewModel.extractAndPlay(presetUrl)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                InputBar(
                    url = inputUrl,
                    onUrlChange = { inputUrl = it },
                    onClear = { inputUrl = "" },
                    onPaste = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!clip.isNullOrBlank()) {
                            inputUrl = clip.trim()
                            Toast.makeText(context, "URL pegada del portapapeles", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExtract = { viewModel.extractAndPlay(inputUrl) },
                    isExtracting = uiState is PlayerUiState.Extracting
                )
            }

            // Modal / Card de Servidores extraídos para la película seleccionada
            if (selectedServers.isNotEmpty() || isLoadingServers) {
                Spacer(modifier = Modifier.height(16.dp))
                ServersSelectorCard(
                    movieTitle = activeMovieTitle ?: "Película",
                    servers = selectedServers,
                    isLoading = isLoadingServers,
                    onSelectServer = { server ->
                        inputUrl = server.iframeUrl
                        viewModel.extractAndPlay(server.iframeUrl)
                    },
                    onClose = { viewModel.clearSelectedServers() }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reproductor y Estado de Extracción
            when (val state = uiState) {
                is PlayerUiState.Idle -> {
                    IdleStateCard(
                        onSelectTestStream = {
                            val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                            inputUrl = testUrl
                            viewModel.extractAndPlay(testUrl)
                        }
                    )
                }
                is PlayerUiState.Extracting -> {
                    ExtractionProgressCard(
                        url = state.url,
                        stepMessage = state.stepMessage,
                        serverName = state.serverName
                    )
                }
                is PlayerUiState.Playing -> {
                    Column {
                        VideoPlayerView(
                            stream = state.stream,
                            onPlayerError = { exception, isExpired ->
                                viewModel.handlePlayerError(exception, isExpired)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        StreamInspectorCard(
                            state = state,
                            onCopyUrl = {
                                copyToClipboard(context, state.stream.url, "URL de video copiada")
                            },
                            onRenewToken = { viewModel.retryLastExtraction() }
                        )
                    }
                }
                is PlayerUiState.Error -> {
                    ErrorCard(
                        state = state,
                        onRetry = { viewModel.retryLastExtraction() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = { showAddCustomDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_custom_extractor_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Extractor Personalizado (API)")
            }
        }
    }

    // Diálogos
    if (showServersDialog) {
        ServersDialog(
            servers = viewModel.getSupportedServers(),
            onDismiss = { showServersDialog = false }
        )
    }

    if (showInfoDialog) {
        ArchitectureInfoDialog(
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showAddCustomDialog) {
        AddCustomExtractorDialog(
            onDismiss = { showAddCustomDialog = false },
            onRegister = { name, domain, regex, mime ->
                viewModel.registerCustomRegexExtractor(name, domain, regex, mime)
                Toast.makeText(context, "Extractor '$name' registrado", Toast.LENGTH_SHORT).show()
                showAddCustomDialog = false
            }
        )
    }

    if (showBackendUrlDialog) {
        BackendSettingsDialog(
            currentUrl = viewModel.backendApiClient.customBackendUrl,
            onTestConnection = { testUrl ->
                val prev = viewModel.backendApiClient.customBackendUrl
                viewModel.backendApiClient.customBackendUrl = testUrl
                val result = viewModel.backendApiClient.checkHealth()
                if (result.isFailure) {
                    viewModel.backendApiClient.customBackendUrl = prev
                }
                result
            },
            onSave = { newUrl ->
                viewModel.backendApiClient.customBackendUrl = newUrl
                Toast.makeText(context, "URL del backend actualizada", Toast.LENGTH_SHORT).show()
                showBackendUrlDialog = false
            },
            onDismiss = { showBackendUrlDialog = false }
        )
    }
}

@Composable
fun MovieScraperSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    searchResults: List<MovieItem>,
    onSelectMovie: (MovieItem) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "APIs de Proveedores de Embeds (VidSrc / Embed.su / AutoEmbed)",
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Consulta directa por título o ID de IMDb sin lidiar con 10 servidores rotos.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movie_search_input"),
                placeholder = { Text("Escribe una película o IMDb ID (e.g. Inception, tt1375666)", fontSize = 13.sp, color = TextMuted) },
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = NeonCyan)
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isSearching) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Buscando en sitio de películas...", color = TextSecondary, fontSize = 13.sp)
                }
            } else if (searchResults.isNotEmpty()) {
                Text(
                    text = "Películas encontradas (${searchResults.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(searchResults) { movie ->
                        MovieCardItem(
                            movie = movie,
                            onClick = { onSelectMovie(movie) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCardItem(
    movie: MovieItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .testTag("movie_item_${movie.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (movie.id.startsWith("tt")) {
                Text(
                    text = "ID: ${movie.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = movie.year?.toString() ?: "Película",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                if (movie.rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = movie.rating.toString(), fontSize = 11.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepViolet),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Ver Servidores", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ServersSelectorCard(
    movieTitle: String,
    servers: List<ServerOption>,
    isLoading: Boolean,
    onSelectServer: (ServerOption) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("servers_selector_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "APIs de Proveedores de Embeds (Sin Anuncios)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = movieTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Consultando APIs de Embeds (VidSrc/Embed.su/AutoEmbed)...", color = TextSecondary)
                }
            } else {
                Text(
                    text = "Selecciona un proveedor para extraer el flujo HLS .m3u8 directo a Media3 ExoPlayer:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    servers.forEach { server ->
                        ServerItemRow(
                            server = server,
                            onClick = { onSelectServer(server) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServerItemRow(
    server: ServerOption,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = server.serverName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                ChipBadge(label = server.quality, color = DeepViolet)
                Spacer(modifier = Modifier.width(4.dp))
                ChipBadge(label = server.language, color = WarningAmber)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = server.iframeUrl,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Extraer", color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun InputBar(
    url: String,
    onUrlChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onExtract: () -> Unit,
    isExtracting: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "URL del Iframe o Servidor",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("iframe_url_input"),
                placeholder = {
                    Text(
                        "https://streamtape.com/e/... o https://voe.sx/e/...",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                },
                trailingIcon = {
                    Row {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextMuted)
                            }
                        }
                        IconButton(onClick = onPaste) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Pegar", tint = NeonCyan)
                        }
                    }
                },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onExtract,
                enabled = url.isNotBlank() && !isExtracting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("extract_and_play_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF00363D),
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isExtracting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF00363D),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Extrayendo...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Extraer Enlace y Reproducir",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PresetsRow(onSelectPreset: (String) -> Unit) {
    val presets = listOf(
        "VidSrc (Inception)" to "https://vidsrc.xyz/embed/movie/tt1375666",
        "Embed.su (Interstellar)" to "https://embed.su/embed/movie/tt0816692",
        "AutoEmbed (The Matrix)" to "https://player.autoembed.cc/embed/movie/tt0133093",
        "MultiEmbed (Dark Knight)" to "https://multiembed.mov/?video_id=tt0468569",
        "Akamai Live HLS" to "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
        "Fastly MP4 1080p" to "https://vjs.zencdn.net/v/oceans.mp4"
    )

    Column {
        Text(
            text = "Streams de prueba y servidores soportados:",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (name, url) ->
                FilledTonalButton(
                    onClick = { onSelectPreset(url) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkSurfaceCard,
                        contentColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("preset_${name.replace(" ", "_")}")
                ) {
                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ExtractionProgressCard(
    url: String,
    stepMessage: String,
    serverName: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extraction_progress_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = NeonCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stepMessage,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (serverName != null) {
                Text(
                    text = "Servidor detectado: $serverName",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IdleStateCard(onSelectTestStream: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("idle_state_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Listo para extraer y reproducir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Busca una película en el catálogo superior o pega la URL del iframe de Streamtape, Voe, Filemoon o StreamWish.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSelectTestStream,
                colors = ButtonDefaults.buttonColors(containerColor = DeepViolet),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cargar Stream de Prueba HLS (.m3u8)")
            }
        }
    }
}

@Composable
fun StreamInspectorCard(
    state: PlayerUiState.Playing,
    onCopyUrl: () -> Unit,
    onRenewToken: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stream_inspector_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reproduciendo (${state.stream.server})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
                Text(
                    text = "${state.extractionMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipBadge(
                    label = if (state.stream.mimeType.contains("mpegurl")) "HLS (.m3u8)" else "MP4 Directo",
                    color = NeonCyan
                )
                ChipBadge(label = state.stream.quality, color = DeepViolet)
                if (state.isAutoReExtraction) {
                    ChipBadge(label = "Token Renovado", color = WarningAmber)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = state.stream.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = onCopyUrl,
                    modifier = Modifier.size(32.dp).testTag("copy_stream_url_button")
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar URL",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (state.stream.headers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Headers inyectados en Media3:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                state.stream.headers.forEach { (k, v) ->
                    Text(
                        text = "$k: $v",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onRenewToken,
                modifier = Modifier.align(Alignment.End).testTag("renew_token_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Re-extraer token nuevo", color = NeonCyan, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ErrorCard(
    state: PlayerUiState.Error,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = state.errorType.userTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (state.canRetry) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("retry_extraction_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reintentar Extracción")
                }
            }
        }
    }
}

@Composable
fun ChipBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ServersDialog(servers: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = NeonCyan) }
        },
        title = {
            Text("Servidores Soportados", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Esta app y MoonGetter extraen los enlaces directos (.m3u8 y .mp4) de los siguientes servidores:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                servers.forEach { server ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(server, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = DarkSurface,
        titleContentColor = TextPrimary
    )
}

@Composable
fun ArchitectureInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido", color = NeonCyan) }
        },
        title = {
            Text("Arquitectura End-to-End", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Backend Ktor (Scraper):\nBusca películas y extrae las URLs de iframes de servidores (Streamtape, Voe, Filemoon) con Rate Limiting y OkHttp/Jsoup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "2. MoonGetter Android:\nRecibe la URL del iframe, resuelve el HTML y decodifica el stream directo (.m3u8 o .mp4), saltando los anuncios del reproductor embebido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "3. ExoPlayer / Media3:\nReproduce con User-Agent y Referer inyectados vía OkHttpDataSource.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        containerColor = DarkSurface,
        titleContentColor = TextPrimary
    )
}

@Composable
fun AddCustomExtractorDialog(
    onDismiss: () -> Unit,
    onRegister: (name: String, domain: String, regex: String, mime: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var regex by remember { mutableStateOf("""['"]file['"]\s*:\s*['"](https?://[^'"]+\.m3u8)""") }
    var isHls by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && domain.isNotBlank()) {
                        val mime = if (isHls) androidx.media3.common.MimeTypes.APPLICATION_M3U8 else androidx.media3.common.MimeTypes.VIDEO_MP4
                        onRegister(name, domain, regex, mime)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Registrar", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
        title = { Text("Registrar Extractor Personalizado", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Servidor (e.g. MyServer)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Dominio (e.g. myserver.com)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = regex,
                    onValueChange = { regex = it },
                    label = { Text("Regex de captura de video") },
                    maxLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Formato: ", color = TextSecondary)
                    TextButton(onClick = { isHls = !isHls }) {
                        Text(if (isHls) "HLS (.m3u8)" else "MP4 Directo", color = NeonCyan)
                    }
                }
            }
        },
        containerColor = DarkSurface,
        titleContentColor = TextPrimary
    )
}

@Composable
fun BackendSettingsDialog(
    currentUrl: String,
    onTestConnection: suspend (String) -> Result<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(url.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Guardar", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
        title = { Text("Configurar Backend Ktor", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Introduce la URL de tu backend desplegado en Render (o tu IP local):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it 
                        testResult = null
                    },
                    label = { Text("URL Base del Backend") },
                    placeholder = { Text("https://tu-servicio.onrender.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            isTesting = true
                            testResult = null
                            scope.launch {
                                val res = onTestConnection(url.trim())
                                isTesting = false
                                testResult = if (res.isSuccess) {
                                    "🟢 ${res.getOrNull()}"
                                } else {
                                    "🔴 ${res.exceptionOrNull()?.localizedMessage ?: "Fallo al conectar"}"
                                }
                            }
                        },
                        enabled = !isTesting && url.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DarkSurfaceCard,
                            contentColor = NeonCyan
                        )
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Probando...", fontSize = 12.sp)
                        } else {
                            Text("Probar Conexión (GET /)", fontSize = 12.sp)
                        }
                    }
                }

                if (testResult != null) {
                    Text(
                        text = testResult!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testResult!!.startsWith("🟢")) SuccessGreen else ErrorRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        containerColor = DarkSurface,
        titleContentColor = TextPrimary
    )
}

private fun copyToClipboard(context: Context, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Direct Stream URL", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}
