import { decodePolyline } from '../routeUtils';
import type { ModeRoute } from '../types';
import type { ModeRouteStrategy, ModeRouteStrategyContext } from './ModeRouteStrategy';

function stripHtmlInstructions(value: string): string {
  return value.replaceAll(/<[^>]*>/g, '');
}

export class OutdoorWalkingRouteStrategy implements ModeRouteStrategy {
  readonly key = 'outdoorWalking' as const;

  async execute(context: ModeRouteStrategyContext): Promise<ModeRoute | null> {
    const { origin, destination, googleMapsApiKey, fetchImpl } = context;
    if (!googleMapsApiKey) return null;

    const originStr = `${origin.latitude},${origin.longitude}`;
    const destinationStr = `${destination.latitude},${destination.longitude}`;
    const url = `https://maps.googleapis.com/maps/api/directions/json?origin=${originStr}&destination=${destinationStr}&mode=walking&key=${googleMapsApiKey}`;
    const response = await fetchImpl(url);
    const data = await response.json();

    if (data.status !== 'OK' || !data.routes?.length) return null;
    const route = data.routes[0];
    const leg = route.legs?.[0];
    if (!leg) return null;

    const polyline = route.overview_polyline?.points
      ? decodePolyline(route.overview_polyline.points)
      : [];

    return {
      durationText: leg.duration?.text ?? '',
      durationSec: leg.duration?.value ?? 0,
      distanceText: leg.distance?.text ?? '',
      viaText: route.summary || '',
      polyline,
      steps: (leg.steps ?? []).map(
        (s: {
          html_instructions?: string;
          distance?: { text?: string };
          duration?: { text?: string };
          maneuver?: string;
          start_location?: { lat?: number; lng?: number };
          end_location?: { lat?: number; lng?: number };
        }) => {
          let focusCoordinate;
          if (s.end_location?.lat != null && s.end_location?.lng != null) {
            focusCoordinate = {
              latitude: s.end_location.lat,
              longitude: s.end_location.lng,
            };
          } else if (s.start_location?.lat != null && s.start_location?.lng != null) {
            focusCoordinate = {
              latitude: s.start_location.lat,
              longitude: s.start_location.lng,
            };
          }

          return {
            instruction: stripHtmlInstructions(s.html_instructions ?? ''),
            distanceText: s.distance?.text ?? '',
            durationText: s.duration?.text ?? '',
            maneuver: s.maneuver,
            focusCoordinate,
          };
        },
      ),
    };
  }
}
