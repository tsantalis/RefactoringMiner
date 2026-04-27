import cc1 from "../constants/maps/indoor/cc1.json";
import hallCombined from "../constants/maps/indoor/hall.json";
import hall1 from "../constants/maps/indoor/hall1.json";
import hall2 from "../constants/maps/indoor/hall2.json";
import mbFloorsCombined from "../constants/maps/indoor/mb_floors_combined.json";
import ve1 from "../constants/maps/indoor/ve1.json";
import ve2 from "../constants/maps/indoor/ve2.json";
import vl1 from "../constants/maps/indoor/vl1.json";
import vl2 from "../constants/maps/indoor/vl2.json";

export interface IndoorNode {
  id: string;
  type: string;
  buildingId: string;
  floor: number;
  x: number;
  y: number;
  label?: string;
  accessible: boolean;
  direction?: "up" | "down" | "both";
  floors?: number[];
}

export interface IndoorEdge {
  source: string;
  target: string;
  type: string;
  weight: number;
  accessible: boolean;
}

export interface IndoorPathSegment {
  floor: number;
  points: { x: number; y: number }[];
}

/** Snap near-horizontal/near-vertical segments to right angles for cleaner display. */
function orthogonalizeSegmentPoints(
  points: { x: number; y: number }[],
): { x: number; y: number }[] {
  if (points.length < 2) return points;
  const result: { x: number; y: number }[] = [points[0]];

  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i];
    const p1 = points[i + 1];
    const dx = p1.x - p0.x;
    const dy = p1.y - p0.y;
    const len = Math.hypot(dx, dy);
    if (len < 1e-6) continue;

    if (Math.abs(dx) >= Math.abs(dy)) {
      result.push({ x: p1.x, y: p0.y });
    } else {
      result.push({ x: p0.x, y: p1.y });
    }
    result.push({ x: p1.x, y: p1.y });
  }
  return result;
}

export interface IndoorRouteStep {
  instruction: string;
  floor: number;
}

export interface IndoorRoute {
  segments: IndoorPathSegment[];
  steps: IndoorRouteStep[];
  totalDistance: number;
  startFloor: number;
  endFloor: number;
}

type FloorData = { nodes: unknown[]; edges: unknown[] };

const ALL_FLOOR_DATA: FloorData[] = [
  cc1 as FloorData,
  hall1 as unknown as FloorData,
  hall2 as unknown as FloorData,
  hallCombined as unknown as FloorData,
  mbFloorsCombined as FloorData,
  ve1 as FloorData,
  ve2 as FloorData,
  vl1 as FloorData,
  vl2 as FloorData,
];

/** App building code "H" (Henry F. Hall) maps to JSON buildingId "Hall". MB includes S2 (MB-S2). */
function buildingIdMatches(
  buildingCode: string,
  nodeBuildingId: string,
): boolean {
  return (
    nodeBuildingId === buildingCode ||
    (buildingCode === "H" && nodeBuildingId === "Hall") ||
    (buildingCode === "MB" && nodeBuildingId === "MB-S2")
  );
}

/** JSON may use prefixed labels (e.g. "H-822", "VL-202-30"); app often sends "822", "202-30". Match both. */
function roomLabelMatches(
  buildingCode: string,
  nodeLabel: string | undefined,
  userLabel: string,
): boolean {
  if (!nodeLabel) return false;
  if (nodeLabel === userLabel) return true;
  const prefix = buildingCode === "H" ? "H" : buildingCode;
  if (nodeLabel === `${prefix}-${userLabel}`) return true;
  return false;
}

const VERTICAL_NODE_TYPES = new Set(["stair_landing", "escalator_landing", "elevator_door"]);
const ENTRY_EXIT_NODE_TYPES = new Set(["building_entry_exit", "entry", "exit"]);
const INTER_FLOOR_WEIGHT = 500;

/** Graph edge weights are in same scale as floor plan units; convert to meters for display. */
const UNITS_PER_METER = 50;

function getFloorQuery(
  buildingCode: string,
  floor: number,
): {
  targetFloor: number;
  matchesNode: (node: IndoorNode) => boolean;
} {
  if (buildingCode === "MB" && floor === -2) {
    return {
      targetFloor: 1,
      matchesNode: (node) => node.buildingId === "MB-S2" && node.floor === 1,
    };
  }

  return {
    targetFloor: floor,
    matchesNode: (node) =>
      buildingIdMatches(buildingCode, node.buildingId) && node.floor === floor,
  };
}

function filterFloorData(
  floorData: FloorData,
  predicate: (node: IndoorNode) => boolean,
): FloorData {
  const allowedNodeIds = new Set(
    (floorData.nodes as IndoorNode[]).filter(predicate).map((node) => node.id),
  );

  return {
    nodes: (floorData.nodes as IndoorNode[]).filter((node) =>
      allowedNodeIds.has(node.id),
    ),
    edges: (floorData.edges as IndoorEdge[]).filter(
      (edge) =>
        allowedNodeIds.has(edge.source) && allowedNodeIds.has(edge.target),
    ),
  };
}

function getBuildingFloors(buildingCode: string): FloorData[] {
  const matchedFloors = ALL_FLOOR_DATA.filter((f) => {
    const firstNode = f.nodes[0] as IndoorNode | undefined;
    return (
      firstNode != null && buildingIdMatches(buildingCode, firstNode.buildingId)
    );
  });

  if (buildingCode !== "H") {
    return matchedFloors;
  }

  return matchedFloors.flatMap((floorData) => {
    if (floorData !== (hallCombined as unknown as FloorData)) {
      return [floorData];
    }

    const filteredHallCombined = filterFloorData(
      floorData,
      (node) => node.floor !== 1 && node.floor !== 2,
    );

    return filteredHallCombined.nodes.length > 0 ? [filteredHallCombined] : [];
  });
}
function isEscalatorEdge(e: IndoorEdge, allNodes: Map<string, IndoorNode>): boolean {
  if (e.type === "escalator") return true;
  const src = allNodes.get(e.source);
  const tgt = allNodes.get(e.target);
  return src?.type === "escalator_landing" || tgt?.type === "escalator_landing";
}

function addBidirectionalEdge(
  adjacency: Map<string, { neighbor: string; weight: number }[]>,
  source: string,
  target: string,
  weight: number
): void {
  adjacency.get(source)!.push({ neighbor: target, weight });
  adjacency.get(target)!.push({ neighbor: source, weight });
}

function resolveDirectionalEscalator(
  e: IndoorEdge,
  allNodes: Map<string, IndoorNode>,
  adjacency: Map<string, { neighbor: string; weight: number }[]>
): boolean {
  const srcNode = allNodes.get(e.source);
  const tgtNode = allNodes.get(e.target);
  const direction = srcNode?.direction ?? tgtNode?.direction;

  if (direction !== "up" && direction !== "down") return false;

  const lower = (srcNode?.floor ?? 0) < (tgtNode?.floor ?? 0) ? srcNode : tgtNode;
  const upper = (srcNode?.floor ?? 0) < (tgtNode?.floor ?? 0) ? tgtNode : srcNode;
  const fromId = direction === "up" ? lower!.id : upper!.id;
  const toId = direction === "up" ? upper!.id : lower!.id;

  if (!adjacency.has(fromId)) adjacency.set(fromId, []);
  if (!adjacency.has(toId)) adjacency.set(toId, []);
  adjacency.get(fromId)!.push({ neighbor: toId, weight: e.weight });
  return true;
}

