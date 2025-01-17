package com.sanction.thunder.email;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmailModuleTest {
  private static final EmailConfiguration EMAIL_CONFIG = mock(EmailConfiguration.class);

  @BeforeAll
  public static void setup() {
    when(EMAIL_CONFIG.getEndpoint()).thenReturn("http://localhost:4567");
    when(EMAIL_CONFIG.getRegion()).thenReturn("us-east-1");
    when(EMAIL_CONFIG.getFromAddress()).thenReturn("test@test.com");
  }

  @Test
  public void testProvideSuccessHtmlDefault() throws IOException {
    when(EMAIL_CONFIG.getSuccessHtmlPath()).thenReturn(null);

    EmailModule emailModule = new EmailModule(EMAIL_CONFIG);

    String expected = Resources.toString(
        Resources.getResource("success.html"), Charsets.UTF_8);
    Assertions.assertEquals(expected, emailModule.provideSuccessHtml());
  }

  @Test
  public void testProvideSuccessHtmlCustom() throws Exception {
    when(EMAIL_CONFIG.getSuccessHtmlPath()).thenReturn(new File(
        Resources.getResource("fixtures/success-page.html").toURI()).getAbsolutePath());

    EmailModule emailModule = new EmailModule(EMAIL_CONFIG);

    String expected = Resources.toString(
        Resources.getResource("fixtures/success-page.html"), Charsets.UTF_8);
    Assertions.assertEquals(expected, emailModule.provideSuccessHtml());
  }

  @Test
  public void testProvideVerificationHtmlDefault() throws IOException {
    when(EMAIL_CONFIG.getVerificationHtmlPath()).thenReturn(null);

    EmailModule emailModule = new EmailModule(EMAIL_CONFIG);

    String expected = Resources.toString(
        Resources.getResource("verification.html"), Charsets.UTF_8);
    Assertions.assertEquals(expected, emailModule.provideVerificationHtml());
  }

  @Test
  public void testProvideVerificationHtmlCustom() throws Exception {
    when(EMAIL_CONFIG.getVerificationHtmlPath()).thenReturn(new File(
        Resources.getResource("fixtures/verification-email.html").toURI()).getAbsolutePath());

    EmailModule emailModule = new EmailModule(EMAIL_CONFIG);

    String expected = Resources.toString(
        Resources.getResource("fixtures/verification-email.html"), Charsets.UTF_8);
    Assertions.assertEquals(expected, emailModule.provideVerificationHtml());
  }

  @Test
  public void testProvideVerificationTextDefault() throws IOException {
    when(EMAIL_CONFIG.getVerificationTextPath()).thenReturn(null);

    EmailModule emailModule = new EmailModule(EMAIL_CONFIG);

    String expected = Resources.toString(
        Resources.getResource("verification.txt"), Charsets.UTF_8);
    Assertions.assertEquals(expected, emailModule.provideVerificationText());
  }

  @Test
  public void testProvideVerificationTextCustom() throws Exception {
    when(EMAIL_CONFIG.getVerificationTextPath()).thenReturn(new File(
        Resources.getResource("fixtures/verification-email.txt").toURI()).getAbsolutePath());

    EmailModule emailModule = new EmailModule(EMAIL_CONFIG);

    String expected = Resources.toString(
        Resources.getResource("fixtures/verification-email.txt"), Charsets.UTF_8);
    Assertions.assertEquals(expected, emailModule.provideVerificationText());
  }
}
