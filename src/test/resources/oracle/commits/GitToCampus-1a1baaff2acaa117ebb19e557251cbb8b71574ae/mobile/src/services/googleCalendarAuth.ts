import * as AuthSession from 'expo-auth-session';
import * as Application from 'expo-application';
import * as SecureStore from 'expo-secure-store';
import * as WebBrowser from 'expo-web-browser';
import { Platform } from 'react-native';

const GOOGLE_CALENDAR_STORAGE_KEY = 'gittocampus.googleCalendar.session.v1';
const GOOGLE_CALENDAR_KEYCHAIN_SERVICE = 'gittocampus.googleCalendar';
const GOOGLE_CALENDAR_LIST_ENDPOINT =
  'https://www.googleapis.com/calendar/v3/users/me/calendarList';
const GOOGLE_CALENDAR_EVENTS_ENDPOINT = 'https://www.googleapis.com/calendar/v3/calendars';
const GOOGLE_CALENDAR_EVENTS_MAX_RESULTS = 100;
const GOOGLE_CALENDAR_EVENTS_LOOKAHEAD_DAYS = 30;
export const ONGOING_EVENT_WITHOUT_END_MAX_AGE_MS = 3 * 60 * 60 * 1000;
const TOKEN_EXPIRY_GRACE_MS = 60_000;
const FALLBACK_ACCESS_TOKEN_TTL_SECONDS = 3600;
const GOOGLE_CALENDAR_REDIRECT_PATH = 'oauthredirect';
const APP_SCHEME = 'gittocampus';
const IOS_BUNDLE_ID_FALLBACK = 'com.gittocampus.mobile';
const ANDROID_PACKAGE_FALLBACK = 'com.anonymous.mobile';
const GOOGLE_CLIENT_ID_SUFFIX = '.apps.googleusercontent.com';

const logCalendarDebug = (message: string, details?: Record<string, unknown> | undefined): void => {
  if (!__DEV__) return;

  if (details) {
    console.info(`[GoogleCalendarAuth] ${message}`, details);
    return;
  }

  console.info(`[GoogleCalendarAuth] ${message}`);
};

const GOOGLE_CALENDAR_DISCOVERY = {
  authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
  tokenEndpoint: 'https://oauth2.googleapis.com/token',
  revocationEndpoint: 'https://oauth2.googleapis.com/revoke',
} as const;

const secureStoreOptions = {
  keychainService: GOOGLE_CALENDAR_KEYCHAIN_SERVICE,
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
} as const;

export const GOOGLE_CALENDAR_READONLY_SCOPE = 'https://www.googleapis.com/auth/calendar.readonly';

export type GoogleCalendarSession = {
  accessToken: string;
  tokenType: string;
  scope: string;
  expiresAt: number;
  refreshToken?: string;
};

export type GoogleCalendarConnectionStatus = 'loading' | 'connected' | 'not_connected' | 'expired';

export type GoogleCalendarSessionState = {
  status: Exclude<GoogleCalendarConnectionStatus, 'loading'>;
  session: GoogleCalendarSession | null;
};

export type GoogleCalendarConnectResult =
  | { type: 'success'; session: GoogleCalendarSession }
  | { type: 'cancel' }
  | { type: 'denied' }
  | { type: 'error'; message: string };

export type GoogleCalendarListItem = {
  id: string;
  name: string;
  accessRole: string | null;
  isPrimary: boolean;
};

export type GoogleCalendarListResult =
  | { type: 'success'; calendars: GoogleCalendarListItem[] }
  | { type: 'error'; message: string };

export type GoogleCalendarEventItem = {
  id: string;
  calendarId: string;
  title: string;
  location: string | null;
  startsAt: number;
  endsAt?: number;
};

type ParsedGoogleCalendarEventItem = GoogleCalendarEventItem & {
  endsAt: number | null;
};

export type GoogleCalendarEventsResult =
  | { type: 'success'; events: GoogleCalendarEventItem[] }
  | { type: 'error'; message: string };

export const isGoogleCalendarEventActiveOrUpcoming = (
  event: Pick<GoogleCalendarEventItem, 'startsAt' | 'endsAt'>,
  nowTimestamp: number,
): boolean =>
  typeof event.endsAt === 'number'
    ? event.endsAt > nowTimestamp
    : event.startsAt >= nowTimestamp ||
      nowTimestamp - event.startsAt <= ONGOING_EVENT_WITHOUT_END_MAX_AGE_MS;

