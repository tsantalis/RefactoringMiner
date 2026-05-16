package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.data.indoor.IndoorRepository

/**
 * Finds a path that may cross multiple floors within one building.
 *
 * Transfer method is controlled by [TransferPreference]:
 *   - ANY           → pick whichever gives the shortest total path
 *   - ELEVATOR_ONLY → accessible mode, only elevators considered
 *   - ESCALATOR     → prefer escalator, fall back to elevator
 *   - STAIRS        → prefer staircase, fall back to elevator
 *
 * Nodes are matched across floors using [IndoorNode.elevatorGroupId].
 */
object CrossFloorNavigator {

    const val VIA_ELEVATOR  = "elevator"
    const val VIA_ESCALATOR = "escalator"
    const val VIA_STAIRCASE = "staircase"

    data class FloorSegment(
        val floor:    Int,
        val path:     List<IndoorNode>,
        val building: String
    )

    sealed class NavStep {
        data class Walk(val segment: FloorSegment) : NavStep()
        data class ChangeFloor(
            val fromFloor:    Int,
            val toFloor:      Int,
            val via:          String,   // VIA_ELEVATOR | VIA_ESCALATOR | VIA_STAIRCASE
            val building:     String,
            val targetNodeId: String    // entry node on the target floor
        ) : NavStep()
    }

    /**
     * Build a cross-floor route inside one building.
     *
     * @param preference  Controls which transfer types are used. Defaults to ANY.
     */
    suspend fun navigate(
        repo:         IndoorRepository,
        building:     String,
        startFloor:   Int,
        startNodeId:  String,
        targetFloor:  Int,
        targetNodeId: String,
        preference:   TransferPreference = TransferPreference.ANY
    ): List<NavStep> {

        // Same floor — simple A* only
        if (startFloor == targetFloor) {
            val floor = repo.getFloor(building, startFloor) ?: return emptyList()
            val path  = IndoorPathfinder.findPath(
                floor.nodes, floor.edges, startNodeId, targetNodeId,
                accessibleOnly = preference == TransferPreference.ELEVATOR_ONLY
            )
            return if (path.isEmpty()) emptyList()
            else listOf(NavStep.Walk(FloorSegment(startFloor, path, building)))
        }

        val startFloorData  = repo.getFloor(building, startFloor)  ?: return emptyList()
        val targetFloorData = repo.getFloor(building, targetFloor) ?: return emptyList()

        val f8Trans = targetFloorData.nodes.filter {
            it.type in listOf("ELEVATOR", "ESCALATOR", "STAIRCASE")
        }

        // Build transfer pairs from primary types, then fallback if needed
        data class TransferPair(
            val startNode:  IndoorNode,
            val targetNode: IndoorNode,
            val via:        String
        )

        fun pairsForTypes(types: List<String>): List<TransferPair> {
            val pairs = mutableListOf<TransferPair>()
            for (type in types) {
                val candidates = startFloorData.nodes.filter { it.type == type }
                for (sn in candidates) {
                    val groupId = sn.elevatorGroupId ?: continue
                    val tn = f8Trans.firstOrNull {
                        it.type == type && it.elevatorGroupId == groupId
                    } ?: continue
                    val via = when (type) {
                        "ELEVATOR"  -> VIA_ELEVATOR
                        "ESCALATOR" -> VIA_ESCALATOR
                        else        -> VIA_STAIRCASE
                    }
                    pairs.add(TransferPair(sn, tn, via))
                }
            }
            return pairs
        }

        // Try primary types first, then fallback
        var pairs = pairsForTypes(preference.primary)
        if (pairs.isEmpty() && preference.fallback.isNotEmpty()) {
            pairs = pairsForTypes(preference.fallback)
        }
        if (pairs.isEmpty()) return emptyList()

        val accessibleOnly = preference == TransferPreference.ELEVATOR_ONLY

        // Pick the pair with the lowest total A* cost
        var bestSteps: List<NavStep> = emptyList()
        var bestCost  = Float.MAX_VALUE

        for (pair in pairs) {
            val seg1 = IndoorPathfinder.findPath(
                startFloorData.nodes, startFloorData.edges,
                startNodeId, pair.startNode.id, accessibleOnly
            )
            if (seg1.isEmpty()) continue

            val seg2 = IndoorPathfinder.findPath(
                targetFloorData.nodes, targetFloorData.edges,
                pair.targetNode.id, targetNodeId, accessibleOnly
            )
            if (seg2.isEmpty()) continue

            val cost = pathCost(seg1) + pathCost(seg2)
            if (cost < bestCost) {
                bestCost  = cost
                bestSteps = listOf(
                    NavStep.Walk(FloorSegment(startFloor, seg1, building)),
                    NavStep.ChangeFloor(
                        fromFloor    = startFloor,
                        toFloor      = targetFloor,
                        via          = pair.via,
                        building     = building,
                        targetNodeId = pair.targetNode.id
                    ),
                    NavStep.Walk(FloorSegment(targetFloor, seg2, building))
                )
            }
        }

        return bestSteps
    }

    private fun pathCost(path: List<IndoorNode>): Float {
        var cost = 0f
        for (i in 0 until path.size - 1) {
            val a = path[i]; val b = path[i + 1]
            val dx = a.x - b.x; val dy = a.y - b.y
            cost += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        return cost
    }
}
