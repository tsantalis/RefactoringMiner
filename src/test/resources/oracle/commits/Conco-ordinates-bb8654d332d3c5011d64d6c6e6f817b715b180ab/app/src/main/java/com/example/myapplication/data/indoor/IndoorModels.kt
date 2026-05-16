package com.example.myapplication.data.indoor

import androidx.compose.ui.geometry.Offset

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
