package com.sanction.thunder.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmailUtilitiesTest {
  private static final String URL_PLACEHOLDER = "CODEGEN-URL";

  @Test
  public void testReplacePlaceholderNoUrl() {
    String contents = "test contents";
    String url = "http://www.test.com";

    assertEquals(contents, EmailUtilities.replaceUrlPlaceholder(contents, url));
  }

  @Test
  public void testReplacePlaceholderWithUrl() {
    String contents = "test contents " + URL_PLACEHOLDER;
    String url = "http://www.test.com";

    String expected = "test contents " + url;

    assertEquals(expected, EmailUtilities.replaceUrlPlaceholder(contents, url));
  }
}