function processEdge(
  e: IndoorEdge,
  allNodes: Map<string, IndoorNode>,
  adjacency: Map<string, { neighbor: string; weight: number }[]>,
  noStairs: boolean,
  noEscalators: boolean,
  noElevators: boolean
): void {
  if (e.type === "elevator") {
    if (!noElevators) addBidirectionalEdge(adjacency, e.source, e.target, e.weight);
    return;
  }

  if (e.type !== "stair" && e.type !== "escalator") {
    addBidirectionalEdge(adjacency, e.source, e.target, e.weight);
    return;
  }

  const escalator = isEscalatorEdge(e, allNodes);

  if (escalator && noEscalators) return;
  if (!escalator && noStairs) return;

  if (escalator && resolveDirectionalEscalator(e, allNodes, adjacency)) return;

  addBidirectionalEdge(adjacency, e.source, e.target, e.weight);
}

function connectVerticalNodesBySuffix(
  nodes: Map<string, IndoorNode>,
  adjacency: Map<string, { neighbor: string; weight: number }[]>,
  noStairs: boolean,
  noEscalators: boolean,
  noElevators: boolean
): void {
  const bySuffix = new Map<string, IndoorNode[]>();

  nodes.forEach((node) => {
    if (!VERTICAL_NODE_TYPES.has(node.type)) return;
    const match = /_(stair_landing|escalator_landing|elevator_door)_(\d+)$/.exec(node.id);
    if (!match) return;
    const key = `${match[1]}_${match[2]}`;
    const list = bySuffix.get(key) ?? [];
    list.push(node);
    bySuffix.set(key, list);
  });
  const shouldSkipInterFloorConnection = (
    a: IndoorNode,
    noStairs: boolean,
    noEscalators: boolean,
    noElevators: boolean,
  ) => {
    if (noStairs && a.type === "stair_landing") return true;
    if (noEscalators && a.type === "escalator_landing") return true;
    if (noElevators && a.type === "elevator_door") return true;
    if (
      a.type === "escalator_landing" &&
      (a.direction === "up" || a.direction === "down")
    ) {
      return true;
    }
  
    return false;
  };
  bySuffix.forEach((connectedNodes) => {
    if (connectedNodes.length < 2) return;
    connectedNodes.sort((a, b) => a.floor - b.floor);

    for (let i = 0; i < connectedNodes.length - 1; i++) {
      const a = connectedNodes[i];
      const b = connectedNodes[i + 1];

      if (
        shouldSkipInterFloorConnection(
          a,
          noStairs,
          noEscalators,
          noElevators,
        )
      ) {
        continue;
      }

      addBidirectionalEdge(adjacency, a.id, b.id, INTER_FLOOR_WEIGHT);
    }
  });
}

function buildGraph(floors: FloorData[], noStairs = false, noEscalators = false, noElevators = false): {
  nodes: Map<string, IndoorNode>;
  adjacency: Map<string, { neighbor: string; weight: number }[]>;
} {
  const nodes = new Map<string, IndoorNode>();
  const adjacency = new Map<string, { neighbor: string; weight: number }[]>();

  const allNodes = new Map<string, IndoorNode>();
  for (const floor of floors) {
    for (const n of floor.nodes as IndoorNode[]) {
      allNodes.set(n.id, n);
    }
  }

  for (const floor of floors) {
    for (const n of floor.nodes as IndoorNode[]) {
      nodes.set(n.id, n);
      if (!adjacency.has(n.id)) adjacency.set(n.id, []);
    }
    for (const e of floor.edges as IndoorEdge[]) {
      if (e.type === "stair" || e.type === "escalator" || e.type === "elevator") {
        const srcNode = allNodes.get(e.source);
        const tgtNode = allNodes.get(e.target);
        const isEscalatorEdge = e.type === "escalator" || srcNode?.type === "escalator_landing" || tgtNode?.type === "escalator_landing";
        const isElevatorEdge = e.type === "elevator" || srcNode?.type === "elevator_door" || tgtNode?.type === "elevator_door";
        if (isElevatorEdge && noElevators) continue;
        if (isEscalatorEdge && noEscalators) continue;
        if (!isEscalatorEdge && !isElevatorEdge && noStairs) continue;

        // Enforce directionality for any vertical edge where either endpoint has a direction
        const direction = srcNode?.direction ?? tgtNode?.direction;
        if (direction === "up" || direction === "down") {
          const lowerNode = (srcNode?.floor ?? 0) < (tgtNode?.floor ?? 0) ? srcNode : tgtNode;
          const upperNode = (srcNode?.floor ?? 0) < (tgtNode?.floor ?? 0) ? tgtNode : srcNode;
          const fromId = direction === "up" ? lowerNode!.id : upperNode!.id;
          const toId = direction === "up" ? upperNode!.id : lowerNode!.id;
          if (!adjacency.has(fromId)) adjacency.set(fromId, []);
          if (!adjacency.has(toId)) adjacency.set(toId, []);
          adjacency.get(fromId)!.push({ neighbor: toId, weight: e.weight });
          continue;
        }
      }
      if (!adjacency.has(e.source)) adjacency.set(e.source, []);
      if (!adjacency.has(e.target)) adjacency.set(e.target, []);
      processEdge(e, allNodes, adjacency, noStairs, noEscalators, noElevators);
    }
  }

  // Connect stair/elevator nodes with the same suffix across floors (same physical location)
  const bySuffix = new Map<string, IndoorNode[]>();
  nodes.forEach((node) => {
    if (VERTICAL_NODE_TYPES.has(node.type)) {
      const match = /_(stair_landing|escalator_landing|elevator_door)_(\d+)$/.exec(node.id);
      if (match) {
        const key = `${match[1]}_${match[2]}`;
        const list = bySuffix.get(key) ?? [];
        list.push(node);
        bySuffix.set(key, list);
      }
    }
  });

  bySuffix.forEach((connectedNodes) => {
    if (connectedNodes.length < 2) return;
    connectedNodes.sort((a, b) => a.floor - b.floor);
    for (let i = 0; i < connectedNodes.length - 1; i++) {
      const a = connectedNodes[i];
      const b = connectedNodes[i + 1];
      if (noStairs && a.type === "stair_landing") continue;
      if (noEscalators && a.type === "escalator_landing") continue;
      if (noElevators && a.type === "elevator_door") continue;
      // If either node has a floors restriction, only connect if both floors are allowed
      const allowedFloors = a.floors ?? b.floors;
      if (allowedFloors && (!allowedFloors.includes(a.floor) || !allowedFloors.includes(b.floor))) continue;
      // Skip auto-connection for directional escalators or stairs — their edges are explicitly in the JSON
      if ((a.direction === "up" || a.direction === "down")) continue;
      adjacency.get(a.id)!.push({ neighbor: b.id, weight: INTER_FLOOR_WEIGHT });
      adjacency.get(b.id)!.push({ neighbor: a.id, weight: INTER_FLOOR_WEIGHT });
    }
  });

  return { nodes, adjacency };
}

