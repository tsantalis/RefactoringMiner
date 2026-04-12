import { LocalizedNode, RawEdge } from "../../../services/floorPlanService";

 // Euclidean distance heuristic between two nodes (in floor plan pixels).
 // (edge weights represent pixel distances, so that straight-line distance never overestimates the true shortest path)
 
function heuristic(a: LocalizedNode, b: LocalizedNode): number {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

function reconstructPath(
  cameFrom: Map<string, string>,
  endId: string,
  nodeMap: Map<string, LocalizedNode>
): LocalizedNode[] {
  const path: LocalizedNode[] = [];
  let cursor: string | undefined = endId;
  while (cursor !== undefined) {
    const node = nodeMap.get(cursor);
    if (node) path.unshift(node);
    cursor = cameFrom.get(cursor);
  }
  return path;
}

function buildAdjacency(
  nodes: LocalizedNode[],
  edges: RawEdge[]
): Map<string, { id: string; weight: number }[]> {
  const adjacency = new Map<string, { id: string; weight: number }[]>(
    nodes.map((n) => [n.id, []])
  );

  for (const edge of edges) {
    adjacency.get(edge.source)?.push({ id: edge.target, weight: edge.weight });
    adjacency.get(edge.target)?.push({ id: edge.source, weight: edge.weight });
  }

  return adjacency;
}

function getLowestFScoreNode(
  openSet: Set<string>,
  fScore: Map<string, number>
): string {
  let bestNode = "";
  let lowest = Infinity;

  for (const id of openSet) {
    const score = fScore.get(id) ?? Infinity;
    if (score < lowest) {
      lowest = score;
      bestNode = id;
    }
  }

  return bestNode;
}

function updateNeighbor(
  current: string,
  neighbor: { id: string; weight: number },
  endNode: LocalizedNode,
  nodeMap: Map<string, LocalizedNode>,
  gScore: Map<string, number>,
  fScore: Map<string, number>,
  cameFrom: Map<string, string>,
  openSet: Set<string>
) {
  const tentativeG = (gScore.get(current) ?? Infinity) + neighbor.weight;

  if (tentativeG >= (gScore.get(neighbor.id) ?? Infinity)) return;

  cameFrom.set(neighbor.id, current);
  gScore.set(neighbor.id, tentativeG);

  const neighborNode = nodeMap.get(neighbor.id);
  if (!neighborNode) return;

  fScore.set(
    neighbor.id,
    tentativeG + heuristic(neighborNode, endNode)
  );

  openSet.add(neighbor.id);
}

// A* pathfinding on the indoor flat floor graph (only same-floor navigation for now).
export function findPath(
  startId: string,
  endId: string,
  nodes: LocalizedNode[],
  edges: RawEdge[]
): LocalizedNode[] {
  const nodeMap = new Map(nodes.map((n) => [n.id, n]));

  const startNode = nodeMap.get(startId);
  const endNode = nodeMap.get(endId);
  if (!startNode || !endNode) return [];
  if (startId === endId) return [startNode];

  const adjacency = buildAdjacency(nodes, edges);

  const openSet = new Set<string>([startId]);
  const closedSet = new Set<string>();

  const gScore = new Map<string, number>([[startId, 0]]);
  const fScore = new Map<string, number>([
    [startId, heuristic(startNode, endNode)],
  ]);
  const cameFrom = new Map<string, string>();

  while (openSet.size > 0) {
    const current = getLowestFScoreNode(openSet, fScore);

    if (current === endId) {
      return reconstructPath(cameFrom, endId, nodeMap);
    }

    openSet.delete(current);
    closedSet.add(current);

    for (const neighbor of adjacency.get(current) ?? []) {
      if (closedSet.has(neighbor.id)) continue;
      updateNeighbor(                            
        current,
        neighbor,
        endNode,
        nodeMap,
        gScore,
        fScore,
        cameFrom,
        openSet
      );
    }
  }

  return [];
}