const isNonEmptyString = (value: unknown): value is string =>
  typeof value === 'string' && value.trim().length > 0;

const parseStoredSession = (value: string): GoogleCalendarSession | null => {
  let parsed: unknown;
  try {
    parsed = JSON.parse(value);
  } catch {
    return null;
  }

  if (!parsed || typeof parsed !== 'object') return null;

  const candidate = parsed as Record<string, unknown>;
  if (!isNonEmptyString(candidate.accessToken)) return null;
  if (!isNonEmptyString(candidate.tokenType)) return null;
  if (!isNonEmptyString(candidate.scope)) return null;
  if (typeof candidate.expiresAt !== 'number' || !Number.isFinite(candidate.expiresAt)) {
    return null;
  }
  if (candidate.refreshToken !== undefined && !isNonEmptyString(candidate.refreshToken)) {
    return null;
  }

  return {
    accessToken: candidate.accessToken,
    tokenType: candidate.tokenType,
    scope: candidate.scope,
    expiresAt: candidate.expiresAt,
    refreshToken: candidate.refreshToken,
  };
};

const isSessionExpired = (session: Pick<GoogleCalendarSession, 'expiresAt'>) =>
  session.expiresAt <= Date.now() + TOKEN_EXPIRY_GRACE_MS;

const getConfiguredClientId = (): string => {
  const platformClientId = Platform.select({
    android: process.env.EXPO_PUBLIC_GOOGLE_CALENDAR_ANDROID_CLIENT_ID,
    ios: process.env.EXPO_PUBLIC_GOOGLE_CALENDAR_IOS_CLIENT_ID,
    default: process.env.EXPO_PUBLIC_GOOGLE_CALENDAR_WEB_CLIENT_ID,
  });

  const fallbackClientId = process.env.EXPO_PUBLIC_GOOGLE_CALENDAR_CLIENT_ID;
  return (platformClientId ?? fallbackClientId ?? '').trim();
};

const getGoogleClientRedirectScheme = (clientId: string): string | null => {
  const normalizedClientId = clientId.trim();
  if (!normalizedClientId.endsWith(GOOGLE_CLIENT_ID_SUFFIX)) return null;

  const clientIdPrefix = normalizedClientId.slice(0, -GOOGLE_CLIENT_ID_SUFFIX.length).trim();
  if (!clientIdPrefix) return null;

  return `com.googleusercontent.apps.${clientIdPrefix}`;
};

const getRedirectScheme = (): string =>
  Platform.OS === 'ios'
    ? (
        getGoogleClientRedirectScheme(
          process.env.EXPO_PUBLIC_GOOGLE_CALENDAR_IOS_CLIENT_ID ?? '',
        ) ??
        Application.applicationId ??
        IOS_BUNDLE_ID_FALLBACK
      ).trim()
    : (
        Application.applicationId ??
        (Platform.OS === 'android' ? ANDROID_PACKAGE_FALLBACK : APP_SCHEME)
      ).trim();

const createRedirectUri = () =>
  AuthSession.makeRedirectUri({
    // Google OAuth for installed apps expects a native redirect URI.
    native: `${getRedirectScheme()}:/${GOOGLE_CALENDAR_REDIRECT_PATH}`,
    path: GOOGLE_CALENDAR_REDIRECT_PATH,
  });

const isExpoGoRedirectUri = (redirectUri: string) => redirectUri.startsWith('exp://');

const toSession = (tokenResponse: AuthSession.TokenResponse): GoogleCalendarSession => {
  const expiresInSeconds = tokenResponse.expiresIn ?? FALLBACK_ACCESS_TOKEN_TTL_SECONDS;

  return {
    accessToken: tokenResponse.accessToken,
    tokenType: tokenResponse.tokenType,
    scope: tokenResponse.scope ?? GOOGLE_CALENDAR_READONLY_SCOPE,
    expiresAt: Date.now() + expiresInSeconds * 1000,
    refreshToken: tokenResponse.refreshToken,
  };
};

const mapPromptErrorToMessage = (error: AuthSession.AuthError | null | undefined): string => {
  const errorCode = (error?.code ?? '').toLowerCase();
  if (errorCode === 'access_denied') {
    return 'Calendar access was denied.';
  }

  const description = error?.description?.trim();
  if (description) return description;

  const message = error?.message?.trim();
  if (message) return message;

  return 'Google authentication failed. Please try again.';
};

