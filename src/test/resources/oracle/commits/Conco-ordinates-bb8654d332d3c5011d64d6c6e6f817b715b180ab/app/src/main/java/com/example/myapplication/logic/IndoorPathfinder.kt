package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorEdge
import com.example.myapplication.data.indoor.IndoorNode
import java.util.PriorityQueue
import kotlin.math.sqrt

object IndoorPathfinder {

    /**
     * A* pathfinding on a normalized indoor nav graph.
     *
     * @param nodes        all nodes for the floor
     * @param edges        all edges for the floor (treated as bidirectional)
     * @param startId      node id of the starting point
     * @param endId        node id of the destination
     * @param accessibleOnly  if true, skip edges where accessible=false
     * @return ordered list of nodes from start to end, empty if no path found
     */
    fun findPath(
        nodes:          List<IndoorNode>,
        edges:          List<IndoorEdge>,
        startId:        String,
        endId:          String,
        accessibleOnly: Boolean = false
    ): List<IndoorNode> {
        if (startId == endId) return listOf(nodes.first { it.id == startId })

        val nodeMap = nodes.associateBy { it.id }
        val goal    = nodeMap[endId] ?: return emptyList()
        if (nodeMap[startId] == null) return emptyList()

        // Build adjacency list (bidirectional)
        val adj = HashMap<String, MutableList<Pair<String, Float>>>()
        edges.forEach { edge ->
            if (accessibleOnly && !edge.accessible) return@forEach
            adj.getOrPut(edge.from) { mutableListOf() }.add(edge.to   to edge.weight)
            adj.getOrPut(edge.to)   { mutableListOf() }.add(edge.from to edge.weight)
        }

        val gScore   = HashMap<String, Float>().apply { put(startId, 0f) }
        val fScore   = HashMap<String, Float>().apply { put(startId, h(nodeMap[startId]!!, goal)) }
        val cameFrom = HashMap<String, String>()
        val closed   = HashSet<String>()
        val openSet  = PriorityQueue<String>(compareBy { fScore[it] ?: Float.MAX_VALUE })
        openSet.add(startId)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()!!
            if (current == endId) return reconstruct(cameFrom, nodeMap, endId)
            closed.add(current)

            adj[current]?.forEach { (neighbor, weight) ->
                if (neighbor in closed) return@forEach
                val tentativeG = (gScore[current] ?: Float.MAX_VALUE) + weight
                if (tentativeG < (gScore[neighbor] ?: Float.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor]   = tentativeG
                    fScore[neighbor]   = tentativeG + h(nodeMap[neighbor] ?: return@forEach, goal)
                    if (!openSet.contains(neighbor)) openSet.add(neighbor)
                }
            }
        }
        return emptyList() // no path
    }

    /** Euclidean distance heuristic (coords are normalized 0–1) */
    private fun h(a: IndoorNode, b: IndoorNode): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun reconstruct(
        cameFrom: Map<String, String>,
        nodeMap:  Map<String, IndoorNode>,
        endId:    String
    ): List<IndoorNode> {
        val path = mutableListOf<IndoorNode>()
        var cur: String? = endId
        while (cur != null) {
            nodeMap[cur]?.let { path.add(it) }
            cur = cameFrom[cur]
        }
        return path.reversed()
    }
}
