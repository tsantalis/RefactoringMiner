import { buildModeRouteFromGoogleLeg } from '../routeUtils';
import type { ModeRoute } from '../types';
import type { ModeRouteStrategy, ModeRouteStrategyContext } from './ModeRouteStrategy';

export class DrivingRouteStrategy implements ModeRouteStrategy {
  readonly key = 'driving' as const;

  async execute(context: ModeRouteStrategyContext): Promise<ModeRoute | null> {
    const { origin, destination, currentTime, googleMapsApiKey, fetchImpl } = context;
    if (!googleMapsApiKey) return null;

    const originStr = `${origin.latitude},${origin.longitude}`;
    const destinationStr = `${destination.latitude},${destination.longitude}`;
    const baseNowMs = currentTime?.getTime() ?? Date.now();
    const trafficParam = currentTime
      ? `&departure_time=${Math.floor(baseNowMs / 1000)}`
      : '&departure_time=now';

    const url = `https://maps.googleapis.com/maps/api/directions/json?origin=${originStr}&destination=${destinationStr}&mode=driving${trafficParam}&key=${googleMapsApiKey}`;
    const response = await fetchImpl(url);
    const data = await response.json();

    if (data.status !== 'OK' || !data.routes?.length) return null;
    const route = data.routes[0];
    const leg = route.legs?.[0];
    if (!leg) return null;

    return buildModeRouteFromGoogleLeg(route, leg, { preferTrafficDuration: true });
  }
}
