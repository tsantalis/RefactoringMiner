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

          const nextSession: GoogleCalendarSession = {
            accessToken: tokens.accessToken,
            idToken: tokens.idToken,
            scope: silentSignIn.data.scopes.join(' '),
            obtainedAt: Date.now(),
            email: silentSignIn.data.user.email,
            name: silentSignIn.data.user.name,
          };

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
      setStatus(session ? 'connected' : 'error');
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
        setStatus(session ? 'connected' : 'error');
        return;
      }

      const tokens = await GoogleSignin.getTokens();
      const nextSession: GoogleCalendarSession = {
        accessToken: tokens.accessToken,
        idToken: tokens.idToken,
        scope: signInResult.data.scopes.join(' '),
        obtainedAt: Date.now(),
        email: signInResult.data.user.email,
        name: signInResult.data.user.name,
      };

      await saveGoogleCalendarSession(nextSession);
      setSession(nextSession);
      setStatus('connected');
    } catch (signInError: unknown) {
      const code =
        typeof signInError === 'object' && signInError && 'code' in signInError
          ? String(signInError.code)
          : '';
      const message = signInError instanceof Error ? signInError.message : '';
      const normalizedError = `${code} ${message}`.toUpperCase();

      if (code === statusCodes.SIGN_IN_CANCELLED) {
        setError(PERMISSION_DENIED_MESSAGE);
      } else if (code === statusCodes.PLAY_SERVICES_NOT_AVAILABLE) {
        setError('Google Play Services is not available on this device.');
      } else if (code === statusCodes.IN_PROGRESS) {
        setError('Google sign-in is already in progress.');
      } else if (
        normalizedError.includes('DEVELOPER_ERROR') ||
        normalizedError.includes('SIGN_IN_FAILED') ||
        normalizedError.includes('NON-RECOVERABLE')
      ) {
        setError(GOOGLE_SIGN_IN_MISCONFIGURED_MESSAGE);
      } else {
        setError(
          signInError instanceof Error
            ? signInError.message
            : 'Google sign-in failed while securing your session.'
        );
      }

      setStatus(session ? 'connected' : 'error');
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
