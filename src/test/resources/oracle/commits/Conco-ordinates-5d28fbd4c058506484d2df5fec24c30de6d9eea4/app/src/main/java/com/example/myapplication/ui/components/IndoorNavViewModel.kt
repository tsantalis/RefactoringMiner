package com.example.myapplication.ui.components

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.indoor.IndoorRepository
import com.example.myapplication.data.indoor.IndoorFloor
import com.example.myapplication.data.indoor.IndoorNode
import com.example.myapplication.logic.IndoorOutdoorRouter
import com.example.myapplication.logic.IndoorOutdoorRouter.Segment
import com.example.myapplication.logic.IndoorStepBuilder
import com.example.myapplication.logic.TransferPreference
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI state ──────────────────────────────────────────────────────────────────

data class IndoorNavUiState(
    // current floor display
    val floor:              IndoorFloor?    = null,
    val currentFloorNumber: Int             = 1,
    val currentBuilding:    String          = "",
    val availableFloors:    List<Int>       = emptyList(),

    // room selection
    val selectedRoom:       com.example.myapplication.data.indoor.IndoorRoom? = null,
    val highlightRoomId:    String?         = null,

    // active route
    val fullRoute:              IndoorOutdoorRouter.FullRoute? = null,
    val currentSegmentIdx:      Int             = 0,
    val pathNodeIds:            Set<String>     = emptySet(),
    val pathEdgeIds:            List<Pair<String, String>> = emptyList(),

    // current segment instruction shown to user
    val instruction:            String          = "",

    // ── Turn-by-turn steps within the current IndoorWalk segment ─────────────
    // All steps for the current walk segment (built by IndoorStepBuilder)
    val currentSteps:           List<IndoorStepBuilder.NavStep> = emptyList(),
    // Index of the active step within currentSteps
    val currentStepIdx:         Int             = 0,
    // Total step count across the whole route (for "Step X / N" display)
    val totalStepCount:         Int             = 0,
    // Running step offset from previous segments
    val stepOffset:             Int             = 0,

    // floor-change overlay (user must confirm they changed floors)
    val pendingFloorChange:     Segment.FloorChange? = null,

    // mid-route advance card: shown when a Walk segment ends but route continues
    val pendingSegmentAdvance:  String?         = null,

    // exit confirmation: set true when the exit-leg path finishes drawing.
    val pendingExitConfirm:     Boolean         = false,

    // outdoor handoff
    val outdoorSegment:         Segment.OutdoorWalk? = null,

    // ── Accessibility & transfer preference ───────────────────────────────────
    val accessibleMode:         Boolean         = false,
    val transferPreference:     TransferPreference = TransferPreference.ANY,

    // misc
    val showNavGraph:           Boolean         = false,
    val isLoading:              Boolean         = false,
    val error:                  String?         = null,
    val hasArrived:             Boolean         = false,
    val isExitLeg:              Boolean         = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * [repo] is injected via [IndoorNavViewModelFactory] so the ViewModel
 * never creates its own dependencies (DIP). Tests can pass a fake repo
 * without needing an Application context.
 */
class IndoorNavViewModel(
    private val repo: IndoorRepository
) : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(IndoorNavUiState())
    val state: StateFlow<IndoorNavUiState> = _state.asStateFlow()

    private data class NavParams(
        val building:    String,
        val destination: IndoorOutdoorRouter.IndoorDestination,
        val startNodeId: String,
        val startFloor:  Int,
        val userGps:     com.google.android.gms.maps.model.LatLng?
    )
    private var lastNavParams: NavParams? = null

    init {
        // Observe only the two preference fields using map + distinctUntilChanged.
        // This is the standard Kotlin Flow pattern — no Compose API involved.
        viewModelScope.launch {
            _state
                .map { it.accessibleMode to it.transferPreference }
                .distinctUntilChanged()
                .drop(1)  // skip initial emission — navigateTo handles the first route
                .collect { (accessible, pref) ->
                    val params = lastNavParams ?: return@collect
                    // Don't recompute if user has already arrived
                    if (_state.value.hasArrived) return@collect
                    val effective = if (accessible) TransferPreference.ELEVATOR_ONLY else pref
                    android.util.Log.d("IndoorPref",
                        "Preference changed → $effective, recomputing")
                    recomputeWithPreference(params, effective)
                }
        }
    }

    private fun recomputeWithPreference(p: NavParams, preference: TransferPreference) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val route = IndoorOutdoorRouter.buildRoute(
                repo          = repo,
                startBuilding = p.building,
                startFloor    = p.startFloor,
                startNodeId   = p.startNodeId,
                destination   = p.destination,
                userGps       = p.userGps,
                preference    = preference
            )
            if (route.segments.isEmpty()) {
                _state.update { it.copy(isLoading = false,
                    error = "No path with ${preference.label}. Try another option.") }
                return@launch
            }
            _state.update { it.copy(isLoading = false, fullRoute = route, currentSegmentIdx = 0) }
            applySegment(0)
        }
    }

    // ── loading ───────────────────────────────────────────────────────────────

    /**
     * Fully resets navigation state and loads the building/floor.
     * Called every time IndoorNavScreen opens to prevent stale route data
     * from a previous session being shown on re-entry.
     */
    fun resetForNewSession(building: String, floor: Int, floors: List<Int>, isExitLeg: Boolean = false) {
        val currentAccessible  = _state.value.accessibleMode
        val currentPreference  = _state.value.transferPreference
        _state.update {
            IndoorNavUiState(
                currentBuilding    = building,
                availableFloors    = floors,
                currentFloorNumber = floor,
                isExitLeg          = isExitLeg,
                accessibleMode     = currentAccessible,
                transferPreference = currentPreference
            )
        }
        loadFloor(building, floor)
    }

    fun loadBuilding(building: String, floor: Int, floors: List<Int>) {
        _state.update { it.copy(currentBuilding = building, availableFloors = floors) }
        loadFloor(building, floor)
    }

    fun switchFloor(building: String, floor: Int) {
        _state.update {
            it.copy(
                currentFloorNumber = floor,
                selectedRoom       = null,
                pathNodeIds        = emptySet(),
                pathEdgeIds        = emptyList()
            )
        }
        loadFloor(building, floor)
    }

    private fun loadFloor(building: String, floor: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = repo.getFloor(building, floor)
            _state.update {
                it.copy(
                    isLoading          = false,
                    floor              = result,
                    currentFloorNumber = floor,
                    currentBuilding    = building,
                    error = if (result == null) "No map data for $building floor $floor" else null
                )
            }
        }
    }

    // ── room interaction ──────────────────────────────────────────────────────

    fun onRoomTap(room: com.example.myapplication.data.indoor.IndoorRoom) =
        _state.update { it.copy(selectedRoom = room) }

    fun dismissRoom() = _state.update { it.copy(selectedRoom = null) }
    fun toggleNavGraph() = _state.update { it.copy(showNavGraph = !it.showNavGraph) }

    /** Toggle accessible mode (♿). The init collector detects the change and recomputes. */
    fun toggleAccessibleMode() {
        _state.update { it.copy(accessibleMode = !it.accessibleMode) }
    }

    /** Set transfer preference. The init collector detects the change and recomputes. */
    fun setTransferPreference(pref: TransferPreference) {
        _state.update { it.copy(transferPreference = pref) }
    }

    /** Effective preference: accessible mode overrides the settings menu choice. */
    private fun effectivePreference(): TransferPreference =
        if (_state.value.accessibleMode) TransferPreference.ELEVATOR_ONLY
        else _state.value.transferPreference

    // ── FULL ROUTE NAVIGATION ─────────────────────────────────────────────────

    /**
     * Build a complete route from the current position to [destination].
     * May span buildings, floors, and outdoor segments.
     *
     * [startFloor] overrides the currently displayed floor — needed when the
     * user's start node is on a different floor than the one loaded in the map
     * (e.g. same-building cross-floor: user is on floor 1, destination is floor 8).
     */
    fun navigateTo(
        destination: IndoorOutdoorRouter.IndoorDestination,
        startNodeId: String,
        building:    String? = null,
        startFloor:  Int?    = null,
        userGps:     LatLng? = null,
        // Explicit preference overrides effectivePreference() — use this when
        // the caller already has the correct preference captured (avoids timing issues).
        preference:  TransferPreference? = null
    ) {
        val state = _state.value
        val resolvedBuilding   = building ?: state.currentBuilding
        val resolvedStartFloor = startFloor ?: state.currentFloorNumber
        val resolvedPreference = preference ?: effectivePreference()

        // Save params so recomputeRoute can replay when preference changes
        lastNavParams = NavParams(
            building    = resolvedBuilding,
            destination = destination,
            startNodeId = startNodeId,
            startFloor  = resolvedStartFloor,
            userGps     = userGps
        )
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // If nodeId is blank, resolve it from the destination floor JSON
            val resolvedDestNodeId = if (destination.nodeId.isBlank()) {
                val destFloorData = repo.getFloor(destination.building, destination.floor)
                val room = destFloorData?.rooms?.firstOrNull { room ->
                    room.label.endsWith(destination.label.substringAfterLast('-'), ignoreCase = true) ||
                    room.id.endsWith(destination.label.substringAfterLast('-'), ignoreCase = true)
                }
                val node = if (room != null) {
                    destFloorData?.nodes?.firstOrNull { it.roomId == room.id }
                } else {
                    destFloorData?.nodes?.firstOrNull { n ->
                        n.roomId?.endsWith(destination.label.substringAfterLast('-'), ignoreCase = true) == true
                    }
                }
                if (node == null) {
                    android.util.Log.e("IndoorNav",
                        "Cannot resolve nodeId for ${destination.label} on floor ${destination.floor}")
                    _state.update { it.copy(isLoading = false,
                        error = "Cannot find room ${destination.label} on floor ${destination.floor}") }
                    return@launch
                }
                android.util.Log.d("IndoorNav",
                    "Resolved nodeId for ${destination.label} → ${node.id}")
                node.id
            } else {
                destination.nodeId
            }

            val resolvedDestination = destination.copy(nodeId = resolvedDestNodeId)

            val route = IndoorOutdoorRouter.buildRoute(
                repo          = repo,
                startBuilding = resolvedBuilding,
                startFloor    = resolvedStartFloor,
                startNodeId   = startNodeId,
                destination   = resolvedDestination,
                userGps       = userGps,
                preference    = resolvedPreference
            )

            android.util.Log.d("IndoorNav",
                "Route: ${route.segments.size} segments, " +
                "start=$startNodeId(F$resolvedStartFloor, $resolvedBuilding) " +
                "→ ${resolvedDestNodeId}(F${destination.floor})")

            if (route.segments.isEmpty()) {
                _state.update { it.copy(isLoading = false,
                    error = "No path found to ${destination.label}") }
                return@launch
            }

            _state.update { it.copy(isLoading = false, fullRoute = route, currentSegmentIdx = 0) }
            applySegment(0)
        }
    }

    /**
     * User signals they have completed the current segment
     * (arrived at exit, boarded elevator, etc.).
     */
    fun advanceToNextSegment() {
        val st = _state.value
        val route = st.fullRoute ?: return
        val nextIdx = st.currentSegmentIdx + 1

        if (nextIdx >= route.segments.size) {
            _state.update { it.copy(hasArrived = true, instruction = "You have arrived!") }
            return
        }

        // If next segment is a floor change, show overlay first
        val nextSeg = route.segments[nextIdx]
        if (nextSeg is Segment.FloorChange) {
            _state.update {
                it.copy(
                    currentSegmentIdx = nextIdx,
                    pendingFloorChange = nextSeg,
                    instruction = nextSeg.instruction
                )
            }
            return
        }

        // If next is outdoor, surface the outdoor segment
        if (nextSeg is Segment.OutdoorWalk) {
            _state.update {
                it.copy(
                    currentSegmentIdx = nextIdx,
                    outdoorSegment    = nextSeg,
                    instruction       = nextSeg.instruction
                )
            }
            return
        }

        _state.update { it.copy(currentSegmentIdx = nextIdx) }
        applySegment(nextIdx)
    }

    /** Call after user has physically changed floors. */
    fun confirmFloorChange() {
        val st = _state.value
        val fc = st.pendingFloorChange ?: return
        _state.update { it.copy(pendingFloorChange = null) }
        // Load the new floor then advance
        loadFloor(fc.building, fc.toFloor)
        advanceToNextSegment()
    }

    /** Call after outdoor navigation hands back to indoor. */
    fun confirmOutdoorArrival() {
        _state.update { it.copy(outdoorSegment = null) }
        advanceToNextSegment()
    }

    fun clearRoute() {
        lastNavParams = null
        _state.update {
            it.copy(
                fullRoute          = null,
                currentSegmentIdx  = 0,
                currentSteps       = emptyList(),
                currentStepIdx     = 0,
                totalStepCount     = 0,
                stepOffset         = 0,
                pathNodeIds        = emptySet(),
                pathEdgeIds        = emptyList(),
                highlightRoomId    = null,
                pendingFloorChange = null,
                pendingSegmentAdvance = null,
                outdoorSegment     = null,
                instruction        = "",
                hasArrived         = false
            )
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun applySegment(idx: Int) {
        val route = _state.value.fullRoute ?: return
        val seg   = route.segments.getOrNull(idx) ?: return

        when (seg) {
            is Segment.IndoorWalk -> {
                val path    = seg.path
                val nodeIds = path.map { it.id }.toSet()

                if (seg.floor != _state.value.currentFloorNumber) {
                    loadFloor(seg.building, seg.floor)
                }

                val isLastSegment = idx == route.segments.size - 1
                val nextSeg       = route.segments.getOrNull(idx + 1)

                if (path.isEmpty() || (path.size == 1 && isLastSegment)) {
                    lastNavParams = null
                    _state.update {
                        it.copy(
                            pathNodeIds           = nodeIds,
                            pathEdgeIds           = emptyList(),
                            instruction           = seg.instruction,
                            pendingSegmentAdvance = null,
                            hasArrived            = true
                        )
                    }
                    return
                }

                // Build turn-by-turn steps for this walk segment
                val destLabel = route.segments.filterIsInstance<Segment.IndoorWalk>()
                    .lastOrNull()?.path?.lastOrNull()?.roomId ?: "your destination"
                val steps = IndoorStepBuilder.build(
                    path             = path,
                    destinationLabel = if (isLastSegment) destLabel else "the ${nextSeg?.let {
                        when (it) {
                            is Segment.FloorChange -> it.via
                            else -> "exit"
                        }
                    } ?: "exit"}"
                )

                // Compute running step offset from previous walk segments
                val stepOffset = route.segments.take(idx)
                    .filterIsInstance<Segment.IndoorWalk>()
                    .sumOf { s ->
                        IndoorStepBuilder.build(s.path).size
                    }

                // Count total steps across all walk segments
                val totalSteps = route.segments
                    .filterIsInstance<Segment.IndoorWalk>()
                    .sumOf { s -> IndoorStepBuilder.build(s.path).size }

                // Apply first step of this segment
                val firstStep = steps.firstOrNull()
                val firstEdges = firstStep?.nodes?.zipWithNext { a, b -> a.id to b.id }
                    ?: emptyList()

                _state.update {
                    it.copy(
                        pathNodeIds           = firstStep?.nodes?.map { n -> n.id }?.toSet() ?: nodeIds,
                        pathEdgeIds           = firstEdges,
                        instruction           = firstStep?.instruction ?: seg.instruction,
                        highlightRoomId       = path.lastOrNull()?.roomId,
                        currentSteps          = steps,
                        currentStepIdx        = 0,
                        totalStepCount        = totalSteps,
                        stepOffset            = stepOffset,
                        pendingSegmentAdvance = if (steps.size > 1) null
                                               else advancePromptFor(isLastSegment, nextSeg),
                        hasArrived            = false
                    )
                }
            }
            is Segment.FloorChange -> {
                _state.update {
                    it.copy(
                        pendingFloorChange    = seg,
                        pendingSegmentAdvance = null,
                        instruction           = seg.instruction
                    )
                }
            }
            is Segment.OutdoorWalk -> {
                _state.update {
                    it.copy(
                        outdoorSegment        = seg,
                        pendingSegmentAdvance = null,
                        instruction           = seg.instruction
                    )
                }
            }
        }
    }

    /** Called when user taps the advance/confirmation button.
     *  - Within a Walk segment: advance to the next turn step.
     *  - At the end of a Walk segment: advance to FloorChange or arrival. */
    fun confirmSegmentAdvance() {
        val st    = _state.value
        val route = st.fullRoute ?: return

        // If there are more steps within the current walk segment, advance step
        if (st.currentSteps.isNotEmpty() && st.currentStepIdx < st.currentSteps.size - 1) {
            advanceStep()
            return
        }

        // All steps done — advance route segment
        val isLast = st.currentSegmentIdx == route.segments.size - 1
        when {
            isLast -> {
                lastNavParams = null
                _state.update { it.copy(pendingSegmentAdvance = null, hasArrived = true) }
            }
            else -> {
                _state.update { it.copy(pendingSegmentAdvance = null) }
                advanceToNextSegment()
            }
        }
    }

    /** Advance to the next turn step within the current IndoorWalk segment. */
    fun advanceStep() {
        val st    = _state.value
        val route = st.fullRoute ?: return
        val steps = st.currentSteps
        val nextIdx = st.currentStepIdx + 1
        if (nextIdx >= steps.size) return

        val nextStep = steps[nextIdx]
        val isLastStep = nextIdx == steps.size - 1
        val isLastSeg  = st.currentSegmentIdx == route.segments.size - 1
        val nextSeg    = route.segments.getOrNull(st.currentSegmentIdx + 1)

        val advancePrompt: String? = if (isLastStep) {
            advancePromptFor(isLastSeg, nextSeg)
        } else null

        _state.update {
            it.copy(
                currentStepIdx        = nextIdx,
                pathNodeIds           = nextStep.nodes.map { n -> n.id }.toSet(),
                pathEdgeIds           = nextStep.nodes.zipWithNext { a, b -> a.id to b.id },
                instruction           = nextStep.instruction,
                pendingSegmentAdvance = advancePrompt
            )
        }
    }

    private fun advancePromptFor(isLastSeg: Boolean, nextSeg: Segment?): String? = when {
        nextSeg is Segment.OutdoorWalk      -> null
        nextSeg is Segment.FloorChange      -> "Have you reached the ${nextSeg.via}?"
        isLastSeg && _state.value.isExitLeg -> "Have you reached the exit?"
        isLastSeg                           -> "Have you arrived at your destination?"
        else                                -> null
    }
}

/**
 * ViewModelProvider.Factory for [IndoorNavViewModel].
 *
 * Injects [IndoorRepository] via constructor so the ViewModel follows DIP
 * and can be tested with a mock repo without needing an Android Context.
 */
class IndoorNavViewModelFactory(
    private val repo: IndoorRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == IndoorNavViewModel::class.java) {
            "IndoorNavViewModelFactory can only create IndoorNavViewModel"
        }
        return IndoorNavViewModel(repo) as T
    }
}