/** Binary min-heap keyed by cost — O(log n) push/pop vs O(n log n) array.sort. */
class MinHeap {
  private heap: { id: string; cost: number }[] = [];

  get size() {
    return this.heap.length;
  }

  push(id: string, cost: number) {
    this.heap.push({ id, cost });
    this.bubbleUp(this.heap.length - 1);
  }

  pop(): { id: string; cost: number } | undefined {
    if (this.heap.length === 0) return undefined;
    const min = this.heap[0];
    const last = this.heap.pop()!;
    if (this.heap.length > 0) {
      this.heap[0] = last;
      this.sinkDown(0);
    }
    return min;
  }

  private bubbleUp(i: number) {
    while (i > 0) {
      const parent = (i - 1) >> 1;
      if (this.heap[parent].cost <= this.heap[i].cost) break;
      [this.heap[parent], this.heap[i]] = [this.heap[i], this.heap[parent]];
      i = parent;
    }
  }

  private sinkDown(i: number) {
    const n = this.heap.length;
    while (true) {
      let smallest = i;
      const l = 2 * i + 1;
      const r = 2 * i + 2;
      if (l < n && this.heap[l].cost < this.heap[smallest].cost) smallest = l;
      if (r < n && this.heap[r].cost < this.heap[smallest].cost) smallest = r;
      if (smallest === i) break;
      [this.heap[smallest], this.heap[i]] = [this.heap[i], this.heap[smallest]];
      i = smallest;
    }
  }
}

/**
 * When set, pathfinding only follows edges that stay on this floor (no stairs/elevators
 * to other floors). Use when start and end are on the same floor.
 * When excludeVerticalTransit is true (same-floor), avoids elevator/stair nodes; if
 * no path exists without them, call again with false to allow a path through them.
 */
const shouldSkipDijkstraNeighbor = (
  neighbor: string,
  visited: Set<string>,
  hasRestrictions: boolean,
  nodes: Map<string, IndoorNode>,
  restrictToFloor: number | null,
  restrictToBuildingId: string | null,
  excludeVerticalTransit: boolean,
) => {
  if (visited.has(neighbor)) return true;

  if (
    hasRestrictions &&
    !isNeighborAllowedUnderRestrictions(
      nodes,
      neighbor,
      restrictToFloor,
      restrictToBuildingId,
      excludeVerticalTransit,
    )
  ) {
    return true;
  }

  return false;
};

const relaxDijkstraNeighbor = (
  current: string,
  neighbor: string,
  weight: number,
  dist: Map<string, number>,
  prev: Map<string, string | null>,
  heap: { push: (id: string, priority: number) => void },
) => {
  const newDist = (dist.get(current) ?? 0) + weight;

  if (newDist < (dist.get(neighbor) ?? Infinity)) {
    dist.set(neighbor, newDist);
    prev.set(neighbor, current);
    heap.push(neighbor, newDist);
  }
};
function dijkstra(
  nodes: Map<string, IndoorNode>,
  adjacency: Map<string, { neighbor: string; weight: number }[]>,
  startId: string,
  endId: string,
  restrictToFloor: number | null = null,
  restrictToBuildingId: string | null = null,
  excludeVerticalTransit: boolean = false,
): { path: string[]; distance: number } | null {
  const dist = new Map<string, number>();
  const prev = new Map<string, string | null>();
  const visited = new Set<string>();
  const heap = new MinHeap();

  nodes.forEach((_, id) => {
    dist.set(id, Infinity);
    prev.set(id, null);
  });

  dist.set(startId, 0);
  heap.push(startId, 0);

  const hasRestrictions = restrictToFloor != null || restrictToBuildingId != null;

  while (heap.size > 0) {
    const { id: current } = heap.pop()!;

    if (visited.has(current)) continue;
    visited.add(current);

    if (current === endId) break;

    for (const { neighbor, weight } of adjacency.get(current) ?? []) {

      if (
        shouldSkipDijkstraNeighbor(
          neighbor,
          visited,
          hasRestrictions,
          nodes,
          restrictToFloor,
          restrictToBuildingId,
          excludeVerticalTransit,
        )
      ) {
        continue;
      }
      
      relaxDijkstraNeighbor(current, neighbor, weight, dist, prev, heap);
    }
  }

  if ((dist.get(endId) ?? Infinity) === Infinity) return null;

  const path = reconstructDijkstraPath(endId, prev);
  return { path, distance: dist.get(endId) ?? 0 };
}

function isNeighborAllowedUnderRestrictions(
  nodes: Map<string, IndoorNode>,
  neighborId: string,
  restrictToFloor: number | null,
  restrictToBuildingId: string | null,
  excludeVerticalTransit: boolean,
): boolean {
  const neighborNode = nodes.get(neighborId);
  if (neighborNode == null) return false;

  if (restrictToFloor != null) {
    const allowedFloors =
      restrictToFloor === -2 ? [1, -2] : [restrictToFloor];
    if (!allowedFloors.includes(neighborNode.floor)) return false;
  }

  if (excludeVerticalTransit && VERTICAL_NODE_TYPES.has(neighborNode.type)) {
    return false;
  }

  if (
    restrictToBuildingId != null &&
    neighborNode.buildingId !== restrictToBuildingId
  ) {
    return false;
  }

  return true;
}

function reconstructDijkstraPath(
  endId: string,
  prev: Map<string, string | null>,
): string[] {
  const path: string[] = [];
  let current: string | null | undefined = endId;
  while (current != null) {
    path.unshift(current);
    current = prev.get(current) ?? null;
  }
  return path;
}

const HALLWAY_NODE_TYPES = new Set([
  "hallway",
  "hallway_waypoint",
  "door_to_hallway",
  "doorway",
]);

function formatFloorLabel(node: IndoorNode): string {
  if (node.buildingId === "MB" && node.floor === -2) {
    return "S2";
  }
  return String(node.floor);
}

// Returns the turn direction from two direction vectors (in/out at a vertex).
function turnFromVectors(
  inDx: number,
  inDy: number,
  outDx: number,
  outDy: number,
): "left" | "right" | "straight" {
  const cross = inDx * outDy - inDy * outDx;
  const lenA = Math.hypot(inDx, inDy) || 1;
  const lenB = Math.hypot(outDx, outDy) || 1;
  const sinAngle = cross / (lenA * lenB);
  if (sinAngle > 0.25) return "right";
  if (sinAngle < -0.25) return "left";
  return "straight";
}

function snapVectorToDisplayedAxis(
  dx: number,
  dy: number,
): { x: number; y: number } {
  if (Math.abs(dx) >= Math.abs(dy)) {
    return { x: dx, y: 0 };
  }
  return { x: 0, y: dy };
}

