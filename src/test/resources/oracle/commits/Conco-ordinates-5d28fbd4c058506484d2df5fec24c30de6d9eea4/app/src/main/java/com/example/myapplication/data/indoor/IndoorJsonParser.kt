package com.example.myapplication.data.indoor

import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses a raw JSON object into an [IndoorFloor] domain model.
 *
 * Extracted from [IndoorRepository] so that:
 * - The repository focuses on data management and caching (SRP)
 * - Parsing logic can be unit-tested independently with static JSON strings
 *   without needing an Android Context or file system
 */
class IndoorJsonParser {

    fun parse(obj: JSONObject): IndoorFloor {
        val rooms     = safeArray(obj, "rooms").orEmpty().map(::parseRoom)
        val corridors = safeArray(obj, "corridors").orEmpty().map(::parseCorridor)
        val nodes     = safeArray(obj, "nodes").orEmpty().map(::parseNode)
        val edges     = safeArray(obj, "edges").orEmpty().map(::parseEdge)
        val pois      = safeArray(obj, "pois").orEmpty().map(::parsePoi)
        val entrances = safeArray(obj, "entrances").orEmpty().map(::parseEntrance)

        return IndoorFloor(
            building  = obj.optString("building", ""),
            floor     = obj.optInt("floor", 1),
            rooms     = rooms,
            corridors = corridors,
            nodes     = nodes,
            edges     = edges,
            pois      = pois,
            entrances = entrances
        )
    }

    /** Safe helper — avoids name clash with JSONObject.optJSONArray() whose
     *  platform implementation may return null despite @NonNull annotation. */
    private fun safeArray(obj: JSONObject, key: String): JSONArray =
        if (obj.has(key)) obj.getJSONArray(key) ?: JSONArray() else JSONArray()

    // ── private parsers ───────────────────────────────────────────────────────

    private fun parseRoom(r: JSONObject) = IndoorRoom(
        id         = r.optString("id", ""),
        type       = r.optString("type", "other"),
        label      = r.optString("label", r.optString("id", "")),
        icon       = r.optString("icon", "").takeIf { it.isNotBlank() },
        polygon    = pts(safeArray(r, "polygon")),
        accessible = r.optBoolean("accessible", true)
    )

    private fun parseCorridor(c: JSONObject) = IndoorCorridor(
        id      = c.optString("id", ""),
        polygon = pts(safeArray(c, "polygon"))
    )

    private fun parseNode(n: JSONObject) = IndoorNode(
        id              = n.optString("id", ""),
        x               = n.optDouble("x", 0.0).toFloat(),
        y               = n.optDouble("y", 0.0).toFloat(),
        type            = n.optString("type", "CORRIDOR"),
        roomId          = n.optString("roomId", "").takeIf { it.isNotBlank() },
        elevatorGroupId = n.optString("elevatorGroupId", "").takeIf { it.isNotBlank() },
        accessible      = n.optBoolean("accessible", true)
    )

    private fun parseEdge(e: JSONObject) = IndoorEdge(
        from       = e.optString("from", ""),
        to         = e.optString("to", ""),
        weight     = e.optDouble("weight", 1.0).toFloat(),
        surface    = e.optString("surface", "SMOOTH"),
        accessible = e.optBoolean("accessible", true)
    )

    private fun parsePoi(p: JSONObject) = IndoorPoi(
        id     = p.optString("id", ""),
        type   = p.optString("type", "other"),
        label  = p.optString("label", ""),
        x      = p.optDouble("x", 0.0).toFloat(),
        y      = p.optDouble("y", 0.0).toFloat(),
        nodeId = p.optString("nodeId", "")
    )

    private fun parseEntrance(e: JSONObject) = IndoorEntrance(
        id     = e.optString("id", ""),
        label  = e.optString("label", ""),
        x      = e.optDouble("x", 0.0).toFloat(),
        y      = e.optDouble("y", 0.0).toFloat(),
        nodeId = e.optString("nodeId", ""),
        floor  = e.optInt("floor", 1)
    )

    private fun pts(arr: JSONArray) = (0 until arr.length()).map { i ->
        val p = arr.getJSONArray(i) ?: JSONArray()
        Offset(p.optDouble(0, 0.0).toFloat(), p.optDouble(1, 0.0).toFloat())
    }

    // ── extension helpers ─────────────────────────────────────────────────────

    private fun JSONArray.orEmpty() = (0 until length()).map { getJSONObject(it) }
}
