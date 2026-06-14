@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pelotcl.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pelotcl.app.generic.data.config.AppConfigLoader
import com.pelotcl.app.generic.data.models.geojson.FeatureCollection
import com.pelotcl.app.generic.data.models.geojson.StopCollection
import com.pelotcl.app.generic.data.models.geojson.StopFeature
import com.pelotcl.app.generic.data.models.itinerary.SelectedStop
import com.pelotcl.app.generic.data.models.stops.Favorite
import com.pelotcl.app.generic.data.models.stops.StationInfo
import com.pelotcl.app.generic.data.models.ui.AllSchedulesInfo
import com.pelotcl.app.generic.data.repository.itinerary.itinerary.ItineraryPreferencesRepository
import com.pelotcl.app.generic.data.repository.offline.mapstyle.MapStyleCompat
import com.pelotcl.app.generic.data.repository.offline.mapstyle.MapStyleRepository
import com.pelotcl.app.generic.service.TransportServiceProvider
import com.pelotcl.app.generic.ui.components.MapCanvas
import com.pelotcl.app.generic.ui.components.favorites.AddFavoriteDialog
import com.pelotcl.app.generic.ui.components.favorites.FavoritesBar
import com.pelotcl.app.generic.ui.components.search.TransportSearchBar
import com.pelotcl.app.generic.ui.screens.Destination
import com.pelotcl.app.generic.ui.screens.plan.AllSchedulesSheetContent
import com.pelotcl.app.generic.ui.screens.plan.LineDetailsBottomSheet
import com.pelotcl.app.generic.ui.screens.plan.LineInfo
import com.pelotcl.app.generic.ui.screens.plan.LinesBottomSheet
import com.pelotcl.app.generic.ui.screens.plan.MapStyleSelectionSheet
import com.pelotcl.app.generic.ui.screens.plan.StationSheetContent
import com.pelotcl.app.generic.ui.screens.plan.itinerary.InlineItinerarySheetContent
import com.pelotcl.app.generic.ui.screens.settings.ItinerarySettingsScreen
import com.pelotcl.app.generic.ui.screens.settings.OfflineSettingsScreen
import com.pelotcl.app.generic.ui.screens.settings.SettingsScreen
import com.pelotcl.app.generic.ui.screens.settings.about.ContactScreen
import com.pelotcl.app.generic.ui.screens.settings.about.CreditsScreen
import com.pelotcl.app.generic.ui.screens.settings.about.LegalScreen
import com.pelotcl.app.generic.ui.theme.AccentColor
import com.pelotcl.app.generic.ui.theme.PeloTheme
import com.pelotcl.app.generic.ui.theme.PrimaryColor
import com.pelotcl.app.generic.ui.theme.SecondaryColor
import com.pelotcl.app.generic.ui.viewmodel.TransportLinesUiState
import com.pelotcl.app.generic.ui.viewmodel.TransportStopsUiState
import com.pelotcl.app.generic.ui.viewmodel.TransportViewModel
import com.pelotcl.app.generic.utils.location.LocationProvider
import com.pelotcl.app.platform.LocalPlatformContext
import com.pelotcl.app.platform.Log
import com.pelotcl.app.platform.appVersionName
import org.maplibre.spatialk.geojson.Position

/**
 * Shared application root (commonMain) — the cross-platform Plan UI assembled from common
 * building blocks (MapCanvas, TransportSearchBar, the sheets, SettingsScreen, …).
 *
 * Each platform provides a [com.pelotcl.app.platform.PlatformContext] via [LocalPlatformContext]
 * and hosts this composable: iOS via `MainViewController()`; Android (eventually) via `MainActivity`.
 * `TransportServiceProvider.initialize` stands in for the Android `PeloApplication.onCreate` bootstrap.
 */
