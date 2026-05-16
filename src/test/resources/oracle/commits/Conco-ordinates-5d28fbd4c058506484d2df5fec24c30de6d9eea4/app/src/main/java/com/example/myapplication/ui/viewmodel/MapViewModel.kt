package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.analytics.AnalyticsProvider
import com.example.myapplication.analytics.NoOpAnalyticsProvider
import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import androidx.lifecycle.viewModelScope
import com.example.myapplication.logic.CampusNavigationEngine
import com.example.myapplication.logic.DateUtils
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.HybridSearchProvider
import com.example.myapplication.logic.ShuttleRouteProvider
import com.example.myapplication.logic.ShuttleService
import com.example.myapplication.logic.SimpleMockRouteProvider
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.models.NavigationState
import com.example.myapplication.ui.models.IndoorJourneyState
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.example.myapplication.logic.IndoorJourneyHandler
import com.example.myapplication.data.indoor.BuildingEntrance


/**
 * [shuttleService] has no default value so callers must inject a concrete
 * implementation.  This makes the ViewModel truly modular – swap in a
 * [MockShuttleService] for tests without touching this class.         [#7]
 *
 * [ShuttleRouteProvider] is constructed internally but receives its two
 * dependencies via constructor injection, keeping it testable as well.  [#1][#2]
 *
 */
