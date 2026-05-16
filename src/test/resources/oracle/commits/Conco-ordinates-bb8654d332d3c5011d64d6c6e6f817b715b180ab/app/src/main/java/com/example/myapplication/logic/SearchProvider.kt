package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.telemetry.CrashReporter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await

sealed class SearchResult {
    data class BuildingResult(val building: Building) : SearchResult()
    data class CampusResult(val campus: Campus) : SearchResult()
    data class GoogleResult(val title: String, val address: String, val placeId: String) : SearchResult()

    /**
     * An indoor room result — e.g. searching "H-829" or "CC-119".
     * [buildingCode] and [floor] are used to open the correct IndoorMapScreen.
     * [nodeId] is the A* destination node (may be null if not yet resolved).
     */
    data class IndoorRoomResult(
        val buildingCode: String,
        val floor:        Int,
        val roomId:       String,   // e.g. "H-8-829"
        val nodeId:       String?,  // nav node linked to this room
        val label:        String    // display label e.g. "H-829 (Floor 8)"
    ) : SearchResult()

    object CurrentLocation : SearchResult()
    object Home : SearchResult()
}

class HybridSearchProvider(
    private val placesClient:  PlacesClient,
    private val indoorRepo:    IndoorRepository? = null   // optional — injected in MapsActivity
) {
    suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return listOf(SearchResult.CurrentLocation, SearchResult.Home)

        val campusMatches = CampusRepo.getAllCampuses().filter { campus ->
            campus.name.contains(query, ignoreCase = true)
        }.map { SearchResult.CampusResult(it) }

        val allBuildings = CampusRepo.getAllCampuses().flatMap { it.buildings }
        val localMatches = allBuildings.filter { building ->
            building.name.contains(query, ignoreCase = true) ||
            building.code.contains(query, ignoreCase = true) ||
            building.address.contains(query, ignoreCase = true)
        }.take(3).map { SearchResult.BuildingResult(it) }

        // ── Indoor room search ────────────────────────────────────────────────
        // Pattern: optional building code + dash + room number
        // Examples: "H-829", "H829", "CC-119", "h 829"
        val indoorMatches = searchIndoorRooms(query)

        val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()
        return try {
            val response = placesClient.findAutocompletePredictions(request).await()
            val googleMatches = response.autocompletePredictions.map { prediction ->
                SearchResult.GoogleResult(
                    title   = prediction.getPrimaryText(null).toString(),
                    address = prediction.getSecondaryText(null).toString(),
                    placeId = prediction.placeId
                )
            }
            // Indoor results shown first — more relevant than Google Places
            indoorMatches + campusMatches + localMatches + googleMatches
        } catch (e: Exception) {
            CrashReporter.setKey("search_query_length", query.length)
            CrashReporter.recordNonFatal(e, "places_autocomplete_failed")
            indoorMatches + campusMatches + localMatches
        }
    }

    /**
     * Tries to parse [query] as an indoor room reference.
     *
     * Supported formats (case-insensitive):
     *   "H-829"   → building H, room 829
     *   "H829"    → building H, room 829
     *   "CC-119"  → building CC, room 119
     *   "h 829"   → building H, room 829
     *
     * Returns up to 3 matching IndoorRoomResult items (one per floor where
     * a matching room exists, e.g. H-112 appears on floor 1 and floor 8).
     */
    private suspend fun searchIndoorRooms(query: String): List<SearchResult.IndoorRoomResult> {
        // Regex: (building code)(separator)(room number)
        val pattern = Regex(
            """^([A-Za-z]{1,3})\s*[-\s]?\s*(\d[\w.-]*)$""",
            RegexOption.IGNORE_CASE
        )
        val match = pattern.matchEntire(query.trim()) ?: return emptyList()

        val buildingCode = match.groupValues[1].uppercase()
        val roomSuffix   = match.groupValues[2]   // e.g. "829", "119", "112-2"

        // Building must exist in our indoor data
        val floorsForBuilding = floorsFor(buildingCode)
        if (floorsForBuilding.isEmpty()) return emptyList()

        val results = mutableListOf<SearchResult.IndoorRoomResult>()

        for (floor in floorsForBuilding) {
            val floorData = indoorRepo?.getFloor(buildingCode, floor) ?: continue
            // Extract the numeric part from a label like "H-829" → "829"
            // Then match rooms whose numeric part starts with roomSuffix (prefix match)
            // or is an exact match. This prevents "H-8" matching "H-258", "H-298" etc.
            val matchingRooms = floorData.rooms.filter { room ->
                val labelNum = room.label.substringAfterLast('-')
                val idNum    = room.id.substringAfterLast('-')
                labelNum.startsWith(roomSuffix, ignoreCase = true) ||
                idNum.startsWith(roomSuffix, ignoreCase = true)
            }
            for (room in matchingRooms) {
                val node = floorData.nodes.firstOrNull { it.roomId == room.id }
                results += SearchResult.IndoorRoomResult(
                    buildingCode = buildingCode,
                    floor        = floor,
                    roomId       = room.id,
                    nodeId       = node?.id,
                    label        = "${room.label} · ${buildingCode} Floor $floor"
                )
            }
        }

        return results.take(3)
    }

    /** Delegates to [IndoorBuildingConfig] — single source of truth for building floors. */
    private fun floorsFor(code: String): List<Int> =
        com.example.myapplication.data.indoor.IndoorBuildingConfig.floorsFor(code)
}