@Composable
fun App() {
    PeloTheme {
        val context = LocalPlatformContext.current
        val viewModel = remember {
            try {
                TransportServiceProvider.initialize(context)
                TransportViewModel(context)
            } catch (t: Throwable) {
                Log.e("PeloApp", "Transport data init failed: ${t.message}")
                null
            }
        }

        // Raptor backs stop/line search; load its .bin assets up front so search works.
        LaunchedEffect(viewModel) {
            val vm = viewModel ?: return@LaunchedEffect
            runCatching { vm.raptorRepository.initialize() }
                .onFailure { Log.e("PeloApp", "Raptor init failed: ${it.message}") }
        }

        if (viewModel != null) {
            RootScaffold(viewModel)
        } else {
            MapCanvas(modifier = Modifier.fillMaxSize(), styleUrl = MapStyleCompat.POSITRON.styleUrl)
        }
    }
}

@Composable
private fun RootScaffold(viewModel: TransportViewModel) {
    val context = LocalPlatformContext.current
    var selectedTab by remember { mutableStateOf(Destination.PLAN) }
    var showLinesSheet by remember { mutableStateOf(false) }

    // Contextual sheet state — shown in a NON-blocking BottomSheetScaffold (map usable behind).
    var selectedLine by remember { mutableStateOf<LineInfo?>(null) }
    var lineDirection by remember { mutableIntStateOf(0) }
    var selectedStation by remember { mutableStateOf<StationInfo?>(null) }
    var allSchedules by remember { mutableStateOf<AllSchedulesInfo?>(null) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }

    val availableDirections by viewModel.availableDirections.collectAsState(initial = emptyList())
    val headsigns by viewModel.headsigns.collectAsState(initial = emptyMap())
    val linesUiState by viewModel.uiState.collectAsState()
    val stopsUiState by viewModel.stopsUiState.collectAsState()
    val userFavorites by viewModel.userFavorites.collectAsState(initial = emptyList())
    val stops = (stopsUiState as? TransportStopsUiState.Success)?.stops

    // Device location — shared by the map's user dot and the itinerary departure.
    var userLocation by remember { mutableStateOf<Position?>(null) }
    val locationProvider = remember { LocationProvider(context) }
    DisposableEffect(Unit) {
        locationProvider.startUpdates { p -> userLocation = Position(latitude = p.latitude, longitude = p.longitude) }
        onDispose { locationProvider.stopUpdates() }
    }

    // Itinerary state (arrival = chosen stop, departure = nearest stop to the user).
    var itineraryArrivalName by remember { mutableStateOf<String?>(null) }
    var itineraryDeparture by remember { mutableStateOf<SelectedStop?>(null) }
    var itineraryArrival by remember { mutableStateOf<SelectedStop?>(null) }
    var itineraryNearby by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(itineraryArrivalName) {
        val arrivalName = itineraryArrivalName ?: return@LaunchedEffect
        runCatching { viewModel.raptorRepository.resolveStopIdsByName(arrivalName) }
            .getOrDefault(emptyList()).takeIf { it.isNotEmpty() }
            ?.let { itineraryArrival = SelectedStop(name = arrivalName, stopIds = it) }
        val loc = userLocation
        if (loc != null) {
            val nearest = runCatching { viewModel.raptorRepository.findNearestStops(loc.latitude, loc.longitude, 5) }
                .getOrDefault(emptyList())
            val names = nearest.map { it.name }.distinct()
            itineraryNearby = names
            names.firstOrNull()?.let { depName ->
                runCatching { viewModel.raptorRepository.resolveStopIdsByName(depName) }
                    .getOrDefault(emptyList()).takeIf { it.isNotEmpty() }
                    ?.let { itineraryDeparture = SelectedStop(name = depName, stopIds = it) }
            }
        }
    }

    val closeSheet = { selectedStation = null; selectedLine = null; allSchedules = null }
    fun showStation(name: String) {
        val stop = stops?.firstOrNull { it.properties.nom.equals(name, ignoreCase = true) }
        selectedStation = if (stop != null) {
            StationInfo(
                nom = stop.properties.nom,
                lignes = viewModel.parseLineCodesFromDesserte(stop.properties.desserte),
                desserte = stop.properties.desserte,
                stopIds = listOf(stop.properties.id),
            )
        } else {
            StationInfo(nom = name, lignes = emptyList())
        }
        selectedLine = null
        allSchedules = null
    }
    fun showLine(name: String) {
        viewModel.selectLine(name)
        lineDirection = 0
        selectedLine = LineInfo(lineName = name, currentStationName = "")
        selectedStation = null
        allSchedules = null
    }
    fun startItinerary(name: String) {
        itineraryArrival = null
        itineraryDeparture = null
        itineraryArrivalName = name
        closeSheet()
    }

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
    val bsScaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val hasSheet = selectedStation != null || selectedLine != null || allSchedules != null
    LaunchedEffect(hasSheet) {
        if (hasSheet) bottomSheetState.partialExpand() else bottomSheetState.hide()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = PrimaryColor) {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = when (destination) {
                            Destination.LINES -> showLinesSheet
                            Destination.PLAN -> selectedTab == Destination.PLAN && !showLinesSheet
                            Destination.SETTINGS -> selectedTab == Destination.SETTINGS
                        },
                        onClick = {
                            when (destination) {
                                Destination.LINES -> { selectedTab = Destination.PLAN; showLinesSheet = true }
                                Destination.PLAN -> { selectedTab = Destination.PLAN; showLinesSheet = false }
                                Destination.SETTINGS -> { selectedTab = Destination.SETTINGS; showLinesSheet = false }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.contentDescription) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = AccentColor,
                            selectedIconColor = SecondaryColor,
                            unselectedIconColor = SecondaryColor,
                            selectedTextColor = SecondaryColor,
                            unselectedTextColor = SecondaryColor,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())
        if (selectedTab == Destination.SETTINGS) {
            SettingsTab(viewModel, Modifier.fillMaxSize()) { selectedTab = Destination.PLAN }
        } else {
            BottomSheetScaffold(
                modifier = contentModifier,
                scaffoldState = bsScaffoldState,
                sheetPeekHeight = if (hasSheet) 360.dp else 0.dp,
                sheetContent = {
                    val sc = allSchedules
                    val ln = selectedLine
                    val st = selectedStation
                    when {
                        sc != null -> AllSchedulesSheetContent(
                            allSchedulesInfo = sc,
                            stationName = selectedLine?.currentStationName ?: "",
                            selectedDirection = lineDirection,
                            availableDirections = availableDirections,
                            headsigns = headsigns,
                            onDirectionChange = { lineDirection = it },
                            onBack = { allSchedules = null },
                        )
                        ln != null -> LineDetailsBottomSheet(
                            viewModel = viewModel,
                            lineInfo = ln,
                            sheetState = null,
                            selectedDirection = lineDirection,
                            onDirectionChange = { lineDirection = it },
                            onDismiss = closeSheet,
                            // Tapping a stop loads its schedules; "back to station" shows the
                            // multi-line station sheet for that stop.
                            onStopClick = { stopName -> selectedLine = selectedLine?.copy(currentStationName = stopName) },
                            onBackToStation = {
                                val s = selectedLine?.currentStationName
                                if (!s.isNullOrBlank()) showStation(s) else closeSheet()
                            },
                            onShowAllSchedules = { lineName, directionName, schedules ->
                                allSchedules = AllSchedulesInfo(lineName = lineName, directionName = directionName, schedules = schedules)
                            },
                        )
                        st != null -> StationSheetContent(
                            stationInfo = st,
                            viewModel = viewModel,
                            onDismiss = closeSheet,
                            onDepartureClick = { lineName, _, _ -> showLine(lineName) },
                            isFavoriteStop = userFavorites.any { it.stopName.equals(st.nom, ignoreCase = true) },
                            onToggleFavoriteStop = {
                                val existing = userFavorites.firstOrNull { it.stopName.equals(st.nom, ignoreCase = true) }
                                if (existing != null) viewModel.removeUserFavorite(existing.id) else showAddFavoriteDialog = true
                            },
                            onAddFavoriteClick = { showAddFavoriteDialog = true },
                            onItineraryClick = { stopName -> startItinerary(stopName) },
                        )
                    }
                },
            ) {
                PlanContent(
                    viewModel = viewModel,
                    stops = stops,
                    userLocation = userLocation,
                    userFavorites = userFavorites,
                    onStopSelected = { showStation(it) },
                    onLineSelected = { showLine(it) },
                    onAddFavoriteClick = { showAddFavoriteDialog = true },
                )
            }
        }
    }

    // Lignes: a blocking modal sheet over the map (the one sheet meant to be blocking).
    if (showLinesSheet) {
        val linesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val allLines = remember(linesUiState, stopsUiState) { viewModel.getAllAvailableLines() }
        ModalBottomSheet(onDismissRequest = { showLinesSheet = false }, sheetState = linesSheetState) {
            LinesBottomSheet(
                allLines = allLines,
                onLineClick = { lineName -> showLinesSheet = false; showLine(lineName) },
                viewModel = viewModel,
            )
        }
    }

    // Itinerary results (kept as a modal for now; the two-search-bar layout comes next).
    if (itineraryArrivalName != null) {
        val itinSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val closeItinerary: () -> Unit = {
            itineraryArrivalName = null
            itineraryArrival = null
            itineraryDeparture = null
            itineraryNearby = emptyList()
        }
        ModalBottomSheet(onDismissRequest = closeItinerary, sheetState = itinSheetState) {
            InlineItinerarySheetContent(
                viewModel = viewModel,
                departureStop = itineraryDeparture,
                arrivalStop = itineraryArrival,
                maxHeight = 600.dp,
                nearbyDepartureStops = itineraryNearby,
                onDepartureFallbackSelected = { itineraryDeparture = it },
                onJourneysChanged = { },
                onSelectedJourneyChanged = { },
                onStartNavigation = { },
                onClose = closeItinerary,
                onRequestExpandSheet = { },
            )
        }
    }

    if (showAddFavoriteDialog) {
        AddFavoriteDialog(
            onDismiss = { showAddFavoriteDialog = false },
            onFavoriteCreated = { name, iconName, stopName ->
                viewModel.addUserFavorite(name, iconName, stopName)
                showAddFavoriteDialog = false
            },
            viewModel = viewModel,
        )
    }
}