const mapUnexpectedErrorToMessage = (error: unknown): string => {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return 'Unable to connect Google Calendar right now. Please try again.';
};

const mapCalendarListErrorToMessage = (error: unknown): string => {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return 'Unable to load calendar list right now. Please retry.';
};

const mapCalendarEventsErrorToMessage = (error: unknown): string => {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return 'Unable to load upcoming classes right now. Please retry.';
};

const getPlatformClientIdEnvName = (): string =>
  Platform.OS === 'android'
    ? 'EXPO_PUBLIC_GOOGLE_CALENDAR_ANDROID_CLIENT_ID'
    : Platform.OS === 'ios'
      ? 'EXPO_PUBLIC_GOOGLE_CALENDAR_IOS_CLIENT_ID'
      : 'EXPO_PUBLIC_GOOGLE_CALENDAR_WEB_CLIENT_ID';

export const saveGoogleCalendarSession = async (session: GoogleCalendarSession): Promise<void> => {
  await SecureStore.setItemAsync(
    GOOGLE_CALENDAR_STORAGE_KEY,
    JSON.stringify(session),
    secureStoreOptions,
  );
};

export const clearGoogleCalendarSession = async (): Promise<void> => {
  await SecureStore.deleteItemAsync(GOOGLE_CALENDAR_STORAGE_KEY, secureStoreOptions);
};

export const getStoredGoogleCalendarSessionState =
  async (): Promise<GoogleCalendarSessionState> => {
    try {
      const storedValue = await SecureStore.getItemAsync(
        GOOGLE_CALENDAR_STORAGE_KEY,
        secureStoreOptions,
      );
      if (!storedValue) {
        return { status: 'not_connected', session: null };
      }

      const session = parseStoredSession(storedValue);
      if (!session) {
        await clearGoogleCalendarSession();
        return { status: 'not_connected', session: null };
      }

      if (isSessionExpired(session)) {
        await clearGoogleCalendarSession();
        return { status: 'expired', session: null };
      }

      return { status: 'connected', session };
    } catch {
      return { status: 'not_connected', session: null };
    }
  };

const parseCalendarListItems = (value: unknown): GoogleCalendarListItem[] => {
  if (!value || typeof value !== 'object') return [];

  const payload = value as { items?: unknown };
  if (!Array.isArray(payload.items)) return [];

  return payload.items
    .map((item) => {
      if (!item || typeof item !== 'object') return null;

      const candidate = item as Record<string, unknown>;
      const id = candidate.id;
      const name = candidate.summaryOverride ?? candidate.summary;
      if (!isNonEmptyString(id) || !isNonEmptyString(name)) return null;

      return {
        id,
        name: name.trim(),
        accessRole: isNonEmptyString(candidate.accessRole) ? candidate.accessRole : null,
        isPrimary: candidate.primary === true,
      } as GoogleCalendarListItem;
    })
    .filter((item): item is GoogleCalendarListItem => item !== null);
};

const parseCalendarEventStartTime = (value: unknown): number | null => {
  if (!value || typeof value !== 'object') return null;

  const start = value as { dateTime?: unknown; date?: unknown };
  const startValue = start.dateTime ?? start.date;
  if (!isNonEmptyString(startValue)) return null;

  const startsAt = new Date(startValue).getTime();
  return Number.isFinite(startsAt) ? startsAt : null;
};

const parseCalendarEventEndTime = (value: unknown): number | null => {
  if (!value || typeof value !== 'object') return null;

  const end = value as { dateTime?: unknown; date?: unknown };
  const endValue = end.dateTime ?? end.date;
  if (!isNonEmptyString(endValue)) return null;

  const endsAt = new Date(endValue).getTime();
  return Number.isFinite(endsAt) ? endsAt : null;
};

const getCalendarPayloadItemCount = (payload: unknown): number => {
  if (!payload || typeof payload !== 'object') return 0;

  const candidate = payload as { items?: unknown };
  return Array.isArray(candidate.items) ? candidate.items.length : 0;
};

const normalizeCalendarEventText = (value: string | null) =>
  (value ?? '').trim().toUpperCase().replace(/\s+/g, ' ');

const toEventDedupKey = (event: GoogleCalendarEventItem) =>
  `${normalizeCalendarEventText(event.title)}|${normalizeCalendarEventText(event.location)}|${event.startsAt}`;

