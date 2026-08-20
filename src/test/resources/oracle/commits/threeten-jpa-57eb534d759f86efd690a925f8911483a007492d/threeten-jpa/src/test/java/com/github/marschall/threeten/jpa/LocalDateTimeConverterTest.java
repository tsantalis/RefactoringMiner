package com.github.marschall.threeten.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Calendar;

import org.junit.jupiter.api.Test;

public class LocalDateTimeConverterTest {

  @Test
  public void secondSet() {
    LocalDateTime attribute = LocalDateTime.of(1960, 1, 1, 23, 3, 20);
    Timestamp databaseColumn = new LocalDateTimeConverter().convertToDatabaseColumn(attribute);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(databaseColumn);

    assertEquals(1960, calendar.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH));
    assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));
    assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
    assertEquals(3, calendar.get(Calendar.MINUTE));
    assertEquals(20, calendar.get(Calendar.SECOND));
  }

}
