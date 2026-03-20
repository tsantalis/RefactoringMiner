import type { LatLng } from 'react-native-maps';
import {
  DirectionsRequest,
  DirectionsRoute,
  DirectionsServiceError,
  DirectionsTravelMode,
  TransitInstruction,
} from '../../types/Directions';
import type {
  GoogleDirectionsLeg,
  GoogleDirectionsResponse,
  GoogleDirectionsStatus,
  GoogleDirectionsStep,
} from '../../types/GoogleDirections';
import { formatDistance, formatDuration } from '../../utils/directionsFormatting';

const DIRECTIONS_API_URL = 'https://maps.googleapis.com/maps/api/directions/json';

type FetchRouteOptions = {
  apiKey: string;
  mode: DirectionsTravelMode;
  fetchImpl?: typeof fetch;
};

type FetchTransitRouteOptions = {
  apiKey: string;
  fetchImpl?: typeof fetch;
};

const isFiniteCoordinate = (point: LatLng) =>
  Number.isFinite(point.latitude) && Number.isFinite(point.longitude);

const mapTravelMode = (mode: DirectionsTravelMode) => {
  switch (mode) {
    case 'walking':
      return 'walking';
    case 'driving':
      return 'driving';
    case 'transit':
      return 'transit';
  }
};

const mapStatusToErrorCode = (status: GoogleDirectionsStatus) => {
  switch (status) {
    case 'ZERO_RESULTS':
      return 'NO_ROUTE' as const;
    case 'REQUEST_DENIED':
      return 'REQUEST_DENIED' as const;
    case 'OVER_QUERY_LIMIT':
    case 'OVER_DAILY_LIMIT':
      return 'OVER_QUERY_LIMIT' as const;
    case 'INVALID_REQUEST':
    case 'MAX_ROUTE_LENGTH_EXCEEDED':
    case 'MAX_WAYPOINTS_EXCEEDED':
    case 'NOT_FOUND':
      return 'INVALID_REQUEST' as const;
    case 'UNKNOWN_ERROR':
    default:
      return 'API_ERROR' as const;
  }
};

const decodeHtmlEntities = (raw: string) =>
  raw
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');

const stripHtmlTags = (raw: string) => {
  let output = '';
  let inTag = false;
  let pendingTag = '';

  for (const char of raw) {
    if (!inTag) {
      if (char === '<') {
        inTag = true;
        pendingTag = '<';
      } else {
        output += char;
      }
      continue;
    }

    pendingTag += char;
    if (char === '>') {
      if (output.length > 0 && output[output.length - 1] !== ' ') {
        output += ' ';
      }
      inTag = false;
      pendingTag = '';
    }
  }

  // Keep unmatched '<' content as text to mirror regex behavior.
  if (pendingTag) {
    output += pendingTag;
  }

  return output;
};

const collapseWhitespace = (raw: string) => {
  let output = '';
  let previousWasWhitespace = false;

  for (const char of raw) {
    const isWhitespace =
      char === ' ' ||
      char === '\n' ||
      char === '\r' ||
      char === '\t' ||
      char === '\f' ||
      char === '\v';
    if (isWhitespace) {
      if (!previousWasWhitespace) {
        output += ' ';
      }
      previousWasWhitespace = true;
      continue;
    }
    output += char;
    previousWasWhitespace = false;
  }

  return output.trim();
};

const stripHtml = (raw?: string) => {
  if (!raw) return '';
  return collapseWhitespace(stripHtmlTags(decodeHtmlEntities(raw)));
};

const toTransitVehicleLabel = (vehicleName?: string) => {
  if (!vehicleName) return null;
  const normalized = vehicleName.trim().toLowerCase();
  if (!normalized) return null;
  if (normalized === 'subway') return 'metro';
  return normalized;
};

