package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorRoom

// ── colours ───────────────────────────────────────────────────────────────────

private val BG         = Color(0xFFF0EDE8)
private val FLOOR_BG   = Color(0xFFEAE6DF)
private val GRID_LINE  = Color(0xFFD8D4CC)
private val CORRIDOR_F = Color(0xFFDDD9D0)
private val CORRIDOR_S = Color(0xFFBBB7AE)
private val PATH_COLOR = Color(0xFF912338)
private val PATH_DOT   = Color(0xFFFFFFFF)

private val ROOM_FILL = mapOf(
    "classroom"      to Color(0xFF4CAF7D).copy(alpha = 0.18f),
    "office"         to Color(0xFF5B9CF6).copy(alpha = 0.18f),
    "washroom"       to Color(0xFF29B6D8).copy(alpha = 0.22f),
    "water_fountain" to Color(0xFF26C6DA).copy(alpha = 0.22f),
    "elevator"       to Color(0xFF9B7FE8).copy(alpha = 0.25f),
    "staircase"      to Color(0xFF8B8B8B).copy(alpha = 0.20f),
    "exit"           to Color(0xFFF0A060).copy(alpha = 0.20f),
    "other"          to Color(0xFF8888AA).copy(alpha = 0.12f)
)
private val ROOM_STROKE = mapOf(
    "classroom"      to Color(0xFF2E9E68),
    "office"         to Color(0xFF3A7FD8),
    "washroom"       to Color(0xFF1A96B8),
    "water_fountain" to Color(0xFF18B0C0),
    "elevator"       to Color(0xFF7A5FC8),
    "staircase"      to Color(0xFF666666),
    "exit"           to Color(0xFFD07020),
    "other"          to Color(0xFF555570)
)
private val NODE_COLOR = mapOf(
    "CORRIDOR"  to Color(0xFF5B9CF6),
    "ROOM"      to Color(0xFF4CAF7D),
    "ELEVATOR"  to Color(0xFF9B7FE8),
    "STAIRCASE" to Color(0xFF888888),
    "ENTRANCE"  to Color(0xFFF0A060)
)

private const val SCALE_MIN = 0.4f
private const val SCALE_MAX = 10f
private const val ZOOM_STEP = 1.35f

// ── map state ─────────────────────────────────────────────────────────────────

/**
 * Encapsulates pan/zoom/fit state for [IndoorMapCanvas].
 *
 * Extracted from the composable body so view logic (pan/zoom math) is
 * separated from rendering logic (Canvas draw calls). Also allows the
 * state to be hoisted and tested independently.
 */
class IndoorMapState {
    var scale      by mutableStateOf(1f)
    var panOffset  by mutableStateOf(Offset.Zero)
    var canvasSize by mutableStateOf(Size.Zero)
    var fitted     by mutableStateOf(false)

    fun toScreen(nx: Float, ny: Float) = Offset(
        nx * canvasSize.width  * scale + panOffset.x,
        ny * canvasSize.height * scale + panOffset.y
    )

    fun toNorm(sx: Float, sy: Float) = Offset(
        (sx - panOffset.x) / (canvasSize.width  * scale),
        (sy - panOffset.y) / (canvasSize.height * scale)
    )

    fun fitToBounds(minX: Float, minY: Float, maxX: Float, maxY: Float, pad: Float = 0.05f) {
        if (canvasSize == Size.Zero) return
        val contentW = (maxX - minX + pad * 2).coerceAtLeast(0.01f)
        val contentH = (maxY - minY + pad * 2).coerceAtLeast(0.01f)
        val newScale = minOf(1f / contentW, 1f / contentH).coerceIn(SCALE_MIN, SCALE_MAX)
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        scale     = newScale
        panOffset = Offset(
            canvasSize.width  / 2f - cx * canvasSize.width  * newScale,
            canvasSize.height / 2f - cy * canvasSize.height * newScale
        )
    }

    fun autoFit(floor: IndoorFloor, pathEdgeIds: List<Pair<String, String>>) {
        if (canvasSize == Size.Zero || fitted) return
        fitted = true

        if (pathEdgeIds.isNotEmpty()) {
            val nodeMap = floor.nodes.associateBy { it.id }
            val pathNodes = pathEdgeIds
                .flatMap { (a, b) -> listOf(nodeMap[a], nodeMap[b]) }
                .filterNotNull()
            if (pathNodes.isNotEmpty()) {
                fitToBounds(
                    minX = pathNodes.minOf { it.x }, minY = pathNodes.minOf { it.y },
                    maxX = pathNodes.maxOf { it.x }, maxY = pathNodes.maxOf { it.y },
                    pad  = 0.08f
                )
                return
            }
        }

        val allPts = floor.rooms.flatMap { it.polygon } + floor.corridors.flatMap { it.polygon }
        if (allPts.isEmpty()) return
        fitToBounds(
            minX = allPts.minOf { it.x }, minY = allPts.minOf { it.y },
            maxX = allPts.maxOf { it.x }, maxY = allPts.maxOf { it.y },
            pad  = 0.03f
        )
    }

