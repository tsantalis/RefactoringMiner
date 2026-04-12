package com.example.myapplication.logic

import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.data.indoor.IndoorRepository
import com.google.android.gms.maps.model.LatLng

/**
 * Orchestrates a complete multi-segment journey:
 *
 *   [Indoor A* in building A]
 *       → exit node → GPS handoff
 *   [Outdoor Google Maps]
 *       → entry GPS → entry node
 *   [Indoor A* in building B, possibly multi-floor]
 *
 * [buildRoute] delegates to three private methods — one per routing case —
 * to satisfy the SRP and reduce cognitive complexity below SonarCloud's limit.
 */
object IndoorOutdoorRouter {

    data class IndoorDestination(
        val building: String,
        val floor:    Int,
        val nodeId:   String,
        val label:    String
    )

    sealed class Segment {
        data class IndoorWalk(
            val building:    String,
            val floor:       Int,
            val path:        List<com.example.myapplication.data.indoor.IndoorNode>,
            val instruction: String
        ) : Segment()

        data class FloorChange(
            val building:    String,
            val fromFloor:   Int,
            val toFloor:     Int,
            val via:         String,
            val instruction: String
        ) : Segment()

        data class OutdoorWalk(
            val origin:      LatLng,
            val destination: LatLng,
            val originLabel: String,
            val destLabel:   String,
            val instruction: String
        ) : Segment()
    }

    data class FullRoute(
        val segments:   List<Segment>,
        val totalSteps: Int = segments.size
    )

    // ── public entry point ────────────────────────────────────────────────────

    suspend fun buildRoute(
        repo:          IndoorRepository,
        startBuilding: String,
        startFloor:    Int,
        startNodeId:   String,
        destination:   IndoorDestination,
        userGps:       LatLng?,
        preference:    TransferPreference = TransferPreference.ANY
    ): FullRoute {
        val sameFloor    = startBuilding == destination.building && startFloor == destination.floor
        val sameBuilding = startBuilding == destination.building

        return when {
            sameFloor    -> buildSingleFloorRoute(repo, startBuilding, startFloor,
                                startNodeId, destination, preference)
            sameBuilding -> buildCrossFloorRoute(repo, startBuilding, startFloor,
                                startNodeId, destination, preference)
            else         -> buildMultiBuildingRoute(repo, startBuilding, startFloor,
                                startNodeId, destination, userGps, preference)
        }
    }

    // ── Case 1: same building, same floor ─────────────────────────────────────

    private suspend fun buildSingleFloorRoute(
        repo:          IndoorRepository,
        startBuilding: String,
        startFloor:    Int,
        startNodeId:   String,
        destination:   IndoorDestination,
        preference:    TransferPreference
    ): FullRoute {
        val floor = repo.getFloor(startBuilding, startFloor) ?: return FullRoute(emptyList())
        val path  = IndoorPathfinder.findPath(
            floor.nodes, floor.edges, startNodeId, destination.nodeId,
            preference == TransferPreference.ELEVATOR_ONLY
        )
        return FullRoute(listOf(Segment.IndoorWalk(
            startBuilding, startFloor, path, "Walk to ${destination.label}"
        )))
    }

    // ── Case 2: same building, different floor ────────────────────────────────

    private suspend fun buildCrossFloorRoute(
        repo:          IndoorRepository,
        startBuilding: String,
        startFloor:    Int,
        startNodeId:   String,
        destination:   IndoorDestination,
        preference:    TransferPreference
    ): FullRoute {
        val steps = CrossFloorNavigator.navigate(
            repo, startBuilding, startFloor, startNodeId,
            destination.floor, destination.nodeId, preference
        )
        val viaType = steps.filterIsInstance<CrossFloorNavigator.NavStep.ChangeFloor>()
            .firstOrNull()?.via ?: "elevator"

        val segments = mutableListOf<Segment>()
        steps.forEach { step ->
            when (step) {
                is CrossFloorNavigator.NavStep.Walk ->
                    segments += Segment.IndoorWalk(
                        step.segment.building, step.segment.floor, step.segment.path,
                        if (step.segment.floor == startFloor) "Walk to the $viaType"
                        else "Walk to ${destination.label}"
                    )
                is CrossFloorNavigator.NavStep.ChangeFloor ->
                    segments += Segment.FloorChange(
                        step.building, step.fromFloor, step.toFloor, step.via,
                        "Take the ${step.via} to floor ${step.toFloor}"
                    )
            }
        }
        return FullRoute(segments)
    }

    // ── Case 3: different buildings ───────────────────────────────────────────

    private suspend fun buildMultiBuildingRoute(
        repo:          IndoorRepository,
        startBuilding: String,
        startFloor:    Int,
        startNodeId:   String,
        destination:   IndoorDestination,
        userGps:       LatLng?,
        preference:    TransferPreference
    ): FullRoute {
        val segments = mutableListOf<Segment>()

        // 3a. Walk to best exit in start building
        val bestExit = if (userGps != null)
            BuildingEntrances.nearest(startBuilding, userGps)
                ?: BuildingEntrances.forBuilding(startBuilding).firstOrNull()
        else BuildingEntrances.forBuilding(startBuilding).firstOrNull()

        if (bestExit != null) {
            val startFloorData = repo.getFloor(startBuilding, startFloor)
            if (startFloorData != null) {
                val exitPath = IndoorPathfinder.findPath(
                    startFloorData.nodes, startFloorData.edges,
                    startNodeId, bestExit.nodeId,
                    preference == TransferPreference.ELEVATOR_ONLY
                )
                if (exitPath.isNotEmpty()) {
                    segments += Segment.IndoorWalk(
                        startBuilding, startFloor, exitPath, "Walk to ${bestExit.label}"
                    )
                }
            }
        }

        // 3b. Outdoor walk to destination building entrance
        val bestEntry = BuildingEntrances.forBuilding(destination.building).firstOrNull()

        if (bestEntry != null) {
            bestExit?.let { exit ->
                segments += Segment.OutdoorWalk(
                    origin      = exit.gps,
                    destination = bestEntry.gps,
                    originLabel = exit.label,
                    destLabel   = bestEntry.label,
                    instruction = "Walk to ${destination.building} building"
                )
            }
        }

        // 3c. Indoor walk (+ possible floor change) inside destination building
        if (bestEntry != null) {
            val steps = CrossFloorNavigator.navigate(
                repo, destination.building,
                bestEntry.floor, bestEntry.nodeId,
                destination.floor, destination.nodeId, preference
            )
            val viaType = steps.filterIsInstance<CrossFloorNavigator.NavStep.ChangeFloor>()
                .firstOrNull()?.via ?: "elevator"

            steps.forEach { step ->
                when (step) {
                    is CrossFloorNavigator.NavStep.Walk ->
                        segments += Segment.IndoorWalk(
                            step.segment.building, step.segment.floor, step.segment.path,
                            if (step.segment.floor == bestEntry.floor) "Walk to the $viaType"
                            else "Walk to ${destination.label}"
                        )
                    is CrossFloorNavigator.NavStep.ChangeFloor ->
                        segments += Segment.FloorChange(
                            step.building, step.fromFloor, step.toFloor, step.via,
                            "Take the ${step.via} to floor ${step.toFloor}"
                        )
                }
            }
        }

        return FullRoute(segments)
    }
}