const dedupeCalendarEvents = (events: GoogleCalendarEventItem[]): GoogleCalendarEventItem[] => {
  const dedupedEvents = new Map<string, GoogleCalendarEventItem>();

  for (const event of events) {
    const key = toEventDedupKey(event);
    if (!dedupedEvents.has(key)) {
      dedupedEvents.set(key, event);
    }
  }

  return Array.from(dedupedEvents.values());
};

const parseCalendarEventItems = (
  calendarId: string,
  value: unknown,
): ParsedGoogleCalendarEventItem[] => {
  if (!value || typeof value !== 'object') return [];

  const payload = value as { items?: unknown };
  if (!Array.isArray(payload.items)) return [];

  return payload.items
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;

      const candidate = item as Record<string, unknown>;
      const startsAt = parseCalendarEventStartTime(candidate.start);
      if (startsAt === null) return null;
      const endsAt = parseCalendarEventEndTime(candidate.end);
      if (endsAt !== null && endsAt < startsAt) return null;

      const rawId = candidate.id;
      const title = isNonEmptyString(candidate.summary)
        ? candidate.summary.trim()
        : 'Untitled event';
      const location = isNonEmptyString(candidate.location) ? candidate.location.trim() : null;
      const id = isNonEmptyString(rawId) ? rawId.trim() : `${calendarId}-${startsAt}-${index}`;
      const status = isNonEmptyString(candidate.status)
        ? candidate.status.toLowerCase().trim()
        : '';
      if (status === 'cancelled') return null;

      return {
        id,
        calendarId,
        title,
        location,
        startsAt,
        endsAt,
      } as ParsedGoogleCalendarEventItem;
    })
    .filter((item): item is ParsedGoogleCalendarEventItem => item !== null);
};

const toGoogleCalendarEventItem = ({
  id,
  calendarId,
  title,
  location,
  startsAt,
  endsAt,
}: ParsedGoogleCalendarEventItem): GoogleCalendarEventItem => ({
  id,
  calendarId,
  title,
  location,
  startsAt,
  ...(typeof endsAt === 'number' ? { endsAt } : {}),
});

