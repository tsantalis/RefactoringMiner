package com.example.myapplication.logic

import com.example.myapplication.data.Building
import com.example.myapplication.data.Campus
import com.example.myapplication.data.CampusRepo
import com.example.myapplication.data.JsonLatLng
import com.example.myapplication.data.indoor.BuildingEntrance
import com.example.myapplication.data.indoor.BuildingEntrances
import com.example.myapplication.ui.models.IndoorJourneyPhase
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [IndoorJourneyHandler].
 *
 * Strategy: inject test data via CampusRepo.setTestCampuses() and
 * BuildingEntrances reflection — same approach used in MapManagerTest
 * and ShuttleRepoTest to avoid needing a real Context.
 */
class IndoorJourneyHandlerTest {

    // ── Shared test data ──────────────────────────────────────────────────────

    // Hall building: small square around (0.5, 0.5) in normalized lat/lng space
    private val hBuilding = Building(
        name    = "Henry F. Hall Building",
        code    = "H",
        wayID   = 1L,
        address = "1455 De Maisonneuve",
        outline = listOf(
            JsonLatLng(45.496, -73.580),
            JsonLatLng(45.498, -73.580),
            JsonLatLng(45.498, -73.578),
            JsonLatLng(45.496, -73.578)
        )
    )

    private val ccBuilding = Building(
        name    = "Central Building",
        code    = "CC",
        wayID   = 2L,
        address = "7141 Sherbrooke W",
        outline = listOf(
            JsonLatLng(45.457, -73.641),
            JsonLatLng(45.459, -73.641),
            JsonLatLng(45.459, -73.639),
            JsonLatLng(45.457, -73.639)
        )
    )

    private val testCampus = Campus(
        name      = "SGW",
        center    = JsonLatLng(45.497, -73.579),
        buildings = listOf(hBuilding, ccBuilding),
        outline   = null
    )

    private val hEntrance = BuildingEntrance(
        nodeId = "node-H-ent-south",
        label  = "H South Entrance",
        gps    = LatLng(45.4960, -73.5789),
        floor  = 1
    )

    private val ccEntrance = BuildingEntrance(
        nodeId = "node-CC-ent-east",
        label  = "CC East Entrance",
        gps    = LatLng(45.4584, -73.6407),
        floor  = 1
    )

    private val ccDestination = SearchResult.IndoorRoomResult(
        buildingCode = "CC",
        floor        = 1,
        roomId       = "CC-1-111",
        nodeId       = "node-cc-111",
        label        = "CC-111 · CC Floor 1"
    )

    private val hDestination = SearchResult.IndoorRoomResult(
        buildingCode = "H",
        floor        = 8,
        roomId       = "H-8-829",
        nodeId       = "node-h-829",
        label        = "H-829 · H Floor 8"
    )

    @Before
    fun setup() {
        CampusRepo.setTestCampuses(listOf(testCampus))
        injectEntrances(mapOf(
            "H"  to listOf(hEntrance),
            "CC" to listOf(ccEntrance)
        ))
    }

    /** Injects entrance data via reflection — same pattern as ShuttleRepo reset. */
    private fun injectEntrances(data: Map<String, List<BuildingEntrance>>) {
        val field = BuildingEntrances::class.java.getDeclaredField("data")
        field.isAccessible = true
        field.set(BuildingEntrances, data)
    }

    // ── onDestinationSelected ─────────────────────────────────────────────────

    @Test
    fun `onDestinationSelected returns DetectingLocation when userGps is null`() {
        val phase = IndoorJourneyHandler.onDestinationSelected(ccDestination, null)
        assertEquals(IndoorJourneyPhase.DetectingLocation, phase)
    }

    @Test
    fun `onDestinationSelected returns AskCurrentRoom when user is inside a building`() {
        // User is inside Hall Building outline
        val insideH = LatLng(45.4970, -73.5790)
        val phase = IndoorJourneyHandler.onDestinationSelected(ccDestination, insideH)
        assertTrue(phase is IndoorJourneyPhase.AskCurrentRoom)
        val ask = phase as IndoorJourneyPhase.AskCurrentRoom
        assertEquals("H", ask.currentBuilding.code)
        assertEquals(ccDestination, ask.destination)
    }

