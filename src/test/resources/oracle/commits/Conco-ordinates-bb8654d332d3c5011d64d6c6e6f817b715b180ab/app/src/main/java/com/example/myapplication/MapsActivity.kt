package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myapplication.analytics.AnalyticsRegistry
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.ShuttleRepo
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.logic.AuthRepository
import com.example.myapplication.logic.DefaultShuttleService
import com.example.myapplication.logic.GoogleCalendarProvider
import com.example.myapplication.logic.IndoorJourneyHandler
import com.example.myapplication.logic.SharedPrefsCalendarPreferences
import com.example.myapplication.logic.TrueLocationProvider
import com.example.myapplication.telemetry.CrashReporter
import com.example.myapplication.ui.components.IndoorJourneyDialogs
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.example.myapplication.ui.screens.AppNavigation
import com.example.myapplication.ui.screens.IndoorNavScreen

import com.example.myapplication.ui.screens.MapScreen
import com.example.myapplication.ui.viewmodel.CalendarViewModel
import com.example.myapplication.ui.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices

class MapsActivity : ComponentActivity() {

    private lateinit var authRepository:      AuthRepository
    private lateinit var viewModel:           MapViewModel
    private lateinit var calendarViewModel:   CalendarViewModel
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    // ── Google Sign-In (must live here — needs ActivityResultLauncher) ─────────
    // ── Google Sign-In (Surgical Update) ─────────
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)

        if (task.isSuccessful) {
            calendarViewModel.clearSelection()
            calendarViewModel.loadCalendarsAndAutoSelect()
        } else {
            // SURGERY: Instead of just logging, update the UI state
            val exception = task.exception
            val errorMessage = when {
                // Check if user cancelled or if it's a network issue
                exception is com.google.android.gms.common.api.ApiException -> {
                    "Connection failed. Please check your internet or try again."
                }
                else -> "Login cancelled or failed."
            }

            // Notify the ViewModel so the UI can show the error
            calendarViewModel.setAuthError(errorMessage)

            CrashReporter.recordNonFatal(exception ?: Exception("Sign-in failed"), "google_sign_in_failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(
                applicationContext, BuildConfig.MAPS_API_KEY
            )
        }
        super.onCreate(savedInstanceState)
        CrashReporter.setKey("screen", "MapsActivity")
        CrashReporter.setKey("app_version", BuildConfig.VERSION_NAME)
        CrashReporter.log("maps_activity_created")
        CampusRepo.initialize(this)
        ShuttleRepo.initialize(this)
        BuildingEntrances.initialize(this)   // ← load building_entrances.json

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        authRepository      = AuthRepository(context = applicationContext)

        val tokenProvider: suspend () -> String? = { authRepository.getCalendarToken() }
        val locationProvider = TrueLocationProvider(fusedLocationClient)
        val routeProvider    = com.example.myapplication.logic.GoogleRouteProvider(BuildConfig.MAPS_API_KEY)
        val calendarProvider = GoogleCalendarProvider(context = this, tokenProvider = tokenProvider)
        val indoorRepo       = IndoorRepository(applicationContext)  // ← for indoor room search

        viewModel = MapViewModel(
            locationProvider  = locationProvider,
            routeProvider     = routeProvider,
            shuttleService    = DefaultShuttleService(ShuttleRepo),
            analyticsProvider = AnalyticsRegistry.provider()
        )
        calendarViewModel = CalendarViewModel(
            calendarProvider    = calendarProvider,
            calendarPreferences = SharedPrefsCalendarPreferences(this),
            locationResolver    = com.example.myapplication.logic.LocationResolver(
                com.example.myapplication.data.CampusBuildingNameProvider()
            )
        )

        val placesClient = com.google.android.libraries.places.api.Places.createClient(this)
        viewModel.initSearch(placesClient, indoorRepo)   // ← pass indoorRepo

        setContent {
            AppNavigation(
                calendarViewModel = calendarViewModel,
                navigationActions = com.example.myapplication.ui.screens.NavigationActions(
                    onNavigateToMap = { buildingCode -> viewModel.navigateToBuildingCode(buildingCode) },
                    onConnectClick  = { connectCalendar() },
                    onSignOutClick  = { signOutCalendar() }
                ),
                onScreenVisible = { screenRoute ->
                    AnalyticsRegistry.provider().trackScreenView(screenRoute)
                },
                userEmail  = authRepository.getSignedInEmail() ?: "",
                mapContent = {
                    MapContent(
                        mapViewModel        = viewModel,
                        calendarViewModel   = calendarViewModel,
                        fusedLocationClient = fusedLocationClient
                    )
                }
            )
        }
    }

    private fun connectCalendar() {
        authRepository.revokeAndSignIn { intent -> signInLauncher.launch(intent) }
    }

    private fun signOutCalendar() {
        authRepository.signOut { calendarViewModel.clearSelection() }
    }
}

