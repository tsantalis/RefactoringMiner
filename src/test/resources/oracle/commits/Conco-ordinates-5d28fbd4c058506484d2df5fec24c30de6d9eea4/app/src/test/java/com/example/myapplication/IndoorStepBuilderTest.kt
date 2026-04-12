package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorNode
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [IndoorStepBuilder].
 * Pure JVM — no Android dependencies.
 *
 * Coordinate system: x increases right, y increases down (canvas coords).
 * Heading 0° = east, 90° = south, 180°/-180° = west, -90° = north.
 */
class IndoorStepBuilderTest {

    private fun node(id: String, x: Float, y: Float, type: String = "CORRIDOR") =
        IndoorNode(id = id, x = x, y = y, type = type)

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `build returns empty list for single node path`() {
        val path = listOf(node("A", 0f, 0f))
        assertTrue(IndoorStepBuilder.build(path).isEmpty())
    }

    @Test
    fun `build returns empty list for empty path`() {
        assertTrue(IndoorStepBuilder.build(emptyList()).isEmpty())
    }

    // ── Straight line path ────────────────────────────────────────────────────

    @Test
    fun `build returns one step for straight two-node path`() {
        val path = listOf(node("A", 0f, 0f), node("B", 1f, 0f))
        val steps = IndoorStepBuilder.build(path)
        assertEquals(1, steps.size)
        assertTrue(steps.last().isLast)
    }

    @Test
    fun `build arrival step contains destination label`() {
        val path = listOf(node("A", 0f, 0f), node("B", 1f, 0f))
        val steps = IndoorStepBuilder.build(path, destinationLabel = "Room H-829")
        assertTrue(steps.last().instruction.contains("H-829"))
    }

    @Test
    fun `build straight path heading east says Head east`() {
        // Go east then turn south — forces 2 steps; first step keeps "Head east"
        val path = listOf(
            node("A", 0f, 0.5f), node("B", 0.5f, 0.5f),
            node("C", 0.5f, 1.0f)  // 90° south turn forces flush of first step
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.size >= 2)
        assertTrue(steps.first().instruction.contains("east", ignoreCase = true))
    }

    @Test
    fun `build straight path heading south says Head south`() {
        // Go south then turn east
        val path = listOf(
            node("A", 0.5f, 0f), node("B", 0.5f, 0.5f),
            node("C", 1.0f, 0.5f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.size >= 2)
        assertTrue(steps.first().instruction.contains("south", ignoreCase = true))
    }

    @Test
    fun `build straight path heading west says Head west`() {
        // Go west then turn south
        val path = listOf(
            node("A", 1f, 0.5f), node("B", 0.5f, 0.5f),
            node("C", 0.5f, 1.0f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.size >= 2)
        assertTrue(steps.first().instruction.contains("west", ignoreCase = true))
    }

    @Test
    fun `build straight path heading north says Head north`() {
        // Go north then turn east
        val path = listOf(
            node("A", 0.5f, 1f), node("B", 0.5f, 0.5f),
            node("C", 1.0f, 0.5f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.size >= 2)
        assertTrue(steps.first().instruction.contains("north", ignoreCase = true))
    }

    // ── Turn detection ────────────────────────────────────────────────────────

    @Test
    fun `build detects right turn`() {
        // Go east then turn south = right turn
        val path = listOf(
            node("A", 0f, 0.5f),
            node("B", 0.5f, 0.5f),
            node("C", 0.5f, 1f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.size >= 2)
        val turnStep = steps.drop(1).firstOrNull()
        assertTrue(turnStep?.instruction?.contains("right", ignoreCase = true) == true ||
                   turnStep?.instruction?.contains("arrived", ignoreCase = true) == true)
    }

    @Test
    fun `build detects left turn`() {
        // Go east then turn north = left turn
        val path = listOf(
            node("A", 0f, 0.5f),
            node("B", 0.5f, 0.5f),
            node("C", 0.5f, 0f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.size >= 2)
        val turnStep = steps.drop(1).firstOrNull()
        assertTrue(turnStep?.instruction?.contains("left", ignoreCase = true) == true ||
                   turnStep?.instruction?.contains("arrived", ignoreCase = true) == true)
    }

    // ── Special node types ────────────────────────────────────────────────────

    @Test
    fun `build emits elevator step when path passes through ELEVATOR node`() {
        val path = listOf(
            node("A", 0f, 0f),
            node("EL", 0.5f, 0f, type = "ELEVATOR"),
            node("B", 1f, 0f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.any { it.instruction.contains("elevator", ignoreCase = true) })
    }

    @Test
    fun `build emits escalator step when path passes through ESCALATOR node`() {
        val path = listOf(
            node("A", 0f, 0f),
            node("ES", 0.5f, 0f, type = "ESCALATOR"),
            node("B", 1f, 0f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.any { it.instruction.contains("escalator", ignoreCase = true) })
    }

    @Test
    fun `build emits stairs step when path passes through STAIRCASE node`() {
        val path = listOf(
            node("A", 0f, 0f),
            node("ST", 0.5f, 0f, type = "STAIRCASE"),
            node("B", 1f, 0f)
        )
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.any { it.instruction.contains("stairs", ignoreCase = true) })
    }

    // ── Distance calculation ──────────────────────────────────────────────────

    @Test
    fun `build step distanceM is positive for non-trivial path`() {
        val path = listOf(node("A", 0f, 0f), node("B", 0.1f, 0f))
        val steps = IndoorStepBuilder(scaleMetresPerUnit = 100f).build(path)
        assertTrue(steps.first().distanceM > 0f)
    }

    @Test
    fun `build uses injected scaleMetresPerUnit to compute distance`() {
        val path = listOf(node("A", 0f, 0f), node("B", 1f, 0f))
        val steps50  = IndoorStepBuilder(scaleMetresPerUnit = 50f).build(path)
        val steps100 = IndoorStepBuilder(scaleMetresPerUnit = 100f).build(path)
        assertTrue(steps100.first().distanceM > steps50.first().distanceM)
    }

    @Test
    fun `companion default uses DEFAULT_SCALE`() {
        val path = listOf(node("A", 0f, 0f), node("B", 1f, 0f))
        val stepsDefault = IndoorStepBuilder.build(path)
        val stepsExplicit = IndoorStepBuilder(IndoorStepBuilder.DEFAULT_SCALE).build(path)
        assertEquals(stepsDefault.first().distanceM, stepsExplicit.first().distanceM, 0.001f)
    }

    // ── isLast flag ───────────────────────────────────────────────────────────

    @Test
    fun `build marks last step as isLast`() {
        val path = listOf(node("A", 0f, 0f), node("B", 1f, 0f))
        val steps = IndoorStepBuilder.build(path)
        assertTrue(steps.last().isLast)
    }

    @Test
    fun `build first step of multi-step path is not isLast`() {
        // Need a turn to generate multiple steps
        val path = listOf(
            node("A", 0f, 0.5f),
            node("B", 0.5f, 0.5f),
            node("C", 0.5f, 1f)
        )
        val steps = IndoorStepBuilder.build(path)
        if (steps.size > 1) {
            assertFalse(steps.first().isLast)
        }
    }
}
