import { GEOJSON_ASSETS } from '../assets/geojson';
import type { GeoJsonFeatureCollection } from '../types/GeoJson';
import type { Campus } from '../types/Campus';
import type { BuildingShape } from '../types/BuildingShape';
import { getFeaturePolygons, normalizeCampusCode, isPointInAnyPolygon } from '../utils/geoJson';
import { getDistance } from 'geolib';
import { getCampusRegion } from '../constants/campuses';

/**
 * Building metadata properties from building_list.json.
 * Keep this loose (Record<string, unknown>) and pull only fields we need.
 */
type BuildingListProps = Record<string, unknown> & {
  unique_id?: string | number;
  Campus?: string;
  Building?: string; // short code e.g., "MB"
  BuildingName?: string;
  'Building Long Name'?: string;
  Address?: string;
  //TEMP: Sprint 2: used for building details
  Images?: string[]; // Array of image URLs
  Services?: Record<string, string>;
};

/**
 * Building boundary properties from building_boundaries.json.
 */
type BuildingBoundaryProps = Record<string, unknown> & {
  unique_id?: string | number;
  id?: string | number;
};

type BuildingMetadata = {
  campus: Campus;
  name: string;
  shortCode?: string;
  address?: string;
  images: string[];
  services?: Record<string, string>;
};

const toStableId = (raw: unknown): string | null => {
  if (typeof raw === 'string' && raw.trim().length > 0) return raw.trim();
  if (typeof raw === 'number' && Number.isFinite(raw)) return String(raw);
  return null;
};

const getBestBuildingName = (props: BuildingListProps): string => {
  const longName = props['Building Long Name'];
  if (typeof longName === 'string' && longName.trim()) return longName.trim();
  if (typeof props.BuildingName === 'string' && props.BuildingName.trim())
    return props.BuildingName.trim();
  if (typeof props.Building === 'string' && props.Building.trim()) return props.Building.trim();

  return 'Unknown Building';
};

const getPolygonCentroid = (polygon: { latitude: number; longitude: number }[] | undefined) => {
  if (!polygon?.length) return null;
  return {
    latitude: polygon.reduce((sum, p) => sum + p.latitude, 0) / polygon.length,
    longitude: polygon.reduce((sum, p) => sum + p.longitude, 0) / polygon.length,
  };
};

const getBuildingDistance = (
  point: { latitude: number; longitude: number },
  building: BuildingShape,
): number => {
  const centroid = getPolygonCentroid(building.polygons[0]);
  return centroid ? getDistance(point, centroid) : Number.MAX_VALUE;
};

const MAX_DISTANCE_METERS_FROM_CAMPUS = 2000;

const getDistanceToCampusMeters = (
  point: { latitude: number; longitude: number },
  campus: Campus,
) => {
  const campusCenter = getCampusRegion(campus);
  return getDistance(point, {
    latitude: campusCenter.latitude,
    longitude: campusCenter.longitude,
  });
};

const isPointNearCampus = (point: { latitude: number; longitude: number }, campus: Campus) =>
  getDistanceToCampusMeters(point, campus) <= MAX_DISTANCE_METERS_FROM_CAMPUS;

const findContainingBuilding = (
  point: { latitude: number; longitude: number },
  campusBuildings: BuildingShape[],
): BuildingShape | undefined => {
  for (const building of campusBuildings) {
    if (isPointInAnyPolygon(point as any, building.polygons)) {
      return building;
    }
  }

  return undefined;
};

const toBuildingMetadataEntry = (
  props: BuildingListProps,
): { id: string; metadata: BuildingMetadata } | null => {
  const id = toStableId(props.unique_id);
  const campus = normalizeCampusCode(props.Campus);
  if (!id || !campus) return null;

  return {
    id,
    metadata: {
      campus,
      name: getBestBuildingName(props),
      shortCode: typeof props.Building === 'string' ? props.Building : undefined,
      address: typeof props.Address === 'string' ? props.Address : undefined,
      images: props.Images ?? [],
      services: props.Services,
    },
  };
};

const toBuildingShape = (
  feature: GeoJsonFeatureCollection<BuildingBoundaryProps>['features'][number],
  metaById: Map<string, BuildingMetadata>,
): BuildingShape | null => {
  const props = feature.properties ?? {};
  const id = toStableId(props.unique_id);
  if (!id) return null;

  const meta = metaById.get(id);
  if (!meta) return null; // Boundary exists without metadata, skip gracefully

  const polygons = getFeaturePolygons(feature);
  if (!polygons.length) return null; // Invalid geometry, skip gracefully

  return {
    id,
    campus: meta.campus,
    name: meta.name,
    polygons,
    shortCode: meta.shortCode,
    address: meta.address,
    images: meta.images,
    services: meta.services,
  };
};

