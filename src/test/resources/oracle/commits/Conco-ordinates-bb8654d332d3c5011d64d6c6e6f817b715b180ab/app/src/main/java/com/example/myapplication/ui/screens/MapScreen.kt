package com.example.myapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.logic.SearchResult
import com.example.myapplication.logic.handleRecenter
import com.example.myapplication.logic.openAppSettings
import com.example.myapplication.map.TrueCameraController
import com.example.myapplication.ui.components.BuildingInfoPopup
import com.example.myapplication.ui.components.CampusMap
import com.example.myapplication.ui.components.CampusSearchBar
import com.example.myapplication.ui.components.CampusToggle
import com.example.myapplication.ui.components.DirectionsHeader
import com.example.myapplication.ui.components.DirectionsInfoPopup
import com.example.myapplication.ui.components.LocationPermissionDialog
import com.example.myapplication.ui.components.NavigationOverlay
import com.example.myapplication.ui.components.NextClassPill
import com.example.myapplication.ui.components.ObserveCameraEffects
import com.example.myapplication.ui.components.ObserveLocationUpdates
import com.example.myapplication.ui.components.rememberMapCamera
import com.example.myapplication.ui.models.BuildingUiState
import com.example.myapplication.ui.models.MapUIMode
import com.example.myapplication.ui.theme.ConcordiaMaroon
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    mapViewModel:             com.example.myapplication.ui.viewmodel.MapViewModel,
    currentCampus:            com.example.myapplication.data.Campus?,
    highlightedBuildingName:  String?,
    searchQuery:              String,
    searchResults:            List<SearchResult>,
    activeSearchField:        String,
    nextClassEvent:           com.example.myapplication.data.ResolvedCalendarEvent?,
    isNextClassUrgent:        Boolean,
    nextClassTimeRemaining:   String,
    fusedLocationClient:      FusedLocationProviderClient,
    onSearchQueryChanged:     (String, String) -> Unit,
    onSearchResult:           (SearchResult, android.content.Context) -> Unit,
    onTransportModeChanged:   (String) -> Unit,
    onToggleSearchExpansion:  (Boolean, String) -> Unit,
    onSwapLocations:          () -> Unit,
    onBackToPreview:          () -> Unit,
    onCampusSelected:         (String) -> Unit,
    onBuildingDismiss:        () -> Unit,
    onDirectionsRequested:    () -> Unit,
    onLocationUpdate:         (com.google.android.gms.maps.model.LatLng, Boolean) -> Unit,
    onNavigateToBuilding:     (String) -> Unit,
    onStartNavigationActions: () -> Unit = {},
    onIndoorMapClick:         () -> Unit = {}
) {
    val uiState = mapViewModel.uiBuildingState
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var showSettingsDialog    by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        context as androidx.activity.ComponentActivity,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    ObserveLocationUpdates(hasLocationPermission, fusedLocationClient,
        onLocationUpdate = { loc -> onLocationUpdate(loc, false) })
    val cameraPositionState = rememberMapCamera()
    val cameraController    = remember(cameraPositionState) { TrueCameraController(cameraPositionState) }
    ObserveCameraEffects(
        cameraPositionState = cameraPositionState,
        cameraController    = cameraController,
        uiState             = uiState,
        currentCampus       = currentCampus,
        mapEvent            = mapViewModel.mapEvent,
        navBearing          = mapViewModel.currentNavBearing
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasLocationPermission = isGranted }

    val mapPaddingBottom = when (uiState.mode) {
        MapUIMode.DIRECTIONS        -> 600
        MapUIMode.ACTIVE_NAVIGATION -> 100
        else                        -> 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CampusMap(
            currentCampus           = currentCampus,
            highlightedBuildingName = highlightedBuildingName,
            cameraPositionState     = cameraPositionState,
            hasLocationPermission   = hasLocationPermission,
            viewModel               = mapViewModel,
            contentPadding          = PaddingValues(bottom = mapPaddingBottom.dp),
            modifier                = Modifier.testTag("campus_map")
        )

        if (uiState.mode != MapUIMode.ACTIVE_NAVIGATION) {
            MapSearchOverlay(
                context                  = context,
                uiState                  = uiState,
                searchQuery              = searchQuery,
                searchResults            = searchResults,
                activeSearchField        = activeSearchField,
                onSearchQueryChanged     = onSearchQueryChanged,
                onSearchResult           = onSearchResult,
                onTransportModeChanged   = onTransportModeChanged,
                onToggleSearchExpansion  = onToggleSearchExpansion,
                onSwapLocations          = onSwapLocations,
                onBackToPreview          = onBackToPreview,
                onStartNavigationActions = onStartNavigationActions
            )
            MapPreviewOverlays(
                uiState                = uiState,
                currentCampus          = currentCampus,
                nextClassEvent         = nextClassEvent,
                isNextClassUrgent      = isNextClassUrgent,
                nextClassTimeRemaining = nextClassTimeRemaining,
                fusedLocationClient    = fusedLocationClient,
                shouldShowRationale    = shouldShowRationale,
                hasLocationPermission  = hasLocationPermission,
                launcher               = launcher,
                cameraController       = cameraController,
                scope                  = scope,
                onShowSettings         = { showSettingsDialog = true },
                context                = context,
                onLocationUpdate       = onLocationUpdate,
                onCampusSelected       = onCampusSelected,
                onNavigateToBuilding   = onNavigateToBuilding
            )
            MapBuildingOverlay(
                uiState                  = uiState,
                onDismiss                = onBuildingDismiss,
                onDirectionsRequested    = onDirectionsRequested,
                onIndoorMapClick         = onIndoorMapClick,
                onTransportModeChanged   = onTransportModeChanged,
                onToggleSearchExpansion  = onToggleSearchExpansion,
                onSwapLocations          = onSwapLocations,
                onBackToPreview          = onBackToPreview,
                onStartNavigationActions = onStartNavigationActions
            )
        } else {
            NavigationOverlay(
                navState        = uiState.navState,
                onRecenterClick = { mapViewModel.forceRecenter() },
                onExit          = { onBackToPreview() },
                destinationName = { mapViewModel.uiBuildingState.destinationName }
            )
        }

        if (showSettingsDialog && !hasLocationPermission) {
            LocationPermissionDialog(
                onOpenSettings = { openAppSettings(context) },
                onDismiss      = { showSettingsDialog = false }
            )
        }
    }
}

