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

    /** No active journey. */
    object Idle : IndoorJourneyPhase()

    /** Destination chosen, checking GPS to determine user's current building. */
    object DetectingLocation : IndoorJourneyPhase()

    /**
     * GPS confirmed user is inside [currentBuilding].
     * Waiting for user to pick their current room.
     */
    data class AskCurrentRoom(
        val currentBuilding: Building,
        val destination:     SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase()

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
    ) : IndoorJourneyPhase()

    /**
     * User confirmed they have exited the building.
     * Outdoor Google Maps navigation is active.
     */
    data class Outdoor(
        val origin:      com.google.android.gms.maps.model.LatLng,
        val destination: com.google.android.gms.maps.model.LatLng,
        val destRoom:    SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase()

    /**
     * GPS confirms user is near destination building.
     * Waiting for user to pick which entrance they used.
     */
    data class AskEntryPoint(
        val building:    Building,
        val entrances:   List<BuildingEntrance>,
        val destination: SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase()

    /**
     * User selected entrance. Indoor navigation active in destination building.
     * May include floor change steps.
     */
    data class IndoorToDestination(
        val buildingCode: String,
        val startFloor:   Int,    // floor the user is currently on (map starts here)
        val startNodeId:  String,
        val destination:  SearchResult.IndoorRoomResult
    ) : IndoorJourneyPhase()

    /** Journey complete. */
    object Arrived : IndoorJourneyPhase()
}

data class IndoorJourneyState(
    val phase:          IndoorJourneyPhase = IndoorJourneyPhase.Idle,
    val errorMessage:   String?            = null
)
