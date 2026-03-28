import { getAllBuildingShapes, findBuildingAt } from './buildingsRepository';
import { getCurrentLocationResult } from './location';
import type { BuildingShape } from '../types/BuildingShape';
import type { UserLocationCoords } from './location';

type DestinationMatchMethod = 'short_code' | 'name' | 'address';
type ManualStartReason = 'permission_denied' | 'location_unavailable' | 'outside_campus';

type AutomaticStartPoint = {
  type: 'automatic';
  coordinates: UserLocationCoords;
  building: BuildingShape | null;
};

type ManualStartPoint = {
  type: 'manual';
  reason: ManualStartReason;
  coordinates?: UserLocationCoords;
};

export type CalendarRouteStartPoint = AutomaticStartPoint | ManualStartPoint;

export type CalendarRouteLocation = {
  destinationBuilding: BuildingShape;
  startPoint: CalendarRouteStartPoint;
  normalizedEventLocation: string;
  rawEventLocation: string;
};

export type CalendarRouteLocationResolveErrorCode =
  | 'MISSING_EVENT_LOCATION'
  | 'UNRECOGNIZED_EVENT_LOCATION';

export type CalendarRouteLocationResult =
  | { type: 'success'; value: CalendarRouteLocation }
  | { type: 'error'; code: CalendarRouteLocationResolveErrorCode; message: string };

export const CALENDAR_LOCATION_NOT_FOUND_MESSAGE =
  'Unable to find route: Location Not Provided/Not Found';

const PUNCTUATION_PATTERN = /[.,;:()[\]{}]/g;
const WHITESPACE_PATTERN = /\s+/g;
const CAMPUS_PREFIX_PATTERN = /\b(SGW|LOY|LOYOLA)\b/g;
const NON_ALPHANUMERIC_PATTERN = /[^A-Z0-9]/g;
const ROOM_CODE_WITH_SEPARATOR_PATTERN = /\b([A-Z]{1,5})\s*[- ]\s*\d{1,4}[A-Z]?\b/;
const ROOM_CODE_COMPACT_PATTERN = /\b([A-Z]{1,5})\d{1,4}[A-Z]?\b/;

const normalizeText = (value: string) =>
  value
    .trim()
    .toUpperCase()
    .replaceAll(PUNCTUATION_PATTERN, ' ')
    .replaceAll(WHITESPACE_PATTERN, ' ')
    .trim();

const normalizeWithoutCampusPrefix = (value: string) =>
  value.replaceAll(CAMPUS_PREFIX_PATTERN, ' ').replaceAll(WHITESPACE_PATTERN, ' ').trim();

const normalizeCode = (value: string) =>
  value.toUpperCase().replaceAll(NON_ALPHANUMERIC_PATTERN, '');

const codeFromRoomPattern = (location: string): string | null => {
  const separatedCodeMatch = ROOM_CODE_WITH_SEPARATOR_PATTERN.exec(location);
  if (separatedCodeMatch?.[1]) {
    return normalizeCode(separatedCodeMatch[1]);
  }

  // Accept compact classroom formats like "H435" and map to building short code "H".
  const compactCodeMatch = ROOM_CODE_COMPACT_PATTERN.exec(location);
  if (compactCodeMatch?.[1]) {
    return normalizeCode(compactCodeMatch[1]);
  }

  return null;
};

const splitWords = (value: string): string[] =>
  value
    .split(' ')
    .map((part) => part.trim())
    .filter(Boolean);

const isNumericToken = (token: string) => /^\d/.test(token);

const tokenizeForLongNameMatching = (normalizedLocation: string): string[] => {
  const sansCampus = normalizeWithoutCampusPrefix(normalizedLocation);
  return splitWords(sansCampus).filter((token) => !isNumericToken(token));
};

const getBuildingSearchTokens = (normalizedLocation: string): string[] => {
  const sansCampus = normalizeWithoutCampusPrefix(normalizedLocation);
  const tokens = splitWords(sansCampus);
  return tokens;
};

const buildDestinationIndexes = (buildings: BuildingShape[]) => {
  const byShortCode = new Map<string, BuildingShape>();
  const byName = new Map<string, BuildingShape>();
  const byAddress = new Map<string, BuildingShape>();

  for (const building of buildings) {
    if (building.shortCode) {
      byShortCode.set(normalizeCode(building.shortCode), building);
    }

    const normalizedName = normalizeCode(building.name);
    if (normalizedName) byName.set(normalizedName, building);

    const normalizedAddress = normalizeCode(building.address ?? '');
    if (normalizedAddress) byAddress.set(normalizedAddress, building);
  }

  return { byShortCode, byName, byAddress };
};

const scoreLongNameMatch = ({
  locationTokens,
  firstToken,
  buildingCanonicalName,
}: {
  locationTokens: string[];
  firstToken: string;
  buildingCanonicalName: string;
}): number => {
  if (!buildingCanonicalName) return 0;
  if (!buildingCanonicalName.includes(normalizeCode(firstToken))) return 0;

  let score = 0;
  for (const token of locationTokens) {
    const canonicalToken = normalizeCode(token);
    if (!canonicalToken) continue;
    if (buildingCanonicalName.includes(canonicalToken)) {
      score += 1;
    }
  }
  return score;
};

