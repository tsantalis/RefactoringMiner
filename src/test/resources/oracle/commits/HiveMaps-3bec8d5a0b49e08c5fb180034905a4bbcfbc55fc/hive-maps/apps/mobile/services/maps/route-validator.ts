import {buildings, campuses, type CampusId} from '@/constants/campus';

export type RouteEndpoint = 
    | {type: 'building'; code: string}
    | {type: 'coordinate'; longitude: number; latitude: number};

export interface CampusRouteRequest {
    origin: RouteEndpoint;
    destination: RouteEndpoint;
}

export interface ValidatedCampusRoute {
    originCampus: CampusId;
    destinationCampus: CampusId;
    isInterCampus: boolean;
    originCode?: string;
    destinationCode?: string;
}

export type ValidationError = 
    | 'UNKNOWN_ORIGIN' | 'UNKNOWN_DESTINATION' | 'SAME_ORIGIN_AND_DESTINATION' | 'COORDINATE_OUT_OF_BOUNDS';

export type ValidationResult = 
    | {valid: true; route: ValidatedCampusRoute}
    | {valid: false; error: ValidationError; message: string};

const CAMPUS_RADIUS_KM = 0.8;

export function getCampusForBuilding(code: string): CampusId | null {
    const building = buildings.find(
        (b) => b.code.toUpperCase() === code.toUpperCase(),
    );
    return building?.campus ?? null;
}

export function haversineKM (
    lon1: number, lat1: number, lon2: number, lat2: number
): number {
    const toRad = (deg: number) => (deg * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
    return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function getNearestCampus(
    longitude: number,
    latitude: number,
): CampusId | null {
    let best: {id: CampusId; dist: number} | null = null;
    for (const campus of Object.values(campuses)) {
        const dist = haversineKM(longitude, latitude, campus.center[0], campus.center[1]);
        if (dist <= CAMPUS_RADIUS_KM && (!best || dist < best.dist)) {
            best = {id: campus.id, dist};
        }
    }
    return best?.id ?? null;
}

export function validateCampusRoute(
    request: CampusRouteRequest,
): ValidationResult {
    const {origin, destination} = request;

    let originCampus: CampusId | null;
    let originCode: string;
    if (origin.type ==='building') {
        originCode = origin.code.toUpperCase();
        originCampus = getCampusForBuilding(originCode);
        if (!originCampus) {
            return {valid: false, error: 'UNKNOWN_ORIGIN', message: `Building "${originCode}" not found.`};
        }
    } else {
        originCampus = getNearestCampus(origin.longitude, origin.latitude);
        originCode = 'USER_LOCATION';
        if (!originCampus) {
            return {valid: false, error: 'COORDINATE_OUT_OF_BOUNDS', message: `Origin coordinates are not within ${CAMPUS_RADIUS_KM}km of any campus.`};
        }
    }

    let destinationCampus: CampusId | null;
    let destinationCode: string;
    if (destination.type === 'building') {
        destinationCode = destination.code.toUpperCase();
        destinationCampus = getCampusForBuilding(destinationCode);
        if (!destinationCampus) {
            return {valid: false, error: 'UNKNOWN_DESTINATION', message: `Building "${destinationCode}" not found.`};
        }
    } else {
        destinationCampus = getNearestCampus(destination.longitude, destination.latitude);
        destinationCode = 'USER_LOCATION';
        if (!destinationCampus) {
            return {valid: false, error: 'COORDINATE_OUT_OF_BOUNDS', message: `Destination coordinates are not within ${CAMPUS_RADIUS_KM}km of any campus.`};
        }
    }

    if (origin.type === 'building' && destination.type === 'building' && originCode === destinationCode) {
        return {valid: false, error: 'SAME_ORIGIN_AND_DESTINATION', message: 'Origin and destination buildings cannot be the same.'};
    }

    if (origin.type === 'coordinate' && destination.type === 'coordinate'
        && haversineKM(origin.longitude, origin.latitude, destination.longitude, destination.latitude) < 0.01) {
        return {valid: false, error: 'SAME_ORIGIN_AND_DESTINATION', message: 'Origin and destination cannot be the same location.'};
    }

    return {
        valid: true,
        route: {
            originCampus,
            destinationCampus,
            isInterCampus: originCampus !== destinationCampus,
            originCode,
            destinationCode,
        },
    };
}