    fun zoomBy(factor: Float) {
        if (canvasSize == Size.Zero) return
        val cx    = canvasSize.width  / 2f
        val cy    = canvasSize.height / 2f
        val next  = (scale * factor).coerceIn(SCALE_MIN, SCALE_MAX)
        val ratio = next / scale
        panOffset = Offset(cx - ratio * (cx - panOffset.x), cy - ratio * (cy - panOffset.y))
        scale     = next
    }
}

/** Creates and remembers an [IndoorMapState] scoped to the current composition. */
@Composable
fun rememberIndoorMapState(): IndoorMapState = remember { IndoorMapState() }

// ── composable ────────────────────────────────────────────────────────────────

@Composable
fun IndoorMapCanvas(
    floor:           IndoorFloor,
    modifier:        Modifier = Modifier,
    highlightRoomId: String?  = null,
    pathNodeIds:     Set<String> = emptySet(),
    pathEdgeIds:     List<Pair<String, String>> = emptyList(),
    showNavGraph:    Boolean  = false,
    onRoomTap:       (IndoorRoom) -> Unit = {}
) {
    val mapState = rememberIndoorMapState()
    val measurer = rememberTextMeasurer()

    // Cache the node map so drawing doesn't rebuild it on every frame
    val nodeMap = remember(floor) { floor.nodes.associateBy { it.id } }

    LaunchedEffect(floor)          { mapState.fitted = false }
    LaunchedEffect(floor, pathEdgeIds) { mapState.fitted = false }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(BG)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val next  = (mapState.scale * zoom).coerceIn(SCALE_MIN, SCALE_MAX)
                        val ratio = next / mapState.scale
                        mapState.panOffset = Offset(
                            centroid.x - ratio * (centroid.x - mapState.panOffset.x) + pan.x,
                            centroid.y - ratio * (centroid.y - mapState.panOffset.y) + pan.y
                        )
                        mapState.scale = next
                    }
                }
                .pointerInput(floor) {
                    detectTapGestures { tap ->
                        if (mapState.canvasSize == Size.Zero) return@detectTapGestures
                        val n = mapState.toNorm(tap.x, tap.y)
                        floor.rooms.firstOrNull { pointInPolygon(n.x, n.y, it.polygon) }
                            ?.let { onRoomTap(it) }
                    }
                }
        ) {
            mapState.canvasSize = size
            mapState.autoFit(floor, pathEdgeIds)

            val toScreen: (Float, Float) -> Offset = { nx, ny -> mapState.toScreen(nx, ny) }

            drawGrid(toScreen)
            drawFloorBackground(floor, toScreen)
            drawFloorCorridors(floor, toScreen)
            drawFloorRooms(floor, highlightRoomId, pathNodeIds, toScreen, measurer, mapState.scale)
            drawNavigationPath(pathEdgeIds, nodeMap, toScreen, mapState.scale)
            drawPointsOfInterest(floor, toScreen, mapState.scale)
            if (showNavGraph) drawNavGraph(floor, nodeMap, toScreen, mapState.scale)
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ZoomButton("+") { mapState.zoomBy(ZOOM_STEP) }
            ZoomButton("−") { mapState.zoomBy(1f / ZOOM_STEP) }
            ZoomButton("⊡") {
                mapState.fitted = false
                val allPts = floor.rooms.flatMap { it.polygon } + floor.corridors.flatMap { it.polygon }
                if (allPts.isNotEmpty() && mapState.canvasSize != Size.Zero) {
                    mapState.fitToBounds(
                        minX = allPts.minOf { it.x }, minY = allPts.minOf { it.y },
                        maxX = allPts.maxOf { it.x }, maxY = allPts.maxOf { it.y }
                    )
                    mapState.fitted = true
                }
            }
        }
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick   = onClick,
        shape     = CircleShape,
        color     = Color.White.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        modifier  = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Light,
                color = Color(0xFF333333))
        }
    }
}

// ── DrawScope extension functions ─────────────────────────────────────────────