/**
 * Presentational map screen: the MapCanvas + the search/favorites/map-style overlay. All taps
 * bubble up to [RootScaffold] (which owns the contextual sheets), so this composable holds no
 * sheet state.
 */
@Composable
private fun PlanContent(
    viewModel: TransportViewModel,
    stops: List<StopFeature>?,
    userLocation: Position?,
    userFavorites: List<Favorite>,
    onStopSelected: (String) -> Unit,
    onLineSelected: (String) -> Unit,
    onAddFavoriteClick: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val linesState by viewModel.uiState.collectAsState()
    val lineRules = remember { TransportServiceProvider.getTransportLineRules() }
    val mapStyleRepo = remember { MapStyleRepository(context, TransportServiceProvider.getMapStyleConfig()) }
    var selectedMapStyle by remember { mutableStateOf(mapStyleRepo.getSelectedStyle()) }
    var showStyleSheet by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    val allLines = when (val s = linesState) {
        is TransportLinesUiState.Success -> s.lines
        is TransportLinesUiState.PartialSuccess -> s.lines
        else -> null
    }
    // Only the strong lines (metro/tram/funicular) on the map, like Android — every bus trace is laggy.
    val strongLines = allLines?.filter { lineRules.isStrongLine(it.properties.lineName) }

    Box(Modifier.fillMaxSize()) {
        MapCanvas(
            modifier = Modifier.fillMaxSize(),
            styleUrl = selectedMapStyle.styleUrl,
            initialLatitude = 45.75,
            initialLongitude = 4.85,
            initialZoom = 12.0,
            lines = strongLines?.let { FeatureCollection(features = it) },
            stops = stops?.let { StopCollection(features = it) },
            userLocation = userLocation,
            onStopClick = { nom -> onStopSelected(nom) },
            onLineClick = { lineName -> onLineSelected(lineName) },
        )

        // The black search zone only bleeds behind the status bar / notch while the search is open.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(if (searchExpanded) Modifier.background(Color.Black) else Modifier)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Column {
                TransportSearchBar(
                    onSearchStops = { q -> viewModel.searchStops(q) },
                    onSearchLines = { q -> viewModel.searchLines(q) },
                    onExpandedChange = { searchExpanded = it },
                    onStopPrimary = { result -> onStopSelected(result.stopName) },
                    onLineSelected = { line -> onLineSelected(line.lineName) },
                )
                if (!searchExpanded) {
                    FavoritesBar(
                        favorites = userFavorites,
                        onAddFavoriteClick = onAddFavoriteClick,
                        onFavoriteClick = { fav -> onStopSelected(fav.stopName) },
                        onRemoveFavoriteClick = { fav -> viewModel.removeUserFavorite(fav.id) },
                    )
                    // Map-style button, below the favorites row (right-aligned).
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        FloatingActionButton(
                            onClick = { showStyleSheet = true },
                            modifier = Modifier.size(48.dp),
                            containerColor = PrimaryColor,
                        ) {
                            Icon(Icons.Filled.Layers, contentDescription = "Style de carte", tint = SecondaryColor)
                        }
                    }
                }
            }
        }
    }

    if (showStyleSheet) {
        MapStyleSelectionSheet(
            isOffline = false,
            downloadedMapStyles = emptySet(),
            selectedMapStyle = selectedMapStyle,
            onDismiss = { showStyleSheet = false },
            onStyleSelected = { style ->
                selectedMapStyle = style
                mapStyleRepo.saveSelectedStyle(style)
                showStyleSheet = false
            },
        )
    }
}

