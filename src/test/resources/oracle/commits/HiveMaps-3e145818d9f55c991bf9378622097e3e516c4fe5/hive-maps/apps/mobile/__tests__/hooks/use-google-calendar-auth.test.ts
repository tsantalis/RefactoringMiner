import { act, renderHook, waitFor } from '@testing-library/react-native';

jest.mock('@react-native-google-signin/google-signin', () => ({
  GoogleSignin: {
    configure: jest.fn(),
    signInSilently: jest.fn(),
    hasPlayServices: jest.fn(),
    signIn: jest.fn(),
    getTokens: jest.fn(),
    revokeAccess: jest.fn(),
    signOut: jest.fn(),
  },
  statusCodes: {
    SIGN_IN_CANCELLED: 'SIGN_IN_CANCELLED',
    PLAY_SERVICES_NOT_AVAILABLE: 'PLAY_SERVICES_NOT_AVAILABLE',
    IN_PROGRESS: 'IN_PROGRESS',
  },
}));

jest.mock('@/storage/auth-storage', () => ({
  loadGoogleCalendarSession: jest.fn(),
  saveGoogleCalendarSession: jest.fn(),
  clearGoogleCalendarSession: jest.fn(),
}));

jest.mock('@/hooks/google-calendar-auth-config', () => ({
  MISSING_GOOGLE_CLIENT_ID_MESSAGE:
    'Google Sign-In is not configured. Add EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID and EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID to the mobile .env file, then rebuild the app.',
  getGoogleCalendarAuthConfig: jest.fn(() => ({
    androidClientId: 'android.apps.googleusercontent.com',
    webClientId: 'web.apps.googleusercontent.com',
    debugSummary: {},
    errorMessage: null,
    isConfigured: true,
    configureOptions: {
      scopes: ['email', 'profile', 'https://www.googleapis.com/auth/calendar.readonly'],
      webClientId: 'web.apps.googleusercontent.com',
    },
  })),
}));

import { GoogleSignin } from '@react-native-google-signin/google-signin';
import { getGoogleCalendarAuthConfig } from '@/hooks/google-calendar-auth-config';
import {
  clearGoogleCalendarSession,
  loadGoogleCalendarSession,
  saveGoogleCalendarSession,
} from '@/storage/auth-storage';
import { useGoogleCalendarAuth } from '@/hooks/use-google-calendar-auth';

const mockGetGoogleCalendarAuthConfig = getGoogleCalendarAuthConfig as jest.MockedFunction<
  typeof getGoogleCalendarAuthConfig
>;
const mockLoadGoogleCalendarSession = loadGoogleCalendarSession as jest.MockedFunction<
  typeof loadGoogleCalendarSession
>;
const mockSaveGoogleCalendarSession = saveGoogleCalendarSession as jest.MockedFunction<
  typeof saveGoogleCalendarSession
>;
const mockClearGoogleCalendarSession = clearGoogleCalendarSession as jest.MockedFunction<
  typeof clearGoogleCalendarSession
>;
const mockConfigure = GoogleSignin.configure as jest.MockedFunction<typeof GoogleSignin.configure>;
const mockSignInSilently = GoogleSignin.signInSilently as jest.MockedFunction<
  typeof GoogleSignin.signInSilently
>;
const mockHasPlayServices = GoogleSignin.hasPlayServices as jest.MockedFunction<
  typeof GoogleSignin.hasPlayServices
>;
const mockSignIn = GoogleSignin.signIn as jest.MockedFunction<typeof GoogleSignin.signIn>;
const mockGetTokens = GoogleSignin.getTokens as jest.MockedFunction<typeof GoogleSignin.getTokens>;
const mockRevokeAccess = GoogleSignin.revokeAccess as jest.MockedFunction<
  typeof GoogleSignin.revokeAccess
>;
const mockSignOut = GoogleSignin.signOut as jest.MockedFunction<typeof GoogleSignin.signOut>;

const configuredAuthConfig = {
  androidClientId: 'android.apps.googleusercontent.com',
  webClientId: 'web.apps.googleusercontent.com',
  debugSummary: {},
  errorMessage: null,
  isConfigured: true,
  configureOptions: {
    scopes: ['email', 'profile', 'https://www.googleapis.com/auth/calendar.readonly'],
    webClientId: 'web.apps.googleusercontent.com',
  },
};