// Returns the turn direction using the same snapped axis logic as the displayed route, so instructions follow the corridor shape the user actually sees on the map.
function getTurnDirection(
  prev: IndoorNode,
  curr: IndoorNode,
  next: IndoorNode,
): "left" | "right" | "straight" {
  const incoming = snapVectorToDisplayedAxis(curr.x - prev.x, curr.y - prev.y);
  const outgoing = snapVectorToDisplayedAxis(next.x - curr.x, next.y - curr.y);
  return turnFromVectors(incoming.x, incoming.y, outgoing.x, outgoing.y);
}

/** Find the nearest room label to a node (for landmark text). */
function getNearestRoomLabel(
  curr: IndoorNode,
  allFloorNodes: IndoorNode[],
  excludeIds: Set<string>,
  maxDistance: number,
  restrictToBuildingId: string | null = null,
): string | null {
  let best: { label: string; dist: number } | null = null;
  for (const n of allFloorNodes) {
    if (
      n.type !== "room" ||
      n.id === curr.id ||
      excludeIds.has(n.id) ||
      !n.label ||
      n.floor !== curr.floor
    )
      continue;
    if (restrictToBuildingId != null && n.buildingId !== restrictToBuildingId)
      continue;
    // Same-floor routes on MB (floor 1) should only reference MB rooms, not MB-S2.
    if (
      restrictToBuildingId == null &&
      curr.buildingId === "MB" &&
      n.buildingId === "MB-S2"
    )
      continue;
    // S2 routes should only reference MB-S2 rooms, not MB (1st floor).
    if (
      restrictToBuildingId == null &&
      curr.buildingId === "MB-S2" &&
      n.buildingId === "MB"
    )
      continue;
    const dx = n.x - curr.x;
    const dy = n.y - curr.y;
    const dist = Math.hypot(dx, dy);
    if (dist <= maxDistance && (!best || dist < best.dist))
      best = { label: n.label, dist };
  }
  return best?.label ?? null;
}

function getEdgeWeight(
  adjacency: Map<string, { neighbor: string; weight: number }[]>,
  fromId: string,
  toId: string,
): number {
  const list = adjacency.get(fromId);
  if (!list) return 0;
  const edge = list.find((e) => e.neighbor === toId);
  return edge?.weight ?? 0;
}

function roundDistanceHuman(units: number): string {
  const m = units / UNITS_PER_METER;
  if (m < 2) return `${Math.round(m * 2) / 2}`;
  if (m < 20) return `${Math.round(m)}`;
  return `${Math.round(m / 5) * 5}`;
}

/**
 * Determine which side of the corridor the destination room is on.
 * Uses the dominant axis of the approach vector (y-down SVG coordinates).
 */
function getArrivalSide(prev: IndoorNode, dest: IndoorNode): "left" | "right" {
  const dx = dest.x - prev.x;
  const dy = dest.y - prev.y;

  /**
   * Mostly horizontal: decide based on whether we're walking east/west
   * and whether the destination is toward the top of the screen (smaller y)
   */
  if (Math.abs(dx) >= Math.abs(dy)) {
    const isEast = dx > 0;
    const destOnTop = dest.y <= prev.y;
    // If destination is on the "left side" relative to travel direction, destOnTop will match isEast.
    return destOnTop === isEast ? "left" : "right";
  }

  /** Mostly vertical: decide based on whether we're walking south/north and
   whether the destination is on the eastern side (larger x).
   */
  const isSouth = dy > 0; // dy > 0 in y-down coordinates means "south"
  const destOnEast = dest.x >= prev.x;
  return destOnEast === isSouth ? "left" : "right";
}

function getArrivalRelationFromDisplayedApproach(
  prev: IndoorNode,
  dest: IndoorNode,
): "left" | "right" | "straight ahead" {
  const dx = dest.x - prev.x;
  const dy = dest.y - prev.y;
  const heading = snapVectorToDisplayedAxis(dx, dy);

  if (heading.x === 0 && heading.y === 0) {
    return getArrivalSide(prev, dest);
  }

  const forward = heading.x === 0 ? Math.abs(dy) : Math.abs(dx);
  const lateral = heading.x === 0 ? Math.abs(dx) : Math.abs(dy);
  const STRAIGHT_AHEAD_RATIO = 0.25;
  const STRAIGHT_AHEAD_MIN_FORWARD = 40;

  if (
    forward >= STRAIGHT_AHEAD_MIN_FORWARD &&
    lateral <= forward * STRAIGHT_AHEAD_RATIO
  ) {
    return "straight ahead";
  }

  return getArrivalSide(prev, dest);
}

function shouldKeepHallwayTurn(
  curr: IndoorNode,
  lastKept: IndoorNode,
  next: IndoorNode,
  turnThresholdRad: number,
): boolean {
  if (!HALLWAY_NODE_TYPES.has(curr.type)) return false;
  if (next.floor !== curr.floor) return false;

  const ax = curr.x - lastKept.x;
  const ay = curr.y - lastKept.y;
  const bx = next.x - curr.x;
  const by = next.y - curr.y;
  const lenA = Math.hypot(ax, ay);
  const lenB = Math.hypot(bx, by);
  if (lenA < 1e-6 || lenB < 1e-6) return false;

  const sinAngle = (ax * by - ay * bx) / (lenA * lenB);
  const cosAngle = (ax * bx + ay * by) / (lenA * lenB);
  const angle = Math.abs(Math.atan2(Math.abs(sinAngle), cosAngle));
  return angle > turnThresholdRad;
}

// Simplify the raw Dijkstra path into waypoints for step generation.
function simplifyPathForSteps(
  pathNodes: IndoorNode[],
  distFromStart: number[],
): { nodes: IndoorNode[]; distances: number[] } {
  if (pathNodes.length <= 2) {
    return {
      nodes: pathNodes,
      distances: distFromStart.slice(0, pathNodes.length),
    };
  }

  const TURN_THRESHOLD_RAD = 18 * (Math.PI / 180);

  const mustKeep = (n: IndoorNode) =>
    n.type === "room" || VERTICAL_NODE_TYPES.has(n.type);

  const keepIndices = new Set<number>([0, pathNodes.length - 1]);
  for (let i = 0; i < pathNodes.length; i++) {
    if (mustKeep(pathNodes[i])) keepIndices.add(i);
  }

  let lastKeptIdx = 0;
  for (let i = 1; i < pathNodes.length - 1; i++) {
    const curr = pathNodes[i];
    const next = pathNodes[i + 1];
    const lastKept = pathNodes[lastKeptIdx];

    if (keepIndices.has(i)) {
      lastKeptIdx = i;
      continue;
    }

    if (curr.floor !== lastKept.floor) {
      keepIndices.add(i);
      lastKeptIdx = i;
      continue;
    }

    if (shouldKeepHallwayTurn(curr, lastKept, next, TURN_THRESHOLD_RAD)) {
      keepIndices.add(i);
      lastKeptIdx = i;
    }
  }

  const sorted = [...keepIndices].sort((a, b) => a - b);
  return {
    nodes: sorted.map((i) => pathNodes[i]),
    distances: sorted.map((i) => distFromStart[i]),
  };
}

