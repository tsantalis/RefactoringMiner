package com.example.myapplication.ui.models

import com.example.myapplication.data.Building
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.logic.SearchResult

/**
 * State machine for the full indoor→outdoor→indoor journey flow.
 *
 * Phases in order:
 *
 *  IDLE
 *   ↓  user picks an IndoorRoomResult from search
 *  DETECT_USER_LOCATION
 *   ↓  GPS says user is inside building X
 *  ASK_CURRENT_ROOM          ← "Which room are you in?"
 *   ↓  user picks room
 *  INDOOR_TO_EXIT            ← A* from current room → building exit
 *   ↓  user taps "I'm outside"
 *  OUTDOOR                   ← Google Maps origin→destination building
 *   ↓  GPS detects user near destination building
 *  ASK_ENTRY_POINT           ← "Which entrance did you use?"
 *   ↓  user picks entrance
 *  INDOOR_TO_DESTINATION     ← A* from entrance → destination room (+ floor change)
 *   ↓  arrived
 *  ARRIVED
 */
sealed class IndoorJourneyPhase {

    /**
     * Whether the UI should show a "Back" button for this phase.
     *
     * Allows the UI to call [MapViewModel.clearJourney] or step back
     * without knowing which phase is active — the phase itself declares
     * whether going back makes sense.
     */
    abstract val canGoBack: Boolean

    /** No active journey. */
    object Idle : IndoorJourneyPhase() {
        override val canGoBack = false
    }

    /** Destination chosen, checking GPS to determine user's current building. */
    object DetectingLocation : IndoorJourneyPhase() {
        override val canGoBack = true   // user can cancel while we search for GPS
    }

    /**
     * GPS confirmed user is inside [currentBuilding].
     * Waiting for user to pick their current room.
     */
    data class AskCurrentRoom(
        val currentBuilding: Building,
        val destination:     SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase() {
        override val canGoBack = true   // user can change destination
    }

    /**
     * User selected their current room.
     * Show indoor map with A* path to the exit.
     */
    data class IndoorToExit(
        val buildingCode: String,
        val floor:        Int,
        val startNodeId:  String,
        val exitNodeId:   String,
        val destination:  SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase() {
        override val canGoBack = true   // user can go back to pick a different room
    }

    /**
     * User confirmed they have exited the building.
     * Outdoor Google Maps navigation is active.
     */
    data class Outdoor(
        val origin:      com.google.android.gms.maps.model.LatLng,
        val destination: com.google.android.gms.maps.model.LatLng,
        val destRoom:    SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase() {
        override val canGoBack = false  // outdoor nav is in progress, cancelling ends the trip
    }

    /**
     * GPS confirms user is near destination building.
     * Waiting for user to pick which entrance they used.
     */
    data class AskEntryPoint(
        val building:    Building,
        val entrances:   List<BuildingEntrance>,
        val destination: SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase() {
        override val canGoBack = true   // user can pick a different entrance
    }

    /**
     * User selected entrance. Indoor navigation active in destination building.
     * May include floor change steps.
     */
    data class IndoorToDestination(
        val buildingCode: String,
        val startFloor:   Int,
        val startNodeId:  String,
        val destination:  SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase() {
        override val canGoBack = false  // actively navigating to destination
    }

    /** Journey complete. */
    object Arrived : IndoorJourneyPhase() {
        override val canGoBack = false  // journey is over
    }
}

data class IndoorJourneyState(
    val phase:          IndoorJourneyPhase = IndoorJourneyPhase.Idle,
    val errorMessage:   String?            = null
)
