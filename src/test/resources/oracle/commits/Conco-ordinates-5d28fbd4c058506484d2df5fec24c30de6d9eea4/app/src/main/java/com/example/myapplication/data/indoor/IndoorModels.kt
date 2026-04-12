package com.example.myapplication.data.indoor

import androidx.compose.ui.geometry.Offset

/**
 * Type-safe enum for room types.
 * Replaces raw String values — prevents typos and makes when exhaustive.
 * [raw] is the JSON string value so parsing stays backward-compatible.
 */
enum class RoomType(val raw: String) {
    CLASSROOM("classroom"),
    OFFICE("office"),
    WASHROOM("washroom"),
    ELEVATOR("elevator"),
    STAIRCASE("staircase"),
    ESCALATOR("escalator"),
    OTHER("other");

    companion object {
        fun fromRaw(raw: String): RoomType =
            entries.firstOrNull { it.raw == raw.lowercase() } ?: OTHER
    }
}

/**
 * Type-safe enum for nav node types.
 * [raw] is the JSON string value so parsing stays backward-compatible.
 */
enum class NodeType(val raw: String) {
    CORRIDOR("CORRIDOR"),
    ROOM("ROOM"),
    ELEVATOR("ELEVATOR"),
    ESCALATOR("ESCALATOR"),
    STAIRCASE("STAIRCASE"),
    ENTRANCE("ENTRANCE");

    companion object {
        fun fromRaw(raw: String): NodeType =
            entries.firstOrNull { it.raw == raw.uppercase() } ?: CORRIDOR
    }
}

data class IndoorRoom(
    val id: String,
    val type: String,           // classroom | office | washroom | elevator | staircase | other
    val label: String,
    val icon: String? = null,   // emoji e.g. "🚻"
    val polygon: List<Offset>,  // normalized 0.0–1.0 coords
    val accessible: Boolean = true
)

data class IndoorCorridor(
    val id: String,
    val polygon: List<Offset>
)

data class IndoorNode(
    val id: String,
    val x: Float,
    val y: Float,
    val type: String,           // CORRIDOR | ROOM | ELEVATOR | STAIRCASE | ENTRANCE
    val roomId: String? = null,
    val elevatorGroupId: String? = null,
    val accessible: Boolean = true
)

data class IndoorEdge(
    val from: String,
    val to: String,
    val weight: Float = 1f,
    val surface: String = "SMOOTH",
    val accessible: Boolean = true
)

data class IndoorPoi(
    val id: String,
    val type: String,
    val label: String,
    val x: Float,
    val y: Float,
    val nodeId: String = ""
)

data class IndoorEntrance(
    val id: String,
    val label: String = "",
    val x: Float,
    val y: Float,
    val nodeId: String = "",
    val floor: Int = 1
)

data class IndoorFloor(
    val building: String,
    val floor: Int,
    val rooms: List<IndoorRoom> = emptyList(),
    val corridors: List<IndoorCorridor> = emptyList(),
    val nodes: List<IndoorNode> = emptyList(),
    val edges: List<IndoorEdge> = emptyList(),
    val pois: List<IndoorPoi> = emptyList(),
    val entrances: List<IndoorEntrance> = emptyList()
)