/**
 * Convert a simplified indoor path into Google Maps-style step-by-step directions.
 */

type BuildStepsState = {
  steps: IndoorRouteStep[];
  nodes: IndoorNode[];
  distances: number[];
  start: IndoorNode;
  endpointIds: Set<string>;
  allFloorNodes: IndoorNode[];
  landmarkBuildingId: string | null;
  currentFloor: number;
  inPassThroughFloor: boolean;
  lastEmittedDistIdx: number;
  lastLandmarkUsed: string | null;
  lastStraightDistUnits: number;
};

function usesFinalApproachArrivalInstruction(
  instruction: string | undefined,
): boolean {
  return (
    instruction?.startsWith("Continue straight for about ") === true ||
    instruction?.startsWith("Continue for about ") === true ||
    instruction?.includes(" and continue for about ") === true
  );
}

function isPassThroughFloorForSteps(
  nodes: IndoorNode[],
  start: IndoorNode,
  fromIdx: number,
  floor: number,
): boolean {
  let sawFirstVertical = false;
  let sawNonVerticalAfterFirst = false;

  for (let j = fromIdx; j < nodes.length; j++) {
    if (nodes[j].floor !== floor) {
      // Leaving the floor — pass-through only if we never walked between two vertical nodes
      return !(sawFirstVertical && sawNonVerticalAfterFirst);
    }
    if (nodes[j].type === "room" && nodes[j].id !== start.id) return false;

    if (VERTICAL_NODE_TYPES.has(nodes[j].type)) {
      if (sawFirstVertical && sawNonVerticalAfterFirst) return false;
      sawFirstVertical = true;
    } else if (sawFirstVertical) {
      sawNonVerticalAfterFirst = true;
    }
  }
  return false;
}

function emitFloorChangeStep(
  state: BuildStepsState,
  i: number,
  node: IndoorNode,
  prev: IndoorNode,
  segDist: number,
  minWalkEmit: number,
): boolean {
  if (node.floor === state.currentFloor) return false;

  if (
    isPassThroughFloorForSteps(state.nodes, state.start, i, node.floor)
  ) {
    state.currentFloor = node.floor;
    state.inPassThroughFloor = true;
    return true;
  }

  state.inPassThroughFloor = false;

  let via = "stairs";
  if (prev.type === "elevator_door" || node.type === "elevator_door") {
    via = "elevator";
  } else if (prev.type === "escalator_landing" || node.type === "escalator_landing") {
    via = "escalator";
  }

  const walkPart =
    segDist >= minWalkEmit ? `Walk about ${roundDistanceHuman(segDist)} m, then ` : "";

  state.steps.push({
    instruction: `${walkPart}take the ${via} to floor ${formatFloorLabel(node)}.`,
    floor: node.floor,
  });

  state.currentFloor = node.floor;
  state.lastEmittedDistIdx = i;
  state.lastStraightDistUnits = 0;
  return true;
}

function foldOrEmitContinueForDestination(
  state: BuildStepsState,
  i: number,
  node: IndoorNode,
  prev: IndoorNode,
  segDist: number,
  minWalkEmit: number,
  shortFinalApproach: number,
): boolean {
  if (!(node.type === "room" && node.id !== state.start.id)) return false;

  const lastStep = state.steps.at(-1);
  const canFoldIntoTurn =
    segDist >= minWalkEmit &&
    segDist <= shortFinalApproach &&
    lastStep?.instruction.startsWith("Turn ") &&
    !lastStep.instruction.includes(" and continue for about ");

  if (canFoldIntoTurn && lastStep) {
    state.steps[state.steps.length - 1] = {
      ...lastStep,
      instruction: lastStep.instruction.replace(
        /\.$/,
        ` and continue for about ${roundDistanceHuman(segDist)} m.`,
      ),
    };
  } else if (segDist >= minWalkEmit) {
    state.steps.push({
      instruction: `Continue for about ${roundDistanceHuman(segDist)} m.`,
      floor: node.floor,
    });
  }

  const lastArrivalContextStep = state.steps.at(-1);
  const arrivalRelation = usesFinalApproachArrivalInstruction(
    lastArrivalContextStep?.instruction,
  )
    ? getArrivalRelationFromDisplayedApproach(prev, node)
    : getArrivalSide(prev, node);

  state.steps.push({
    instruction:
      arrivalRelation === "straight ahead"
        ? `Room ${node.label ?? "destination"} will be straight ahead.`
        : `Room ${node.label ?? "destination"} will be on your ${arrivalRelation}.`,
    floor: node.floor,
  });

  state.lastEmittedDistIdx = i;
  state.lastStraightDistUnits = 0;
  return true;
}

function emitOrMergeContinueStraight(
  state: BuildStepsState,
  node: IndoorNode,
  segDist: number,
): void {
  const lastStep = state.steps.at(-1);
  const lastIsStraight =
    lastStep?.instruction.startsWith("Continue straight for about ") ||
    lastStep?.instruction.startsWith("Continue for about ");

  if (lastIsStraight) {
    state.steps.pop();
    const mergedUnits = state.lastStraightDistUnits + segDist;
    state.steps.push({
      instruction: `Continue straight for about ${roundDistanceHuman(mergedUnits)} m.`,
      floor: node.floor,
    });
    state.lastStraightDistUnits = mergedUnits;
  } else {
    state.steps.push({
      instruction: `Continue straight for about ${roundDistanceHuman(segDist)} m.`,
      floor: node.floor,
    });
    state.lastStraightDistUnits = segDist;
  }
}

function emitStraightWaypoint(
  state: BuildStepsState,
  i: number,
  node: IndoorNode,
  segDist: number,
  minStraightEmit: number,
): void {
  if (segDist < minStraightEmit) return;
  emitOrMergeContinueStraight(state, node, segDist);
  state.lastEmittedDistIdx = i;
}

function emitWalkLeadingToTurnIfNeeded(
  state: BuildStepsState,
  i: number,
  node: IndoorNode,
  segDist: number,
  minWalkEmit: number,
): void {
  if (segDist < minWalkEmit) return;
  emitOrMergeContinueStraight(state, node, segDist);
}