const toWalkingInstruction = (
  step: GoogleDirectionsStep,
  legIndex: number,
  stepIndex: number,
): TransitInstruction => {
  const detailParts: string[] = [];
  if (step.distance?.text) detailParts.push(step.distance.text);
  if (step.duration?.text) detailParts.push(`about ${step.duration.text}`);

  return {
    id: `walk-${legIndex}-${stepIndex}`,
    type: 'walk',
    title: stripHtml(step.html_instructions) || 'Walk',
    detail: detailParts.length > 0 ? detailParts.join(', ') : null,
  };
};

const toTransitInstruction = (
  step: GoogleDirectionsStep,
  legIndex: number,
  stepIndex: number,
): TransitInstruction => {
  const details = step.transit_details;
  const line = details?.line;
  const lineShortName = line?.short_name?.trim() ?? null;
  const lineName = line?.name?.trim() ?? null;
  const vehicleLabel = toTransitVehicleLabel(line?.vehicle?.name);

  const detailParts: string[] = [];
  if (typeof details?.num_stops === 'number') {
    detailParts.push(details.num_stops === 1 ? 'Ride 1 stop' : `Ride ${details.num_stops} stops`);
  }
  if (step.duration?.text) {
    detailParts.push(step.duration.text);
  }

  let title = 'Board transit';
  if (lineShortName) {
    title = vehicleLabel
      ? `Board the ${lineShortName} ${vehicleLabel}`
      : `Board the ${lineShortName}`;
  } else if (lineName) {
    title = `Board ${lineName}`;
  } else if (vehicleLabel) {
    title = `Board ${vehicleLabel}`;
  }

  return {
    id: `transit-${legIndex}-${stepIndex}`,
    type: 'transit',
    title,
    subtitle: details?.headsign ? `Toward ${details.headsign}` : null,
    detail: detailParts.length > 0 ? detailParts.join(', ') : null,
    departureTimeText: details?.departure_time?.text ?? null,
    arrivalTimeText: details?.arrival_time?.text ?? null,
    departureStopName: details?.departure_stop?.name ?? null,
    arrivalStopName: details?.arrival_stop?.name ?? null,
    lineShortName,
    lineColor: line?.color ?? null,
    lineTextColor: line?.text_color ?? null,
    vehicleType: line?.vehicle?.type ?? null,
  };
};

const extractTransitInstructions = (
  steps: GoogleDirectionsStep[] | undefined,
  legIndex: number,
): TransitInstruction[] => {
  if (!steps || steps.length === 0) return [];

  return steps.reduce<TransitInstruction[]>((instructions, step, stepIndex) => {
    if (step.travel_mode === 'WALKING') {
      instructions.push(toWalkingInstruction(step, legIndex, stepIndex));
      return instructions;
    }

    if (step.travel_mode === 'TRANSIT' && step.transit_details) {
      instructions.push(toTransitInstruction(step, legIndex, stepIndex));
      return instructions;
    }

    return instructions;
  }, []);
};

const toQueryString = (query: Record<string, string | number>) =>
  Object.entries(query)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&');

const toLatLngParam = ({ latitude, longitude }: LatLng) => `${latitude},${longitude}`;

export const buildDirectionsApiUrl = (
  request: DirectionsRequest,
  apiKey: string,
  mode: DirectionsTravelMode = 'walking',
): string => {
  const query: Record<string, string | number> = {
    origin: toLatLngParam(request.origin),
    destination: toLatLngParam(request.destination),
    mode: mapTravelMode(mode),
    units: request.units ?? 'metric',
    key: apiKey,
  };

  if (request.language) {
    query.language = request.language;
  }

  if (request.departureTime) {
    query.departure_time = request.departureTime;
  }

  return `${DIRECTIONS_API_URL}?${toQueryString(query)}`;
};

type ParsedDirectionsCoreResult = {
  route: DirectionsRoute;
  legs: GoogleDirectionsLeg[];
};

const resolveApiKey = (apiKey: string) => {
  const trimmedApiKey = apiKey.trim();
  if (!trimmedApiKey) {
    throw new DirectionsServiceError(
      'MISSING_API_KEY',
      'Google Directions API key is missing. Set EXPO_PUBLIC_GOOGLE_MAPS_API_KEY.',
    );
  }
  return trimmedApiKey;
};