/**
 * Parse and join datasets once, then reuse results (performance).
 */
let cachedAllBuildings: BuildingShape[] | null = null;

const buildMetadataMap = (
  buildingList: GeoJsonFeatureCollection<BuildingListProps>,
): Map<string, BuildingMetadata> => {
  const metaById = new Map<string, BuildingMetadata>();
  for (const feature of buildingList.features) {
    const props = (feature.properties ?? {}) as BuildingListProps;
    const entry = toBuildingMetadataEntry(props);
    if (!entry) continue;

    metaById.set(entry.id, entry.metadata);
  }
  return metaById;
};

const joinBoundariesToMeta = (
  boundaries: GeoJsonFeatureCollection<BuildingBoundaryProps>,
  metaById: ReturnType<typeof buildMetadataMap>,
): BuildingShape[] => {
  const results: BuildingShape[] = [];
  for (const feature of boundaries.features) {
    const building = toBuildingShape(feature, metaById);
    if (!building) continue;

    results.push(building);
  }
  return results;
};

const buildAllBuildingsCache = (): BuildingShape[] => {
  const buildingList =
    GEOJSON_ASSETS.buildingList as unknown as GeoJsonFeatureCollection<BuildingListProps>;
  const boundaries =
    GEOJSON_ASSETS.buildingBoundaries as unknown as GeoJsonFeatureCollection<BuildingBoundaryProps>;
  return joinBoundariesToMeta(boundaries, buildMetadataMap(buildingList));
};

/**
 * Returns ALL joined building shapes (both campuses).
 * Useful for debugging or future features.
 */
export const getAllBuildingShapes = (): BuildingShape[] => {
  cachedAllBuildings ??= buildAllBuildingsCache();
  return cachedAllBuildings;
};

/**
 * Returns campus-filtered building shapes for rendering.
 */
export const getCampusBuildingShapes = (campus: Campus): BuildingShape[] => {
  return getAllBuildingShapes().filter((building) => building.campus === campus);
};

/**
 * Optional helper: lookup by id for future selection/popup work.
 */
export const getBuildingShapeById = (id: string): BuildingShape | undefined => {
  return getAllBuildingShapes().find((building) => building.id === id);
};

/**
 * Find the first building that contains the given point
 * First determines which campus the user is closest to, then only searches buildings on that campus.
 * Early exits if user is too far from any campus
 * Return undefined if no building has that point
 */
export const findBuildingAt = (point: {
  latitude: number;
  longitude: number;
}): BuildingShape | undefined => {
  const closestCampus = findClosestCampus(point);
  if (!isPointNearCampus(point, closestCampus)) return undefined;
  const campusBuildings = getCampusBuildingShapes(closestCampus);
  return findContainingBuilding(point, campusBuildings);
};

/**
 * Find the closest campus to the user's current location.
 * Returns the Campus that is nearest to the given coordinates.
 */
export const findClosestCampus = (userCoords: { latitude: number; longitude: number }): Campus => {
  const sgwCenter = getCampusRegion('SGW');
  const loyolaCenter = getCampusRegion('LOYOLA');

  const distanceToSGW = getDistance(userCoords, {
    latitude: sgwCenter.latitude,
    longitude: sgwCenter.longitude,
  });

  const distanceToLoyola = getDistance(userCoords, {
    latitude: loyolaCenter.latitude,
    longitude: loyolaCenter.longitude,
  });

  return distanceToSGW <= distanceToLoyola ? 'SGW' : 'LOYOLA';
};

/**
 * Find the nearest building(s) to a given point.
 * Returns an array sorted by distance (closest first).
 * Useful for GPS drift scenarios where user is between buildings.
 * Limit defaults to 5 to avoid overwhelming suggestions.
 */
export const findNearestBuildings = (
  point: { latitude: number; longitude: number },
  limit = 5,
): Array<{ building: BuildingShape; distance: number }> => {
  const closestCampus = findClosestCampus(point);
  const campusBuildings = getCampusBuildingShapes(closestCampus);

  const buildingsWithDistance = campusBuildings.map((building) => ({
    building,
    distance: getBuildingDistance(point, building),
  }));

  const sortedByDistance = buildingsWithDistance.toSorted((a, b) => a.distance - b.distance);

  return sortedByDistance.slice(0, limit);
};