function emitTurnWaypoint({
  state,
  i,
  node,
  prev,
  next,
  turn,
  segDist,
  minWalkEmit,
  minContinue,
}: {
  state: BuildStepsState;
  i: number;
  node: IndoorNode;
  prev: IndoorNode;
  next: IndoorNode;
  turn: "left" | "right";
  segDist: number;
  minWalkEmit: number;
  minContinue: number;
}): void {
  emitWalkLeadingToTurnIfNeeded(state, i, node, segDist, minWalkEmit);

  state.lastEmittedDistIdx = i;
  state.lastStraightDistUnits = 0; // turn ends any straight run

  const nextSegDist =
    state.distances[Math.min(i + 1, state.nodes.length - 1)] - state.distances[i];
  const sameFloorNext = state.nodes[i + 1]?.floor === node.floor;
  const continueStr =
    nextSegDist >= minContinue && sameFloorNext
      ? ` and continue for about ${roundDistanceHuman(nextSegDist)} m`
      : "";

  if (continueStr) state.lastEmittedDistIdx = i + 1;

  const nextIsDestination =
    i + 1 === state.nodes.length - 1 && state.nodes[i + 1]?.type === "room";
  const LANDMARK_RADIUS_UNITS = 80;

  const rawLandmark = nextIsDestination
    ? null
    : getNearestRoomLabel(
      node,
      state.allFloorNodes,
      state.endpointIds,
      LANDMARK_RADIUS_UNITS,
      state.landmarkBuildingId,
    );

  const landmark = rawLandmark === state.lastLandmarkUsed ? null : rawLandmark;
  const atPhrase = landmark ? ` at room ${landmark}` : "";
  state.lastLandmarkUsed = rawLandmark ?? state.lastLandmarkUsed;

  const continueInstruction = `Turn ${turn}${atPhrase}${continueStr}.`;
  const lastStep = state.steps.at(-1);
  if (lastStep?.instruction === continueInstruction) return;

  state.steps.push({
    instruction: continueInstruction,
    floor: node.floor,
  });
}

function emitRouteStepsFromSimplifiedNodes(
  state: BuildStepsState,
): void {
  const MIN_WALK_EMIT = UNITS_PER_METER;
  const MIN_STRAIGHT_EMIT = 80;
  const MIN_CONTINUE = UNITS_PER_METER;
  const SHORT_FINAL_APPROACH = 220; // ~11 m

  for (let i = 1; i < state.nodes.length; i++) {
    const node = state.nodes[i];
    const prev = state.nodes[i - 1];
    const next = state.nodes[i + 1];
    // `lastEmittedDistIdx` is an index into `state.distances`.
    const segDist = state.distances[i] - state.distances[state.lastEmittedDistIdx];

    if (emitFloorChangeStep(state, i, node, prev, segDist, MIN_WALK_EMIT))
      continue;

    if (state.inPassThroughFloor) continue;

    if (
      foldOrEmitContinueForDestination(
        state,
        i,
        node,
        prev,
        segDist,
        MIN_WALK_EMIT,
        SHORT_FINAL_APPROACH,
      )
    )
      continue;

    // Hallway waypoint (turn or straight)
    if (!next || !HALLWAY_NODE_TYPES.has(node.type)) continue;

    const turn = getTurnDirection(prev, node, next);
    if (turn === "straight") {
      emitStraightWaypoint(
        state,
        i,
        node,
        segDist,
        MIN_STRAIGHT_EMIT,
      );
      continue;
    }

    emitTurnWaypoint({
      state,
      i,
      node,
      prev,
      next,
      turn,
      segDist,
      minWalkEmit: MIN_WALK_EMIT,
      minContinue: MIN_CONTINUE,
    });
  }
}

function buildSteps(
  allPathNodes: IndoorNode[],
  allDistFromStart: number[],
  _adjacency: Map<string, { neighbor: string; weight: number }[]>,
  allFloorNodes: IndoorNode[],
  landmarkBuildingId: string | null = null,
): IndoorRouteStep[] {
  if (allPathNodes.length === 0) return [];

  const { nodes, distances } = simplifyPathForSteps(
    allPathNodes,
    allDistFromStart,
  );

  // IDs of start and destination — excluded from landmark references.
  const endpointIds = new Set<string>();
  if (allPathNodes.length > 0) endpointIds.add(allPathNodes[0].id);
  if (allPathNodes.length > 1) {
    endpointIds.add(allPathNodes.at(-1)!.id);
  }

  const steps: IndoorRouteStep[] = [];
  const start = nodes[0];
  steps.push({
    instruction: `Start at room ${start.label ?? start.id}.`,
    floor: start.floor,
  });

  if (nodes.length === 1) return steps;

  const state: BuildStepsState = {
    steps,
    nodes,
    distances,
    start,
    endpointIds,
    allFloorNodes,
    landmarkBuildingId,
    currentFloor: start.floor,
    inPassThroughFloor: false,
    lastEmittedDistIdx: 0,
    lastLandmarkUsed: null,
    lastStraightDistUnits: 0,
  };

  emitRouteStepsFromSimplifiedNodes(state);
  return steps;
}

function findIndoorRouteEndpoints(
  nodes: Map<string, IndoorNode>,
  buildingCode: string,
  startRoomLabel: string,
  endRoomLabel: string,
): { startNode?: IndoorNode; endNode?: IndoorNode } {
  let startNode: IndoorNode | undefined;
  let endNode: IndoorNode | undefined;

  for (const node of nodes.values()) {
    if (node.type !== "room") continue;
    if (!buildingIdMatches(buildingCode, node.buildingId)) continue;

    if (roomLabelMatches(buildingCode, node.label, startRoomLabel)) {
      startNode = node;
    }
    if (roomLabelMatches(buildingCode, node.label, endRoomLabel)) {
      endNode = node;
    }
  }

  return { startNode, endNode };
}

function buildSameRoomIndoorRoute(
  startNode: IndoorNode,
  startRoomLabel: string,
): IndoorRoute {
  return {
    segments: [
      {
        floor: startNode.floor,
        points: [{ x: startNode.x, y: startNode.y }],
      },
    ],
    steps: [
      {
        instruction: `You are already at room ${startRoomLabel}`,
        floor: startNode.floor,
      },
    ],
    totalDistance: 0,
    startFloor: startNode.floor,
    endFloor: startNode.floor,
  };
}

function findBestIndoorPath(
  nodes: Map<string, IndoorNode>,
  adjacency: Map<string, { neighbor: string; weight: number }[]>,
  startNode: IndoorNode,
  endNode: IndoorNode,
) {
  const sameFloor = startNode.floor === endNode.floor;
  let result = dijkstra(
    nodes,
    adjacency,
    startNode.id,
    endNode.id,
    sameFloor ? startNode.floor : null,
    null,
    true, // prefer route that avoids elevator/stairs
  );

  result ??= dijkstra(
      nodes,
      adjacency,
      startNode.id,
      endNode.id,
      sameFloor ? startNode.floor : null,
      null,
      false,
    );

  return result;
}

