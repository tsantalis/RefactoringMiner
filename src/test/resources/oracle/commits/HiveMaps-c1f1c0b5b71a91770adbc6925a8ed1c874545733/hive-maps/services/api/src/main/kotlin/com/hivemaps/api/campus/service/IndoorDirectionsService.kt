package com.hivemaps.api.campus.service

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.atan2
import java.util.PriorityQueue
import com.hivemaps.api.campus.domain.IndoorNode
import com.hivemaps.api.campus.domain.IndoorEdge
import com.hivemaps.api.campus.domain.Direction
import com.hivemaps.api.campus.domain.DirectionType
import com.hivemaps.api.campus.domain.NoRouteFoundException
import com.hivemaps.api.campus.domain.NodeNotFoundException
import com.hivemaps.api.campus.repository.IndoorDirectionsRepository
import org.springframework.stereotype.Service

@Service
class IndoorDirectionsService(
    private val indoorDirectionsRepository: IndoorDirectionsRepository
) {
    private fun runDijkstra(startNode: IndoorNode, endNode: IndoorNode, accessibleOnly: Boolean): Pair<Map<String, IndoorEdge?>, Boolean> {
        val distances = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE } // the shortest distance to the node (node_id, distance)
        val connectingEdge = mutableMapOf<String, IndoorEdge?>().withDefault { null } // the edge connecting the node to the shortest path to the node (node_id, IndoorEdge)
        val visited = mutableMapOf<String, Boolean>().withDefault { false } // whether the node has already been visited or not (node_id, true/false)
        val pq = PriorityQueue<Pair<Double, IndoorNode>>(compareBy { it.first }) // the priority queue to sort the candidate indoor nodes (distance, IndoorNode)
        var pathFound = false

        distances[startNode.id] = 0.0
        pq.add(0.0 to startNode)

        // while there are IndoorNode candidates
        while (pq.isNotEmpty()) {
            val (dist, node) = pq.poll()

            // skip if the node has already been visited
            if (visited.getValue(node.id)) continue

            // break if the destination node has been found
            if (node.id == endNode.id) {
                pathFound = true
                break
            }

            // we can only traverse virtual nodes if they are the starting point
            if (node.id != startNode.id && node.isVirtual) continue

            // for each neighbouring node, add it to the list of candidates
            node.outgoingEdges.forEach { edge ->
                val newDist = dist + edge.distance
                if (newDist < distances.getValue(edge.endNode.id) && //criteria 1: endNode can be navigated to in a faster way
                   (!accessibleOnly || (edge.wheelchairAccessible && edge.endNode.wheelchairAccessible))) { //criteria 2: the path is wheelchair accessible if required
                    distances[edge.endNode.id] = newDist
                    connectingEdge[edge.endNode.id] = edge
                    pq.add(newDist to edge.endNode)
                }
            }

            visited[node.id] = true
        }

        return connectingEdge to pathFound
    }

    private fun reconstructPath(connectingEdge: Map<String, IndoorEdge?>, endNode: IndoorNode): List<IndoorEdge> {
        val path = mutableListOf<IndoorEdge>()
        var current: IndoorEdge? = connectingEdge.getValue(endNode.id)
        while (current != null) {
            // only add the edge if both the start node and the end node are not virtual
            if (!current.startNode.isVirtual && !current.endNode.isVirtual) {
                path.add(current)
            }
            current = connectingEdge.getValue(current.startNode.id)
        }

        return path.reversed()
    }
    
    // using Dijkstra's algorithm to find the shortest path
    private fun getPath(startNode: IndoorNode, endNode: IndoorNode, accessibleOnly: Boolean = false): List<IndoorEdge> {
        // the start and end nodes must be accessible if accessibleOnly is true
        if (accessibleOnly && !startNode.wheelchairAccessible) return emptyList()
        if (accessibleOnly && !endNode.wheelchairAccessible) return emptyList()

        val (connectingEdge, pathFound) = runDijkstra(startNode, endNode, accessibleOnly)

        if (!pathFound) {
            return emptyList()
        }

        return reconstructPath(connectingEdge, endNode)
    }

    // Returns the angle of an edge in degrees ( from -180 to 180 )
    private fun getAngle(edge: IndoorEdge): Double {
        val dx = edge.endNode.longitude - edge.startNode.longitude 
        val dy = edge.endNode.latitude - edge.startNode.latitude

        return Math.toDegrees(atan2(dy, dx))
    }

    private fun getDirectionType(edge: IndoorEdge, previousEdgeAngle: Double?, currentAngle: Double): DirectionType {
        return when {
            edge.startNode.floor != edge.endNode.floor -> DirectionType.UP_OR_DOWN
            previousEdgeAngle == null -> DirectionType.STRAIGHT
            else -> {
                val angleDiff = ((currentAngle - previousEdgeAngle) + 540) % 360 - 180
                when {
                    -45 < angleDiff && angleDiff <= 45 -> DirectionType.STRAIGHT
                    45 <= angleDiff && angleDiff <= 135 -> DirectionType.LEFT
                    -135 <= angleDiff && angleDiff <= -45 -> DirectionType.RIGHT
                    else -> DirectionType.BACK
                }
            }
        }
    }

    // assume first and last directions are "move straight"
    private fun getDirections(startNode: IndoorNode, endNode: IndoorNode, accessibleOnly: Boolean = false): List<Direction> {
        val path = getPath(startNode, endNode, accessibleOnly)

        if (path.isEmpty()) return emptyList()

        val directions = mutableListOf<Direction>()
        var previousEdgeAngle: Double? = null
        var nodes = mutableListOf<IndoorNode>()
        var distance = 0.0

        path.forEach { edge ->
            nodes.add(edge.startNode)
            val currentAngle = getAngle(edge)
            val directionType = getDirectionType(edge, previousEdgeAngle, currentAngle)

            if (directionType == DirectionType.STRAIGHT) {
                previousEdgeAngle = currentAngle
                distance += edge.distance
                return@forEach
            }
            if (distance > 0.0) {
                directions.add(
                    Direction(
                        direction = DirectionType.STRAIGHT,
                        distance = distance,
                        description = "Go straight %.2fm".format(distance),
                        nodes = nodes
                    )
                )
                distance = 0.0
            }

            val description = when (directionType) {
                DirectionType.LEFT -> "Turn left"
                DirectionType.RIGHT -> "Turn right"
                DirectionType.BACK -> "Turn around"
                DirectionType.UP_OR_DOWN -> "Take the stairs/escalator/elevator to floor ${edge.endNode.floor}"
                DirectionType.STRAIGHT -> "Go straight"
            }

            if (directionType == DirectionType.UP_OR_DOWN) {
                directions.add(
                    Direction(
                        direction = DirectionType.UP_OR_DOWN,
                        distance = 0.0,
                        description = description,
                        nodes = mutableListOf(edge.startNode, edge.endNode)
                    )
                )
                nodes = mutableListOf()
            }
            else {
                directions.add(
                    Direction(
                        direction = directionType,
                        distance = 0.0,
                        description = description,
                        nodes = mutableListOf(edge.startNode)
                    )
                )
                distance = edge.distance
                nodes = mutableListOf(edge.startNode)
            }

            previousEdgeAngle = currentAngle
        }

        // the last direction is assumed to be straight
        nodes.add(path.last().endNode)
        directions.add(
            Direction(
                direction = DirectionType.STRAIGHT,
                distance = distance,
                description = "Go straight %.2fm".format(distance),
                nodes = nodes
            )
        )

        return directions
    }

    fun getDirections(building: String, startNodeId: String, endNodeId: String, accessibleOnly: Boolean = false): List<Direction> {
        val nodes = indoorDirectionsRepository.findIndoorNodesByBuilding(building)
        val startNode = nodes[startNodeId] ?: throw NodeNotFoundException("Start node not found")
        val endNode = nodes[endNodeId] ?: throw NodeNotFoundException("End Node not found")
        val path = getDirections(startNode, endNode, accessibleOnly)

        if (path.isEmpty()) {
            throw NoRouteFoundException(startNodeId, endNodeId)
        }

        return path
    }

    fun getRooms(building: String, floor: String?): List<IndoorNode> {
        return indoorDirectionsRepository.findIndoorNodesByBuilding(building)
            .values
            .filter { it.label == "Room" && (floor == null || it.floor == floor)}
    }

    private fun getDistance(startLongitude: Double, startLatitude: Double, endLongitude: Double, endLatitude: Double): Double {
        val earthRadius = 6371000.0 //meter

        val dLat = Math.toRadians(endLatitude - startLatitude)
        val dLon = Math.toRadians(endLongitude - startLongitude)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(startLatitude)) * cos(Math.toRadians(endLatitude)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun getNearestNode(building: String, floor: String, longitude: Double, latitude: Double): IndoorNode {
        var closestDistance = Double.MAX_VALUE
        var closestNode: IndoorNode? = null

        indoorDirectionsRepository.findIndoorNodesByBuilding(building)
            .values
            .forEach {
                if (it.floor != floor) return@forEach

                val distance = getDistance(longitude, latitude, it.longitude, it.latitude)

                if (distance < closestDistance) {
                    closestDistance = distance
                    closestNode = it
                }
            }

        // the closest node must be within 20 meters to be valid
        if (closestNode == null || closestDistance > 20) {
            throw NodeNotFoundException("There are no nodes on $building $floor within 20m")
        }

        return closestNode
    }
}
  