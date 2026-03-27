import {getApiBaseUrl} from './campus-api';

export type BuildingCode = string;
export type FloorId = string;
export type IndoorCampusId = string;
export type DirectionType = 'STRAIGHT' | 'LEFT' | 'RIGHT' | 'BACK' | 'UP_OR_DOWN' | 'DEFAULT';

export interface FloorSummary {
    id: FloorId;
    label: string;
    sortOrder: number;
}

export interface FloorDetailsResponse {
    buildingCode: BuildingCode;
    floor: { id: FloorId; label: string };
    planGeometry: GeoJSON.Geometry;
    rooms: GeoJSON.FeatureCollection;
}

export interface SupportedIndoorBuilding {
    campusId: IndoorCampusId;
    buildingCode: BuildingCode;
}

export interface IndoorNodeResponse {
    id: string;
    label: string;
    wheelchairAccessible: boolean;
    floor: string;
    building: string;
    longitude: number;
    latitude: number;
}

export interface IndoorDirectionsResponse {
    direction: DirectionType;
    distance: number;
    description: string;
    nodes: IndoorNodeResponse[];
}

const REQUEST_TIMEOUT_MS = 10000;
const baseUrl = getApiBaseUrl();

type HttpStatusError = Error & { status?: number };

export function normalizeIndoorBuildingCode(value: string | undefined | null): BuildingCode | null {
    if (!value) return null;
    const normalized = value.trim().toUpperCase();
    return normalized.length > 0 ? normalized : null;
}

function getHttpStatus(error: unknown): number | null {
    if (typeof error !== 'object' || error === null || !('status' in error)) return null;
    const status = (error as HttpStatusError).status;
    return typeof status === 'number' ? status : null;
}

async function getIndoorJson<T>(path: string): Promise<T> {
    const url = `${baseUrl}${path}`;
    const controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
    const timeout = controller ? setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS) : null;

    try {
        const response = await fetch(url, controller ? {signal: controller.signal} : undefined);
        if (!response.ok) {
            const error = new Error(`Indoor API request failed (${response.status})`) as HttpStatusError;
            error.status = response.status;
            throw error;
        }
        return (await response.json()) as T;
    } finally {
        if (timeout) clearTimeout(timeout);
    }
}

export async function fetchSupportedIndoorBuildings(): Promise<SupportedIndoorBuilding[]> {
    const buildings = await getIndoorJson<SupportedIndoorBuilding[]>('/api/indoor/buildings');
    return buildings.map((building) => ({
        campusId: building.campusId.toUpperCase(),
        buildingCode: building.buildingCode.toUpperCase(),
    }));
}

export async function fetchBuildingFloors(campusId: IndoorCampusId, buildingCode: BuildingCode): Promise<FloorSummary[]> {
    const path = `/api/campuses/${encodeURIComponent(campusId)}/buildings/${encodeURIComponent(buildingCode)}/floors`;
    const floors = await getIndoorJson<FloorSummary[]>(path);
    return [...floors].sort((a, b) => a.sortOrder - b.sortOrder);
}

export async function fetchFloorDetails(
    campusId: IndoorCampusId,
    buildingCode: BuildingCode,
    floorId: FloorId,
): Promise<FloorDetailsResponse | null> {
    const path = `/api/campuses/${encodeURIComponent(campusId)}/buildings/${encodeURIComponent(buildingCode)}/floors/${encodeURIComponent(floorId)}`;

    try {
        return await getIndoorJson<FloorDetailsResponse>(path);
    } catch (error) {
        if (getHttpStatus(error) === 404) return null;
        throw error;
    }
}

export async function fetchNearestNode(
    buildingCode: BuildingCode,
    floorId: FloorId,
    longitude: number,
    latitude: number,
): Promise<IndoorNodeResponse> {
    const path = `/api/indoor-directions/building/${encodeURIComponent(buildingCode)}/floor/${encodeURIComponent(floorId)}/nearest-node?longitude=${longitude}&latitude=${latitude}`;

    try {
        return await getIndoorJson<IndoorNodeResponse>(path);
    } catch (error) {
        if (getHttpStatus(error) === 404) {
            throw new Error(`No nodes found on ${buildingCode} floor ${floorId} within 20 meters`);
        }
        throw error;
    }
}

export async function fetchIndoorRooms(
    buildingCode: BuildingCode,
    floorId: FloorId,
): Promise<IndoorNodeResponse[]>{
    const path = `/api/indoor-directions/building/${encodeURIComponent(buildingCode)}/rooms?floor=${encodeURIComponent(floorId)}`;
    return getIndoorJson<IndoorNodeResponse[]>(path);
}

export async function fetchIndoorDirections(
    buildingCode: BuildingCode,
    startNodeId: string,
    endNodeId: string,
    accessible = false,
): Promise<IndoorDirectionsResponse[]>{
    const path = `/api/indoor-directions/building/${encodeURIComponent(buildingCode)}/from/${encodeURIComponent(startNodeId)}/to/${encodeURIComponent(endNodeId)}?accessible=${accessible}`;
    return getIndoorJson<IndoorDirectionsResponse[]>(path);
}
