package com.github.marschall.threeten.jpa.oracle.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import oracle.sql.TIMESTAMPTZ;
import oracle.sql.ZONEIDMAP;

public class ZonedDateTimeConverterTest {

  @Test
  public void convertToDatabaseColumnPositiveOffset() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    ZonedDateTime attribute = ZonedDateTime.of(date, time, offset);

    TIMESTAMPTZ timestamptz = TimestamptzConverter.zonedDateTimeToTimestamptz(attribute);
    byte[] actual = timestamptz.toBytes();
    byte[] expected = new byte[]{119, -59, 1, 31, 8, 27, 57, 39, 86, -51, 0, 22, 60};
    assertArrayEquals(expected, actual);
  }

  @Test
  public void convertToDatabaseColumnNegativeOffset() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(-2, -30);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    ZonedDateTime attribute = ZonedDateTime.of(date, time, offset);

    TIMESTAMPTZ timestamptz = TimestamptzConverter.zonedDateTimeToTimestamptz(attribute);
    byte[] actual = timestamptz.toBytes();
    byte[] expected = new byte[]{119, -59, 1, 31, 12, 57, 57, 39, 86, -51, 0, 18, 30};
    assertArrayEquals(expected, actual);
  }

  @Test
  public void convertToEntityAttributePositiveOffset() {
    byte[] timestamptz = new byte[]{119, -59, 1, 31, 8, 27, 57, 39, 86, -51, 0, 22, 60};
    TIMESTAMPTZ dbData = new TIMESTAMPTZ(timestamptz);

    ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    ZonedDateTime exptected = ZonedDateTime.of(date, time, offset);
    ZonedDateTime actual = TimestamptzConverter.timestamptzToZonedDateTime(dbData);
    assertEquals(exptected, actual);
  }

  @Test
  public void convertToEntityAttributeNegativeOffset() {
    byte[] timestamptz = new byte[]{119, -59, 1, 31, 12, 57, 57, 39, 86, -51, 0, 18, 30};
    TIMESTAMPTZ dbData = new TIMESTAMPTZ(timestamptz);

    ZoneOffset offset = ZoneOffset.ofHoursMinutes(-2, -30);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    ZonedDateTime exptected = ZonedDateTime.of(date, time, offset);
    ZonedDateTime actual = TimestamptzConverter.timestamptzToZonedDateTime(dbData);
    assertEquals(exptected, actual);
  }

  @Test
  public void convertToEntityAttributeNamedZone() {
    byte[] timestamptz = new byte[]{119, -57, 1, 15, 17, 1, 1, 0, 0, 0, 0, -119, -100};
    TIMESTAMPTZ dbData = new TIMESTAMPTZ(timestamptz);

    ZoneId zoneId = ZoneId.of("US/Pacific");
    LocalDate date = LocalDate.of(1999, 1, 15);
    LocalTime time = LocalTime.of(8, 0, 0);
    ZonedDateTime exptected = ZonedDateTime.of(date, time, zoneId);
    ZonedDateTime actual = TimestamptzConverter.timestamptzToZonedDateTime(dbData);
    assertEquals(exptected, actual);
  }

  @Test
  public void convertToEntityAttributeNamedZoneEuropeZurich() {
    int[] expected = new int[]{120, 115, 10, 24, 24, 46, 1, 0, 0, 0, 0, 134, 88};
    byte[] data = convert(expected);
    ZonedDateTime zonedDateTime = TimestamptzConverter.timestamptzToZonedDateTime(new TIMESTAMPTZ(data));
    assertNotNull(zonedDateTime);
  }

  @Test
  public void convertToDatabaseColumnEuropeZurich() {
    ZoneId zoneId = ZoneId.of("Europe/Zurich");
    ZonedDateTime zonedDateTime = LocalDateTime.of(LocalDate.of(2015, 10, 25), LocalTime.of(1, 45)).atZone(zoneId);
    byte[] bytes = TimestamptzConverter.zonedDateTimeToTimestamptz(zonedDateTime).toBytes();
    int[] expected = new int[]{120, 115, 10, 24, 24, 46, 1, 0, 0, 0, 0, 134, 88};
    assertEquals(expected.length, bytes.length);
    for (int i = 0; i < expected.length; i++) {
      int expectedValue = expected[i];
      int actualValue = bytes[i] & 0xFF;
      assertEquals(expectedValue, actualValue);
    }
  }

  @Test
  public void berlinConversion() {
    byte[] expected = new byte[] {120, 116, 9, 22, 16, 1, 1, 0, 0, 0, 0, -123, -4};

    ZoneId zoneId = ZoneId.of("Europe/Berlin");
    ZonedDateTime zonedDateTime = ZonedDateTime.of(2016, 9, 22, 17, 0, 0, 0, zoneId);
    byte[] actual = TimestamptzConverter.zonedDateTimeToTimestamptz(zonedDateTime).toBytes();
    
    assertArrayEquals(expected, actual);
  }

  @Test
  public void allZones() {
    LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2015, 10, 25), LocalTime.of(1, 45));
    IntStream.range(0, 8192)
      .mapToObj(ZONEIDMAP::getRegion)
      .filter(region -> region != null)
      .map(region -> {
        try {
          return ZoneId.of(region);
        } catch (ZoneRulesException e) {
          return null;
        }
      })
      .filter(zoneId -> zoneId != null)
      .forEach(zoneId -> {
        ZonedDateTime expected = localDateTime.atZone(zoneId);
        ZonedDateTime actual = TimestamptzConverter.timestamptzToZonedDateTime(TimestamptzConverter.zonedDateTimeToTimestamptz(expected));
        assertEquals(expected, actual);
      });
  }

  private static byte[] convert(int[] original) {
    byte[] result = new byte[original.length];
    for (int i = 0; i < original.length; i++) {
      result[i] = (byte) original[i];
    }
    return result;
  }

}
