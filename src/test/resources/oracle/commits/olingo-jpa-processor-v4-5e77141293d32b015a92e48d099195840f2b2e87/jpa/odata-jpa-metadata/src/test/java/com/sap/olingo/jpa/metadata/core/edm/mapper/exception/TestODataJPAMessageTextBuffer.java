package com.sap.olingo.jpa.metadata.core.edm.mapper.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.processor.core.testmodel.LocaleEnumeration;

public class TestODataJPAMessageTextBuffer {
  private static String BUNDLE_NAME = "test-i18n";
  private ODataJPAMessageTextBuffer cut;

  @BeforeEach
  public void setup() {
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME);
  }

  @Test
  public void checkDefaultLocale() {
    assertEquals(ODataJPAMessageTextBuffer.DEFAULT_LOCALE.getLanguage(), cut.getLocale().getLanguage());
  }

  @Test
  public void checkSetLocaleGerman() {
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, Locale.GERMANY);
    assertEquals("de", cut.getLocale().getLanguage());
  }

  @Test
  public void checkSetLocaleReset() {
    // Set first to German
    checkSetLocaleGerman();
    // Then reset to default
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME);
    assertEquals(ODataJPAMessageTextBuffer.DEFAULT_LOCALE.getLanguage(), cut.getLocale().getLanguage());
  }

  @Test
  public void checkGetDefaultLocaleText() {
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("An English message", act);
  }

  @Test
  public void checkGetGermanText() {
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, Locale.GERMANY);
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("Ein deutscher Text", act);
  }

  // %1$s
  @Test
  public void checkGetTextWithParameter() {
    String act = cut.getText(this, "SECOND_MESSAGE", "Hugo", "Willi");
    assertEquals("Hugo looks for Willi", act);
  }

  @Test
  public void checkSetLocalesNull() {
    Enumeration<Locale> locales = null;
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, locales);
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("An English message", act);
  }

  @Test
  public void checkSetLocalesRestDefaultWithNull() {
    // First set to German
    checkSetLocaleGerman();
    // Then reset default
    Enumeration<Locale> locales = null;
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, locales);
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("An English message", act);
  }

  @Test
  public void checkSetLocalesRestDefaultWithEmpty() {
    // First set to German
    checkSetLocaleGerman();
    // Then reset default
    Enumeration<Locale> locales = new LocaleEnumeration(new ArrayList<Locale>());
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, locales);
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("An English message", act);
  }

  @Test
  public void checkSetLocalesFirstMatches() {

    ArrayList<Locale> localesList = new ArrayList<>();
    localesList.add(Locale.GERMAN);
    localesList.add(Locale.CANADA_FRENCH);
    Enumeration<Locale> locales = new LocaleEnumeration(localesList);
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, locales);
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("Ein deutscher Text", act);
  }

  @Test
  public void checkSetLocalesSecondMatches() {

    ArrayList<Locale> localesList = new ArrayList<>();
    localesList.add(Locale.CANADA_FRENCH);
    localesList.add(Locale.GERMAN);
    Enumeration<Locale> locales = new LocaleEnumeration(localesList);
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, locales);
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("Ein deutscher Text", act);
  }

  @Test
  public void checkSetLocalesNonMatches() {

    ArrayList<Locale> localesList = new ArrayList<>();
    localesList.add(Locale.CANADA_FRENCH);
    localesList.add(Locale.SIMPLIFIED_CHINESE);
    Enumeration<Locale> locales = new LocaleEnumeration(localesList);
    cut = new ODataJPAMessageTextBuffer(BUNDLE_NAME, locales);
    String act = cut.getText(this, "FIRST_MESSAGE");
    assertEquals("An English message", act);
  }
}