const validateDirectionsRequest = (request: DirectionsRequest) => {
  if (!isFiniteCoordinate(request.origin) || !isFiniteCoordinate(request.destination)) {
    throw new DirectionsServiceError('INVALID_COORDINATES', 'Origin or destination is invalid.');
  }
};

const requestDirectionsJson = async (
  url: string,
  fetchImpl?: typeof fetch,
): Promise<GoogleDirectionsResponse> => {
  const directionsFetch = fetchImpl ?? fetch;

  let response: Response;
  try {
    response = await directionsFetch(url);
  } catch (error) {
    throw new DirectionsServiceError(
      'NETWORK_ERROR',
      'Unable to reach Google Directions API. Check internet connectivity.',
      {
        providerMessage: error instanceof Error ? error.message : 'Unknown network error',
      },
    );
  }

  if (!response.ok) {
    throw new DirectionsServiceError(
      'API_ERROR',
      `Google Directions API request failed with HTTP ${response.status}.`,
      { providerMessage: response.statusText },
    );
  }

  return (await response.json()) as GoogleDirectionsResponse;
};

const parseDirectionsRoute = (
  data: GoogleDirectionsResponse,
  units: DirectionsRequest['units'] = 'metric',
): ParsedDirectionsCoreResult => {
  if (data.status !== 'OK') {
    const code = mapStatusToErrorCode(data.status);
    throw new DirectionsServiceError(code, data.error_message ?? `Directions request failed.`, {
      providerStatus: data.status,
      providerMessage: data.error_message,
    });
  }

  const route = data.routes?.[0];
  if (!route || !route.overview_polyline?.points) {
    throw new DirectionsServiceError('NO_ROUTE', 'No valid outdoor route was returned.');
  }

  const legs = route.legs ?? [];
  const distanceMeters = legs.reduce((sum, leg) => sum + (leg.distance?.value ?? 0), 0);
  const durationSeconds = legs.reduce((sum, leg) => sum + (leg.duration?.value ?? 0), 0);
  const distanceText = legs[0]?.distance?.text ?? formatDistance(distanceMeters, units);
  const durationText = legs[0]?.duration?.text ?? formatDuration(durationSeconds);

  const bounds =
    route.bounds?.northeast && route.bounds?.southwest
      ? {
          northeast: {
            latitude: route.bounds.northeast.lat,
            longitude: route.bounds.northeast.lng,
          },
          southwest: {
            latitude: route.bounds.southwest.lat,
            longitude: route.bounds.southwest.lng,
          },
        }
      : null;

  return {
    route: {
      polyline: route.overview_polyline.points,
      distanceMeters,
      distanceText,
      durationSeconds,
      durationText,
      bounds,
    },
    legs,
  };
};

const fetchGoogleDirectionsCore = async (
  request: DirectionsRequest,
  { apiKey, mode, fetchImpl }: FetchRouteOptions,
): Promise<ParsedDirectionsCoreResult> => {
  const trimmedApiKey = resolveApiKey(apiKey);
  validateDirectionsRequest(request);
  const url = buildDirectionsApiUrl(request, trimmedApiKey, mode);
  const data = await requestDirectionsJson(url, fetchImpl);
  return parseDirectionsRoute(data, request.units ?? 'metric');
};

export const fetchGoogleDirectionsRoute = async (
  request: DirectionsRequest,
  options: FetchRouteOptions,
): Promise<DirectionsRoute> => {
  const result = await fetchGoogleDirectionsCore(request, options);
  return result.route;
};

export const fetchGoogleTransitRoute = async (
  request: DirectionsRequest,
  { apiKey, fetchImpl }: FetchTransitRouteOptions,
): Promise<DirectionsRoute> => {
  const result = await fetchGoogleDirectionsCore(request, {
    apiKey,
    mode: 'transit',
    fetchImpl,
  });

  const transitInstructions = result.legs.flatMap((leg, legIndex) =>
    extractTransitInstructions(leg.steps, legIndex),
  );

  return {
    ...result.route,
    transitInstructions,
  };
};