function buildIndoorRouteFromDijkstraResult(
  result: { path: string[]; distance: number },
  graphNodes: Map<string, IndoorNode>,
  graphAdjacency: Map<string, { neighbor: string; weight: number }[]>,
  startNode: IndoorNode,
  endNode: IndoorNode,
): IndoorRoute {
  const pathNodes = result.path
    .map((id) => graphNodes.get(id))
    .filter((n): n is IndoorNode => n !== undefined);

  const distFromStart: number[] = [0];
  for (let i = 1; i < pathNodes.length; i++) {
    const w = getEdgeWeight(
      graphAdjacency,
      pathNodes[i - 1].id,
      pathNodes[i].id,
    );
    distFromStart[i] = distFromStart[i - 1] + w;
  }

  const segments: IndoorPathSegment[] = [];
  let currentSegment: IndoorPathSegment | null = null;

  for (const node of pathNodes) {
    const seg = currentSegment as any;
    if (seg?.floor !== node.floor) {
      if (currentSegment) segments.push(currentSegment);
      currentSegment = { floor: node.floor, points: [] };
    }
    currentSegment!.points.push({ x: node.x, y: node.y });
  }
  if (currentSegment) segments.push(currentSegment);

  const orthogonalSegments = segments;

  const allFloorNodes = Array.from(graphNodes.values());
  const landmarkBuildingId =
    startNode.buildingId === "MB-S2" && endNode.buildingId === "MB-S2"
      ? "MB-S2"
      : null;

  return {
    segments: orthogonalSegments,
    steps: buildSteps(
      pathNodes,
      distFromStart,
      graphAdjacency,
      allFloorNodes,
      landmarkBuildingId,
    ),
    totalDistance: result.distance,
    startFloor: startNode.floor,
    endFloor: endNode.floor,
  };
}

export interface SpecialFloorNode {
  id: string;
  x: number;
  y: number;
  type: "bathroom" | "stairs" | "elevator" | "escalator";
  category?: string;
}

export function getSpecialNodesForFloor(
  buildingCode: string,
  floor: number,
): SpecialFloorNode[] {
  const floors = getBuildingFloors(buildingCode);
  if (floors.length === 0) return [];

  const floorQuery = getFloorQuery(buildingCode, floor);

  const specialNodes: SpecialFloorNode[] = [];

  for (const floorData of floors) {
    const nodes = floorData.nodes as IndoorNode[];

    for (const node of nodes) {
      if (!floorQuery.matchesNode(node)) continue;

      // Check for bathrooms
      const category = (node as any).category;
      if (category === "male_washroom" || category === "female_washroom") {
        specialNodes.push({
          id: node.id,
          x: node.x,
          y: node.y,
          type: "bathroom",
          category,
        });
        continue;
      }

      // Check for stairs, elevators, escalators
      if (node.type === "stair_landing") {
        specialNodes.push({
          id: node.id,
          x: node.x,
          y: node.y,
          type: "stairs",
        });
      } else if (node.type === "elevator_door") {
        specialNodes.push({
          id: node.id,
          x: node.x,
          y: node.y,
          type: "elevator",
        });
      } else if (node.type === "escalator_landing") {
        specialNodes.push({
          id: node.id,
          x: node.x,
          y: node.y,
          type: "escalator",
        });
      }
    }
  }

  return specialNodes;
}

export function findIndoorRoute(
  buildingCode: string,
  startRoomLabel: string,
  endRoomLabel: string,
  noStairs = false,
  noEscalators = false,
  noElevators = false,
): IndoorRoute | null {
  const floors = getBuildingFloors(buildingCode);
  if (floors.length === 0) return null;

  const { nodes, adjacency } = buildGraph(floors, noStairs, noEscalators, noElevators);
  const { startNode, endNode } = findIndoorRouteEndpoints(
    nodes,
    buildingCode,
    startRoomLabel,
    endRoomLabel,
  );

  if (!startNode || !endNode) return null;
  if (startNode.id === endNode.id) {
    return buildSameRoomIndoorRoute(startNode, startRoomLabel);
  }

  const result = findBestIndoorPath(nodes, adjacency, startNode, endNode);
  if (!result) return null;

  return buildIndoorRouteFromDijkstraResult(
    result,
    nodes,
    adjacency,
    startNode,
    endNode,
  );
}

export function findIndoorRouteToNodeId(
  buildingCode: string,
  startRoomLabel: string,
  targetNodeId: string,
  noStairs = false,
  noEscalators = false,
  noElevators = false,
): IndoorRoute | null {
  const floors = getBuildingFloors(buildingCode);
  if (floors.length === 0) return null;

  const { nodes, adjacency } = buildGraph(floors, noStairs, noEscalators, noElevators);

  let startNode: IndoorNode | undefined;
  for (const node of nodes.values()) {
    if (node.type !== "room") continue;
    if (!buildingIdMatches(buildingCode, node.buildingId)) continue;
    if (roomLabelMatches(buildingCode, node.label, startRoomLabel)) {
      startNode = node;
      break;
    }
  }

  const endNode = nodes.get(targetNodeId);
  if (!startNode || !endNode) return null;
  if (startNode.id === endNode.id) {
    return buildSameRoomIndoorRoute(startNode, startRoomLabel);
  }

  const result = findBestIndoorPath(nodes, adjacency, startNode, endNode);
  if (!result) return null;

  return buildIndoorRouteFromDijkstraResult(
    result,
    nodes,
    adjacency,
    startNode,
    endNode,
  );
}
const findStartNode = (
  nodes: Map<string, IndoorNode>,
  buildingCode: string,
  startRoomLabel: string,
): IndoorNode | undefined => {
  for (const node of nodes.values()) {
    if (node.type !== "room") continue;
    if (!buildingIdMatches(buildingCode, node.buildingId)) continue;

    if (roomLabelMatches(buildingCode, node.label, startRoomLabel)) {
      return node;
    }
  }

  return undefined;
};

const findExitNodes = (
  nodes: Map<string, IndoorNode>,
  buildingCode: string,
): IndoorNode[] => {
  const exitNodes: IndoorNode[] = [];

  for (const node of nodes.values()) {
    if (
      ENTRY_EXIT_NODE_TYPES.has(node.type) &&
      buildingIdMatches(buildingCode, node.buildingId)
    ) {
      exitNodes.push(node);
    }
  }

  return exitNodes;
};

/** Route from a room to the nearest building exit. Returns null if no exit nodes exist or no path found. */
export function findRouteToNearestExit(
  buildingCode: string,
  startRoomLabel: string,
  noStairs = false,
  noEscalators = false,
  noElevators = false,
): IndoorRoute | null {
  const floors = getBuildingFloors(buildingCode);
  if (floors.length === 0) return null;

  const { nodes, adjacency } = buildGraph(floors, noStairs, noEscalators, noElevators);

  const startNode = findStartNode(nodes, buildingCode, startRoomLabel);
  if (!startNode) return null;

  const exitNodes = findExitNodes(nodes, buildingCode);
  if (exitNodes.length === 0) return null;

  let bestResult: { path: string[]; distance: number } | null = null;
  let bestExitNode: IndoorNode | null = null;

  for (const exitNode of exitNodes) {
    const result = findBestIndoorPath(nodes, adjacency, startNode, exitNode);
    if (result && (!bestResult || result.distance < bestResult.distance)) {
      bestResult = result;
      bestExitNode = exitNode;
    }
  }

  if (!bestResult || !bestExitNode) return null;

  const route = buildIndoorRouteFromDijkstraResult(bestResult, nodes, adjacency, startNode, bestExitNode);
  route.steps.push({ instruction: "Exit the building.", floor: bestExitNode.floor });
  return route;
}

