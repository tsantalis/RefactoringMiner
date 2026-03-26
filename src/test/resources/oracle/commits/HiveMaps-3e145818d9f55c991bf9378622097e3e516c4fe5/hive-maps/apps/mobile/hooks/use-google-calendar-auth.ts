import { useEffect, useState } from 'react';

import { GoogleSignin, statusCodes } from '@react-native-google-signin/google-signin';

import {
  MISSING_GOOGLE_CLIENT_ID_MESSAGE,
  getGoogleCalendarAuthConfig,
} from '@/hooks/google-calendar-auth-config';
import {
  clearGoogleCalendarSession,
  loadGoogleCalendarSession,
  saveGoogleCalendarSession,
  type GoogleCalendarSession,
} from '@/storage/auth-storage';

const PERMISSION_DENIED_MESSAGE =
  'Google Calendar permission was denied. Hive Maps cannot access your schedule unless you approve the request.';
const GOOGLE_SIGN_IN_MISCONFIGURED_MESSAGE =
  'Google Sign-In is misconfigured. Confirm the Android OAuth client matches com.anonymous.mobile and your app signing SHA-1, set EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID to the OAuth Web client id, then rebuild the app.';

type AuthStatus = 'idle' | 'loading' | 'prompting' | 'connecting' | 'connected' | 'error';

GoogleSignin.configure(getGoogleCalendarAuthConfig().configureOptions);

type GoogleTokens = {
  accessToken: string;
  idToken?: string | null;
};

type GoogleUserData = {
  scopes: string[];
  user: {
    email?: string | null;
    name?: string | null;
  };
};

function buildGoogleCalendarSession(data: GoogleUserData, tokens: GoogleTokens): GoogleCalendarSession {
  return {
    accessToken: tokens.accessToken,
    idToken: tokens.idToken,
    scope: data.scopes.join(' '),
    obtainedAt: Date.now(),
    email: data.user.email,
    name: data.user.name,
  };
}

function getErrorStatus(currentSession: GoogleCalendarSession | null): AuthStatus {
  return currentSession ? 'connected' : 'error';
}

function getGoogleSignInErrorMessage(signInError: unknown) {
  const code =
    typeof signInError === 'object' && signInError && 'code' in signInError
      ? String(signInError.code)
      : '';
  const message = signInError instanceof Error ? signInError.message : '';
  const normalizedError = `${code} ${message}`.toUpperCase();

  if (code === statusCodes.SIGN_IN_CANCELLED) {
    return PERMISSION_DENIED_MESSAGE;
  }

  if (code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
    return 'Google Play Services is not available on this device.';
  }

  if (code === statusCodes.IN_PROGRESS) {
    return 'Google sign-in is already in progress.';
  }

  const isMisconfiguredError =
    normalizedError.includes('DEVELOPER_ERROR') ||
    normalizedError.includes('SIGN_IN_FAILED') ||
    normalizedError.includes('NON-RECOVERABLE');

  if (isMisconfiguredError) {
    return GOOGLE_SIGN_IN_MISCONFIGURED_MESSAGE;
  }

  return signInError instanceof Error
    ? signInError.message
    : 'Google sign-in failed while securing your session.';
}

export function useGoogleCalendarAuth() {
  const [session, setSession] = useState<GoogleCalendarSession | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    (async () => {
      const storedSession = await loadGoogleCalendarSession();
      if (!mounted) return;

      setSession(storedSession);

      try {
        const silentSignIn = await GoogleSignin.signInSilently();
        if (!mounted) return;

        if (silentSignIn.type === 'success') {
          const tokens = await GoogleSignin.getTokens();
          if (!mounted) return;

          const nextSession = buildGoogleCalendarSession(silentSignIn.data, tokens);

          await saveGoogleCalendarSession(nextSession);
          if (!mounted) return;

          setSession(nextSession);
          setStatus('connected');
          return;
        }
      } catch {
        if (!mounted) return;
      }

      await clearGoogleCalendarSession();
      if (!mounted) return;

      setSession(null);
      setStatus('idle');
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const connect = async () => {
    const authConfig = getGoogleCalendarAuthConfig();

    if (!authConfig.isConfigured) {
      setError(authConfig.errorMessage ?? MISSING_GOOGLE_CLIENT_ID_MESSAGE);
      setStatus(getErrorStatus(session));
      return;
    }

    setError(null);
    setStatus('prompting');

    try {
      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
      setStatus('connecting');

      const signInResult = await GoogleSignin.signIn();
      if (signInResult.type === 'cancelled') {
        setError(PERMISSION_DENIED_MESSAGE);
        setStatus(getErrorStatus(session));
        return;
      }

      const tokens = await GoogleSignin.getTokens();
      const nextSession = buildGoogleCalendarSession(signInResult.data, tokens);

      await saveGoogleCalendarSession(nextSession);
      setSession(nextSession);
      setStatus('connected');
    } catch (signInError: unknown) {
      setError(getGoogleSignInErrorMessage(signInError));
      setStatus(getErrorStatus(session));
    }
  };

  const disconnect = async () => {
    try {
      await GoogleSignin.revokeAccess();
      await GoogleSignin.signOut();
    } catch {
      /* ignore revoke failures and clear the local session anyway */
    }

    await clearGoogleCalendarSession();
    setSession(null);
    setError(null);
    setStatus('idle');
  };

  return {
    connect,
    disconnect,
    error,
    isConfigured: getGoogleCalendarAuthConfig().isConfigured,
    isReady: true,
    session,
    status,
  };
}
