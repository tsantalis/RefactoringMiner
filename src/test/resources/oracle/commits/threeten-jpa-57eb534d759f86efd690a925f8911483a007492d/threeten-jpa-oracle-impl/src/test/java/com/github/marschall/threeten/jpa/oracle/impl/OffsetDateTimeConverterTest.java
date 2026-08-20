package com.github.marschall.threeten.jpa.oracle.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import oracle.sql.TIMESTAMPTZ;

public class OffsetDateTimeConverterTest {

  @Test
  public void convertToDatabaseColumnPositiveOffset() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    OffsetDateTime attribute = OffsetDateTime.of(date, time, offset);

    TIMESTAMPTZ timestamptz = TimestamptzConverter.offsetDateTimeToTimestamptz(attribute);
    byte[] actual = timestamptz.toBytes();
    byte[] expected = new byte[]{119, -59, 1, 31, 8, 27, 57, 39, 86, -51, 0, 22, 60};
    assertArrayEquals(expected, actual);
  }

  @Test
  public void convertToDatabaseColumnNegativeOffset() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(-2, -30);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    OffsetDateTime attribute = OffsetDateTime.of(date, time, offset);

    TIMESTAMPTZ timestamptz = TimestamptzConverter.offsetDateTimeToTimestamptz(attribute);
    byte[] actual = timestamptz.toBytes();
    byte[] expected = new byte[]{119, -59, 1, 31, 12, 57, 57, 39, 86, -51, 0, 18, 30};
    assertArrayEquals(expected, actual);
  }

  @Test
  public void convertToDatabaseColumnJulianDate() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(11, 11);
    LocalDate date = LocalDate.of(1111, 11, 11);
    LocalTime time = LocalTime.of(11, 11, 11, 110_000_000);
    OffsetDateTime attribute = OffsetDateTime.of(date, time, offset);

    TIMESTAMPTZ timestamptz = TimestamptzConverter.offsetDateTimeToTimestamptz(attribute);
    byte[] actual = timestamptz.toBytes();
    byte[] expected = new byte[]{111, 111, 11, 11, 1, 1, 12, 6, -114, 119, -128, 31, 71};
    assertArrayEquals(expected, actual);
  }

  @Test
  public void convertToEntityAttributePositiveOffset() {
    byte[] timestamptz = new byte[]{119, -59, 1, 31, 8, 27, 57, 39, 86, -51, 0, 22, 60};
    TIMESTAMPTZ dbData = new TIMESTAMPTZ(timestamptz);

    ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    OffsetDateTime exptected = OffsetDateTime.of(date, time, offset);
    OffsetDateTime actual = TimestamptzConverter.timestamptzToOffsetDateTime(dbData);
    assertEquals(exptected, actual);
  }

  @Test
  public void convertToEntityAttributeNegativeOffset() {
    byte[] timestamptz = new byte[]{119, -59, 1, 31, 12, 57, 57, 39, 86, -51, 0, 18, 30};
    TIMESTAMPTZ dbData = new TIMESTAMPTZ(timestamptz);

    ZoneOffset offset = ZoneOffset.ofHoursMinutes(-2, -30);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 660000000);
    OffsetDateTime exptected = OffsetDateTime.of(date, time, offset);
    OffsetDateTime actual = TimestamptzConverter.timestamptzToOffsetDateTime(dbData);
    assertEquals(exptected, actual);
  }

  @Test
  public void convertToEntityAttributeNamedZone() {
    byte[] timestamptz = new byte[]{119, -57, 1, 15, 17, 1, 1, 0, 0, 0, 0, -119, -100};
    TIMESTAMPTZ dbData = new TIMESTAMPTZ(timestamptz);

    ZoneOffset offset = ZoneOffset.ofHours(-8);
    LocalDate date = LocalDate.of(1999, 1, 15);
    LocalTime time = LocalTime.of(8, 0, 0);
    OffsetDateTime exptected = OffsetDateTime.of(date, time, offset);
    OffsetDateTime actual = TimestamptzConverter.timestamptzToOffsetDateTime(dbData);
    assertEquals(exptected, actual);
  }

  @Test
  public void convertToEntityAttributeJulianDate() {
    byte[] timestamptz = new byte[]{111, 111, 11, 11, 1, 1, 12, 6, -114, 119, -128, 31, 71};
    TIMESTAMPTZ dbData = new TIMESTAMPTZ(timestamptz);

    ZoneOffset offset = ZoneOffset.ofHoursMinutes(11, 11);
    LocalDate date = LocalDate.of(1111, 11, 11);
    LocalTime time = LocalTime.of(11, 11, 11, 110_000_000);
    OffsetDateTime exptected = OffsetDateTime.of(date, time, offset);
    OffsetDateTime actual = TimestamptzConverter.timestamptzToOffsetDateTime(dbData);
    assertEquals(exptected, actual);
  }

}
