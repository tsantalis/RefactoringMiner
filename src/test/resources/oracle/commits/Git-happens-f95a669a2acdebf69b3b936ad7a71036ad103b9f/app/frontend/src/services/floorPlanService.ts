import hData from "../../assets/indoor_floor_plans/hall.json";
import ccData from "../../assets/indoor_floor_plans/cc1.json";
import mbData from "../../assets/indoor_floor_plans/mb_floors_combined.json";
import veData from "../../assets/indoor_floor_plans/ve.json";
import vlData from "../../assets/indoor_floor_plans/vl_floors_combined.json";

export type IndoorBuildingId = "H" | "CC" | "MB" | "VE" | "VL";

export type FloorNumber = "1" | "2" | "8" | "9" | "S2";

export type LocalizedNodeType =
  | "entrance"
  | "elevator"
  | "stairs"
  | "washroom"
  | "classroom"
  | "water_fountain"
  | "room"
  | "doorway"
  | "stair_landing"
  | "hallway_waypoint"
  | "building_entry_exit";

export interface LocalizedNode {
  id: string;
  label: string;
  nodeType: LocalizedNodeType;
  /** Floor this node lives on. Used by multi-floor pathfinding to split paths by floor. */
  floor: FloorNumber;
  x: number;
  y: number;
}

export interface FloorPlanRegistryEntry {
  buildingId: IndoorBuildingId;
  floorNumber: FloorNumber;
  localizedNodes: LocalizedNode[];
  edges: RawEdge[];
  // True when the floor plan PNG already has styled map-pin POI icons embedded on it, so we know not to render the SVG overlay icons for that floor
  poiIconsEmbedded: boolean;
}

export interface RawMapNode {
  id: string;
  type: string;
  buildingId: string;
  floor: number | string;
  x: number;
  y: number;
  label?: string;
  accessible?: boolean;
}

export interface RawEdge {
  source: string;
  target: string;
  type: string;
  weight: number;
  accessible?: boolean;
}

const SUPPORTED_INDOOR_BUILDINGS = new Set<string>([
  "H",
  "CC",
  "MB",
  "VE",
  "VL",
]);

const isSupportedIndoorBuildingId = (
  buildingId: string,
): buildingId is IndoorBuildingId => SUPPORTED_INDOOR_BUILDINGS.has(buildingId);

const ALL_RAW_NODES: RawMapNode[] = [
  ...(hData?.nodes || []),
  ...(ccData?.nodes || []),
  ...(mbData?.nodes || []),
  ...(veData?.nodes || []),
  ...(vlData?.nodes || []),
];

const ALL_RAW_EDGES: RawEdge[] = [
  ...((hData as any)?.edges || []),
  ...((ccData as any)?.edges || []),
  ...((mbData as any)?.edges || []),
  ...((veData as any)?.edges || []),
  ...((vlData as any)?.edges || []),
];

export const getSupportedFloorsForBuilding = (
  buildingId: string,
): FloorNumber[] => {
  if (!isSupportedIndoorBuildingId(buildingId)) {
    return [];
  }

  switch (buildingId) {
    case "H":
      return ["1", "2", "8", "9"];
    case "MB":
      return ["1", "S2"];
    case "CC":
      return ["1"];
    case "VE":
      return ["1", "2"];
    case "VL":
      return ["1", "2"];
    default:
      return [];
  }
};

/**
 * Weight assigned to a single cross-floor hop (elevator or staircase).
 * Large enough that the planner prefers same-floor routes, but small enough
 * that it will cross floors when needed.
 */
export const CROSS_FLOOR_WEIGHT = 500;

/**
 * Returns a grouping key used to match the *same* physical elevator shaft or
 * stairwell across different floors.
 *
 * - Elevators: full normalised label  (e.g. "helevator1", "elevator2")
 * - Stairs:    trailing digit only    (e.g. "1" from "stairs1" AND "stirs1")
 *   The digit-only match handles the VE building typo ("stirs1" vs "stairs1").
 *
 * Returns null for nodes with no usable label (e.g. Hall stair landings without
 * labels) — those are silently skipped.
 */
function crossFloorKey(node: RawMapNode): string | null {
  const normalised = (node.label || "")
    .toLowerCase()
    .replaceAll(/[^a-z0-9]/g, "");

  if (!normalised) return null;

  if (node.type === "stair_landing") {
    const regex = /\d+/;
    const match = regex.exec(normalised);

    const digit = match ? match[0] : null;
    return digit ? `stair_${digit}` : null;
  }

  if (node.type === "elevator_door") {
    return `elev_${normalised}`;
  }

  return null;
}

/**
 * Generates virtual edges that connect the same elevator shaft / stairwell
 * across floors, using label-based matching (see `crossFloorKey`).
 *
 * Only nodes whose type is `elevator_door` or `stair_landing` are considered.
 * Nodes on the same floor are never connected to each other here.
 */
export function buildCrossFloorEdges(buildingNodes: RawMapNode[]): RawEdge[] {
  const groups = groupCrossFloorNodes(buildingNodes);

  return Array.from(groups.values()).flatMap(createEdgesForGroup);
}
function groupCrossFloorNodes(nodes: RawMapNode[]) {
  const groups = new Map<string, RawMapNode[]>();

  for (const node of nodes) {
    if (!isCrossFloorNode(node)) continue;

    const key = crossFloorKey(node);
    if (!key) continue;

    const group = groups.get(key) ?? [];
    group.push(node);
    groups.set(key, group);
  }

  return groups;
}
function isCrossFloorNode(node: RawMapNode) {
  return node.type === "elevator_door" || node.type === "stair_landing";
}
function createEdgesForGroup(nodes: RawMapNode[]): RawEdge[] {
  const edges: RawEdge[] = [];

  for (let i = 0; i < nodes.length; i++) {
    for (let j = i + 1; j < nodes.length; j++) {
      if (sameFloor(nodes[i], nodes[j])) continue;

      edges.push(createEdge(nodes[i], nodes[j]));
    }
  }

  return edges;
}
function sameFloor(a: RawMapNode, b: RawMapNode) {
  return String(a.floor) === String(b.floor);
}

