package com.github.marschall.threeten.jpa.oracle.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class UuidConverterTest {

  @Test
  public void conversion() {
    UUID original = UUID.fromString("60734841-87a2-4ce3-a47b-0ce5b3259cc0");
    byte[] raw = UuidConverter.toRaw(original);
    UUID copy = UuidConverter.fromRaw(raw);
    assertEquals(original, copy);
  }

}