@Composable
private fun SettingsTab(viewModel: TransportViewModel, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalPlatformContext.current
    var route by remember { mutableStateOf("root") }
    val backToRoot = { route = "root" }
    // Full-screen (no inset padding) so settings covers the whole screen, behind the notch too.
    Box(modifier) {
        when (route) {
            "legal" -> LegalScreen(
                legalSections = remember { AppConfigLoader.getConfig().about.legalSections },
                onBackClick = backToRoot,
            )
            "credits" -> CreditsScreen(onBackClick = backToRoot)
            "contact" -> ContactScreen(onBackClick = backToRoot)
            "offline" -> OfflineSettingsScreen(viewModel = viewModel, onBackClick = backToRoot)
            "itinerary" -> {
                val cfg = remember { AppConfigLoader.getConfig().itinerarySettings }
                val prefs = remember { ItineraryPreferencesRepository(context) }
                ItinerarySettingsScreen(
                    screenTitle = cfg.screenTitle,
                    sectionTitle = cfg.sectionTitle,
                    options = cfg.options,
                    onBackClick = backToRoot,
                    onOptionToggle = { key, enabled -> prefs.setOptionEnabled(key, enabled) },
                    getInitialOptionState = { opt -> prefs.isOptionEnabled(opt.key, opt.defaultEnabled) },
                )
            }
            else -> SettingsScreen(
                versionName = appVersionName(context),
                onBackClick = onBack,
                onItineraryClick = { route = "itinerary" },
                onLegalClick = { route = "legal" },
                onCreditsClick = { route = "credits" },
                onContactClick = { route = "contact" },
                onOfflineClick = { route = "offline" },
            )
        }
    }
}
