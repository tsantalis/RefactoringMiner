package com.github.marschall.threeten.jpa.oracle.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Period;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import oracle.sql.INTERVALYM;

public class PeriodConverterTest {

  @Test
  public void convertAndBack() {
    Period period = Period.of(100, 10, 0);
    Period converted = IntervalConverter.intervalymToPeriod(IntervalConverter.periodToIntervalym(period));
    assertEquals(period, converted);
  }

  @Test
  public void day() {
    Period period = Period.ofDays(1);
    try {
      IntervalConverter.periodToIntervalym(period);
      fail("days not allowed");
    } catch (IllegalArgumentException e) {
      // should reach here
    }
  }

  @Test
  public void truncation() {
    ZonedDateTime zoned = ZonedDateTime.parse("2017-01-31T11:52:47.971+01:00[Europe/Zurich]");
    assertEquals(ZonedDateTime.parse("2017-02-28T11:52:47.971+01:00[Europe/Zurich]"), zoned.plusMonths(1L));

    zoned = ZonedDateTime.parse("2016-02-29T11:52:47.971+01:00[Europe/Zurich]");
    assertEquals(ZonedDateTime.parse("2017-02-28T11:52:47.971+01:00[Europe/Zurich]"), zoned.plusMonths(12L));

    zoned = ZonedDateTime.parse("2016-02-29T11:52:47.971+01:00[Europe/Zurich]");
    assertEquals(ZonedDateTime.parse("2017-02-28T11:52:47.971+01:00[Europe/Zurich]"), zoned.plusYears(1L));
  }

  @Test
  public void reference() {
    byte[] data = new byte[] {-128, 0, 0, 123, 62};
    Period attribute = Period.of(123, 2, 0);
    assertEquals(attribute, IntervalConverter.intervalymToPeriod(new INTERVALYM(data)));
    assertArrayEquals(data, IntervalConverter.periodToIntervalym(attribute).toBytes());
  }

  @Test
  public void negativeReference() {
    byte[] data = new byte[] {127, -1, -1, -123, 58};
    Period attribute = Period.of(123, 2, 0).negated();
    assertEquals(attribute, IntervalConverter.intervalymToPeriod(new INTERVALYM(data)));
    assertArrayEquals(data, IntervalConverter.periodToIntervalym(attribute).toBytes());
  }

}