// ── private overlays ──────────────────────────────────────────────────────────

@Composable
private fun BoxScope.MapSearchOverlay(
    context:                  android.content.Context,
    uiState:                  BuildingUiState,
    searchQuery:              String,
    searchResults:            List<SearchResult>,
    activeSearchField:        String,
    onSearchQueryChanged:     (String, String) -> Unit,
    onSearchResult:           (SearchResult, android.content.Context) -> Unit,
    onTransportModeChanged:   (String) -> Unit,
    onToggleSearchExpansion:  (Boolean, String) -> Unit,
    onSwapLocations:          () -> Unit,
    onBackToPreview:          () -> Unit,
    onStartNavigationActions: () -> Unit
) {
    if (uiState.mode == MapUIMode.PREVIEW) {
        CampusSearchBar(
            query         = searchQuery,
            results       = searchResults,
            onQueryChange = { onSearchQueryChanged(it, "main") },
            onResultClick = { onSearchResult(it, context) },
            modifier      = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )
    } else {
        DirectionsOverlay(
            context                  = context,
            uiState                  = uiState,
            searchQuery              = searchQuery,
            searchResults            = searchResults,
            activeSearchField        = activeSearchField,
            onTransportModeChanged   = onTransportModeChanged,
            onToggleSearchExpansion  = onToggleSearchExpansion,
            onSwapLocations          = onSwapLocations,
            onBackToPreview          = onBackToPreview,
            onSearchResult           = onSearchResult,
            onSearchQueryChanged     = onSearchQueryChanged,
            onStartNavigationActions = onStartNavigationActions
        )
    }
}

@Composable
private fun BoxScope.DirectionsOverlay(
    context:                  android.content.Context,
    uiState:                  BuildingUiState,
    searchQuery:              String,
    searchResults:            List<SearchResult>,
    activeSearchField:        String,
    onTransportModeChanged:   (String) -> Unit,
    onToggleSearchExpansion:  (Boolean, String) -> Unit,
    onSwapLocations:          () -> Unit,
    onBackToPreview:          () -> Unit,
    onStartNavigationActions: () -> Unit,
    onSearchResult:           (SearchResult, android.content.Context) -> Unit,
    onSearchQueryChanged:     (String, String) -> Unit
) {
    if (uiState.isSearchExpanded) {
        DirectionsHeader(
            uiState            = uiState,
            onBackClick        = { onToggleSearchExpansion(false, "") },
            onStartQueryChange = { onSearchQueryChanged(it, "start") },
            onDestQueryChange  = { onSearchQueryChanged(it, "dest") },
            modifier           = Modifier.align(Alignment.TopCenter)
        )
    }
    if (uiState.mode == MapUIMode.DIRECTIONS) {
        DirectionsInfoPopup(
            uiState            = uiState,
            onModeChange       = onTransportModeChanged,
            onStartClick       = { onToggleSearchExpansion(true, "start") },
            onDestinationClick = { onToggleSearchExpansion(true, "dest") },
            onSwapClick        = onSwapLocations,
            onClose            = onBackToPreview,
            onStartNavigation  = onStartNavigationActions,
            modifier           = Modifier.align(Alignment.BottomCenter)
        )
    }
    DirectionsSearchResults(
        context           = context,
        uiState           = uiState,
        searchQuery       = searchQuery,
        searchResults     = searchResults,
        activeSearchField = activeSearchField,
        onSearchResult    = onSearchResult
    )
}

