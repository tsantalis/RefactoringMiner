package com.example.myapplication.logic

import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.data.indoor.IndoorRepository
import com.google.android.gms.maps.model.LatLng
import com.example.myapplication.logic.TransferPreference

/**
 * Orchestrates a complete multi-segment journey:
 *
 *   [Indoor A* in building A]
 *       → exit node → GPS handoff
 *   [Outdoor Google Maps]
 *       → entry GPS → entry node
 *   [Indoor A* in building B, possibly multi-floor]
 *
 * Each segment is independently navigable and the UI advances
 * the phase when the user signals arrival (manual button or
 * future GPS proximity check).
 */
object IndoorOutdoorRouter {

    // ── data model ────────────────────────────────────────────────────────────

    data class IndoorDestination(
        val building:   String,
        val floor:      Int,
        val nodeId:     String,
        val label:      String  // human-readable e.g. "H-829"
    )

    sealed class Segment {
        /** Walk inside a single building floor. */
        data class IndoorWalk(
            val building: String,
            val floor:    Int,
            val path:     List<com.example.myapplication.data.indoor.IndoorNode>,
            val instruction: String
        ) : Segment()

        /** Take elevator, escalator, or staircase to another floor. */
        data class FloorChange(
            val building:  String,
            val fromFloor: Int,
            val toFloor:   Int,
            /** "elevator" | "escalator" | "staircase" */
            val via:       String,
            val instruction: String
        ) : Segment()

        /** Walk outside between two buildings. */
        data class OutdoorWalk(
            val origin:      LatLng,
            val destination: LatLng,
            val originLabel: String,
            val destLabel:   String,
            val instruction: String
        ) : Segment()
    }

    data class FullRoute(
        val segments:    List<Segment>,
        val totalSteps:  Int            = segments.size
    )

    // ── builder ───────────────────────────────────────────────────────────────

    /**
     * Build a complete route from a starting indoor position
     * to a destination indoor position (potentially in a different building).
     *
     * @param repo             IndoorRepository
     * @param startBuilding    e.g. "CC"
     * @param startFloor       e.g. 1
     * @param startNodeId      current node id
     * @param destination      target room descriptor
     * @param userGps          current GPS (used to pick nearest exit)
     * @param accessible       prefer elevator + ramps
     */
    suspend fun buildRoute(
        repo:          IndoorRepository,
        startBuilding: String,
        startFloor:    Int,
        startNodeId:   String,
        destination:   IndoorDestination,
        userGps:       LatLng?,
        preference:    TransferPreference = TransferPreference.ANY
    ): FullRoute {

        val segments = mutableListOf<Segment>()

        val sameBuildingAndFloor =
            startBuilding == destination.building && startFloor == destination.floor

        // ── Case 1: same floor, same building ────────────────────────────────
        if (sameBuildingAndFloor) {
            val floor = repo.getFloor(startBuilding, startFloor) ?: return FullRoute(emptyList())
            val path  = IndoorPathfinder.findPath(
                floor.nodes, floor.edges, startNodeId, destination.nodeId, preference == TransferPreference.ELEVATOR_ONLY
            )
            segments += Segment.IndoorWalk(
                startBuilding, startFloor, path,
                "Walk to ${destination.label}"
            )
            return FullRoute(segments)
        }

        // ── Case 2: same building, different floor ────────────────────────────
        if (startBuilding == destination.building) {
            val steps = CrossFloorNavigator.navigate(
                repo, startBuilding,
                startFloor, startNodeId,
                destination.floor, destination.nodeId,
                preference
            )
            // Walk instructions: first Walk leads to the transfer node,
            // subsequent Walk leads to the destination.
            // Look ahead to find the ChangeFloor step to get the via type.
            val viaType = steps.filterIsInstance<CrossFloorNavigator.NavStep.ChangeFloor>()
                .firstOrNull()?.via ?: "elevator"

            steps.forEach { step ->
                when (step) {
                    is CrossFloorNavigator.NavStep.Walk ->
                        segments += Segment.IndoorWalk(
                            step.segment.building, step.segment.floor,
                            step.segment.path,
                            if (step.segment.floor == startFloor)
                                "Walk to the $viaType"
                            else
                                "Walk to ${destination.label}"
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

        // ── Case 3: different buildings ───────────────────────────────────────

        // 3a. Indoor walk to the best exit in startBuilding
        val startExits = BuildingEntrances.forBuilding(startBuilding)
        val bestExit   = if (userGps != null)
            BuildingEntrances.nearest(startBuilding, userGps) ?: startExits.firstOrNull()
        else startExits.firstOrNull()

        if (bestExit != null) {
            val startFloorData = repo.getFloor(startBuilding, startFloor)
            if (startFloorData != null) {
                val exitPath = IndoorPathfinder.findPath(
                    startFloorData.nodes, startFloorData.edges,
                    startNodeId, bestExit.nodeId, preference == TransferPreference.ELEVATOR_ONLY
                )
                if (exitPath.isNotEmpty()) {
                    segments += Segment.IndoorWalk(
                        startBuilding, startFloor, exitPath,
                        "Walk to ${bestExit.label}"
                    )
                }
            }
        }

        // 3b. Outdoor walk to destination building entrance
        val destEntrances = BuildingEntrances.forBuilding(destination.building)
        val bestEntry     = destEntrances.firstOrNull()   // closest to exit — refine later

        if (bestExit != null && bestEntry != null) {
            segments += Segment.OutdoorWalk(
                origin      = bestExit.gps,
                destination = bestEntry.gps,
                originLabel = bestExit.label,
                destLabel   = bestEntry.label,
                instruction = "Walk to ${destination.building} building"
            )
        }

        // 3c. Indoor walk (+ possible floor change) inside destination building
        if (bestEntry != null) {
            val steps = CrossFloorNavigator.navigate(
                repo, destination.building,
                bestEntry.floor, bestEntry.nodeId,
                destination.floor, destination.nodeId,
                preference
            )
            val viaType2 = steps.filterIsInstance<CrossFloorNavigator.NavStep.ChangeFloor>()
                .firstOrNull()?.via ?: "elevator"

            steps.forEach { step ->
                when (step) {
                    is CrossFloorNavigator.NavStep.Walk ->
                        segments += Segment.IndoorWalk(
                            step.segment.building, step.segment.floor,
                            step.segment.path,
                            if (step.segment.floor == bestEntry.floor)
                                "Walk to the $viaType2"
                            else
                                "Walk to ${destination.label}"
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
