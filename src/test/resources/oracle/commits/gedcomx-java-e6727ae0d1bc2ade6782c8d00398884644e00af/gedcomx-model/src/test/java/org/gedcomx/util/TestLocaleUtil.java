package org.gedcomx.util;

import org.junit.Test;
import org.gedcomx.common.TextValue;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Class for...
 * User: Randy Wilson
 * Date: 7/31/2014
 * Time: 12:03 PM
 */
public class TestLocaleUtil {

  @Test
  public void testTextValueLocales() {
    List<TextValue> list = Arrays.asList(
            value("Spanish", "es"),
            value("UK English", "en-GB"),
            value("US English", "en-US"),
            value("Korean", "ko"));
    assertEquals("Spanish", LocaleUtil.findClosestLocale(list, new Locale("es")).getValue());
    assertEquals("UK English", LocaleUtil.findClosestLocale(list, LocaleUtil.getSimpleLocale("en-GB")).getValue());
    assertEquals("UK English", LocaleUtil.findClosestLocale(list, LocaleUtil.getSimpleLocale("en")).getValue()); // first English in list
    assertEquals("US English", LocaleUtil.findClosestLocale(list, LocaleUtil.getSimpleLocale("en-US")).getValue()); // first English in list
    assertEquals("Korean", LocaleUtil.findClosestLocale(list, Locale.KOREAN).getValue()); //ko
    assertEquals("Korean", LocaleUtil.findClosestLocale(list, Locale.KOREA).getValue()); //ko-KR
    // Test a case where there is no good match to the preferred language, but there is to the default language.
    assertEquals("UK English", LocaleUtil.findClosestLocale(list, Locale.JAPANESE, Locale.CANADA).getValue());
    assertEquals("US English", LocaleUtil.findClosestLocale(list, Locale.JAPANESE, Locale.US).getValue());
  }

  private static TextValue value(String text, String lang) {
    TextValue v = new TextValue(text);
    v.setLang(lang);
    return v;
  }

  @Test
  public void testGetBasicLocale() {
    tryLocale("en", "en", "", "");
    tryLocale("en-us", "en", "US", "");
    tryLocale("en-US", "en", "US", "");
    tryLocale("en_us", "en", "US", "");
    tryLocale("en_US", "en", "US", "");
    tryLocale("en-latn-US", "en", "US", "");
    tryLocale("en-latn-US", "en", "US", "");
    tryLocale("ko-Hang-KR", "ko", "KR", "");
  }

  private void tryLocale(String orig, String expectedLanguage, String expectedCountry, String expectedVariant) {
    Locale locale = LocaleUtil.getSimpleLocale(orig);
    assertEquals(expectedLanguage, locale.getLanguage());
    assertEquals(expectedCountry, locale.getCountry());
    assertEquals(expectedVariant, locale.getVariant());
  }

  @Test
  public void testClosestLocale() {
    Set<Locale> list = new LinkedHashSet<Locale>(Arrays.asList(
            new Locale("es"),
            new Locale("en", "GB"),
            new Locale("en", "US"),
            new Locale("ko")));
    assertEquals("es", LocaleUtil.findClosestLocale(list, new Locale("es")).getLanguage());
    assertEquals("en_GB", LocaleUtil.findClosestLocale(list, new Locale("en", "GB")).toString());
    assertEquals("en_GB", LocaleUtil.findClosestLocale(list, new Locale("en")).toString()); // first English in list
    assertEquals("ko", LocaleUtil.findClosestLocale(list, Locale.KOREAN).getLanguage()); //ko
    assertEquals("ko", LocaleUtil.findClosestLocale(list, Locale.KOREA).toString()); //ko-KR
    // Test a case where there is no good match to the preferred language, but there is to the default language.
    assertEquals("en_GB", LocaleUtil.findClosestLocale(list, Locale.JAPANESE, Locale.CANADA).toString());
    assertEquals("en_US", LocaleUtil.findClosestLocale(list, Locale.JAPANESE, Locale.US).toString());
  }

}