describe('useGoogleCalendarAuth', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    mockGetGoogleCalendarAuthConfig.mockReturnValue(configuredAuthConfig);
    mockLoadGoogleCalendarSession.mockResolvedValue(null);
    mockSaveGoogleCalendarSession.mockResolvedValue(undefined);
    mockClearGoogleCalendarSession.mockResolvedValue(undefined);

    mockSignInSilently.mockResolvedValue({ type: 'cancelled' });
    mockHasPlayServices.mockResolvedValue(true);
    mockSignIn.mockResolvedValue({
      type: 'success',
      data: {
        scopes: ['email', 'profile'],
        user: { email: 'student@example.edu', name: 'Student' },
      },
    });
    mockGetTokens.mockResolvedValue({
      accessToken: 'access-token',
      idToken: 'id-token',
    });
    mockRevokeAccess.mockResolvedValue(undefined);
    mockSignOut.mockResolvedValue(undefined);
  });

  it('restores a silently signed-in session on mount', async () => {
    mockSignInSilently.mockResolvedValue({
      type: 'success',
      data: {
        scopes: ['email', 'profile'],
        user: { email: 'student@example.edu', name: 'Student' },
      },
    });

    const { result } = renderHook(() => useGoogleCalendarAuth());

    await waitFor(() => expect(result.current.status).toBe('connected'));

    expect(mockSaveGoogleCalendarSession).toHaveBeenCalledTimes(1);
    expect(result.current.session?.email).toBe('student@example.edu');
    expect(result.current.error).toBeNull();
  });

  it('returns to idle and clears stale session data when silent sign-in is unavailable', async () => {
    const { result } = renderHook(() => useGoogleCalendarAuth());

    await waitFor(() => expect(result.current.status).toBe('idle'));

    expect(mockClearGoogleCalendarSession).toHaveBeenCalledTimes(1);
    expect(result.current.session).toBeNull();
  });

  it('surfaces a configuration error before attempting sign-in', async () => {
    mockGetGoogleCalendarAuthConfig.mockReturnValue({
      ...configuredAuthConfig,
      errorMessage:
        'Google Sign-In is not configured. Add EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID and EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID to the mobile .env file, then rebuild the app.',
      isConfigured: false,
      configureOptions: { scopes: configuredAuthConfig.configureOptions.scopes },
    });

    const { result } = renderHook(() => useGoogleCalendarAuth());

    await waitFor(() => expect(result.current.status).toBe('idle'));

    await act(async () => {
      await result.current.connect();
    });

    expect(mockHasPlayServices).not.toHaveBeenCalled();
    expect(result.current.status).toBe('error');
    expect(result.current.error).toMatch(/not configured/i);
  });

  it('maps non-recoverable sign-in failures to the setup guidance', async () => {
    mockSignIn.mockRejectedValueOnce(new Error('A non-recoverable sign in failure occurred'));

    const { result } = renderHook(() => useGoogleCalendarAuth());

    await waitFor(() => expect(result.current.status).toBe('idle'));

    await act(async () => {
      await result.current.connect();
    });

    expect(result.current.status).toBe('error');
    expect(result.current.error).toMatch(/misconfigured/i);
  });

  it('stores the session after a successful interactive sign-in', async () => {
    const { result } = renderHook(() => useGoogleCalendarAuth());

    await waitFor(() => expect(result.current.status).toBe('idle'));

    await act(async () => {
      await result.current.connect();
    });

    expect(mockHasPlayServices).toHaveBeenCalledWith({ showPlayServicesUpdateDialog: true });
    expect(mockSaveGoogleCalendarSession).toHaveBeenCalledTimes(1);
    expect(result.current.status).toBe('connected');
    expect(result.current.session?.email).toBe('student@example.edu');
  });

  it('clears the local session on disconnect even when revoke access fails', async () => {
    mockSignInSilently.mockResolvedValue({
      type: 'success',
      data: {
        scopes: ['email', 'profile'],
        user: { email: 'student@example.edu', name: 'Student' },
      },
    });
    mockRevokeAccess.mockRejectedValueOnce(new Error('network'));

    const { result } = renderHook(() => useGoogleCalendarAuth());

    await waitFor(() => expect(result.current.status).toBe('connected'));

    await act(async () => {
      await result.current.disconnect();
    });

    expect(mockClearGoogleCalendarSession).toHaveBeenCalled();
    expect(result.current.status).toBe('idle');
    expect(result.current.session).toBeNull();
    expect(result.current.error).toBeNull();
  });
});