class MapViewModel(
    private val locationProvider: com.example.myapplication.logic.LocationProvider? = null,
    private val routeProvider: com.example.myapplication.logic.RouteProvider? = null,
    private val shuttleService: ShuttleService,
    private val analyticsProvider: AnalyticsProvider = NoOpAnalyticsProvider,
    private val navigationEngine: com.example.myapplication.logic.NavigationEngine = CampusNavigationEngine()
) : ViewModel() {

    private val shuttleRouteProvider = ShuttleRouteProvider(
        shuttleService      = shuttleService,
        googleRouteProvider = routeProvider ?: SimpleMockRouteProvider()
    )

    // Pre-indexed for O(1) building code lookup — avoids flatMap on every navigation call
    private val buildingIndex: Map<String, com.example.myapplication.data.Building> by lazy {
        CampusRepo.getAllCampuses()
            .flatMap { it.buildings }
            .associateBy { it.code.lowercase() }
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    var searchQuery by mutableStateOf("")
        private set

    var currentNavBearing by mutableStateOf(0f)
        private set

    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set

    private var searchProvider: HybridSearchProvider? = null
    private var isManualCampusSelection = false

    // ── Map UI state ───────────────────────────────────────────────────────────

    var uiBuildingState by mutableStateOf(BuildingUiState())
        private set

    var currentCampus by mutableStateOf<Campus?>(null)
        private set

    private var lastProcessedLocation: LatLng? = null

    var highlightedBuildingName by mutableStateOf<String?>(null)
        private set

    var mapEvent by mutableStateOf<LatLng?>(null)
        private set

    var activeSearchField by mutableStateOf("main")
        private set

    var indoorJourneyState by mutableStateOf(IndoorJourneyState())
        private set

    /**
     * The indoor building/floor/startNode the UI should currently display.
     * Derived from [indoorJourneyState] phase — the UI observes this instead
     * of computing it inside the composable (avoids dual source of truth).
     *
     * Null means no indoor map should be shown for the journey flow.
     */
    val indoorNavTarget: Triple<String, Int, String>?
        get() = when (val phase = indoorJourneyState.phase) {
            is IndoorJourneyPhase.IndoorToExit        ->
                Triple(phase.buildingCode, phase.floor, phase.startNodeId)
            is IndoorJourneyPhase.IndoorToDestination ->
                Triple(phase.buildingCode, phase.startFloor, phase.startNodeId)
            else -> null
        }

    private val indoorBuildingCodes = setOf("CC", "H", "MB", "EV")

    fun handleMapTap(building: Building?, imageUrl: String? = null) {
        if (uiBuildingState.mode == MapUIMode.DIRECTIONS) return
        uiBuildingState = BuildingUiState(
            isVisible    = building != null,
            building     = building,
            address      = building?.address,
            imageUrl     = imageUrl,
            hasIndoorMap = building?.code?.uppercase() in indoorBuildingCodes
        )
    }

    fun onCampusSelected(name: String) {
        val found = CampusRepo.getCampusByName(name)
        if (found != null) {
            isManualCampusSelection = true
            currentCampus = found
            highlightedBuildingName = null
        }
    }

    fun processLocationUpdate(userLocation: LatLng, isForce: Boolean = false) {
        if (isForce) isManualCampusSelection = false
        lastProcessedLocation = userLocation

        // 1. Handle Campus State
        updateCampusState(userLocation)

        // 2. Handle Building Highlighting
        val detected = CampusRepo.getCampus(userLocation)
        val buildingAtPos = detected?.buildings?.firstOrNull { building ->
            com.google.maps.android.PolyUtil.containsLocation(userLocation, building.getGoogleOutline(), false)
        }
        highlightedBuildingName = buildingAtPos?.name

        // 3. Handle Navigation Logic (Only if in ACTIVE_NAVIGATION mode)
        if (uiBuildingState.mode == MapUIMode.ACTIVE_NAVIGATION) {
            handleActiveNavigationUpdate(userLocation, isForce)
        }

        // 4. Indoor journey: auto-detect arrival near destination building
        val journeyPhase = indoorJourneyState.phase
        if (journeyPhase is IndoorJourneyPhase.Outdoor) {
            if (IndoorJourneyHandler.isNearBuilding(userLocation, journeyPhase.destRoom.buildingCode)) {
                indoorJourneyState = IndoorJourneyState(
                    phase = IndoorJourneyHandler.onNearDestinationBuilding(journeyPhase)
                )
            }
        }
    }

    private fun updateCampusState(userLocation: LatLng) {
        val detected = CampusRepo.getCampus(userLocation)
        if (detected == null) {
            isManualCampusSelection = false
            currentCampus = null
            return
        }

        if (!isManualCampusSelection) {
            if (currentCampus?.name != detected.name) {
                currentCampus = detected
            }
        } else if (detected.name == currentCampus?.name) {
            isManualCampusSelection = false
        }
    }

    private fun handleActiveNavigationUpdate(userLocation: LatLng, isForce: Boolean) {
        // Pass building center — logic layer stays decoupled from data layer (Dependency Rule)
        val arrived = navigationEngine.checkArrivalWithBuilding(
            userPos        = userLocation,
            buildingCenter = uiBuildingState.building?.getCenter()
        )
        if (arrived && !uiBuildingState.navState.hasArrived) {
            uiBuildingState = uiBuildingState.copy(
                navState = uiBuildingState.navState.copy(hasArrived = true)
            )
        }

        // Route Refresh Logic
        val distanceMoved = lastRouteUpdateLocation?.let {
            com.google.maps.android.SphericalUtil.computeDistanceBetween(it, userLocation)
        } ?: Double.MAX_VALUE

        if (distanceMoved > 15.0 || isForce) {
            lastRouteUpdateLocation = userLocation
            calculateRouteWithState()
        }

        // Camera/Bearing Logic
        val newBearing = navigationEngine.calculateBearing(
            userLocation,
            uiBuildingState.routePoints,
            uiBuildingState.navState.currentBearing
        )

        uiBuildingState = uiBuildingState.copy(
            navState = uiBuildingState.navState.copy(currentBearing = newBearing)
        )

        if (uiBuildingState.navState.isAutoCenterEnabled) {
            mapEvent = userLocation
        }
    }

    fun handleSearchResult(result: SearchResult, context: android.content.Context) {
        val resultName   = result.displayName
        val resultCoords = result.coordinates(lastProcessedLocation)
        if (uiBuildingState.mode == MapUIMode.DIRECTIONS) {
            val selectedBuilding = if (result is SearchResult.BuildingResult) result.building else null
            // Prepare all new values first, then commit in one atomic copy()
            val updatedState = if (activeSearchField == "start") {
                uiBuildingState.copy(startLocationName = resultName, startPoint = resultCoords)
            } else {
                uiBuildingState.copy(destinationName = resultName, building = selectedBuilding, endPoint = resultCoords)
            }
            uiBuildingState = updatedState.copy(isSearchExpanded = false)
            searchResults = emptyList()
            resultCoords?.let { setMapEventWithOffset(it) }
            calculateRouteWithState()
            return
        }
        when (result) {
            is SearchResult.CampusResult -> {
                onCampusSelected(result.campus.name)
                resultCoords?.let { setMapEventWithOffset(it) }
                uiBuildingState = uiBuildingState.copy(isVisible = false, building = null)
            }
            is SearchResult.BuildingResult -> {
                val b = result.building
                highlightedBuildingName = b.name
                uiBuildingState = uiBuildingState.copy(isVisible = true, building = b, endPoint = b.getCenter())
                com.example.myapplication.logic.MapInteractionHandler.handleSearchSelection(b, this, context)
                CampusRepo.getAllCampuses().find { it.buildings.contains(b) }?.let {
                    currentCampus = it
                    isManualCampusSelection = true
                }
                b.getGoogleOutline().firstOrNull()?.let { mapEvent = it }
            }
            is SearchResult.CurrentLocation -> {
                locationProvider?.getUserLocation { location ->
                    location?.let {
                        mapEvent = it
                        processLocationUpdate(it, isForce = true)
                        uiBuildingState = uiBuildingState.copy(startPoint = it)
                    }
                }
            }
            is SearchResult.Home -> {
                val homePos = LatLng(45.51723868665001, -73.627297124046)
                mapEvent = homePos
                uiBuildingState = uiBuildingState.copy(startPoint = homePos)
            }
            is SearchResult.GoogleResult -> { /* Future implementation */ }
            is SearchResult.IndoorRoomResult -> {
                val nextPhase = IndoorJourneyHandler.onDestinationSelected(
                    destination = result,
                    userGps     = lastProcessedLocation
                )
                indoorJourneyState = IndoorJourneyState(phase = nextPhase)

                // Case 2: user is already outdoors — skip the indoor-to-exit leg
                // and jump straight to outdoor navigation toward the destination building.
                if (nextPhase is IndoorJourneyPhase.Outdoor) {
                    startOutdoorLeg(
                        origin      = nextPhase.origin,
                        destination = nextPhase.destination,
                        destLabel   = nextPhase.destRoom.label
                    )
                }
            }
        }
        searchResults = emptyList()
    }

    fun initSearch(
        client:     com.google.android.libraries.places.api.net.PlacesClient,
        indoorRepo: com.example.myapplication.data.indoor.IndoorRepository
    ) {
        searchProvider = HybridSearchProvider(client, indoorRepo)
        searchResults  = listOf(SearchResult.CurrentLocation)
    }

    // ── Indoor journey helpers ─────────────────────────────────────────────────

    fun setJourneyPhase(phase: IndoorJourneyPhase) {
        indoorJourneyState = IndoorJourneyState(phase = phase)
        // When transitioning to Outdoor, trigger the outdoor nav leg automatically.
        // This keeps the phase-transition logic in the ViewModel rather than
        // inside the MapContent composable (avoids business logic in the UI layer).
        if (phase is IndoorJourneyPhase.Outdoor) {
            startOutdoorLeg(
                origin      = phase.origin,
                destination = phase.destination,
                destLabel   = phase.destRoom.label
            )
        }
    }

    fun clearJourney() {
        indoorJourneyState = IndoorJourneyState(phase = IndoorJourneyPhase.Idle)
    }

    fun onCurrentRoomSelected(nodeId: String, label: String, buildingCode: String? = null, floor: Int? = null) {
        val phase = indoorJourneyState.phase as? IndoorJourneyPhase.AskCurrentRoom ?: return
        val next  = IndoorJourneyHandler.onCurrentRoomSelected(phase, nodeId, label, floor ?: 1)
        indoorJourneyState = IndoorJourneyState(phase = next)
    }

    fun onUserExited() {
        val phase = indoorJourneyState.phase as? IndoorJourneyPhase.IndoorToExit ?: return
        val gps   = lastProcessedLocation ?: return
        val next  = IndoorJourneyHandler.onUserExited(phase, gps)
        indoorJourneyState = IndoorJourneyState(phase = next)
    }

    fun onEntranceSelected(entrance: BuildingEntrance) {
        val phase = indoorJourneyState.phase as? IndoorJourneyPhase.AskEntryPoint ?: return
        val next  = IndoorJourneyHandler.onEntranceSelected(phase, entrance)
        indoorJourneyState = IndoorJourneyState(phase = next)
    }

    /**
     * Called when IndoorNavScreen hands off to outdoor navigation.
     * Sets up the outdoor route and triggers Google Maps directions.
     */
    fun startOutdoorLeg(
        origin:      com.google.android.gms.maps.model.LatLng,
        destination: com.google.android.gms.maps.model.LatLng,
        destLabel:   String
    ) {
        uiBuildingState = uiBuildingState.copy(
            mode                  = MapUIMode.DIRECTIONS,
            startPoint            = origin,
            endPoint              = destination,
            destinationName       = destLabel,
            selectedTransportMode = "walk",
            routePoints           = emptyList(),
            routeDuration         = "-- min",
            routeDistance         = "-- m",
            routeBounds           = null,
            routeErrorMessage     = null
        )
        calculateRouteWithState()
    }

    fun onSearchQueryChanged(newQuery: String, field: String = "main") {
        activeSearchField = field
        when (field) {
            "main"  -> searchQuery = newQuery
            "start" -> uiBuildingState = uiBuildingState.copy(startLocationName = newQuery)
            "dest"  -> uiBuildingState = uiBuildingState.copy(destinationName = newQuery)
        }
        viewModelScope.launch {
            searchProvider?.let { searchResults = it.search(newQuery) }
        }
    }

    fun onDirectionsRequested() {
        if (uiBuildingState.mode != MapUIMode.DIRECTIONS) {
            analyticsProvider.trackNavigationEnter("map_directions")
        }
        uiBuildingState = uiBuildingState.copy(
            mode            = MapUIMode.DIRECTIONS,
            destinationName = uiBuildingState.building?.name ?: ""
        )
    }

    fun onBackToPreview() {
        uiBuildingState = uiBuildingState.copy(
            mode = MapUIMode.PREVIEW,
            navState = NavigationState() // This clears hasArrived and currentBearing
        )
    }
    fun onStartQueryChanged(newQuery: String) {
        uiBuildingState = uiBuildingState.copy(startLocationName = newQuery)
    }

    fun onTransportModeChanged(mode: String) {
        uiBuildingState = uiBuildingState.copy(selectedTransportMode = mode)
        calculateRouteWithState()
    }

    fun calculateRouteWithState() {
        val start = if (uiBuildingState.mode == MapUIMode.ACTIVE_NAVIGATION) {
            lastProcessedLocation
        } else {
            uiBuildingState.startPoint ?: lastProcessedLocation
        } ?: return

        val end = uiBuildingState.endPoint ?: uiBuildingState.building?.getCenter() ?: return
        // ... rest of the function remains the same ...
        val isShuttle = uiBuildingState.selectedTransportMode == "shuttle"
        val provider  = if (isShuttle) shuttleRouteProvider else routeProvider
        viewModelScope.launch {
            val shuttleSnapshot = if (isShuttle) {
                val locationToUse = uiBuildingState.startPoint ?: lastProcessedLocation
                val nearestStop   = shuttleService.resolveNearestStop(locationToUse)
                val fromCampus    = nearestStop?.campus ?: "SGW"
                ShuttleSnapshot(
                    availability  = shuttleService.checkAvailability(fromCampus),
                    statusMessage = shuttleService.statusMessage(fromCampus),
                    stopName      = nearestStop?.name   ?: "",
                    stopCampus    = nearestStop?.campus ?: "",
                    stops         = shuttleService.getAllStops()
                )
            } else null
            val routeData = try {
                provider?.getRoute(start, end, uiBuildingState.selectedTransportMode)
            } catch (e: Exception) {
                null // Network / API crash — handled below as unavailable
            }
            val modeName = uiBuildingState.selectedTransportMode
                .replaceFirstChar { it.uppercase() }

            uiBuildingState = if (routeData != null) {
                val nextInstruction = routeData.instructions.firstOrNull() ?: "Follow the path"

                uiBuildingState.copy(
                    routePoints = routeData.points,
                    routeSegments = routeData.segments,
                    routeDuration = routeData.duration,
                    routeDistance = routeData.distance,
                    routeDurationSeconds = routeData.durationSeconds,
                    navState = uiBuildingState.navState.copy(
                        currentInstruction = nextInstruction
                    ),
                    routeErrorMessage = null // Clear any previous errors
                ).also {
                    startLiveEtaUpdates()
                }
            }
            else {
                uiBuildingState.copy(
                    routePoints       = emptyList(),
                    routeDuration     = "-- min",
                    routeDistance     = "-- m",
                    routeSegments = emptyList(),
                    routeBounds       = null,
                    routeErrorMessage = "$modeName route unavailable between these points."
                )
            }.let { state ->
                if (shuttleSnapshot != null) state.copy(
                    shuttleAvailability      = shuttleSnapshot.availability,
                    shuttleStatusMessage     = shuttleSnapshot.statusMessage,
                    nearestShuttleStopName   = shuttleSnapshot.stopName,
                    nearestShuttleStopCampus = shuttleSnapshot.stopCampus,
                    shuttleStops             = shuttleSnapshot.stops
                ) else state
            }
        }
    }

    /**
     * Navigate to a building by code — Map domain only, no Calendar awareness.
     *
     * Accepts a generic building code string so this method works for any
     * feature that needs map navigation (Calendar, search, deep links, etc.).
     * The caller is responsible for extracting the code from their domain object.
     *
     * Uses [buildingIndex] for O(1) lookup instead of flatMap O(n).
     * State is committed in a single atomic copy() to prevent UI flickering.
     */
    fun navigateToBuildingCode(buildingCode: String) {
        if (uiBuildingState.mode != MapUIMode.DIRECTIONS) {
            analyticsProvider.trackNavigationEnter("calendar_to_directions")
        }
        // O(1) lookup — no flatMap iteration
        val building = buildingIndex[buildingCode.lowercase()]

        // Resolve values first, then commit in ONE atomic copy() (PR review: single-update policy).
        // Includes route reset so UI never sees DIRECTIONS mode with stale polyline from
        // a previous navigation — calculateRouteWithState() fills them in asynchronously.
        val newDestName = building?.name ?: buildingCode
        uiBuildingState = uiBuildingState.copy(
            mode              = MapUIMode.DIRECTIONS,
            destinationName   = newDestName,
            building          = building,
            endPoint          = building?.getCenter(),
            // Route reset — atomically cleared so the UI shows a clean blank state
            // before the new polyline arrives from calculateRouteWithState()
            routePoints       = emptyList(),
            routeDuration     = "-- min",
            routeDistance     = "-- m",
            routeBounds       = null,
            routeErrorMessage = null
        )

        if (building != null) {
            calculateRouteWithState()
        } else {
            // Building not in local index — fall back to search.
            // Do NOT call onSearchQueryChanged() here: it would write uiBuildingState
            // a second time (destinationName overwrite), violating the single-update policy.
            // Instead, set only the search fields and trigger the provider directly.
            activeSearchField = "dest"
            viewModelScope.launch {
                searchProvider?.let { searchResults = it.search(buildingCode) }
            }
        }
    }

    fun toggleSearchExpansion(expanded: Boolean, field: String = "main") {
        activeSearchField = field
        uiBuildingState   = uiBuildingState.copy(isSearchExpanded = expanded)
    }
    private var lastRouteUpdateLocation: LatLng? = null

    fun startNavigation() {
        val destinationLabel = uiBuildingState.building?.name
            ?: uiBuildingState.destinationName
            ?: "destination"

        if (uiBuildingState.endPoint == null && uiBuildingState.building == null) return

        uiBuildingState = uiBuildingState.copy(
            mode = MapUIMode.ACTIVE_NAVIGATION,
            navState = NavigationState(
                hasArrived          = false,
                isAutoCenterEnabled = true,
                currentInstruction  = "Follow the path to $destinationLabel"
            )
        )
        calculateRouteWithState()
    }
    fun forceRecenter() {
        toggleAutoCenter(true)
        lastProcessedLocation?.let {
            mapEvent = it
        }
    }
    fun swapLocations() {
        val currentStartLatLng  = uiBuildingState.startPoint ?: lastProcessedLocation
        val currentDestLatLng   = uiBuildingState.endPoint   ?: uiBuildingState.building?.getCenter()
        val currentDestBuilding = uiBuildingState.building
        uiBuildingState = uiBuildingState.copy(
            startLocationName = uiBuildingState.destinationName,
            destinationName   = uiBuildingState.startLocationName,
            startPoint        = currentDestLatLng,
            endPoint          = currentStartLatLng,
            building          = if (!uiBuildingState.isStartCurrentLocation) null else currentDestBuilding
        )
        uiBuildingState.endPoint?.let { setMapEventWithOffset(it) }
        highlightedBuildingName = uiBuildingState.building?.name
        calculateRouteWithState()
    }

    fun setMapEventWithOffset(target: LatLng) {
        mapEvent = LatLng(target.latitude - 0.005, target.longitude)
    }

    fun toggleAutoCenter(enabled: Boolean) {
        uiBuildingState = uiBuildingState.copy(
            navState = uiBuildingState.navState.copy(isAutoCenterEnabled = enabled)
        )
    }

    // Inside MapViewModel.kt
    private var tickerJob: kotlinx.coroutines.Job? = null
    private fun startLiveEtaUpdates() {
        tickerJob?.cancel()

        // FIX: Don't start the infinite ticker if there's no time to count down.
        // This prevents tests from "buffering" indefinitely.
        if (uiBuildingState.routeDurationSeconds <= 0) return

        tickerJob = viewModelScope.launch {
            DateUtils.minuteTicker.collect {
                val currentSeconds = uiBuildingState.routeDurationSeconds
                if (currentSeconds > 60) {
                    val updatedSeconds = currentSeconds - 60
                    uiBuildingState = uiBuildingState.copy(
                        routeDurationSeconds = updatedSeconds,
                        routeDuration = "${updatedSeconds / 60} min"
                    )
                } else {
                    // Once we hit 1 min or 0, we should stop the ticker
                    // to save battery and allow tests to finish.
                    uiBuildingState = uiBuildingState.copy(
                        routeDurationSeconds = 0,
                        routeDuration = "1 min"
                    )
                    tickerJob?.cancel() // Stop the loop
                }
            }
        }
    }
    private data class ShuttleSnapshot(
        val availability:  com.example.myapplication.data.ShuttleAvailability,
        val statusMessage: String,
        val stopName:      String,
        val stopCampus:    String,
        val stops:         List<com.example.myapplication.data.ShuttleStop>
    )
}