@Composable
private fun BoxScope.DirectionsSearchResults(
    context:           android.content.Context,
    uiState:           BuildingUiState,
    searchQuery:       String,
    searchResults:     List<SearchResult>,
    activeSearchField: String,
    onSearchResult:    (SearchResult, android.content.Context) -> Unit
) {
    val currentFieldText = when (activeSearchField) {
        "start" -> uiState.startLocationName
        "dest"  -> uiState.destinationName
        else    -> searchQuery
    }
    if (activeSearchField == "main" || currentFieldText.isEmpty()) return
    Card(
        modifier  = Modifier.padding(horizontal = 24.dp).offset(y = 190.dp).zIndex(1f),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
            items(searchResults) { result ->
                val title = when (result) {
                    is SearchResult.BuildingResult   -> result.building.name
                    is SearchResult.CampusResult     -> result.campus.name
                    is SearchResult.GoogleResult     -> result.title
                    is SearchResult.CurrentLocation  -> "Your position"
                    is SearchResult.Home             -> "Home"
                    is SearchResult.IndoorRoomResult -> result.label
                }
                ListItem(
                    headlineContent = { Text(title) },
                    modifier        = Modifier.clickable { onSearchResult(result, context) }
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MapPreviewOverlays(
    uiState:                BuildingUiState,
    currentCampus:          com.example.myapplication.data.Campus?,
    nextClassEvent:         com.example.myapplication.data.ResolvedCalendarEvent?,
    isNextClassUrgent:      Boolean,
    nextClassTimeRemaining: String,
    fusedLocationClient:    FusedLocationProviderClient,
    shouldShowRationale:    Boolean,
    hasLocationPermission:  Boolean,
    launcher:               androidx.activity.result.ActivityResultLauncher<String>,
    cameraController:       TrueCameraController,
    scope:                  kotlinx.coroutines.CoroutineScope,
    onShowSettings:         () -> Unit,
    context:                android.content.Context,
    onLocationUpdate:       (com.google.android.gms.maps.model.LatLng, Boolean) -> Unit,
    onCampusSelected:       (String) -> Unit,
    onNavigateToBuilding:   (String) -> Unit
) {
    val mode = uiState.mode
    if (mode == MapUIMode.ACTIVE_NAVIGATION) return
    if (mode == MapUIMode.PREVIEW) {
        NextClassPill(
            nextEvent       = nextClassEvent,
            isUrgent        = isNextClassUrgent,
            timeRemaining   = nextClassTimeRemaining,
            onNavigateClick = {
                nextClassEvent?.destinationBuildingCode?.let { onNavigateToBuilding(it) }
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 28.dp)
        )
    }
    if (mode != MapUIMode.DIRECTIONS) {
        CampusToggle(
            selectedCampusName = currentCampus?.name,
            onCampusClick      = { onCampusSelected(it) },
            modifier           = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 160.dp)
        )
        ExtendedFloatingActionButton(
            onClick = {
                handleRecenter(
                    client              = fusedLocationClient,
                    hasPermission       = hasLocationPermission,
                    shouldShowRationale = shouldShowRationale,
                    launcher            = launcher,
                    context             = context,
                    onShowSettings      = onShowSettings
                ) { userLocation ->
                    scope.launch { cameraController.animateTo(userLocation, 18.5f) }
                    onLocationUpdate(userLocation, true)
                }
            },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 24.dp),
            containerColor = ConcordiaMaroon,
            contentColor   = Color.White,
            icon           = { Icon(Icons.Default.MyLocation, contentDescription = null) },
            text           = { Text("RECENTER") }
        )
    }
}

@Composable
private fun BoxScope.MapBuildingOverlay(
    uiState:                  BuildingUiState,
    onDismiss:                () -> Unit,
    onDirectionsRequested:    () -> Unit,
    onIndoorMapClick:         () -> Unit,
    onTransportModeChanged:   (String) -> Unit,
    onToggleSearchExpansion:  (Boolean, String) -> Unit,
    onSwapLocations:          () -> Unit,
    onBackToPreview:          () -> Unit,
    onStartNavigationActions: () -> Unit
) {
    if (!uiState.isVisible) return
    val building = uiState.building ?: return
    if (uiState.mode == MapUIMode.PREVIEW) {
        BuildingInfoPopup(
            building          = building,
            uiState           = uiState,
            onDismiss         = onDismiss,
            onDirectionsClick = onDirectionsRequested,
            onIndoorMapClick  = onIndoorMapClick
        )
    } else {
        DirectionsInfoPopup(
            uiState            = uiState,
            onModeChange       = onTransportModeChanged,
            onStartClick       = { onToggleSearchExpansion(true, "start") },
            onDestinationClick = { onToggleSearchExpansion(true, "dest") },
            onSwapClick        = onSwapLocations,
            onClose            = onBackToPreview,
            onStartNavigation  = onStartNavigationActions,
            modifier           = Modifier.align(Alignment.BottomCenter)
        )
    }
}
