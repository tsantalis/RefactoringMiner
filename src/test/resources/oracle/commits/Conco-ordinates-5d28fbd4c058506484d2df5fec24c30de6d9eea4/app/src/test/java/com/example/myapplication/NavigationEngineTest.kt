package com.example.myapplication.logic

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CampusNavigationEngine].
 *
 * [checkArrivalWithBuilding] now accepts a [LatLng] center rather than a
 * Building object — the logic layer is decoupled from the data layer.
 */
class NavigationEngineTest {

    private lateinit var engine: CampusNavigationEngine

    // Hall Building center ~45.497, -73.578
    private val hallCenter = LatLng(45.4972, -73.5789)

    @Before
    fun setup() {
        engine = CampusNavigationEngine()
    }

    // ── checkArrival (generic point) ──────────────────────────────────────────

    @Test
    fun `checkArrival returns true when within 15 metres`() {
        val dest = LatLng(45.497, -73.579)
        val user = LatLng(45.4970, -73.5790)
        assertTrue(engine.checkArrival(user, dest))
    }

    @Test
    fun `checkArrival returns false when far away`() {
        val dest = LatLng(45.497, -73.579)
        val user = LatLng(45.458, -73.640)
        assertFalse(engine.checkArrival(user, dest))
    }

    // ── checkArrivalWithBuilding ──────────────────────────────────────────────

    @Test
    fun `checkArrivalWithBuilding returns false when buildingCenter is null`() {
        assertFalse(engine.checkArrivalWithBuilding(hallCenter, null))
    }

    @Test
    fun `checkArrivalWithBuilding returns true when within 50m of building center`() {
        // User is at the center — 0m away, well within 50m default radius
        assertTrue(engine.checkArrivalWithBuilding(hallCenter, hallCenter))
    }

    @Test
    fun `checkArrivalWithBuilding returns false when far from building center`() {
        val farAway = LatLng(45.458, -73.640)
        assertFalse(engine.checkArrivalWithBuilding(farAway, hallCenter))
    }

    @Test
    fun `checkArrivalWithBuilding respects custom radius`() {
        // 5m away from center
        val nearby = LatLng(45.49725, -73.5789)
        assertTrue(engine.checkArrivalWithBuilding(nearby, hallCenter, radiusMetres = 10.0))
        assertFalse(engine.checkArrivalWithBuilding(nearby, hallCenter, radiusMetres = 1.0))
    }

    // ── calculateBearing ─────────────────────────────────────────────────────

    @Test
    fun `calculateBearing returns currentBearing when no route points are far enough`() {
        val user = LatLng(45.497, -73.579)
        val route = listOf(LatLng(45.497001, -73.579001))
        val result = engine.calculateBearing(user, route, 90f)
        assertEquals(90f, result, 0.01f)
    }

    @Test
    fun `calculateBearing returns currentBearing for empty route`() {
        val bearing = engine.calculateBearing(LatLng(45.497, -73.579), emptyList(), 45f)
        assertEquals(45f, bearing, 0.01f)
    }

    @Test
    fun `calculateBearing returns valid heading when target point is far`() {
        val user  = LatLng(45.497, -73.579)
        val route = listOf(LatLng(45.507, -73.579)) // ~1km north
        val result = engine.calculateBearing(user, route, 0f)
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
