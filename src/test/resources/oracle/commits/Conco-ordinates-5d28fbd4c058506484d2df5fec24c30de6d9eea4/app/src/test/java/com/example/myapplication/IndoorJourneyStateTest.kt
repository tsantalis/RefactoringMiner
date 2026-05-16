package com.example.myapplication.ui.models

import com.example.myapplication.data.Building
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.logic.SearchResult
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [IndoorJourneyPhase] and [IndoorJourneyState].
 * Pure JVM — exercises sealed class branches and data class methods.
 */
class IndoorJourneyStateTest {

    private val building = Building(
        name    = "Henry F. Hall Building",
        code    = "H",
        wayID   = 1L,
        address = "1455 De Maisonneuve Blvd W",
        outline = listOf(JsonLatLng(45.496, -73.580), JsonLatLng(45.498, -73.578))
    )

    private val destination = SearchResult.IndoorRoomResult(
        buildingCode = "CC",
        floor        = 1,
        roomId       = "CC-1-111",
        nodeId       = "node-cc-111",
        label        = "CC-111 · CC Floor 1"
    )

    private val entrance = BuildingEntrance(
        nodeId = "node-CC-1-ent",
        label  = "CC East Entrance",
        gps    = LatLng(45.458, -73.640),
        floor  = 1
    )

    // ── IndoorJourneyPhase.Idle ───────────────────────────────────────────────

    @Test
    fun `Idle phase is singleton object`() {
        assertSame(IndoorJourneyPhase.Idle, IndoorJourneyPhase.Idle)
    }

    // ── IndoorJourneyPhase.DetectingLocation ──────────────────────────────────

    @Test
    fun `DetectingLocation phase is singleton object`() {
        assertSame(IndoorJourneyPhase.DetectingLocation, IndoorJourneyPhase.DetectingLocation)
    }

    // ── IndoorJourneyPhase.AskCurrentRoom ─────────────────────────────────────

    @Test
    fun `AskCurrentRoom holds building and destination`() {
        val phase = IndoorJourneyPhase.AskCurrentRoom(building, destination)
        assertEquals(building, phase.currentBuilding)
        assertEquals(destination, phase.destination)
    }

    @Test
    fun `AskCurrentRoom data class equality`() {
        val phase1 = IndoorJourneyPhase.AskCurrentRoom(building, destination)
        val phase2 = IndoorJourneyPhase.AskCurrentRoom(building, destination)
        assertEquals(phase1, phase2)
    }

    // ── IndoorJourneyPhase.IndoorToExit ───────────────────────────────────────

    @Test
    fun `IndoorToExit holds correct fields`() {
        val phase = IndoorJourneyPhase.IndoorToExit(
            buildingCode = "H",
            floor        = 1,
            startNodeId  = "node-h-110",
            exitNodeId   = "node-h-ent-south",
            destination  = destination
        )
        assertEquals("H", phase.buildingCode)
        assertEquals(1, phase.floor)
        assertEquals("node-h-110", phase.startNodeId)
        assertEquals("node-h-ent-south", phase.exitNodeId)
        assertEquals(destination, phase.destination)
    }

    @Test
    fun `IndoorToExit data class copy works`() {
        val phase = IndoorJourneyPhase.IndoorToExit("H", 1, "s", "e", destination)
        val copy  = phase.copy(floor = 8)
        assertEquals(8, copy.floor)
        assertEquals("H", copy.buildingCode)
    }

    // ── IndoorJourneyPhase.Outdoor ────────────────────────────────────────────

    @Test
    fun `Outdoor holds origin, destination, and destRoom`() {
        val origin = LatLng(45.497, -73.579)
        val dest   = LatLng(45.458, -73.640)
        val phase  = IndoorJourneyPhase.Outdoor(origin, dest, destination)
        assertEquals(origin, phase.origin)
        assertEquals(dest, phase.destination)
        assertEquals(destination, phase.destRoom)
    }

    // ── IndoorJourneyPhase.AskEntryPoint ──────────────────────────────────────