    @Test
    fun `onDestinationSelected returns Outdoor when user is outside all buildings`() {
        // User is at Loyola — outside all building outlines
        val outsideAll = LatLng(45.458, -73.640)
        val phase = IndoorJourneyHandler.onDestinationSelected(ccDestination, outsideAll)
        assertTrue(phase is IndoorJourneyPhase.Outdoor)
        val outdoor = phase as IndoorJourneyPhase.Outdoor
        assertEquals(outsideAll, outdoor.origin)
        assertEquals(ccDestination, outdoor.destRoom)
    }

    @Test
    fun `onDestinationSelected returns Idle when outside and no entrance data for destination`() {
        injectEntrances(emptyMap()) // remove entrance data
        val outsideAll = LatLng(0.0, 0.0)
        val phase = IndoorJourneyHandler.onDestinationSelected(ccDestination, outsideAll)
        assertEquals(IndoorJourneyPhase.Idle, phase)
    }

    // ── onCurrentRoomSelected ─────────────────────────────────────────────────

    @Test
    fun `onCurrentRoomSelected returns IndoorToDestination when same building`() {
        val phase = IndoorJourneyPhase.AskCurrentRoom(hBuilding, hDestination)
        val result = IndoorJourneyHandler.onCurrentRoomSelected(
            phase       = phase,
            startNodeId = "node-h-110",
            startLabel  = "H-110",
            startFloor  = 1
        )
        assertTrue(result is IndoorJourneyPhase.IndoorToDestination)
        val dest = result as IndoorJourneyPhase.IndoorToDestination
        assertEquals("H", dest.buildingCode)
        assertEquals(1, dest.startFloor)
        assertEquals("node-h-110", dest.startNodeId)
    }

    @Test
    fun `onCurrentRoomSelected same building is case insensitive`() {
        // building code "h" vs destination "H"
        val lowerBuilding = hBuilding.copy()
        val phase = IndoorJourneyPhase.AskCurrentRoom(lowerBuilding, hDestination)
        val result = IndoorJourneyHandler.onCurrentRoomSelected(
            phase, "node-h-110", "H-110", 1
        )
        assertTrue(result is IndoorJourneyPhase.IndoorToDestination)
    }

    @Test
    fun `onCurrentRoomSelected returns IndoorToExit when different building`() {
        val phase = IndoorJourneyPhase.AskCurrentRoom(hBuilding, ccDestination)
        val result = IndoorJourneyHandler.onCurrentRoomSelected(
            phase       = phase,
            startNodeId = "node-h-110",
            startLabel  = "H-110",
            startFloor  = 1
        )
        assertTrue(result is IndoorJourneyPhase.IndoorToExit)
        val exit = result as IndoorJourneyPhase.IndoorToExit
        assertEquals("H", exit.buildingCode)
        assertEquals("node-h-ent-south", exit.exitNodeId)
        assertEquals(ccDestination, exit.destination)
    }

    @Test
    fun `onCurrentRoomSelected returns Idle when different building and no exits`() {
        injectEntrances(mapOf("CC" to listOf(ccEntrance))) // no H entrance
        val phase = IndoorJourneyPhase.AskCurrentRoom(hBuilding, ccDestination)
        val result = IndoorJourneyHandler.onCurrentRoomSelected(
            phase, "node-h-110", "H-110"
        )
        assertEquals(IndoorJourneyPhase.Idle, result)
    }

    // ── onUserExited ──────────────────────────────────────────────────────────

