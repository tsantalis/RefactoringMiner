const GOOGLE_CALENDAR_SCOPE = 'https://www.googleapis.com/auth/calendar.readonly';

export const GOOGLE_SCOPES = ['email', 'profile', GOOGLE_CALENDAR_SCOPE];

export const MISSING_GOOGLE_CLIENT_ID_MESSAGE =
  'Google Sign-In is not configured. Add EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID and EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID to the mobile .env file, then rebuild the app.';
export const INVALID_GOOGLE_CLIENT_ID_MESSAGE =
  'Google Sign-In is misconfigured. EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID and EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID must be valid Google OAuth client ids ending in .apps.googleusercontent.com.';

type EnvSource = Record<string, string | undefined>;

function maskClientId(clientId: string) {
  if (!clientId) return '';
  if (clientId.length <= 12) return `${clientId.slice(0, 4)}...`;

  return `${clientId.slice(0, 8)}...${clientId.slice(-24)}`;
}

function isValidGoogleClientId(clientId: string) {
  return /\.apps\.googleusercontent\.com$/i.test(clientId);
}

export function getGoogleCalendarAuthConfig(env: EnvSource = process.env) {
  const androidClientId = env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID?.trim() || '';
  const webClientId = env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID?.trim() || '';

  const hasAndroidClientId = androidClientId.length > 0;
  const hasWebClientId = webClientId.length > 0;
  const hasClientIds = hasAndroidClientId && hasWebClientId;

  const isAndroidClientIdValid = hasAndroidClientId && isValidGoogleClientId(androidClientId);
  const isWebClientIdValid = hasWebClientId && isValidGoogleClientId(webClientId);
  const isValid = isAndroidClientIdValid && isWebClientIdValid;
  const errorMessage = !hasClientIds
    ? MISSING_GOOGLE_CLIENT_ID_MESSAGE
    : !isValid
      ? INVALID_GOOGLE_CLIENT_ID_MESSAGE
      : null;

  return {
    androidClientId,
    webClientId,
    debugSummary: {
      envVars: ['EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID', 'EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID'],
      hasAndroidClientId,
      hasWebClientId,
      isValid,
      maskedAndroidClientId: maskClientId(androidClientId),
      maskedWebClientId: maskClientId(webClientId),
    },
    errorMessage,
    isConfigured: isValid,
    configureOptions: {
      scopes: GOOGLE_SCOPES,
      ...(isValid ? { webClientId } : {}),
    },
  };
}
