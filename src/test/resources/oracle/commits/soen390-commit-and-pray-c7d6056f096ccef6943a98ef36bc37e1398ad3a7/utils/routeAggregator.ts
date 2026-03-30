import { parseBuildingLocation } from "./buildingParser";
import { getBuildingCoordinate } from "./buildingCoordinates";
import { IndoorPathfinder } from "./indoorPathfinder";
import { AllCampusData } from "../data/buildings";
import { getDistanceMeters, isValidCoordinate, type LatLng } from "./geometry";

export type CombinedNavigationStep = {
  instruction: string;
  distance: string; 
  source: "indoor" | "outdoor";
  duration?: string;
  nodeId?: string;
  nodeLabel?: string;
  startNodeId?: string;
  startNodeLabel?: string;
  endNodeId?: string;
  endNodeLabel?: string;
  buildingCode?: string;
  floor?: number;
  coordinates: { latitude: number; longitude: number };
  maneuver?: string;
};

const pathfinder = new IndoorPathfinder(AllCampusData);

type Coordinates = LatLng;

function pickCoordinate(
  primary: Partial<Coordinates> | null | undefined,
  fallback: Coordinates
): Coordinates {
  return isValidCoordinate(primary) ? { latitude: primary.latitude, longitude: primary.longitude } : fallback;
}

function inferFloorFromRoom(room: string): number | null {
  const normalized = room.trim().toUpperCase();
  if (!normalized) return null;

  // Basement notation like S2.134 or S2
  const basementMatch = /^S\s*([0-9]+)/.exec(normalized);
  if (basementMatch) {
    return -Number(basementMatch[1]);
  }

  // Dot notation like 1.294 or 8.120
  const dotFloorMatch = /^([0-9]+)\./.exec(normalized);
  if (dotFloorMatch) {
    return Number(dotFloorMatch[1]);
  }

  // Plain room like 820 -> usually floor 8
  const plainMatch = /^([0-9]{2,4})$/.exec(normalized);
  if (plainMatch) {
    return Number(plainMatch[1][0]);
  }

  return null;
}

function findBestEntryNode(
  buildingCode: string,
  referencePoint: { latitude: number; longitude: number },
  transportMode: string,
  targetFloor?: number | null
) {
  const buildingFloors = AllCampusData.filter(f => f.meta.buildingId === buildingCode);
  const allEntries = buildingFloors
    .flatMap(f => f.nodes.filter(n => n.type === 'building_entry'))
    .filter((n) => isValidCoordinate({ latitude: n.latitude, longitude: n.longitude }));

  if (allEntries.length === 0) return null;

  if (buildingCode === 'MB' && transportMode === 'TRANSIT' && (targetFloor == null || targetFloor === -2)) {
    const s2Entry = allEntries.find(n => n.floor === -2);
    if (s2Entry) return s2Entry;
  }

  if (targetFloor != null) {
    const sameFloorEntries = allEntries.filter((entry) => entry.floor === targetFloor);
    if (sameFloorEntries.length > 0) {
      return sameFloorEntries.sort((a, b) => {
        const distA = getDistanceMeters(referencePoint.latitude, referencePoint.longitude, a.latitude!, a.longitude!);
        const distB = getDistanceMeters(referencePoint.latitude, referencePoint.longitude, b.latitude!, b.longitude!);
        return distA - distB;
      })[0];
    }
  }

  return allEntries.sort((a, b) => {
    const distA = getDistanceMeters(referencePoint.latitude, referencePoint.longitude, a.latitude!, a.longitude!);
    const distB = getDistanceMeters(referencePoint.latitude, referencePoint.longitude, b.latitude!, b.longitude!);
    return distA - distB;
  })[0];
}

export async function getStitchedRoute(
  originRaw: string,
  destinationRaw: string,
  isAccessible: boolean,
  transportMode: string,
  userLocation: { latitude: number; longitude: number },
  fetchOutdoorSteps: (s: any, e: any) => Promise<any[]>
): Promise<CombinedNavigationStep[]> {
  const origin = parseBuildingLocation(originRaw);
  const dest = parseBuildingLocation(destinationRaw);
  if (!dest) return [];

  const route: CombinedNavigationStep[] = [];
  let outdoorStart = userLocation;

  // Leg 1: Indoor Exit
  if (origin?.room) {
    const bestExit = findBestEntryNode(origin.buildingCode, userLocation, transportMode);
    if (bestExit) {
      const indoorPath = pathfinder.findShortestPath(origin.room, bestExit.id, {
        wheelchairAccessible: isAccessible,
        avoidStairs: isAccessible,
        preferElevators: isAccessible,
      });
      if (indoorPath?.length) {
        const startNode = indoorPath[0];
        const endNode = indoorPath[indoorPath.length - 1];
        const indoorExitCoordinate = pickCoordinate(
          { latitude: endNode.latitude, longitude: endNode.longitude },
          { latitude: bestExit.latitude!, longitude: bestExit.longitude! }
        );

        route.push({
          instruction: `Exit ${origin.buildingCode} via ${bestExit.label}`,
          distance: `${Math.max(1, indoorPath.length - 1) * 5}m`,
          source: "indoor",
          buildingCode: endNode.buildingId,
          floor: endNode.floor,
          nodeId: endNode.id,
          nodeLabel: endNode.label,
          startNodeId: startNode.id,
          startNodeLabel: startNode.label,
          endNodeId: endNode.id,
          endNodeLabel: endNode.label,
          coordinates: indoorExitCoordinate,
        });
        outdoorStart = indoorExitCoordinate;
      }
    }
  }

  // Leg 2: Outdoor
  let outdoorEnd = getBuildingCoordinate(dest.buildingCode) || outdoorStart;
  const targetFloor = dest.room ? inferFloorFromRoom(dest.room) : null;
  const bestEntry = findBestEntryNode(dest.buildingCode, outdoorStart, transportMode, targetFloor);
  if (bestEntry) {
    outdoorEnd = pickCoordinate(
      { latitude: bestEntry.latitude, longitude: bestEntry.longitude },
      outdoorEnd
    );
  }

  const outdoorSteps = await fetchOutdoorSteps(outdoorStart, {
    ...outdoorEnd
  });

  outdoorSteps.forEach(s => route.push({ ...s, source: "outdoor" }));

  // Leg 3: Indoor Arrival
  if (dest.room && bestEntry) {
    const arrivalPath = pathfinder.findShortestPath(bestEntry.id, dest.room, {
      wheelchairAccessible: isAccessible,
      avoidStairs: isAccessible,
      preferElevators: isAccessible,
    });
    if (arrivalPath?.length) {
      const startNode = arrivalPath[0];
      const endNode = arrivalPath[arrivalPath.length - 1];
      const handoffCoordinate = pickCoordinate(
        { latitude: startNode.latitude, longitude: startNode.longitude },
        pickCoordinate(
          { latitude: endNode.latitude, longitude: endNode.longitude },
          outdoorEnd
        )
      );

      route.push({
        instruction: `Head to ${dest.room}`,
        distance: `${Math.max(1, arrivalPath.length - 1) * 5}m`,
        source: "indoor",
        buildingCode: endNode.buildingId,
        floor: endNode.floor,
        nodeId: endNode.id,
        nodeLabel: endNode.label,
        startNodeId: startNode.id,
        startNodeLabel: startNode.label,
        endNodeId: endNode.id,
        endNodeLabel: endNode.label,
        coordinates: handoffCoordinate,
      });
    }
  }

  return route;
}