    @Test
    fun `onUserExited returns Outdoor with CC entrance as destination`() {
        val exitPhase = IndoorJourneyPhase.IndoorToExit(
            buildingCode = "H",
            floor        = 1,
            startNodeId  = "node-h-110",
            exitNodeId   = "node-H-ent-south",
            destination  = ccDestination
        )
        val userGps = LatLng(45.4960, -73.5789)
        val result = IndoorJourneyHandler.onUserExited(exitPhase, userGps)

        assertTrue(result is IndoorJourneyPhase.Outdoor)
        val outdoor = result as IndoorJourneyPhase.Outdoor
        assertEquals(userGps, outdoor.origin)
        assertEquals(ccEntrance.gps, outdoor.destination)
        assertEquals(ccDestination, outdoor.destRoom)
    }

    @Test
    fun `onUserExited returns Idle when no entrance data for destination`() {
        injectEntrances(mapOf("H" to listOf(hEntrance))) // no CC entrance
        val exitPhase = IndoorJourneyPhase.IndoorToExit(
            buildingCode = "H", floor = 1,
            startNodeId  = "s", exitNodeId = "e",
            destination  = ccDestination
        )
        val result = IndoorJourneyHandler.onUserExited(exitPhase, LatLng(45.497, -73.579))
        assertEquals(IndoorJourneyPhase.Idle, result)
    }

    // ── onNearDestinationBuilding ─────────────────────────────────────────────

    @Test
    fun `onNearDestinationBuilding returns AskEntryPoint with CC building and entrances`() {
        val outdoorPhase = IndoorJourneyPhase.Outdoor(
            origin      = LatLng(45.497, -73.579),
            destination = LatLng(45.458, -73.640),
            destRoom    = ccDestination
        )
        val result = IndoorJourneyHandler.onNearDestinationBuilding(outdoorPhase)

        assertTrue(result is IndoorJourneyPhase.AskEntryPoint)
        val ask = result as IndoorJourneyPhase.AskEntryPoint
        assertEquals("CC", ask.building.code)
        assertEquals(listOf(ccEntrance), ask.entrances)
        assertEquals(ccDestination, ask.destination)
    }

    @Test
    fun `onNearDestinationBuilding returns Idle when building code not found`() {
        val unknownDest = ccDestination.copy(buildingCode = "UNKNOWN")
        val outdoorPhase = IndoorJourneyPhase.Outdoor(
            origin      = LatLng(45.497, -73.579),
            destination = LatLng(45.458, -73.640),
            destRoom    = unknownDest
        )
        val result = IndoorJourneyHandler.onNearDestinationBuilding(outdoorPhase)
        assertEquals(IndoorJourneyPhase.Idle, result)
    }

    // ── onEntranceSelected ────────────────────────────────────────────────────

    @Test
    fun `onEntranceSelected returns IndoorToDestination with entrance as start`() {
        val askPhase = IndoorJourneyPhase.AskEntryPoint(
            building    = ccBuilding,
            entrances   = listOf(ccEntrance),
            destination = ccDestination
        )
        val result = IndoorJourneyHandler.onEntranceSelected(askPhase, ccEntrance)

        assertTrue(result is IndoorJourneyPhase.IndoorToDestination)
        val dest = result as IndoorJourneyPhase.IndoorToDestination
        assertEquals("CC", dest.buildingCode)
        assertEquals(ccEntrance.floor, dest.startFloor)
        assertEquals(ccEntrance.nodeId, dest.startNodeId)
        assertEquals(ccDestination, dest.destination)
    }

    // ── isNearBuilding ────────────────────────────────────────────────────────

    @Test
    fun `isNearBuilding returns true when within 60m of building center`() {
        val nearH = LatLng(45.4970, -73.5789) // right at H center
        assertTrue(IndoorJourneyHandler.isNearBuilding(nearH, "H"))
    }

    @Test
    fun `isNearBuilding returns false when far from building`() {
        val farAway = LatLng(0.0, 0.0) // null island
        assertFalse(IndoorJourneyHandler.isNearBuilding(farAway, "H"))
    }

    @Test
    fun `isNearBuilding returns false for unknown building code`() {
        val anywhere = LatLng(45.497, -73.579)
        assertFalse(IndoorJourneyHandler.isNearBuilding(anywhere, "UNKNOWN"))
    }
}
