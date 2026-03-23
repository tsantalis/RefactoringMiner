import { Text, View, StyleSheet, Pressable, ActivityIndicator, Image, ScrollView } from 'react-native';
import React, { useCallback, useRef } from 'react';
import { useFocusEffect } from 'expo-router';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { useCalendar } from '../../context/CalendarContext';
import SignInGoogle from '@/components/SignInGoogle';

type ThemeOption = 'light' | 'dark' | 'system';

export default function SettingsScreen() {
  const { theme, setTheme, colorScheme } = useTheme();
  const { user, isLoading, signOut, getAccessToken } = useAuth();
  const { calendars, selectedCalendarId, isLoadingCalendars, selectCalendar, clearCalendars, fetchCalendars } = useCalendar();
  const isDark = colorScheme === 'dark';

  // Use refs to keep closures pointed at the latest functions.
  const getAccessTokenRef = useRef(getAccessToken);
  getAccessTokenRef.current = getAccessToken;
  const fetchCalendarsRef = useRef(fetchCalendars);
  fetchCalendarsRef.current = fetchCalendars;

  // Refresh calendar list when screen comes into focus.
  // Use user?.id as dependency (not the whole user object) to avoid re-triggering
  // when the access token refreshes.
  useFocusEffect(
    useCallback(() => {
      if (!user) return;
      (async () => {
        try {
          const token = await getAccessTokenRef.current();
          if (token) {
            await fetchCalendarsRef.current(token);
          }
        } catch (e) {
          console.error('Background calendar refresh failed:', e);
        }
      })();
    }, [user?.id])
  );

  const handleSignOut = async () => {
    try {
      await clearCalendars();
      await signOut();
    } catch (error) {
      console.error('Failed to sign out:', error);
    }
  };

  const handleCalendarSelect = async (calendarId: string) => {
    // Toggle off if already selected, otherwise select the new one
    await selectCalendar(selectedCalendarId === calendarId ? null : calendarId);
  };

  const themeOptions: { label: string; value: ThemeOption }[] = [
    { label: 'Light', value: 'light' },
    { label: 'Dark', value: 'dark' },
    { label: 'System', value: 'system' },
  ];

  return (
    <ScrollView style={[styles.container, { backgroundColor: isDark ? '#000000' : '#f2f2f7' }]}>
      <Text style={[styles.sectionTitle, { color: isDark ? '#8e8e93' : '#6e6e73' }]}>
        Appearance
      </Text>
      <View style={[styles.optionsContainer, { backgroundColor: isDark ? '#1c1c1e' : '#ffffff' }]}>
        {themeOptions.map((option, index) => (
          <Pressable
            key={option.value}
            style={[
              styles.option,
              index < themeOptions.length - 1 && styles.optionBorder,
              { borderBottomColor: isDark ? '#38383a' : '#e5e5ea' },
            ]}
            onPress={() => setTheme(option.value)}
          >
            <Text style={[styles.optionText, { color: isDark ? '#ffffff' : '#000000' }]}>
              {option.label}
            </Text>
            {theme === option.value && (
              <Text style={styles.checkmark}>✓</Text>
            )}
          </Pressable>
        ))}
      </View>

      <Text style={[styles.sectionTitle, { color: isDark ? '#8e8e93' : '#6e6e73', marginTop: 30 }]}>
        Account
      </Text>
      
      {isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator />
        </View>
      ) : user ? (
        <View>
          <View style={[styles.optionsContainer, { backgroundColor: isDark ? '#1c1c1e' : '#ffffff' }]}>
            <View style={styles.userInfoContainer}>
              {user.photo && (
                <Image 
                  source={{ uri: user.photo }} 
                  style={styles.profileImage}
                />
              )}
              <View style={styles.userTextContainer}>
              <Text style={[styles.userName, { color: isDark ? '#ffffff' : '#000000' }]}>
                {user.name}
              </Text>
              <Text style={[styles.userEmail, { color: isDark ? '#8e8e93' : '#6e6e73' }]}>
                {user.email}
              </Text>
              </View>
            </View>
          </View>
          
          {/* Calendars section */}
          <Text style={[styles.sectionTitle, { color: isDark ? '#8e8e93' : '#6e6e73', marginTop: 30 }]}>
            Calendars
          </Text>
          <View style={[styles.optionsContainer, { backgroundColor: isDark ? '#1c1c1e' : '#ffffff' }]}>
            {isLoadingCalendars && calendars.length === 0 ? (
              // Only block the UI with a spinner on first load when there is no
              // cached data yet. Background refreshes are silent.
              <View style={styles.loadingContainer}>
                <ActivityIndicator />
              </View>
            ) : calendars.length === 0 ? (
              <View style={styles.option}>
                <Text style={[styles.optionText, { color: isDark ? '#8e8e93' : '#6e6e73' }]}>
                  No calendars found
                </Text>
              </View>
            ) : (
              calendars.map((cal, index) => (
                <Pressable
                  key={cal.id}
                  testID={`calendar-item-${cal.id}`}
                  style={[
                    styles.option,
                    index < calendars.length - 1 && styles.optionBorder,
                    { borderBottomColor: isDark ? '#38383a' : '#e5e5ea' },
                  ]}
                  onPress={() => handleCalendarSelect(cal.id)}
                >
                  <View style={styles.checkboxRow}>
                    <View
                      style={[
                        styles.checkbox,
                        {
                          borderColor: isDark ? '#636366' : '#c7c7cc',
                          backgroundColor:
                            selectedCalendarId === cal.id
                              ? '#912338'
                              : 'transparent',
                        },
                      ]}
                    >
                      {selectedCalendarId === cal.id && (
                        <Text style={styles.checkboxTick}>✓</Text>
                      )}
                    </View>
                    <Text
                      style={[
                        styles.optionText,
                        { color: isDark ? '#ffffff' : '#000000', marginLeft: 12 },
                      ]}
                    >
                      {cal.summary}
                    </Text>
                  </View>
                </Pressable>
              ))
            )}
          </View>

          <View style={[styles.optionsContainer, { backgroundColor: isDark ? '#1c1c1e' : '#ffffff', marginTop: 16, marginBottom: 40 }]}>
            <Pressable
              style={styles.option}
              onPress={handleSignOut}
            >
              <Text style={[styles.optionText, { color: '#ff3b30' }]}>
                Log Out
              </Text>
            </Pressable>
          </View>
        </View>
      ) : (
        <SignInGoogle />
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 20,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '500',
    textTransform: 'uppercase',
    marginLeft: 16,
    marginBottom: 8,
  },
  optionsContainer: {
    marginHorizontal: 16,
    borderRadius: 10,
    overflow: 'hidden',
  },
  option: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  optionBorder: {
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  optionText: {
    fontSize: 17,
  },
  checkmark: {
    fontSize: 17,
    color: '#007aff',
  },
  loadingContainer: {
    padding: 20,
    alignItems: 'center',
  },
  userInfoContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    gap: 12,
  },
  profileImage: {
    width: 56,
    height: 56,
    borderRadius: 28,
  },
  userTextContainer: {
    flex: 1,
  },
  userName: {
    fontSize: 17,
    fontWeight: '600',
    marginBottom: 4,
  },
  userEmail: {
    fontSize: 15,
  },
  checkboxRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 4,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxTick: {
    color: '#ffffff',
    fontSize: 13,
    fontWeight: '700',
  },
});
