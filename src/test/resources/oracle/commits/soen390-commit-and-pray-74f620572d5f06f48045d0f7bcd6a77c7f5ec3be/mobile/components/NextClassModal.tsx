import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import { useTheme } from '../context/ThemeContext';
import { ParsedNextClass, NextClassStatus, NO_CLASS_BEHAVIOR } from '../hooks/useNextClass';
import { DEV_OVERRIDE_TIME } from '../utils/devConfig';
import { styles } from '../styles/nextClassModal.styles';

const TIMEOUT_MS = 30_000; // 30 seconds, how often the "in X mins" counter updates

interface NextClassModalProps {
  nextClass: ParsedNextClass | null;
  status: NextClassStatus;
  isLoading: boolean;
}

// Helpers
function getNow(): Date {
  return DEV_OVERRIDE_TIME ? new Date(DEV_OVERRIDE_TIME) : new Date();
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
}

function getMinutesUntil(target: Date): number {
  const diffMs = target.getTime() - getNow().getTime();
  return Math.max(0, Math.round(diffMs / 60000));
}

// New function to have time in format "HHhMMm" or "MMm"
function formattedTimeUntil(minutes: number): string {
  if (minutes <= 0) return '0 min'; // Only applicable to walking time
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h${m}m`;
}

// End of helpers

export default function NextClassModal({ nextClass, status, isLoading }: NextClassModalProps) {
  const { colorScheme } = useTheme();
  const isDark = colorScheme === 'dark';

  // Live "in X mins" counter. Updates every 30s
  const [minutesUntil, setMinutesUntil] = useState<number>(
    nextClass ? getMinutesUntil(nextClass.startTime) : 0,
  );

  useEffect(() => {
    if (!nextClass) return;
    setMinutesUntil(getMinutesUntil(nextClass.startTime));

    const id = setInterval(() => {
      setMinutesUntil(getMinutesUntil(nextClass.startTime));
    }, TIMEOUT_MS);

    return () => clearInterval(id);
  }, [nextClass]);

  // Loading state
  if (isLoading || status === 'loading') {
    return (
      <View style={[styles.card, isDark ? styles.cardDark : styles.cardLight]}>
        <ActivityIndicator color="#922338" style={{ marginVertical: 12 }} />
      </View>
    );
  }

  // No calendar selected state
  if (status === 'no_calendar') {
    return null; // Silent. User simply hasn't picked a calendar yet
  }

  // School day finished state
  if (status === 'done_today') {
    return (
      <View style={[styles.card, isDark ? styles.cardDark : styles.cardLight]}>
        <View style={styles.doneRow}>
          <Ionicons name="checkmark-circle" size={22} color="#16a34a" />
          <Text style={[styles.doneText, isDark ? styles.textDark : styles.textMain]}>
            School day finished. See you tomorrow!
          </Text>
        </View>
      </View>
    );
  }

  // No classes today state
  if (status === 'no_class') {
    if (NO_CLASS_BEHAVIOR === 'hide') return null;
    return (
      <View style={[styles.card, isDark ? styles.cardDark : styles.cardLight]}>
        <View style={styles.doneRow}>
          <Ionicons name="calendar-outline" size={22} color="#6B7280" />
          <Text style={[styles.doneText, isDark ? styles.textDark : styles.textMain]}>
            No classes today
          </Text>
        </View>
      </View>
    );
  }

  // Error state
  if (status === 'error' || !nextClass) {
    return null;
  }

  // Next class found
  const buildingLabel = nextClass.buildingCode || '?';
  const roomLabel = nextClass.room
    ? `${nextClass.buildingCode}-${nextClass.room}`
    : nextClass.buildingCode;

  return (
    <View style={[styles.card, isDark ? styles.cardDark : styles.cardLight]}>

      <View style={styles.topSection}>

        {/* Left column: building badge + navigate icon */}
        <View style={styles.leftCol}>
          <View style={styles.buildingBadge}>
            <Text style={styles.buildingBadgeText} numberOfLines={1} adjustsFontSizeToFit>
              {buildingLabel}
            </Text>
          </View>
          <View style={styles.badgeIconSpacer}>
            <Ionicons name="navigate-circle-outline" size={26} color="#922338" />
          </View>
        </View>

        {/* Right column: labels + class info */}
        <View style={styles.rightCol}>

          {/* "NEXT CLASS"  ·  "In X mins" */}
          <View style={styles.headerRow}>
            <Text style={[styles.labelText, isDark ? styles.textMutedDark : styles.textMuted]}>
              NEXT CLASS
            </Text>
            <Text style={[styles.countdownText, isDark ? styles.textMutedDark : styles.textMuted]}>
              {minutesUntil <= 0 ? 'Starting now' : `In ${formattedTimeUntil(minutesUntil)}`}
            </Text>
          </View>

          {/* Class name */}
          <Text
            style={[styles.className, isDark ? styles.textDark : styles.textMain]}
            numberOfLines={1}
          >
            {nextClass.title || 'Unknown Class'}
          </Text>

          {/* Room + time range */}
          <View style={styles.detailRow}>
            <Text style={[styles.roomText, isDark ? styles.textMutedDark : styles.textMuted]}>
              {roomLabel}
            </Text>
            <Ionicons
              name="time-outline"
              size={13}
              color={isDark ? '#9CA3AF' : '#6B7280'}
              style={{ marginLeft: 8, marginRight: 3 }}
            />
            <Text style={[styles.timeText, isDark ? styles.textMutedDark : styles.textMuted]}>
              {formatTime(nextClass.startTime)} – {formatTime(nextClass.endTime)}
            </Text>
          </View>

        </View>
      </View>

      <View style={[styles.divider, isDark ? styles.dividerDark : styles.dividerLight]} />

      {/* Bottom section: walk time + button*/}
      <View style={styles.bottomSection}>
        <View style={styles.walkTimeRow}>
          <Ionicons name="walk-outline" size={16} color={isDark ? '#D1D5DB' : '#374151'} />
          <Text style={[styles.walkText, isDark ? styles.textDark : styles.textMain]}>
            {nextClass.walkingMinutes != null
              ? ` ${formattedTimeUntil(nextClass.walkingMinutes)} walk`
              : ' Walk time unavailable'}
          </Text>
        </View>

        {/* Get Directions: placeholder, no functionality yet */}
        <TouchableOpacity
          style={styles.directionsButton}
          activeOpacity={0.8}
          onPress={() => {
            // TODO: implement indoor/outdoor navigation to next class
          }}
        >
          <Ionicons name="navigate" size={14} color="#FFFFFF" style={{ marginRight: 5 }} />
          <Text style={styles.directionsButtonText}>Get Directions</Text>
        </TouchableOpacity>
      </View>

    </View>
  );
}
