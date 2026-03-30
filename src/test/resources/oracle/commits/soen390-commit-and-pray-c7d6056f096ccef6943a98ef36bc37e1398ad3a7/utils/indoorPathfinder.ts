import { PriorityQueue } from './priorityQueue';

export interface IndoorNode {
  id: string;
  type: string;
  floor: number;
  x: number;
  y: number;
  label: string;
  buildingId: string;
  accessible: boolean;
}

export interface IndoorEdge {
  source: string;
  target: string;
  type: string;
  weight: number;
  accessible: boolean;
}

export interface FloorData {
  nodes: IndoorNode[];
  edges: IndoorEdge[];
}

export interface PathfindingOptions {
  wheelchairAccessible?: boolean;
  avoidStairs?: boolean;
  preferElevators?: boolean;
}

export class IndoorPathfinder {
  private readonly nodes: Map<string, IndoorNode> = new Map();
  private readonly adjacencyList: Map<string, IndoorEdge[]> = new Map();

  constructor(allFloorData: FloorData[]) {
    // 1. Load all nodes
    allFloorData.forEach((floor) => {
      if (!floor?.nodes) return;
      floor.nodes.forEach((node) => this.nodes.set(node.id, node));
    });

    // 2. Load all JSON edges (Ensure bi-directionality)
    allFloorData.forEach((floor) => {
      if (!floor?.edges) return;
      floor.edges.forEach((edge) => {
        this.addEdge(edge);
        this.addEdge({
          source: edge.target,
          target: edge.source,
          weight: edge.weight,
          type: edge.type,
          accessible: edge.accessible,
        });
      });
    });
  }

  private addEdge(edge: IndoorEdge) {
    if (!this.adjacencyList.has(edge.source)) {
      this.adjacencyList.set(edge.source, []);
    }
    this.adjacencyList.get(edge.source)?.push(edge);
  }

  private resolveNode(reference: string): IndoorNode | null {
    const ref = reference.trim();
    if (!ref) return null;

    const byId = this.nodes.get(ref);
    if (byId) return byId;

    const byLabel = Array.from(this.nodes.values()).find((n) => n.label.trim() === ref);
    return byLabel ?? null;
  }

  private getEdgeWeight(
    edge: IndoorEdge,
    wheelchairAccessible: boolean,
    avoidStairs: boolean,
    preferElevators: boolean,
    sourceNode: IndoorNode,
    targetNode: IndoorNode,
    routeStartsAndEndsSameFloor: boolean,
    targetFloor: number
  ): number {
    const edgeType = edge.type.toLowerCase();
    const isStair = edgeType.includes('stair');
    const isElevator = edgeType.includes('elevator');
    const isFloorTransition = sourceNode.floor !== targetNode.floor;

    if (wheelchairAccessible && !edge.accessible) {
      return Infinity;
    }

    if (avoidStairs && isStair) {
      return Infinity;
    }

    if (preferElevators) {
      if (isStair) {
        return Infinity;
      }

      // Prefer elevator transitions over other vertical alternatives.
      if (isElevator) {
        return edge.weight * 0.5;
      }
    }

    // Strongly discourage pointless vertical travel when both endpoints are on the same floor.
    if (routeStartsAndEndsSameFloor && isFloorTransition) {
      return edge.weight + 10000;
    }

    // Prefer transitions that move toward the destination floor over those that move away.
    if (!routeStartsAndEndsSameFloor && isFloorTransition) {
      const currentDistanceToTarget = Math.abs(sourceNode.floor - targetFloor);
      const nextDistanceToTarget = Math.abs(targetNode.floor - targetFloor);
      if (nextDistanceToTarget > currentDistanceToTarget) {
        return edge.weight + 200;
      }
    }

    return edge.weight;
  }

  public findShortestPath(
    startReference: string,
    endReference: string,
    options: PathfindingOptions = {}
  ): IndoorNode[] | null {
    const wheelchairAccessible = options.wheelchairAccessible ?? true;
    const avoidStairs = options.avoidStairs ?? true;
    const preferElevators = options.preferElevators ?? true;
    const startNode = this.resolveNode(startReference);
    if (!startNode) return null;

    const endNodeCandidate = this.resolveNode(endReference);
    if (!endNodeCandidate) return null;

    const endNode = endNodeCandidate.buildingId === startNode.buildingId
      ? endNodeCandidate
      : null;
    if (!endNode) return null;

    const routeStartsAndEndsSameFloor = startNode.floor === endNode.floor;
    const targetFloor = endNode.floor;

    const distances: Record<string, number> = {};
    const previous: Record<string, string | null> = {};
    const pq = new PriorityQueue<string>();

    this.nodes.forEach((_, id) => {
      distances[id] = Infinity;
      previous[id] = null;
    });

    distances[startNode.id] = 0;
    pq.enqueue(startNode.id, 0);

    while (!pq.isEmpty()) {
      const currentId = pq.dequeue()!;
      if (currentId === endNode.id) break;

      const neighbors = this.adjacencyList.get(currentId) || [];
      for (const edge of neighbors) {
        const sourceNode = this.nodes.get(edge.source);
        const targetNode = this.nodes.get(edge.target);
        if (!sourceNode || !targetNode) continue;

        const weight = this.getEdgeWeight(
          edge,
          wheelchairAccessible,
          avoidStairs,
          preferElevators,
          sourceNode,
          targetNode,
          routeStartsAndEndsSameFloor,
          targetFloor
        );
        if (weight === Infinity) continue;

        const alt = distances[currentId] + weight;
        if (alt < distances[edge.target]) {
          distances[edge.target] = alt;
          previous[edge.target] = currentId;
          pq.enqueue(edge.target, alt);
        }
      }
    }

    const path = this.reconstructPath(previous, endNode.id);
    return path.length > 1 ? path : null;
  }

  private reconstructPath(previous: Record<string, string | null>, endId: string): IndoorNode[] {
    const path: IndoorNode[] = [];
    let curr: string | null = endId;
    while (curr !== null) {
      const node = this.nodes.get(curr);
      if (node) path.unshift(node);
      curr = previous[curr];
    }
    return path;
  }
}
