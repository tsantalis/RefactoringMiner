package com.sanction.thunder.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResponseTypeTest {

  @Test
  public void testJsonResponseType() {
    assertEquals(ResponseType.JSON, ResponseType.fromString("json"));
    assertEquals("json", ResponseType.JSON.toString());
  }

  @Test
  public void testHtmlResponseType() {
    assertEquals(ResponseType.HTML, ResponseType.fromString("html"));
    assertEquals("html", ResponseType.HTML.toString());
  }

  @Test
  public void testNullResponseTypeFromString() {
    assertNull(ResponseType.fromString("unknown"));
  }
}
