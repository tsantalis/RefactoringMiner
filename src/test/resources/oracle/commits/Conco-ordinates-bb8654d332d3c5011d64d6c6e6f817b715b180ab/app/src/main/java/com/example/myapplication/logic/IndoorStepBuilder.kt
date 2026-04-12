package com.example.myapplication.logic

import com.example.myapplication.data.indoor.IndoorNode
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.abs

/**
 * Converts a raw A* node list into human-readable turn-by-turn steps.
 *
 * Algorithm:
 * 1. Compute heading (angle) of each edge in the path.
 * 2. At each node, compute the heading change (delta).
 * 3. If |delta| < STRAIGHT_THRESHOLD → merge with previous segment (same direction).
 * 4. If |delta| >= STRAIGHT_THRESHOLD → start a new step with a turn instruction.
 * 5. Special nodes (ELEVATOR, ESCALATOR, STAIRCASE, ENTRANCE) always force a new step.
 */
object IndoorStepBuilder {

    // Heading change below this threshold is considered "straight"
    private const val STRAIGHT_THRESHOLD = 25.0   // degrees

    data class NavStep(
        val instruction: String,          // e.g. "Go straight", "Turn left", "Take the elevator"
        val nodes:       List<IndoorNode>, // all nodes in this step (for path highlighting)
        val distanceM:   Float,            // estimated distance in metres (1 unit ≈ 50m for H building)
        val isLast:      Boolean = false
    )

    /**
     * Build turn-by-turn steps from a flat node list (A* output).
     *
     * [scaleMetresPerUnit] converts normalised 0-1 coordinates to metres.
     * H building is ~100m wide, so 1 unit ≈ 100m. Default is a reasonable estimate.
     */
    fun build(
        path:               List<IndoorNode>,
        scaleMetresPerUnit: Float = 100f,
        destinationLabel:   String = "your destination"
    ): List<NavStep> {
        if (path.size < 2) return emptyList()

        val steps   = mutableListOf<NavStep>()
        var segStart = 0                     // index of the first node in the current segment
        var segNodes = mutableListOf(path[0])

        for (i in 1 until path.size) {
            val prev = path[i - 1]
            val curr = path[i]
            val isLast = i == path.size - 1

            // Force a step boundary at special node types
            val forceBreak = curr.type in listOf("ELEVATOR", "ESCALATOR", "STAIRCASE")

            // Compute turn angle at curr (needs next node to determine direction)
            val turnDeg = if (i < path.size - 1) {
                headingChange(path[i - 1], path[i], path[i + 1])
            } else 0.0

            val isTurn = abs(turnDeg) >= STRAIGHT_THRESHOLD

            segNodes.add(curr)

            if (forceBreak || isTurn || isLast) {
                // Close current segment
                val dist = segmentDistance(segNodes, scaleMetresPerUnit)
                val instruction = if (steps.isEmpty()) {
                    // First step — describe overall direction
                    buildFirstInstruction(path[0], path.last())
                } else {
                    // Instruction is the TURN that happened at the END of the previous segment
                    // (i.e. what the user does to start THIS step)
                    val prevHeading = headingDeg(path[segStart], prev)
                    val newHeading  = headingDeg(prev, curr)
                    val delta       = normaliseDelta(newHeading - prevHeading)
                    turnInstruction(delta, curr)
                }

                steps.add(NavStep(
                    instruction = instruction,
                    nodes       = segNodes.toList(),
                    distanceM   = dist,
                    isLast      = isLast && !forceBreak
                ))

                segStart = i
                segNodes = mutableListOf(curr)

                // If forced break for elevator/escalator, emit a "take the X" step
                if (forceBreak) {
                    val xferInstruction = when (curr.type) {
                        "ELEVATOR"  -> "Take the elevator to the next floor"
                        "ESCALATOR" -> "Take the escalator to the next floor"
                        "STAIRCASE" -> "Take the stairs to the next floor"
                        else        -> "Transfer"
                    }
                    steps.add(NavStep(
                        instruction = xferInstruction,
                        nodes       = listOf(curr),
                        distanceM   = 0f,
                        isLast      = isLast
                    ))
                    segStart = i
                    segNodes = mutableListOf(curr)
                }
            }
        }

        // Append arrival step
        if (steps.isNotEmpty()) {
            val last = steps.removeLast()
            steps.add(last.copy(
                instruction = "You have arrived at $destinationLabel",
                isLast      = true
            ))
        }

        return steps
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Heading in degrees from node a to node b (0° = right, 90° = down in canvas coords). */
    private fun headingDeg(a: IndoorNode, b: IndoorNode): Double =
        Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()))

    /** Signed heading change at node b, given incoming a→b and outgoing b→c. */
    private fun headingChange(a: IndoorNode, b: IndoorNode, c: IndoorNode): Double {
        val h1 = headingDeg(a, b)
        val h2 = headingDeg(b, c)
        return normaliseDelta(h2 - h1)
    }

    /** Normalise angle delta to [-180, 180]. */
    private fun normaliseDelta(delta: Double): Double =
        ((delta + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

    /** Human-readable turn instruction. */
    private fun turnInstruction(delta: Double, atNode: IndoorNode): String {
        // Special node types override turn language
        if (atNode.type == "ELEVATOR")  return "Take the elevator"
        if (atNode.type == "ESCALATOR") return "Take the escalator"
        if (atNode.type == "STAIRCASE") return "Take the stairs"
        return when {
            delta < -120 -> "Turn around"
            delta < -45  -> "Turn left"
            delta < -STRAIGHT_THRESHOLD -> "Bear left"
            delta >  120 -> "Turn around"
            delta >  45  -> "Turn right"
            delta >  STRAIGHT_THRESHOLD -> "Bear right"
            else         -> "Continue straight"
        }
    }

    /** First step instruction based on overall heading. */
    private fun buildFirstInstruction(start: IndoorNode, end: IndoorNode): String {
        val heading = headingDeg(start, end)
        val cardinal = when {
            heading in -22.5..22.5    -> "east"
            heading in 22.5..67.5     -> "southeast"
            heading in 67.5..112.5    -> "south"
            heading in 112.5..157.5   -> "southwest"
            heading > 157.5 || heading < -157.5 -> "west"
            heading in -157.5..-112.5 -> "northwest"
            heading in -112.5..-67.5  -> "north"
            else                      -> "northeast"
        }
        return "Head $cardinal"
    }

    /** Euclidean distance along the segment in metres. */
    private fun segmentDistance(nodes: List<IndoorNode>, scale: Float): Float {
        var d = 0f
        for (i in 1 until nodes.size) {
            val dx = nodes[i].x - nodes[i-1].x
            val dy = nodes[i].y - nodes[i-1].y
            d += sqrt(dx * dx + dy * dy) * scale
        }
        return d
    }
}
