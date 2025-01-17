package com.sanction.thunder.util;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class EmailUtilitiesTest {
  private static final String URL_PLACEHOLDER = "CODEGEN-URL";

  @Test
  public void testReplacePlaceholderNoUrl() {
    String contents = "test contents";
    String url = "http://www.test.com";

    Assertions.assertEquals(contents, EmailUtilities.replaceUrlPlaceholder(contents, url));
  }

  @Test
  public void testReplacePlaceholderWithUrl() {
    String contents = "test contents " + URL_PLACEHOLDER;
    String url = "http://www.test.com";

    String expected = "test contents " + url;

    Assertions.assertEquals(expected, EmailUtilities.replaceUrlPlaceholder(contents, url));
  }
}
