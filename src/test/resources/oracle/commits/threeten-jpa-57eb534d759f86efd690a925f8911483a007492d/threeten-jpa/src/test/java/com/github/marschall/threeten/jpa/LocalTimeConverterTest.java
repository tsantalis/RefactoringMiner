package com.github.marschall.threeten.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Calendar;

import org.junit.jupiter.api.Test;

public class LocalTimeConverterTest {

  @Test
  public void secondSet() {
    LocalTime attribute = LocalTime.of(23, 3, 20);
    Time databaseColumn = new LocalTimeConverter().convertToDatabaseColumn(attribute);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(databaseColumn);

    assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
    assertEquals(3, calendar.get(Calendar.MINUTE));
    assertEquals(20, calendar.get(Calendar.SECOND));
  }

}
