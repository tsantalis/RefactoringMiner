import type { LatLng } from '../../utils/mapUtils';
import { distanceMeters } from '../../utils/mapUtils';
import type { MapRegion, ModeRoute, NavigationStep } from './types';

type GoogleLocation = {
  lat?: number;
  lng?: number;
};

type GoogleLegStep = {
  html_instructions?: string;
  distance?: { text?: string };
  duration?: { text?: string };
  maneuver?: string;
  start_location?: GoogleLocation;
  end_location?: GoogleLocation;
};

type GoogleLeg = {
  distance?: { text?: string };
  duration?: { text?: string; value?: number };
  duration_in_traffic?: { text?: string; value?: number };
  steps?: GoogleLegStep[];
};

type GoogleRoute = {
  summary?: string;
  overview_polyline?: { points?: string };
};

function decodeNextValue(encoded: string, cursor: { index: number }): number {
  let b: number;
  let shift = 0;
  let result = 0;

  do {
    b = (encoded.codePointAt(cursor.index++) ?? 0) - 63;
    result |= (b & 0x1f) << shift;
    shift += 5;
  } while (b >= 0x20);

  return result & 1 ? ~(result >> 1) : result >> 1;
}

export function decodePolyline(encoded: string): LatLng[] {
  const points: LatLng[] = [];
  const cursor = { index: 0 };
  let lat = 0;
  let lng = 0;

  while (cursor.index < encoded.length) {
    lat += decodeNextValue(encoded, cursor);
    lng += decodeNextValue(encoded, cursor);

    points.push({ latitude: lat / 1e5, longitude: lng / 1e5 });
  }

  return points;
}

export function stripHtmlInstructions(value: string): string {
  if (value.length === 0) return value;

  let inTag = false;
  let output = '';

  for (const char of value) {
    if (char === '<') {
      inTag = true;
      continue;
    }

    if (char === '>') {
      inTag = false;
      continue;
    }

    if (!inTag) {
      output += char;
    }
  }

  return output;
}

function resolveFocusCoordinate(step: GoogleLegStep): LatLng | undefined {
  if (step.end_location?.lat != null && step.end_location.lng != null) {
    return {
      latitude: step.end_location.lat,
      longitude: step.end_location.lng,
    };
  }

  if (step.start_location?.lat != null && step.start_location.lng != null) {
    return {
      latitude: step.start_location.lat,
      longitude: step.start_location.lng,
    };
  }

  return undefined;
}

export function mapGoogleLegSteps(steps: GoogleLegStep[] | undefined): NavigationStep[] {
  if (!steps) return [];

  return steps.map((step) => ({
    instruction: stripHtmlInstructions(step.html_instructions ?? ''),
    distanceText: step.distance?.text ?? '',
    durationText: step.duration?.text ?? '',
    maneuver: step.maneuver,
    focusCoordinate: resolveFocusCoordinate(step),
  }));
}

export function buildModeRouteFromGoogleLeg(
  route: GoogleRoute,
  leg: GoogleLeg,
  options?: { preferTrafficDuration?: boolean },
): ModeRoute {
  const duration =
    options?.preferTrafficDuration && leg.duration_in_traffic
      ? leg.duration_in_traffic
      : leg.duration;

  return {
    durationText: duration?.text ?? '',
    durationSec: duration?.value ?? 0,
    distanceText: leg.distance?.text ?? '',
    viaText: route.summary || '',
    polyline: route.overview_polyline?.points ? decodePolyline(route.overview_polyline.points) : [],
    steps: mapGoogleLegSteps(leg.steps),
  };
}

export function calculateBounds(coords: LatLng[]) {
  let minLat = coords[0].latitude;
  let maxLat = coords[0].latitude;
  let minLng = coords[0].longitude;
  let maxLng = coords[0].longitude;

  for (const c of coords) {
    if (c.latitude < minLat) minLat = c.latitude;
    if (c.latitude > maxLat) maxLat = c.latitude;
    if (c.longitude < minLng) minLng = c.longitude;
    if (c.longitude > maxLng) maxLng = c.longitude;
  }

  return { minLat, maxLat, minLng, maxLng };
}

export function boundsToRegion(bounds: ReturnType<typeof calculateBounds>): MapRegion {
  const PADDING = 1.4;
  const latDelta = (bounds.maxLat - bounds.minLat) * PADDING || 0.005;
  const lngDelta = (bounds.maxLng - bounds.minLng) * PADDING || 0.005;

  return {
    latitude: (bounds.minLat + bounds.maxLat) / 2,
    longitude: (bounds.minLng + bounds.maxLng) / 2,
    latitudeDelta: latDelta,
    longitudeDelta: lngDelta,
  };
}

export function midpointOrFallback(polyline: LatLng[], fallback: LatLng): LatLng {
  if (polyline.length === 0) return fallback;
  return polyline[Math.floor(polyline.length / 2)];
}

export function minutesBetween(startMs: number, endMs: number): number {
  if (endMs <= startMs) return 0;
  return Math.round((endMs - startMs) / 60_000);
}

export function formatMinutesLabel(seconds: number): string {
  const minutes = Math.max(1, Math.round(seconds / 60));
  return `${minutes} min`;
}

export function formatDistanceLabel(distance: number): string {
  if (distance >= 1000) return `${(distance / 1000).toFixed(1)} km`;
  return `${Math.max(1, Math.round(distance))} m`;
}

export function dedupePolyline(points: LatLng[]): LatLng[] {
  const deduped: LatLng[] = [];

  for (const point of points) {
    const last = deduped.at(-1);
    if (!last || distanceMeters(last, point) >= 1) {
      deduped.push(point);
    }
  }

  return deduped;
}