export const fetchGoogleCalendarListAsync = async (): Promise<GoogleCalendarListResult> => {
  const state = await getStoredGoogleCalendarSessionState();
  if (state.status !== 'connected' || !state.session) {
    return {
      type: 'error',
      message: 'Connect Google Calendar before loading your calendars.',
    };
  }

  try {
    const response = await fetch(GOOGLE_CALENDAR_LIST_ENDPOINT, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${state.session.accessToken}`,
        Accept: 'application/json',
      },
    });

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        return {
          type: 'error',
          message: 'Calendar authorization expired. Reconnect Google Calendar and try again.',
        };
      }
      return {
        type: 'error',
        message: 'Unable to load calendar list right now. Please retry.',
      };
    }

    const payload: unknown = await response.json();
    return {
      type: 'success',
      calendars: parseCalendarListItems(payload),
    };
  } catch (error) {
    return {
      type: 'error',
      message: mapCalendarListErrorToMessage(error),
    };
  }
};

export const fetchGoogleCalendarEventsAsync = async (
  calendarIds: string[],
): Promise<GoogleCalendarEventsResult> => {
  const uniqueCalendarIds = [...new Set(calendarIds.map((id) => id.trim()).filter(Boolean))];
  logCalendarDebug('Fetching calendar events', {
    requestedCalendarIds: calendarIds,
    uniqueCalendarIds,
  });

  if (uniqueCalendarIds.length === 0) {
    logCalendarDebug('Skipping event fetch because no calendars are selected');
    return { type: 'success', events: [] };
  }

  const state = await getStoredGoogleCalendarSessionState();
  if (state.status !== 'connected' || !state.session) {
    logCalendarDebug('Cannot fetch events because calendar session is unavailable', {
      status: state.status,
    });
    return {
      type: 'error',
      message: 'Connect Google Calendar before loading your upcoming classes.',
    };
  }

  const now = new Date();
  const nowTimestamp = now.getTime();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const endOfLookaheadWindow = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate() + GOOGLE_CALENDAR_EVENTS_LOOKAHEAD_DAYS + 1,
  );
  const timeMin = startOfToday.toISOString();
  const timeMax = endOfLookaheadWindow.toISOString();
  const requestedTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone?.trim();
  logCalendarDebug('Using event query window', {
    now: now.toISOString(),
    nowTimestamp,
    timeMin,
    timeMax,
    lookaheadDays: GOOGLE_CALENDAR_EVENTS_LOOKAHEAD_DAYS,
    requestedTimeZone: requestedTimeZone || null,
  });

  try {
    const fetchCalendarEventResponses = async ({
      requestTimeMin,
      requestTimeMax,
    }: {
      requestTimeMin: string;
      requestTimeMax?: string;
    }) => {
      logCalendarDebug('Requesting events for window', {
        requestTimeMin,
        requestTimeMax: requestTimeMax ?? null,
        calendarCount: uniqueCalendarIds.length,
      });

      const responses = await Promise.all(
        uniqueCalendarIds.map(async (calendarId) => {
          const endpoint =
            `${GOOGLE_CALENDAR_EVENTS_ENDPOINT}/${encodeURIComponent(calendarId)}` +
            `/events?singleEvents=true&orderBy=startTime&timeMin=${encodeURIComponent(
              requestTimeMin,
            )}` +
            `${requestTimeMax ? `&timeMax=${encodeURIComponent(requestTimeMax)}` : ''}` +
            `${requestedTimeZone ? `&timeZone=${encodeURIComponent(requestedTimeZone)}` : ''}` +
            `&maxResults=${GOOGLE_CALENDAR_EVENTS_MAX_RESULTS}`;

          const response = await fetch(endpoint, {
            method: 'GET',
            headers: {
              Authorization: `Bearer ${state.session!.accessToken}`,
              Accept: 'application/json',
            },
          });

          if (!response.ok) {
            return { calendarId, response, payload: null as unknown };
          }

          const payload: unknown = await response.json();
          return { calendarId, response, payload };
        }),
      );

      logCalendarDebug('Received calendar event responses', {
        statuses: responses.map(({ calendarId, response, payload }) => ({
          calendarId,
          status: response.status,
          ok: response.ok,
          itemCount: response.ok ? getCalendarPayloadItemCount(payload) : null,
        })),
      });

      return responses;
    };

    const toActiveOrUpcomingEvents = (
      responses: Awaited<ReturnType<typeof fetchCalendarEventResponses>>,
    ) => {
      const parsedEvents = responses.flatMap(({ calendarId, payload }) =>
        parseCalendarEventItems(calendarId, payload),
      );
      const activeOrUpcomingEvents = parsedEvents
        .filter((event) => isGoogleCalendarEventActiveOrUpcoming(event, nowTimestamp))
        .sort((a, b) => a.startsAt - b.startsAt);

      logCalendarDebug('Applied active/upcoming event filter', {
        parsedEventCount: parsedEvents.length,
        activeOrUpcomingCount: activeOrUpcomingEvents.length,
        droppedEventCount: parsedEvents.length - activeOrUpcomingEvents.length,
        sampleActiveOrUpcomingEvents: activeOrUpcomingEvents.slice(0, 5).map((event) => ({
          id: event.id,
          calendarId: event.calendarId,
          title: event.title,
          location: event.location,
          startsAt: new Date(event.startsAt).toISOString(),
          endsAt: typeof event.endsAt === 'number' ? new Date(event.endsAt).toISOString() : null,
        })),
      });

      return activeOrUpcomingEvents;
    };

    const resolveResponseErrors = (
      responses: Awaited<ReturnType<typeof fetchCalendarEventResponses>>,
    ): GoogleCalendarEventsResult | null => {
      const successfulResponses = responses.filter(({ response }) => response.ok);
      if (successfulResponses.length > 0) return null;

      const unauthorizedResponses = responses.filter(
        ({ response }) => response.status === 401 || response.status === 403,
      );
      if (unauthorizedResponses.length > 0) {
        logCalendarDebug('Calendar event request unauthorized', {
          unauthorizedCalendarIds: unauthorizedResponses.map(({ calendarId }) => calendarId),
        });
        return {
          type: 'error',
          message: 'Calendar authorization expired. Reconnect Google Calendar and try again.',
        };
      }

      logCalendarDebug('Calendar event requests failed', {
        statuses: responses.map(({ calendarId, response }) => ({
          calendarId,
          status: response.status,
        })),
      });
      return {
        type: 'error',
        message: 'Unable to load upcoming classes right now. Please retry.',
      };
    };

    const windowResponses = await fetchCalendarEventResponses({
      requestTimeMin: timeMin,
      requestTimeMax: timeMax,
    });
    const windowErrorResult = resolveResponseErrors(windowResponses);
    if (windowErrorResult) return windowErrorResult;

    const windowEvents = toActiveOrUpcomingEvents(
      windowResponses.filter(({ response }) => response.ok),
    );
    const dedupedEvents = dedupeCalendarEvents(windowEvents.map(toGoogleCalendarEventItem));

    logCalendarDebug('Returning active/upcoming events', {
      windowEventCount: windowEvents.length,
      dedupedEventCount: dedupedEvents.length,
      dedupedEvents: dedupedEvents.slice(0, 5).map((event) => ({
        id: event.id,
        calendarId: event.calendarId,
        title: event.title,
        location: event.location,
        startsAt: new Date(event.startsAt).toISOString(),
        endsAt: typeof event.endsAt === 'number' ? new Date(event.endsAt).toISOString() : null,
      })),
    });

    return {
      type: 'success',
      events: dedupedEvents,
    };
  } catch (error) {
    logCalendarDebug('Calendar event request threw an error', {
      message: error instanceof Error ? error.message : String(error),
    });
    return {
      type: 'error',
      message: mapCalendarEventsErrorToMessage(error),
    };
  }
};

export const connectGoogleCalendarAsync = async (): Promise<GoogleCalendarConnectResult> => {
  const clientId = getConfiguredClientId();
  if (!clientId) {
    return {
      type: 'error',
      message:
        'Google Calendar OAuth client ID is missing. Configure EXPO_PUBLIC_GOOGLE_CALENDAR_*_CLIENT_ID.',
    };
  }

  const redirectUri = createRedirectUri();
  if (__DEV__) {
    console.info('[GoogleCalendarAuth] OAuth request config', {
      clientId,
      redirectUri,
      platform: Platform.OS,
    });
  }

  if (isExpoGoRedirectUri(redirectUri)) {
    return {
      type: 'error',
      message:
        'Google Calendar sign-in is not supported in Expo Go. Use a development build and try again.',
    };
  }

  try {
    WebBrowser.maybeCompleteAuthSession();

    const request = await AuthSession.loadAsync(
      {
        clientId,
        redirectUri,
        scopes: [GOOGLE_CALENDAR_READONLY_SCOPE],
        responseType: AuthSession.ResponseType.Code,
        usePKCE: true,
      },
      GOOGLE_CALENDAR_DISCOVERY,
    );

    const promptResult = await request.promptAsync(GOOGLE_CALENDAR_DISCOVERY);

    if (promptResult.type === 'cancel' || promptResult.type === 'dismiss') {
      return { type: 'cancel' };
    }

    if (promptResult.type === 'locked') {
      return {
        type: 'error',
        message: 'Google sign-in is already active. Please wait and try again.',
      };
    }

    if (promptResult.type === 'error') {
      const oauthErrorCode = (promptResult.params.error ?? '').toLowerCase();

      if (oauthErrorCode === 'access_denied') {
        return { type: 'denied' };
      }

      if (oauthErrorCode === 'deleted_client' || oauthErrorCode === 'invalid_client') {
        return {
          type: 'error',
          message:
            `Google OAuth client is invalid or deleted. Update ${getPlatformClientIdEnvName()} ` +
            'in mobile/.env and rebuild the dev client.',
        };
      }

      const oauthErrorDescription = (promptResult.params.error_description ?? '').trim();
      return {
        type: 'error',
        message: oauthErrorDescription || mapPromptErrorToMessage(promptResult.error),
      };
    }

    if (promptResult.type !== 'success') {
      return {
        type: 'error',
        message: 'Google sign-in did not complete. Please try again.',
      };
    }

    let tokenResponse = promptResult.authentication;

    if (!tokenResponse) {
      const authCode = promptResult.params.code;
      if (!isNonEmptyString(authCode)) {
        return {
          type: 'error',
          message: 'Google authentication completed without an authorization code.',
        };
      }

      tokenResponse = await AuthSession.exchangeCodeAsync(
        {
          clientId,
          code: authCode,
          redirectUri,
          extraParams: request.codeVerifier
            ? {
                code_verifier: request.codeVerifier,
              }
            : undefined,
        },
        GOOGLE_CALENDAR_DISCOVERY,
      );
    }

    const session = toSession(tokenResponse);
    await saveGoogleCalendarSession(session);

    return {
      type: 'success',
      session,
    };
  } catch (error) {
    return {
      type: 'error',
      message: mapUnexpectedErrorToMessage(error),
    };
  }
};
