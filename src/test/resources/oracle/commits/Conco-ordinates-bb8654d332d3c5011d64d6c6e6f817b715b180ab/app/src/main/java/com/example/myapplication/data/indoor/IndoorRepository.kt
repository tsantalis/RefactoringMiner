package com.example.myapplication.data.indoor

import android.content.Context
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class IndoorRepository(private val context: Context) {

    private val cache = mutableMapOf<String, IndoorFloor>()

    suspend fun getFloor(building: String, floor: Int): IndoorFloor? =
        withContext(Dispatchers.IO) {
            val key = "${building.lowercase()}_$floor"
            cache[key] ?: loadFromRaw(building, floor)?.also { cache[key] = it }
        }

    fun clearCache() = cache.clear()

    // ── private ───────────────────────────────────────────────────────────────

    private fun loadFromRaw(building: String, floor: Int): IndoorFloor? {
        // Android resource names cannot contain '-', so negative floors use 'n' prefix.
        // e.g. floor -2 → "indoor_mb_floorn2"
        val floorSuffix = if (floor < 0) "n${-floor}" else "$floor"
        val resName = "indoor_${building.lowercase()}_floor$floorSuffix"
        val resId   = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return null
        return try {
            val text = context.resources.openRawResource(resId).bufferedReader().readText()
            parse(JSONObject(text))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parse(obj: JSONObject): IndoorFloor {
        fun pts(arr: JSONArray) = (0 until arr.length()).map { i ->
            val p = arr.getJSONArray(i)
            Offset(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
        }

        val rooms = obj.optJSONArray("rooms").orEmpty().map { r ->
            IndoorRoom(
                id         = r.getString("id"),
                type       = r.optString("type", "other"),
                label      = r.optString("label", r.getString("id")),
                icon       = r.optString("icon").takeIf { it.isNotBlank() },
                polygon    = pts(r.getJSONArray("polygon")),
                accessible = r.optBoolean("accessible", true)
            )
        }

        val corridors = obj.optJSONArray("corridors").orEmpty().map { c ->
            IndoorCorridor(
                id      = c.getString("id"),
                polygon = pts(c.getJSONArray("polygon"))
            )
        }

        val nodes = obj.optJSONArray("nodes").orEmpty().map { n ->
            IndoorNode(
                id              = n.getString("id"),
                x               = n.getDouble("x").toFloat(),
                y               = n.getDouble("y").toFloat(),
                type            = n.optString("type", "CORRIDOR"),
                roomId          = n.optString("roomId").takeIf { it.isNotBlank() },
                elevatorGroupId = n.optString("elevatorGroupId").takeIf { it.isNotBlank() },
                accessible      = n.optBoolean("accessible", true)
            )
        }

        val edges = obj.optJSONArray("edges").orEmpty().map { e ->
            IndoorEdge(
                from       = e.getString("from"),
                to         = e.getString("to"),
                weight     = e.optDouble("weight", 1.0).toFloat(),
                surface    = e.optString("surface", "SMOOTH"),
                accessible = e.optBoolean("accessible", true)
            )
        }

        val pois = obj.optJSONArray("pois").orEmpty().map { p ->
            IndoorPoi(
                id     = p.getString("id"),
                type   = p.optString("type", "other"),
                label  = p.optString("label", ""),
                x      = p.getDouble("x").toFloat(),
                y      = p.getDouble("y").toFloat(),
                nodeId = p.optString("nodeId", "")
            )
        }

        val entrances = obj.optJSONArray("entrances").orEmpty().map { e ->
            IndoorEntrance(
                id     = e.getString("id"),
                label  = e.optString("label", ""),
                x      = e.getDouble("x").toFloat(),
                y      = e.getDouble("y").toFloat(),
                nodeId = e.optString("nodeId", ""),
                floor  = e.optInt("floor", 1)
            )
        }

        return IndoorFloor(
            building  = obj.getString("building"),
            floor     = obj.getInt("floor"),
            rooms     = rooms,
            corridors = corridors,
            nodes     = nodes,
            edges     = edges,
            pois      = pois,
            entrances = entrances
        )
    }

    // Helper: treat null JSONArray as empty list
    private fun JSONObject.optJSONArray(name: String) =
        if (has(name)) getJSONArray(name) else JSONArray()

    private fun JSONArray.orEmpty() = (0 until length()).map { getJSONObject(it) }
}