private fun DrawScope.drawFloorBackground(
    floor:    IndoorFloor,
    toScreen: (Float, Float) -> Offset
) {
    val allPts = floor.rooms.flatMap { it.polygon } + floor.corridors.flatMap { it.polygon }
    if (allPts.isEmpty()) return
    val pad = 0.03f
    val tl  = toScreen(allPts.minOf { it.x } - pad, allPts.minOf { it.y } - pad)
    val br  = toScreen(allPts.maxOf { it.x } + pad, allPts.maxOf { it.y } + pad)
    drawRect(FLOOR_BG,   topLeft = tl, size = Size(br.x - tl.x, br.y - tl.y))
    drawRect(CORRIDOR_S, topLeft = tl, size = Size(br.x - tl.x, br.y - tl.y), style = Stroke(2f))
}

private fun DrawScope.drawFloorCorridors(
    floor:    IndoorFloor,
    toScreen: (Float, Float) -> Offset
) {
    floor.corridors.forEach { c ->
        drawPolygon(c.polygon, CORRIDOR_F, CORRIDOR_S, toScreen, 1.5f)
    }
}

private fun DrawScope.drawFloorRooms(
    floor:           IndoorFloor,
    highlightRoomId: String?,
    pathNodeIds:     Set<String>,
    toScreen:        (Float, Float) -> Offset,
    measurer:        androidx.compose.ui.text.TextMeasurer,
    scale:           Float
) {
    floor.rooms.forEach { room ->
        val hi     = room.id == highlightRoomId
        val onPath = pathNodeIds.isNotEmpty() &&
            floor.nodes.any { it.roomId == room.id && it.id in pathNodeIds }
        val fill = when {
            hi     -> Color(0xFFFFD740).copy(.30f)
            onPath -> PATH_COLOR.copy(.12f)
            else   -> ROOM_FILL[room.type] ?: Color.Gray.copy(.12f)
        }
        val stroke = when {
            hi     -> Color(0xFFFFAA00)
            onPath -> PATH_COLOR
            else   -> ROOM_STROKE[room.type] ?: Color.Gray
        }
        drawPolygon(room.polygon, fill, stroke, toScreen, if (hi || onPath) 3f else 2f)

        if (room.polygon.size >= 3) {
            val cx  = room.polygon.map { it.x }.average().toFloat()
            val cy  = room.polygon.map { it.y }.average().toFloat()
            val pos = toScreen(cx, cy)
            val fs  = (9f * scale).coerceIn(9f, 16f)
            val lbl = room.icon?.let { "$it ${room.label}" } ?: room.label
            val m   = measurer.measure(lbl, TextStyle(
                color = stroke, fontSize = fs.sp, fontWeight = FontWeight.SemiBold
            ))
            drawContext.canvas.nativeCanvas.drawText(
                lbl, pos.x, pos.y + m.size.height / 2f - 4f * density,
                android.graphics.Paint().apply {
                    color       = android.graphics.Color.argb(180, 255, 255, 255)
                    textSize    = fs * density
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    strokeWidth = fs * density * 0.35f
                    style       = android.graphics.Paint.Style.STROKE
                }
            )
            drawText(m, topLeft = Offset(pos.x - m.size.width / 2f, pos.y - m.size.height / 2f))
        }
    }
}

private fun DrawScope.drawNavigationPath(
    pathEdgeIds: List<Pair<String, String>>,
    nodeMap:     Map<String, com.example.myapplication.data.indoor.IndoorNode>,
    toScreen:    (Float, Float) -> Offset,
    scale:       Float
) {
    if (pathEdgeIds.isEmpty()) return

    // Solid path line
    pathEdgeIds.forEach { (fromId, toId) ->
        val a = nodeMap[fromId] ?: return@forEach
        val b = nodeMap[toId]   ?: return@forEach
        drawLine(PATH_COLOR, toScreen(a.x, a.y), toScreen(b.x, b.y),
            (6f * scale).coerceIn(4f, 14f))
    }
    // Dashed overlay
    pathEdgeIds.forEach { (fromId, toId) ->
        val a = nodeMap[fromId] ?: return@forEach
        val b = nodeMap[toId]   ?: return@forEach
        drawLine(PATH_DOT.copy(alpha = 0.55f), toScreen(a.x, a.y), toScreen(b.x, b.y),
            (2f * scale).coerceIn(1f, 5f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f))
    }
    // Start marker
    pathEdgeIds.firstOrNull()?.first?.let { startId ->
        nodeMap[startId]?.let { n ->
            val pos = toScreen(n.x, n.y)
            val r   = (8f * scale).coerceIn(6f, 16f)
            drawCircle(Color(0xFF4CAF50), r, pos)
            drawCircle(Color.White, r, pos, style = Stroke(2f))
        }
    }
    // End marker
    pathEdgeIds.lastOrNull()?.second?.let { endId ->
        nodeMap[endId]?.let { n ->
            val pos = toScreen(n.x, n.y)
            val r   = (8f * scale).coerceIn(6f, 16f)
            drawCircle(PATH_COLOR, r, pos)
            drawCircle(Color.White, r, pos, style = Stroke(2f))
        }
    }
}

