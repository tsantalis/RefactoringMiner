package com.github.marschall.threeten.jpa.oracle.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import oracle.sql.INTERVALDS;

public class DurationConverterTest {

  @Test
  public void convertAndBackAllFields() {
    Duration duration = Duration.parse("P" + "2D" + "T" + "3H" + "4M" + "20.345S");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void nanosOnly() {
    Duration duration = Duration.parse("P" + "T" + "0.123456789S");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void secondsOnly() {
    Duration duration = Duration.parse("P" + "T" + "20S");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void hoursOnly() {
    Duration duration = Duration.parse("P" + "T" + "3H");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void daysOnly() {
    Duration duration = Duration.parse("P" + "2D");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void secondsAndNanos() {
    Duration duration = Duration.parse("P" + "T" + "20.345S");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void secondsAndMinutes() {
    Duration duration = Duration.parse("P" + "T" + "4M" + "20S");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void secondsAndMinutesAndHours() {
    Duration duration = Duration.parse("P" + "T" + "3H" + "4M" + "20S");
    Duration converted = IntervalConverter.intervaldsToDuration(IntervalConverter.durationToIntervalds(duration));
    assertEquals(duration, converted);
  }

  @Test
  public void reference() {
    byte[] data = new byte[] {-128, 0, 0, 4, 65, 72, 70, -115, 59, 115, -128};
    Duration attribute = Duration.parse("P4DT5H12M10.222S");
    assertEquals(attribute, IntervalConverter.intervaldsToDuration(new INTERVALDS(data)));
    assertArrayEquals(data, IntervalConverter.durationToIntervalds(attribute).toBytes());
  }

  @Test
  public void negativeReference() {
    byte[] data = new byte[] {127, -1, -1, -4, 55, 48, 50, 114, -60, -116, -128};
    Duration attribute = Duration.parse("P4DT5H12M10.222S").negated();
    assertEquals(attribute, IntervalConverter.intervaldsToDuration(new INTERVALDS(data)));
    assertArrayEquals(data, IntervalConverter.durationToIntervalds(attribute).toBytes());
  }

}
