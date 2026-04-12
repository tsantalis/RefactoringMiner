package com.example.myapplication.data.indoor

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

data class BuildingEntrance(
    val nodeId: String,
    val label:  String,
    val gps:    LatLng,
    val floor:  Int = 1
)

object BuildingEntrances {

    private var data: Map<String, List<BuildingEntrance>> = emptyMap()

    /**
     * Must be called once at app startup (e.g. in MapsActivity.onCreate)
     * before any navigation is triggered.
     */
    fun initialize(context: Context) {
        if (data.isNotEmpty()) return  // already loaded
        data = loadFromRaw(context)
    }

    /** All entrances for a building code (e.g. "CC", "H"). */
    fun forBuilding(code: String): List<BuildingEntrance> =
        data[code.uppercase()] ?: emptyList()

    /** Entrance closest to a GPS point — uses Haversine via SphericalUtil for accuracy. */
    fun nearest(code: String, gps: LatLng): BuildingEntrance? =
        forBuilding(code).minByOrNull { e ->
            com.google.maps.android.SphericalUtil.computeDistanceBetween(e.gps, gps)
        }

    // ── JSON parsing ──────────────────────────────────────────────────────────

    private fun loadFromRaw(context: Context): Map<String, List<BuildingEntrance>> {
        return try {
            val resId = context.resources.getIdentifier(
                "building_entrances", "raw", context.packageName
            )
            if (resId == 0) return emptyMap()

            val json = context.resources.openRawResource(resId)
                .bufferedReader()
                .use { it.readText() }

            parseJson(json)
        } catch (e: Exception) {
            android.util.Log.e("BuildingEntrances", "Failed to load building_entrances.json: ${e.message}")
            emptyMap()
        }
    }

    @VisibleForTesting
    internal fun parseJson(json: String): Map<String, List<BuildingEntrance>> {
        val root   = JSONObject(json)
        val result = mutableMapOf<String, List<BuildingEntrance>>()

        // Use names() instead of keys() — safer under org.json in JVM unit test environments
        val names = root.names() ?: return result
        for (i in 0 until names.length()) {
            val buildingCode = names.getString(i)
            val arr      = root.getJSONArray(buildingCode)
            val entrances = (0 until arr.length()).map { j ->
                val obj = arr.getJSONObject(j)
                BuildingEntrance(
                    nodeId = obj.getString("nodeId"),
                    label  = obj.getString("label"),
                    gps    = LatLng(obj.getDouble("lat"), obj.getDouble("lng")),
                    floor  = obj.optInt("floor", 1)
                )
            }
            result[buildingCode.uppercase()] = entrances
        }

        return result
    }
}
//  //{ "nodeId": "node-CC-1-ent-e", "label": "CC East Entrance", "lat": 45.458460, "lng": -73.640729, "floor": 1 }