/** Route from the nearest building entrance to a destination room. Returns null if no entry nodes exist or no path found. */
export function findRouteFromNearestExit(
  buildingCode: string,
  endRoomLabel: string,
  noStairs = false,
  noEscalators = false,
  noElevators = false,
): IndoorRoute | null {
  const floors = getBuildingFloors(buildingCode);
  if (floors.length === 0) return null;

  const { nodes, adjacency } = buildGraph(floors, noStairs, noEscalators, noElevators);

  let endNode: IndoorNode | undefined;
  for (const node of nodes.values()) {
    if (node.type !== "room") continue;
    if (!buildingIdMatches(buildingCode, node.buildingId)) continue;
    if (roomLabelMatches(buildingCode, node.label, endRoomLabel)) {
      endNode = node;
      break;
    }
  }
  if (!endNode) return null;

  const entryNodes: IndoorNode[] = [];
  for (const node of nodes.values()) {
    if (ENTRY_EXIT_NODE_TYPES.has(node.type) && buildingIdMatches(buildingCode, node.buildingId)) {
      entryNodes.push(node);
    }
  }
  // Fall back to floor-1 hallway nodes when building has no explicit entry/exit markers
  if (entryNodes.length === 0) {
    for (const node of nodes.values()) {
      if (node.floor === 1 && HALLWAY_NODE_TYPES.has(node.type) && buildingIdMatches(buildingCode, node.buildingId)) {
        entryNodes.push(node);
      }
    }
  }
  if (entryNodes.length === 0) return null;

  let bestResult: { path: string[]; distance: number } | null = null;
  let bestEntryNode: IndoorNode | null = null;

  for (const entryNode of entryNodes) {
    const result = findBestIndoorPath(nodes, adjacency, entryNode, endNode);
    if (result && (!bestResult || result.distance < bestResult.distance)) {
      bestResult = result;
      bestEntryNode = entryNode;
    }
  }

  if (!bestResult || !bestEntryNode) return null;

  const route = buildIndoorRouteFromDijkstraResult(bestResult, nodes, adjacency, bestEntryNode, endNode);
  if (route.steps.length > 0) {
    route.steps[0] = { instruction: "Enter the building.", floor: bestEntryNode.floor };
  }
  return route;
}

/**
 * Bounds (width, height) of floor plan images. When you map JSON coordinates to a PNG,
 * use that PNG's pixel dimensions here so the overlay aligns with the image.
 * Key: `${buildingCode}-${floor}` (e.g. "MB-1", "MB--2" for S2).
 */
const FLOOR_PLAN_IMAGE_BOUNDS: Record<
  string,
  { width: number; height: number }
> = {
  "MB-1": { width: 1024, height: 1024 },
  "MB--2": { width: 1024, height: 1024 },
  "H-1": { width: 1024, height: 1024 },
  "H-2": { width: 1024, height: 1024 },
  "H-8": { width: 1024, height: 1024 },
  "H-9": { width: 1024, height: 1024 },
  "VE-1": { width: 1024, height: 1024 },
  "VE-2": { width: 1385, height: 650 },
  "VL-1": { width: 1024, height: 1024 },
  "VL-2": { width: 1024, height: 1024 },
};

export function getFloorBounds(
  buildingCode: string,
  floor: number,
): { width: number; height: number } {
  const imageKey = `${buildingCode}-${floor}`;
  const imageBounds = FLOOR_PLAN_IMAGE_BOUNDS[imageKey];
  if (imageBounds) {
    return imageBounds;
  }

  let floorData: FloorData | undefined;

  if (buildingCode === "H" && floor === 1) {
    floorData = hall1 as unknown as FloorData;
  } else if (buildingCode === "H" && floor === 2) {
    floorData = hall2 as unknown as FloorData;
  } else {
    floorData = ALL_FLOOR_DATA.find((f) => {
      const nodes = f.nodes as IndoorNode[];
      const { matchesNode } = getFloorQuery(buildingCode, floor);
      return nodes.some(matchesNode);
    });
  }

  if (!floorData) return { width: 2000, height: 1500 };

  let maxX = 0;
  let maxY = 0;
  const { matchesNode } = getFloorQuery(buildingCode, floor);

  for (const n of floorData.nodes as IndoorNode[]) {
    if (!matchesNode(n)) continue;
    if (n.x > maxX) maxX = n.x;
    if (n.y > maxY) maxY = n.y;
  }

  return { width: maxX + 200, height: maxY + 200 };
}

export function getGraphFloorBounds(
  buildingCode: string,
  floor: number,
): { width: number; height: number } {
  let floorData: FloorData | undefined;

  if (buildingCode === "H" && floor === 1) {
    floorData = hall1 as unknown as FloorData;
  } else if (buildingCode === "H" && floor === 2) {
    floorData = hall2 as unknown as FloorData;
  } else {
    floorData = ALL_FLOOR_DATA.find((f) => {
      const nodes = f.nodes as IndoorNode[];
      const { matchesNode } = getFloorQuery(buildingCode, floor);
      return nodes.some(matchesNode);
    });
  }

  if (!floorData) return { width: 2000, height: 1500 };

  let maxX = 0;
  let maxY = 0;
  const { matchesNode } = getFloorQuery(buildingCode, floor);

  for (const n of floorData.nodes as IndoorNode[]) {
    if (!matchesNode(n)) continue;
    if (n.x > maxX) maxX = n.x;
    if (n.y > maxY) maxY = n.y;
  }

  const key = `${buildingCode}-${floor}`;
  switch (key) {
    case "VL-1":
      return { width: maxX + 220, height: maxY + 100 };
    case "VL-2":
      return { width: maxX + 200, height: maxY + 200 };
    case "VE-1":
      return { width: maxX + 200, height: maxY + 100 };
    case "VE-2":
      return { width: maxX + 150, height: maxY + 100 };
    case "CC-1":
      return { width: maxX + 550, height: maxY + 170 };
    case "H-1":
      return { width: maxX + 20, height: maxY + 40 };
    case "H-2":
      return { width: maxX + 20, height: maxY + 255 };
    case "MB-1":
      return { width: maxX + 60, height: maxY + 50 };
    case "MB--2":
      return { width: maxX + 80, height: maxY + 50 };
    case "H-8":
      return { width: maxX + 200, height: maxY + 200 };
    case "H-9":
      return { width: maxX + 100, height: maxY + 200 };
    default:
      return { width: maxX + 20, height: maxY + 255 };
  }
}

export const __indoorDirectionsTestUtils = {
  getFloorQuery,
  orthogonalizeSegmentPoints,
  buildGraph,
  dijkstra,
  formatFloorLabel,
  getNearestRoomLabel,
  getArrivalRelationFromDisplayedApproach,
  simplifyPathForSteps,
  buildSteps,
  findBestIndoorPath,
  addBidirectionalEdge,
  isEscalatorEdge,
  resolveDirectionalEscalator,
  processEdge,
  connectVerticalNodesBySuffix,
  isPassThroughFloorForSteps,
};