// ── MapContent ────────────────────────────────────────────────────────────────

@Composable
private fun MapContent(
    mapViewModel:        MapViewModel,
    calendarViewModel:   CalendarViewModel,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
) {
    // Simple indoor map overlay (building popup → Indoor button)
    var indoorTarget by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // Full journey indoor overlay (search → H-829 → full route)
    var journeyIndoorTarget by remember {
        mutableStateOf<Triple<String, Int, String?>?>(null)
        // Triple(buildingCode, floor, startNodeId)
    }

    // ── Main map screen ───────────────────────────────────────────────────────
    MapScreen(
        mapViewModel             = mapViewModel,
        currentCampus            = mapViewModel.currentCampus,
        highlightedBuildingName  = mapViewModel.highlightedBuildingName,
        searchQuery              = mapViewModel.searchQuery,
        searchResults            = mapViewModel.searchResults,
        activeSearchField        = mapViewModel.activeSearchField,
        nextClassEvent           = calendarViewModel.nextUpcomingEvent,
        isNextClassUrgent        = calendarViewModel.isNextClassUrgent,
        nextClassTimeRemaining   = calendarViewModel.nextClassTimeRemaining,
        fusedLocationClient      = fusedLocationClient,
        onSearchQueryChanged     = { q, f -> mapViewModel.onSearchQueryChanged(q, f) },
        onSearchResult           = { r, ctx -> mapViewModel.handleSearchResult(r, ctx) },
        onTransportModeChanged   = { mapViewModel.onTransportModeChanged(it) },
        onToggleSearchExpansion  = { e, f -> mapViewModel.toggleSearchExpansion(e, f) },
        onSwapLocations          = { mapViewModel.swapLocations() },
        onBackToPreview          = { mapViewModel.onBackToPreview() },
        onCampusSelected         = { mapViewModel.onCampusSelected(it) },
        onBuildingDismiss        = { mapViewModel.handleMapTap(null) },
        onDirectionsRequested    = { mapViewModel.onDirectionsRequested() },
        onLocationUpdate         = { loc, force -> mapViewModel.processLocationUpdate(loc, force) },
        onNavigateToBuilding     = { mapViewModel.navigateToBuildingCode(it) },
        onStartNavigationActions = { mapViewModel.startNavigation() },
        onIndoorMapClick         = {
            val code = mapViewModel.uiBuildingState.building?.code ?: return@MapScreen
            indoorTarget = Pair(code, 1)
        }
    )

    // ── Indoor journey state ──────────────────────────────────────────────────
    // Read phase once; used by dialogs, journeyIndoorTarget derivation, and
    // the IndoorNavScreen below. Direct read (not LaunchedEffect) is synchronous
    // and avoids timing gaps between phase change and screen transition.
    val journeyPhase = mapViewModel.indoorJourneyState.phase
    val context      = androidx.compose.ui.platform.LocalContext.current
    val indoorRepo   = remember(context) {
        com.example.myapplication.data.indoor.IndoorRepository(context.applicationContext)
    }

    // ── Indoor journey dialogs ────────────────────────────────────────────────
    IndoorJourneyDialogs(
        phase              = journeyPhase,
        indoorRepo         = indoorRepo,
        onRoomResolved     = { nodeId, label, buildingCode, floor ->
            mapViewModel.onCurrentRoomSelected(nodeId, label, buildingCode, floor)
        },
        onConfirmExit      = { mapViewModel.onUserExited() },
        onEntranceSelected = { entrance -> mapViewModel.onEntranceSelected(entrance) },
        onDismiss          = { mapViewModel.clearJourney() }
    )

    // Set journeyIndoorTarget and handle outdoor phase transitions.
    // Direct derivation (not LaunchedEffect) ensures synchronous response to phase changes.
    when (journeyPhase) {
        is IndoorJourneyPhase.IndoorToExit -> {
            if (journeyIndoorTarget?.first != journeyPhase.buildingCode ||
                journeyIndoorTarget?.second != journeyPhase.floor) {
                journeyIndoorTarget = Triple(
                    journeyPhase.buildingCode,
                    journeyPhase.floor,
                    journeyPhase.startNodeId
                )
            }
        }
        is IndoorJourneyPhase.IndoorToDestination -> {
            if (journeyIndoorTarget?.first != journeyPhase.buildingCode ||
                journeyIndoorTarget?.second != journeyPhase.startFloor) {
                journeyIndoorTarget = Triple(
                    journeyPhase.buildingCode,
                    journeyPhase.startFloor,
                    journeyPhase.startNodeId
                )
            }
        }
        is IndoorJourneyPhase.Outdoor -> {
            // User has exited the building — close indoor screen and start outdoor nav.
            // startOutdoorLeg triggers Google Maps directions to the destination building.
            if (journeyIndoorTarget != null) {
                journeyIndoorTarget = null
                mapViewModel.startOutdoorLeg(
                    origin      = journeyPhase.origin,
                    destination = journeyPhase.destination,
                    destLabel   = journeyPhase.destRoom.label
                )
            }
        }
        is IndoorJourneyPhase.Idle -> {
            if (journeyIndoorTarget != null) journeyIndoorTarget = null
        }
        else -> { /* AskCurrentRoom, AskEntryPoint, DetectingLocation — no-op */ }
    }

    // ── Simple indoor map (building popup → Indoor button) ────────────────────
    AnimatedVisibility(
        visible  = indoorTarget != null,
        enter    = slideInVertically { it },
        exit     = slideOutVertically { it },
        modifier = Modifier.fillMaxSize()
    ) {
        indoorTarget?.let { (code, floor) ->
            IndoorNavScreen(
                building        = code,
                availableFloors = floorsFor(code),
                initialFloor    = floor,
                onBack          = { indoorTarget = null }
            )
        }
    }

    // ── Full journey indoor nav (search-triggered) ────────────────────────────
    AnimatedVisibility(
        visible  = journeyIndoorTarget != null,
        enter    = slideInVertically { it },
        exit     = slideOutVertically { it },
        modifier = Modifier.fillMaxSize()
    ) {
        journeyIndoorTarget?.let { (code, floor, startNode) ->
            val phase = journeyPhase

            val destination = when (phase) {
                is IndoorJourneyPhase.IndoorToExit ->
                    // Destination is the exit node of the current building
                    com.example.myapplication.logic.IndoorOutdoorRouter.IndoorDestination(
                        building = phase.buildingCode,
                        floor    = phase.floor,
                        nodeId   = phase.exitNodeId,
                        label    = "Exit"
                    )
                is IndoorJourneyPhase.IndoorToDestination ->
                    // Same building, possibly different floor.
                    // nodeId may be null if the search couldn't find the node at query time;
                    // IndoorNavViewModel.navigateTo will resolve it from the floor JSON.
                    com.example.myapplication.logic.IndoorOutdoorRouter.IndoorDestination(
                        building = phase.destination.buildingCode,
                        floor    = phase.destination.floor,
                        nodeId   = phase.destination.nodeId ?: "",  // "" triggers re-resolution in VM
                        label    = phase.destination.label
                    ).also {
                        android.util.Log.d("JOURNEY",
                            "IndoorToDestination: building=${phase.destination.buildingCode}" +
                            " floor=${phase.destination.floor}" +
                            " nodeId='${phase.destination.nodeId}'" +
                            " startFloor=${phase.startFloor}" +
                            " startNodeId=${phase.startNodeId}")
                    }
                else -> null
            }

            IndoorNavScreen(
                building        = code,
                // Pass all floors so the user can see the floor selector during cross-floor nav
                availableFloors = floorsFor(code),
                initialFloor    = floor,
                destination     = destination,
                startNodeId     = startNode,
                // Tell CrossFloorNavigator which floor the user's start node is on.
                // For IndoorToDestination this may differ from the destination floor.
                startFloor      = floor,
                // Only show exit confirmation card when walking to an exit (cross-building)
                onConfirmExit   = if (phase is IndoorJourneyPhase.IndoorToExit) {
                    {
                        // Call onUserExited() first — this transitions phase to Outdoor.
                        // The when(journeyPhase) block above detects Outdoor phase and
                        // calls startOutdoorLeg + sets journeyIndoorTarget=null.
                        // Do NOT set journeyIndoorTarget=null here — the Outdoor handler does it.
                        mapViewModel.onUserExited()
                    }
                } else null,
                onBack = {
                    journeyIndoorTarget = null
                    mapViewModel.clearJourney()
                },
                onOutdoorHandoff = { outdoorSeg ->
                    // Hand off to Google Maps for the outdoor leg
                    journeyIndoorTarget = null
                    mapViewModel.startOutdoorLeg(
                        origin      = outdoorSeg.origin,
                        destination = outdoorSeg.destination,
                        destLabel   = outdoorSeg.destLabel
                    )
                }
            )
        }
    }
}

private fun floorsFor(code: String): List<Int> =
    com.example.myapplication.data.indoor.IndoorBuildingConfig.floorsFor(code)
