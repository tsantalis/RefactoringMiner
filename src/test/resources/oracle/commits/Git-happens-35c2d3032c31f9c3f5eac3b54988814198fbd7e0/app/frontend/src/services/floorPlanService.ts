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

  // We know what PNG assets exist for each building and what floors they have.
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

// ---------------------------------------------------------------------------
// Cross-floor edge generation
// ---------------------------------------------------------------------------

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
    .replace(/[^a-z0-9]/g, "");

  if (!normalised) return null;

  if (node.type === "stair_landing") {
    const digit = normalised.match(/\d+/)?.[0];
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
  const groups = new Map<string, RawMapNode[]>();

  for (const node of buildingNodes) {
    if (node.type !== "elevator_door" && node.type !== "stair_landing") {
      continue;
    }
    const key = crossFloorKey(node);
    if (!key) continue;

    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(node);
  }

  const edges: RawEdge[] = [];

  for (const [, nodes] of groups) {
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        // Only link nodes that are on *different* floors
        if (String(nodes[i].floor) === String(nodes[j].floor)) continue;

        edges.push({
          source: nodes[i].id,
          target: nodes[j].id,
          type: "cross_floor",
          weight: CROSS_FLOOR_WEIGHT,
          accessible: !!(nodes[i].accessible && nodes[j].accessible),
        });
      }
    }
  }

  return edges;
}

// ---------------------------------------------------------------------------
// Shared node mapping helper
// ---------------------------------------------------------------------------

function toLocalizedNode(node: RawMapNode): LocalizedNode {
  let nodeType = (node.type || "room") as LocalizedNodeType;
  // Normalise legacy JSON type names to the canonical LocalizedNodeType values
  if ((nodeType as string) === "elevator_door") nodeType = "elevator";
  // Hall washrooms are typed "room" but identified via their id
  if (nodeType === "room" && node.id.toLowerCase().includes("washroom"))
    nodeType = "washroom";
  return {
    id: node.id,
    label: node.label || node.id,
    nodeType,
    floor: String(node.floor) as FloorNumber,
    x: node.x,
    y: node.y,
  };
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

export const getFloorPlanRegistryEntry = (
  buildingId: string,
  floorNumber: FloorNumber,
): FloorPlanRegistryEntry | null => {
  if (!isSupportedIndoorBuildingId(buildingId)) {
    return null;
  }

  // Find all raw JSON nodes that match the requested building and floor,
  const rawNodesForFloor = ALL_RAW_NODES.filter((node) => {
    const nodeBuilding = String(node.buildingId);
    const nodeFloor = String(node.floor);

    // 1. Handle the Hall Building mismatch ("H" -> "Hall")
    if (buildingId === "H") {
      return nodeBuilding === "Hall" && nodeFloor === String(floorNumber);
    }

    // 2. Handle standard matches (CC, MB, VE, VL)
    return nodeBuilding === buildingId && nodeFloor === String(floorNumber);
  });

  //If no nodes are found, return null
  if (rawNodesForFloor.length === 0) {
    console.warn(`No JSON nodes found for ${buildingId} Floor ${floorNumber}`);
    return null;
  }

  const localizedNodes: LocalizedNode[] = rawNodesForFloor.map(toLocalizedNode);

  const nodeIdSet = new Set(rawNodesForFloor.map((n) => n.id));
  const edges = ALL_RAW_EDGES.filter(
    (e) => nodeIdSet.has(e.source) && nodeIdSet.has(e.target),
  );

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
 * path can be split by floor for display (see the next task).
 */
export const getBuildingGraph = (
  buildingId: string,
): { nodes: LocalizedNode[]; edges: RawEdge[] } | null => {
  if (!isSupportedIndoorBuildingId(buildingId)) return null;

  const rawBuilding = ALL_RAW_NODES.filter((node) => {
    const nb = String(node.buildingId);
    // Hall building mismatch
    return buildingId === "H" ? nb === "Hall" : nb === buildingId;
  });

  if (rawBuilding.length === 0) return null;

  const nodes = rawBuilding.map(toLocalizedNode);

  const nodeIdSet = new Set(rawBuilding.map((n) => n.id));
  const sameFloorEdges = ALL_RAW_EDGES.filter(
    (e) => nodeIdSet.has(e.source) && nodeIdSet.has(e.target),
  );

  const crossFloorEdges = buildCrossFloorEdges(rawBuilding);

  return {
    nodes,
    edges: [...sameFloorEdges, ...crossFloorEdges],
  };
};
