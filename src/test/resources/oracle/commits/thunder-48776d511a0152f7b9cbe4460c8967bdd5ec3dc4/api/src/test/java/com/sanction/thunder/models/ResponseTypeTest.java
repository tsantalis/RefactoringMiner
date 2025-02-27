package com.sanction.thunder.models;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class ResponseTypeTest {

  @Test
  public void testJsonResponseType() {
    Assertions.assertAll("Assert equal JSON response type.",
        () -> Assertions.assertEquals(ResponseType.JSON, ResponseType.fromString("json")),
        () -> Assertions.assertEquals("json", ResponseType.JSON.toString()));
  }

  @Test
  public void testHtmlResponseType() {
    Assertions.assertAll("Assert equal HTML response type.",
        () -> Assertions.assertEquals(ResponseType.HTML, ResponseType.fromString("html")),
        () -> Assertions.assertEquals("html", ResponseType.HTML.toString()));
  }

  @Test
  public void testNullResponseTypeFromString() {
    Assertions.assertNull(ResponseType.fromString("unknown"));
  }
}