    @Test
    fun `AskEntryPoint holds building, entrances, and destination`() {
        val entrances = listOf(entrance)
        val phase = IndoorJourneyPhase.AskEntryPoint(building, entrances, destination)
        assertEquals(building, phase.building)
        assertEquals(entrances, phase.entrances)
        assertEquals(destination, phase.destination)
    }

    // ── IndoorJourneyPhase.IndoorToDestination ────────────────────────────────

    @Test
    fun `IndoorToDestination holds all navigation fields`() {
        val phase = IndoorJourneyPhase.IndoorToDestination(
            buildingCode = "CC",
            startFloor   = 1,
            startNodeId  = "node-cc-ent",
            destination  = destination
        )
        assertEquals("CC", phase.buildingCode)
        assertEquals(1, phase.startFloor)
        assertEquals("node-cc-ent", phase.startNodeId)
        assertEquals(destination, phase.destination)
    }

    // ── IndoorJourneyPhase.Arrived ────────────────────────────────────────────

    @Test
    fun `Arrived is singleton object`() {
        assertSame(IndoorJourneyPhase.Arrived, IndoorJourneyPhase.Arrived)
    }

    // ── IndoorJourneyState ────────────────────────────────────────────────────

    @Test
    fun `IndoorJourneyState defaults to Idle phase with no error`() {
        val state = IndoorJourneyState()
        assertEquals(IndoorJourneyPhase.Idle, state.phase)
        assertNull(state.errorMessage)
    }

    @Test
    fun `IndoorJourneyState holds phase and error`() {
        val state = IndoorJourneyState(
            phase        = IndoorJourneyPhase.Arrived,
            errorMessage = "Something went wrong"
        )
        assertEquals(IndoorJourneyPhase.Arrived, state.phase)
        assertEquals("Something went wrong", state.errorMessage)
    }

    @Test
    fun `IndoorJourneyState data class copy works`() {
        val state = IndoorJourneyState()
        val updated = state.copy(phase = IndoorJourneyPhase.DetectingLocation)
        assertEquals(IndoorJourneyPhase.DetectingLocation, updated.phase)
        assertNull(updated.errorMessage)
    }

    @Test
    fun `IndoorJourneyState equality`() {
        val s1 = IndoorJourneyState(phase = IndoorJourneyPhase.Idle)
        val s2 = IndoorJourneyState(phase = IndoorJourneyPhase.Idle)
        assertEquals(s1, s2)
    }

    // ── canGoBack property ────────────────────────────────────────────────────

    @Test
    fun `Idle canGoBack is false`() {
        assertFalse(IndoorJourneyPhase.Idle.canGoBack)
    }

    @Test
    fun `DetectingLocation canGoBack is true`() {
        assertTrue(IndoorJourneyPhase.DetectingLocation.canGoBack)
    }

    @Test
    fun `AskCurrentRoom canGoBack is true`() {
        assertTrue(IndoorJourneyPhase.AskCurrentRoom(building, destination).canGoBack)
    }

    @Test
    fun `IndoorToExit canGoBack is true`() {
        val phase = IndoorJourneyPhase.IndoorToExit("H", 1, "s", "e", destination)
        assertTrue(phase.canGoBack)
    }

    @Test
    fun `Outdoor canGoBack is false`() {
        val phase = IndoorJourneyPhase.Outdoor(
            com.google.android.gms.maps.model.LatLng(45.497, -73.579),
            com.google.android.gms.maps.model.LatLng(45.458, -73.640),
            destination
        )
        assertFalse(phase.canGoBack)
    }

    @Test
    fun `AskEntryPoint canGoBack is true`() {
        val phase = IndoorJourneyPhase.AskEntryPoint(building, listOf(entrance), destination)
        assertTrue(phase.canGoBack)
    }

    @Test
    fun `IndoorToDestination canGoBack is false`() {
        val phase = IndoorJourneyPhase.IndoorToDestination("H", 1, "node-s", destination)
        assertFalse(phase.canGoBack)
    }

    @Test
    fun `Arrived canGoBack is false`() {
        assertFalse(IndoorJourneyPhase.Arrived.canGoBack)
    }
}