private fun DrawScope.drawPointsOfInterest(
    floor:    IndoorFloor,
    toScreen: (Float, Float) -> Offset,
    scale:    Float
) {
    floor.pois.forEach { poi ->
        val icon = poiEmoji(poi.type)
        val pos  = toScreen(poi.x, poi.y)
        val sz   = (22f * scale).coerceIn(16f, 44f)
        drawCircle(Color.White, sz * 0.72f, pos)
        drawCircle(poiRingColor(poi.type), sz * 0.72f, pos, style = Stroke(sz * 0.12f))
        drawContext.canvas.nativeCanvas.drawText(
            icon, pos.x, pos.y + sz * 0.38f,
            android.graphics.Paint().apply {
                textSize    = sz * 0.85f
                textAlign   = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
        )
    }
    // Entrances
    floor.entrances.forEach { e ->
        val pos = toScreen(e.x, e.y)
        val r   = (10f * scale).coerceIn(7f, 20f)
        drawCircle(Color(0xFFF0A060), r, pos)
        drawCircle(Color(0xFFCC7020), r, pos, style = Stroke(2f))
    }
}

private fun DrawScope.drawNavGraph(
    floor:    IndoorFloor,
    nodeMap:  Map<String, com.example.myapplication.data.indoor.IndoorNode>,
    toScreen: (Float, Float) -> Offset,
    scale:    Float
) {
    floor.edges.forEach { edge ->
        val a = nodeMap[edge.from] ?: return@forEach
        val b = nodeMap[edge.to]   ?: return@forEach
        drawLine(Color(0xFF9B7FE8).copy(.4f), toScreen(a.x, a.y), toScreen(b.x, b.y), 1.5f)
    }
    val nr = (5f * scale).coerceIn(3f, 10f)
    floor.nodes.forEach { node ->
        val col = NODE_COLOR[node.type] ?: Color(0xFF5B9CF6)
        val pos = toScreen(node.x, node.y)
        drawCircle(col.copy(.6f), nr, pos)
        drawCircle(col, nr, pos, style = Stroke(1.5f))
    }
}


private fun DrawScope.drawGrid(toScreen: (Float, Float) -> Offset) {
    var x = 0f; while (x <= 1f) {
        drawLine(GRID_LINE, toScreen(x, 0f), toScreen(x, 1f), 0.5f); x += 0.05f
    }
    var y = 0f; while (y <= 1f) {
        drawLine(GRID_LINE, toScreen(0f, y), toScreen(1f, y), 0.5f); y += 0.05f
    }
}

private fun DrawScope.drawPolygon(
    pts: List<Offset>, fill: Color, stroke: Color,
    toScreen: (Float, Float) -> Offset, sw: Float = 1.5f
) {
    if (pts.size < 3) return
    val path = Path().apply {
        pts.forEachIndexed { i, p ->
            val (sx, sy) = toScreen(p.x, p.y)
            if (i == 0) moveTo(sx, sy) else lineTo(sx, sy)
        }
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(sw))
}

private fun pointInPolygon(px: Float, py: Float, poly: List<Offset>): Boolean {
    var inside = false; var j = poly.lastIndex
    for (i in poly.indices) {
        val xi = poly[i].x; val yi = poly[i].y
        val xj = poly[j].x; val yj = poly[j].y
        if ((yi > py) != (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi)
            inside = !inside
        j = i
    }
    return inside
}

private fun poiEmoji(type: String) = when (type) {
    "washroom" -> "🚻"; "water_fountain" -> "💧"
    "elevator" -> "🛗"; "staircase" -> "🪜"; "exit" -> "🚪"; else -> "📍"
}

private fun poiRingColor(type: String) = when (type) {
    "washroom" -> Color(0xFF1A96B8); "water_fountain" -> Color(0xFF18B0C0)
    "elevator" -> Color(0xFF7A5FC8); "staircase" -> Color(0xFF666666)
    "exit"     -> Color(0xFFD07020); else -> Color(0xFF555570)
}

