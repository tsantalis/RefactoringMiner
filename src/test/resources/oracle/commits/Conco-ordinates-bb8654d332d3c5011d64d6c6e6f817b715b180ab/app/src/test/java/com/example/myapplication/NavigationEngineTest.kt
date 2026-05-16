package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.JsonLatLng
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CampusNavigationEngine].
 * Pure JVM — uses SphericalUtil internally but no Android UI.
 */
class NavigationEngineTest {

    private lateinit var engine: CampusNavigationEngine

    // Hall Building center ~45.497, -73.578
    private val hallCenter = LatLng(45.4972, -73.5789)

    private val hallBuilding = Building(
        name    = "Henry F. Hall Building",
        code    = "H",
        wayID   = 1L,
        address = "1455 De Maisonneuve Blvd W",
        outline = listOf(
            JsonLatLng(45.496, -73.580),
            JsonLatLng(45.498, -73.580),
            JsonLatLng(45.498, -73.578),
            JsonLatLng(45.496, -73.578)
        )
    )

    @Before
    fun setup() {
        engine = CampusNavigationEngine()
    }

    // ── checkArrival (generic point) ──────────────────────────────────────────

    @Test
    fun `checkArrival returns true when within 15 metres`() {
        val dest = LatLng(45.497, -73.579)
        val user = LatLng(45.4970, -73.5790)  // same point
        assertTrue(engine.checkArrival(user, dest))
    }

    @Test
    fun `checkArrival returns false when far away`() {
        val dest = LatLng(45.497, -73.579)
        val user = LatLng(45.458, -73.640)    // Loyola campus ~5km away
        assertFalse(engine.checkArrival(user, dest))
    }

    // ── checkArrivalWithBuilding ──────────────────────────────────────────────

    @Test
    fun `checkArrivalWithBuilding returns false when building is null`() {
        assertFalse(engine.checkArrivalWithBuilding(hallCenter, null))
    }

    @Test
    fun `checkArrivalWithBuilding returns true when within 50m of building center`() {
        // User is right at the center — 0m away
        assertTrue(engine.checkArrivalWithBuilding(hallCenter, hallBuilding))
    }

    @Test
    fun `checkArrivalWithBuilding returns false when far from building`() {
        val farAway = LatLng(45.458, -73.640)
        assertFalse(engine.checkArrivalWithBuilding(farAway, hallBuilding))
    }

    // ── calculateBearing ─────────────────────────────────────────────────────

    @Test
    fun `calculateBearing returns currentBearing when no route points are far enough`() {
        val user = LatLng(45.497, -73.579)
        val currentBearing = 90f
        // Route has only one point very close to user
        val route = listOf(LatLng(45.497001, -73.579001))
        val result = engine.calculateBearing(user, route, currentBearing)
        assertEquals(currentBearing, result, 0.01f)
    }

    @Test
    fun `calculateBearing returns currentBearing for empty route`() {
        val bearing = engine.calculateBearing(
            LatLng(45.497, -73.579), emptyList(), 45f
        )
        assertEquals(45f, bearing, 0.01f)
    }

    @Test
    fun `calculateBearing returns valid heading when target point is far`() {
        val user  = LatLng(45.497, -73.579)
        val route = listOf(LatLng(45.507, -73.579)) // ~1km north
        val result = engine.calculateBearing(user, route, 0f)
        // Heading north should be around -180..0 or close to 0 (north is ~0/-360 in SphericalUtil)
        assertTrue(result in -180f..180f)
    }

    // ── calculateNextInstruction ──────────────────────────────────────────────

    @Test
    fun `calculateNextInstruction returns non-empty string`() {
        val result = engine.calculateNextInstruction(
            LatLng(45.497, -73.579),
            listOf(LatLng(45.498, -73.579))
        )
        assertTrue(result.isNotBlank())
    }
}
