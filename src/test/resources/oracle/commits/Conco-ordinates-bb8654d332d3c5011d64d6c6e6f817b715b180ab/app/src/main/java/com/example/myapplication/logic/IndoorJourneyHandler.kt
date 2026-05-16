package com.example.myapplication.logic

import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.example.myapplication.ui.models.IndoorJourneyState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

/**
 * Pure logic for advancing the indoor journey state machine.
 *
 * Kept as a standalone object (not inside MapViewModel) so it can be
 * unit-tested without Android dependencies.
 *
 * MapViewModel calls these functions and updates its own state accordingly.
 */
object IndoorJourneyHandler {

    /**
     * Called when user selects an [IndoorRoomResult] from the search bar.
     *
     * Checks whether [userGps] is inside a known building.
     *   - If yes → transition to AskCurrentRoom
     *   - If no  → transition to Outdoor immediately (user is already outside)
     */
    fun onDestinationSelected(
        destination: SearchResult.IndoorRoomResult,
        userGps:     LatLng?
    ): IndoorJourneyPhase {
        if (userGps == null) return IndoorJourneyPhase.DetectingLocation

        val allBuildings = CampusRepo.getAllBuildings()

        val currentBuilding = allBuildings.firstOrNull { building ->
            PolyUtil.containsLocation(userGps, building.getGoogleOutline(), false)
        }

        return if (currentBuilding != null) {
            IndoorJourneyPhase.AskCurrentRoom(currentBuilding, destination)
        } else {
            // User is outside — skip straight to outdoor nav
            val destEntrances = BuildingEntrances.forBuilding(destination.buildingCode)
            val bestEntry     = destEntrances.firstOrNull()
            if (bestEntry != null) {
                IndoorJourneyPhase.Outdoor(
                    origin      = userGps,
                    destination = bestEntry.gps,
                    destRoom    = destination
                )
            } else {
                IndoorJourneyPhase.Idle // no entrance data, can't navigate
            }
        }
    }

    /**
     * Called when the user picks their current room (or "Building Entrance").
     *
     * Routing logic:
     *   - Same building as destination → IndoorToDestination (skip going outside)
     *     The CrossFloorNavigator inside IndoorNavViewModel will handle floor changes
     *     automatically when the start floor differs from the destination floor.
     *   - Different building → IndoorToExit (walk to nearest exit first)
     *
     * [startNodeId] is the resolved nav node id of the current room or entrance.
     */
    fun onCurrentRoomSelected(
        phase:       IndoorJourneyPhase.AskCurrentRoom,
        startNodeId: String,
        startLabel:  String,
        startFloor:  Int = 1   // floor the user's start node is on
    ): IndoorJourneyPhase {
        val currentBuildingCode = phase.currentBuilding.code
        val destBuildingCode    = phase.destination.buildingCode

        // Case 3: same building (same floor or cross-floor) —
        // navigate directly to the destination without leaving the building.
        // startFloor = the floor the user is currently on (where the start node is).
        // CrossFloorNavigator will handle the transition to the destination floor.
        if (currentBuildingCode.equals(destBuildingCode, ignoreCase = true)) {
            return IndoorJourneyPhase.IndoorToDestination(
                buildingCode = destBuildingCode,
                startFloor   = startFloor,
                startNodeId  = startNodeId,
                destination  = phase.destination
            )
        }

        // Case 1: different building — walk to the nearest exit first,
        // then hand off to outdoor navigation.
        val exits    = BuildingEntrances.forBuilding(currentBuildingCode)
        val exitNode = exits.firstOrNull() ?: return IndoorJourneyPhase.Idle

        return IndoorJourneyPhase.IndoorToExit(
            buildingCode = currentBuildingCode,
            floor        = exitNode.floor,
            startNodeId  = startNodeId,
            exitNodeId   = exitNode.nodeId,
            destination  = phase.destination
        )
    }

    /**
     * Called when user confirms they have exited the building.
     * Returns OutdoorWalk phase.
     */
    fun onUserExited(
        phase:    IndoorJourneyPhase.IndoorToExit,
        userGps:  LatLng
    ): IndoorJourneyPhase {
        val destEntrances = BuildingEntrances.forBuilding(phase.destination.buildingCode)
        val bestEntry = destEntrances.firstOrNull() ?: return IndoorJourneyPhase.Idle

        return IndoorJourneyPhase.Outdoor(
            origin      = userGps,
            destination = bestEntry.gps,
            destRoom    = phase.destination
        )
    }

    /**
     * Called when GPS detects the user is near the destination building,
     * OR when Google Maps reports arrival.
     * Returns AskEntryPoint phase.
     */
    fun onNearDestinationBuilding(
        phase: IndoorJourneyPhase.Outdoor
    ): IndoorJourneyPhase {
        val allBuildings  = CampusRepo.getAllBuildings()
        val destBuilding  = allBuildings.firstOrNull { it.code == phase.destRoom.buildingCode }
            ?: return IndoorJourneyPhase.Idle
        val entrances     = BuildingEntrances.forBuilding(phase.destRoom.buildingCode)

        return IndoorJourneyPhase.AskEntryPoint(destBuilding, entrances, phase.destRoom)
    }

    /**
     * Called when user picks which entrance they used.
     * Returns IndoorToDestination phase.
     */
    fun onEntranceSelected(
        phase:    IndoorJourneyPhase.AskEntryPoint,
        entrance: BuildingEntrance
    ): IndoorJourneyPhase {
        return IndoorJourneyPhase.IndoorToDestination(
            buildingCode = phase.destination.buildingCode,
            startFloor   = entrance.floor,   // floor the entrance is on (user starts here)
            startNodeId  = entrance.nodeId,
            destination  = phase.destination
        )
    }

    /**
     * Checks if [userGps] is within 40m of the destination building.
     * Used in processLocationUpdate to auto-trigger AskEntryPoint.
     */
    fun isNearBuilding(userGps: LatLng, buildingCode: String): Boolean {
        val allBuildings = CampusRepo.getAllBuildings()
        val building     = allBuildings.firstOrNull { it.code == buildingCode } ?: return false
        val center       = building.getCenter()
        return com.google.maps.android.SphericalUtil
            .computeDistanceBetween(userGps, center) < 60.0
    }
}
