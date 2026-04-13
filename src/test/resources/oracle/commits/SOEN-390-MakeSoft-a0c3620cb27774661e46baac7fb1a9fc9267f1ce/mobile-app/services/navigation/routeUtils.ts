import type { LatLng } from '../../utils/mapUtils';
import { distanceMeters } from '../../utils/mapUtils';
import type { MapRegion } from './types';

export function decodePolyline(encoded: string): LatLng[] {
  const points: LatLng[] = [];
  let index = 0;
  let lat = 0;
  let lng = 0;

  while (index < encoded.length) {
    let b: number;
    let shift = 0;
    let result = 0;
    do {
      b = (encoded.codePointAt(index++) ?? 0) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    lat += result & 1 ? ~(result >> 1) : result >> 1;

    shift = 0;
    result = 0;
    do {
      b = (encoded.codePointAt(index++) ?? 0) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    lng += result & 1 ? ~(result >> 1) : result >> 1;

    points.push({ latitude: lat / 1e5, longitude: lng / 1e5 });
  }

  return points;
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