function createEdge(a: RawMapNode, b: RawMapNode): RawEdge {
  return {
    source: a.id,
    target: b.id,
    type: "cross_floor",
    weight: CROSS_FLOOR_WEIGHT,
    accessible: !!(a.accessible && b.accessible),
  };
}
const VALID_NODE_TYPES = new Set<LocalizedNodeType>([
  "entrance",
  "elevator",
  "stairs",
  "washroom",
  "classroom",
  "water_fountain",
  "room",
  "doorway",
  "stair_landing",
  "hallway_waypoint",
  "building_entry_exit",
]);

const VALID_FLOORS = new Set<FloorNumber>(["1", "2", "8", "9", "S2"]);

function resolveNodeType(
  rawType: string | undefined,
  nodeId: string,
): LocalizedNodeType {
  const type = rawType || "room";
  // Normalise legacy JSON type name to the canonical LocalizedNodeType value
  if (type === "elevator_door") return "elevator";
  // Hall washrooms are typed "room" but identified via their id
  if (type === "room" && nodeId.toLowerCase().includes("washroom"))
    return "washroom";
  if (VALID_NODE_TYPES.has(type as LocalizedNodeType))
    return type as LocalizedNodeType;
  console.warn(
    `Unknown node type "${type}" for node "${nodeId}", falling back to "room"`,
  );
  return "room";
}

/**
 * Resolves a raw JSON floor value to a validated FloorNumber.
 * Falls back to "1" and warns when the value is not in the known set.
 */
function resolveFloor(rawFloor: number | string, nodeId: string): FloorNumber {
  const floor = String(rawFloor);
  if (VALID_FLOORS.has(floor as FloorNumber)) return floor as FloorNumber;
  console.warn(
    `Unknown floor "${floor}" for node "${nodeId}", falling back to "1"`,
  );
  return "1";
}

function toLocalizedNode(node: RawMapNode): LocalizedNode {
  return {
    id: node.id,
    label: node.label || node.id,
    nodeType: resolveNodeType(node.type, node.id),
    floor: resolveFloor(node.floor, node.id),
    x: node.x,
    y: node.y,
  };
}

function matchesBuildingId(
  node: RawMapNode,
  buildingId: IndoorBuildingId,
): boolean {
  const nb = String(node.buildingId);
  return buildingId === "H" ? nb === "Hall" : nb === buildingId;
}

/** Returns only edges whose source and target both exist in the given id set. */
function filterEdgesForNodes(
  edges: RawEdge[],
  nodeIds: Set<string>,
): RawEdge[] {
  return edges.filter((e) => nodeIds.has(e.source) && nodeIds.has(e.target));
}

export const getFloorPlanRegistryEntry = (
  buildingId: string,
  floorNumber: FloorNumber,
): FloorPlanRegistryEntry | null => {
  if (!isSupportedIndoorBuildingId(buildingId)) {
    return null;
  }

  const rawNodesForFloor = ALL_RAW_NODES.filter(
    (node) =>
      matchesBuildingId(node, buildingId) &&
      String(node.floor) === String(floorNumber),
  );

  if (rawNodesForFloor.length === 0) {
    console.warn(`No JSON nodes found for ${buildingId} Floor ${floorNumber}`);
    return null;
  }

  const localizedNodes: LocalizedNode[] = rawNodesForFloor.map(toLocalizedNode);

  const nodeIdSet = new Set(rawNodesForFloor.map((n) => n.id));
  const edges = filterEdgesForNodes(ALL_RAW_EDGES, nodeIdSet);

  // Floors whose PNG already contains styled map-pin icons — skip the SVG overlay for those
  const EMBEDDED_ICON_FLOORS = new Set(["H_8", "H_9", "VE_2"]);
  const poiIconsEmbedded = EMBEDDED_ICON_FLOORS.has(
    `${buildingId}_${floorNumber}`,
  );

  return {
    buildingId,
    floorNumber,
    localizedNodes,
    edges,
    poiIconsEmbedded,
  };
};

/**
 * Returns the complete graph for an entire building — all floors combined —
 * with same-floor edges from the JSON **plus** generated cross-floor edges
 * linking matching elevator shafts and stairwells.
 *
 * Use this as input to `findPath` for multi-floor indoor navigation.
 * The returned `LocalizedNode` objects each carry a `floor` field so the
 * path can be split by floor for display.
 */
export const getBuildingGraph = (
  buildingId: string,
): { nodes: LocalizedNode[]; edges: RawEdge[] } | null => {
  if (!isSupportedIndoorBuildingId(buildingId)) return null;

  const rawBuilding = ALL_RAW_NODES.filter((node) =>
    matchesBuildingId(node, buildingId),
  );

  if (rawBuilding.length === 0) return null;

  const nodes = rawBuilding.map(toLocalizedNode);

  const nodeIdSet = new Set(rawBuilding.map((n) => n.id));
  const sameFloorEdges = filterEdgesForNodes(ALL_RAW_EDGES, nodeIdSet);
  const crossFloorEdges = buildCrossFloorEdges(rawBuilding);

  return {
    nodes,
    edges: [...sameFloorEdges, ...crossFloorEdges],
  };
};