const matchByLongNameHeuristic = (
  normalizedLocation: string,
  indexes: ReturnType<typeof buildDestinationIndexes>,
): { building: BuildingShape; method: DestinationMatchMethod } | null => {
  const locationTokens = tokenizeForLongNameMatching(normalizedLocation);
  const firstToken = locationTokens[0];
  if (!firstToken) return null;

  let bestMatch: { building: BuildingShape; score: number } | null = null;

  for (const [canonicalName, building] of indexes.byName.entries()) {
    const score = scoreLongNameMatch({
      locationTokens,
      firstToken,
      buildingCanonicalName: canonicalName,
    });
    if (score <= 0) continue;

    if (!bestMatch || score > bestMatch.score) {
      bestMatch = { building, score };
    }
  }

  if (!bestMatch) return null;
  return { building: bestMatch.building, method: 'name' };
};

const getShortCodeMatch = (
  code: string,
  indexes: ReturnType<typeof buildDestinationIndexes>,
): { building: BuildingShape; method: DestinationMatchMethod } | null => {
  if (!code) return null;
  const building = indexes.byShortCode.get(code);
  return building ? { building, method: 'short_code' } : null;
};

const matchByAnyShortCodeToken = (
  tokens: string[],
  indexes: ReturnType<typeof buildDestinationIndexes>,
): { building: BuildingShape; method: DestinationMatchMethod } | null => {
  for (const token of tokens) {
    const shortCodeMatch = getShortCodeMatch(normalizeCode(token), indexes);
    if (shortCodeMatch) return shortCodeMatch;
  }

  return null;
};

const matchByCanonicalName = (
  canonicalLocation: string,
  indexes: ReturnType<typeof buildDestinationIndexes>,
): { building: BuildingShape; method: DestinationMatchMethod } | null => {
  if (!canonicalLocation) return null;

  for (const [canonicalName, building] of indexes.byName.entries()) {
    if (canonicalLocation.includes(canonicalName) || canonicalName.includes(canonicalLocation)) {
      return { building, method: 'name' };
    }
  }

  return null;
};

const matchByCanonicalAddress = (
  canonicalLocation: string,
  indexes: ReturnType<typeof buildDestinationIndexes>,
): { building: BuildingShape; method: DestinationMatchMethod } | null => {
  if (!canonicalLocation) return null;

  for (const [canonicalAddress, building] of indexes.byAddress.entries()) {
    if (
      canonicalAddress &&
      (canonicalLocation.includes(canonicalAddress) || canonicalAddress.includes(canonicalLocation))
    ) {
      return { building, method: 'address' };
    }
  }

  return null;
};

const matchDestinationBuilding = (
  normalizedLocation: string,
): { building: BuildingShape; method: DestinationMatchMethod } | null => {
  const indexes = buildDestinationIndexes(getAllBuildingShapes());
  const tokens = getBuildingSearchTokens(normalizedLocation);
  const firstTokenCode = normalizeCode(tokens[0] ?? '');
  const shouldPreferShortCode = firstTokenCode.length > 0 && firstTokenCode.length <= 2;
  const shouldPreferLongName = firstTokenCode.length >= 4;

  const prioritizedMatches: Array<{
    building: BuildingShape;
    method: DestinationMatchMethod;
  } | null> = [
    getShortCodeMatch(codeFromRoomPattern(normalizedLocation) ?? '', indexes),
    shouldPreferShortCode ? getShortCodeMatch(firstTokenCode, indexes) : null,
    shouldPreferLongName ? matchByLongNameHeuristic(normalizedLocation, indexes) : null,
    matchByAnyShortCodeToken(tokens, indexes),
    matchByLongNameHeuristic(normalizedLocation, indexes),
  ];

  for (const match of prioritizedMatches) {
    if (match) return match;
  }

  const canonicalLocation = normalizeCode(normalizedLocation);
  return (
    matchByCanonicalName(canonicalLocation, indexes) ??
    matchByCanonicalAddress(canonicalLocation, indexes)
  );
};

const resolveStartPoint = async (): Promise<CalendarRouteStartPoint> => {
  const currentLocationResult = await getCurrentLocationResult();

  if (currentLocationResult.type === 'permission_denied') {
    return { type: 'manual', reason: 'permission_denied' };
  }
  if (currentLocationResult.type === 'unavailable') {
    return { type: 'manual', reason: 'location_unavailable' };
  }

  const coordinates = currentLocationResult.coords;
  const containingBuilding = findBuildingAt(coordinates) ?? null;

  if (!containingBuilding) {
    return { type: 'manual', reason: 'outside_campus', coordinates };
  }

  return {
    type: 'automatic',
    coordinates,
    building: containingBuilding,
  };
};

export const getManualStartReasonMessage = (reason: ManualStartReason): string => {
  if (reason === 'permission_denied') {
    return 'Location permission required—please select your starting building manually';
  }
  if (reason === 'location_unavailable') {
    return 'Could not generate route—try again';
  }
  return 'Location permission required—please select your starting building manually';
};

export const resolveCalendarRouteLocation = async (
  eventLocation: string | null,
): Promise<CalendarRouteLocationResult> => {
  if (!eventLocation?.trim()) {
    return {
      type: 'error',
      code: 'MISSING_EVENT_LOCATION',
      message: CALENDAR_LOCATION_NOT_FOUND_MESSAGE,
    };
  }

  const rawEventLocation = eventLocation.trim();
  const normalizedEventLocation = normalizeText(rawEventLocation);
  const destinationMatch = matchDestinationBuilding(normalizedEventLocation);

  if (!destinationMatch) {
    return {
      type: 'error',
      code: 'UNRECOGNIZED_EVENT_LOCATION',
      message: CALENDAR_LOCATION_NOT_FOUND_MESSAGE,
    };
  }

  const startPoint = await resolveStartPoint();

  return {
    type: 'success',
    value: {
      destinationBuilding: destinationMatch.building,
      startPoint,
      normalizedEventLocation,
      rawEventLocation,
    },
  };
};
