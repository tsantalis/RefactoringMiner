package com.github.marschall.threeten.jpa.mssql.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import microsoft.sql.DateTimeOffset;

public class DateTimeOffsetConverterTest {

  @Test
  public void positiveOffset() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 123456700);
    OffsetDateTime attribute = OffsetDateTime.of(date, time, offset);

    DateTimeOffset dbData = DateTimeOffsetConverter.offsetDateTimeToDateTimeOffset(attribute);

    assertEquals("1997-01-31 09:26:56.1234567 +02:00", dbData.toString());
    assertEquals(attribute, DateTimeOffsetConverter.dateTimeOffsetToOffsetDateTime(dbData));
  }

  @Test
  public void negativeOffset() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(-2, -30);
    LocalDate date = LocalDate.of(1997, 1, 31);
    LocalTime time = LocalTime.of(9, 26, 56, 123456700);
    OffsetDateTime attribute = OffsetDateTime.of(date, time, offset);

    DateTimeOffset dbData = DateTimeOffsetConverter.offsetDateTimeToDateTimeOffset(attribute);

    assertEquals("1997-01-31 09:26:56.1234567 -02:30", dbData.toString());
    assertEquals(attribute, DateTimeOffsetConverter.dateTimeOffsetToOffsetDateTime(dbData));
  }

  @Test
  @Disabled
  public void julanCalendar() {
    ZoneOffset offset = ZoneOffset.ofHoursMinutes(11, 11);
    LocalDate date = LocalDate.of(1111, 11, 11);
    LocalTime time = LocalTime.of(11, 11, 11, 111111100);
    OffsetDateTime attribute = OffsetDateTime.of(date, time, offset);

    DateTimeOffset dbData = DateTimeOffsetConverter.offsetDateTimeToDateTimeOffset(attribute);

    assertEquals("1111-11-11 11:11:11.1111111 +11:11", dbData.toString());
    assertEquals(attribute, DateTimeOffsetConverter.dateTimeOffsetToOffsetDateTime(dbData));
  }

}
