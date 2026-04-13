import { buildModeRouteFromGoogleLeg } from '../routeUtils';
import type { ModeRoute } from '../types';
import type { ModeRouteStrategy, ModeRouteStrategyContext } from './ModeRouteStrategy';

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

    return buildModeRouteFromGoogleLeg(route, leg);
  }
}
