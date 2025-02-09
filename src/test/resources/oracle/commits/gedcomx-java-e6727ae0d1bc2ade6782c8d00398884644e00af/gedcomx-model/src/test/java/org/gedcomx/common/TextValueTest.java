package org.gedcomx.common;

import org.junit.Test;

import static org.junit.Assert.*;


public class TextValueTest {
  @Test
  public void testDefaultCtor() throws Exception {
    TextValue literal = new TextValue();
    literal.setLang("en-US");
    literal.setValue("value");

    assertEquals(literal.getLang(), "en-US");
    assertEquals(literal.getValue(), "value");
  }

  @Test
  public void testValueCtor() throws Exception {
    TextValue literal = new TextValue("value");

    assertNull(literal.getLang());
    assertEquals(literal.getValue(), "value");
  }

  @Test
  public void testEqualsAndHash() throws Exception {
    TextValue literal1 = new TextValue("value");
    literal1.setLang("lang");
    TextValue literal2 = new TextValue("value");
    literal2.setLang("lang");
    TextValue literal3 = new TextValue("not-matching");
    TextValue literal4 = new TextValue("not-matching");
    literal4.setLang("lang");

    assertTrue(literal1.equals(literal1));
    assertFalse(literal1.equals(null));
    assertTrue(literal1.equals(literal1));
    assertTrue(literal1.equals(literal2));
    assertEquals(literal1.hashCode(), literal2.hashCode());
    assertFalse(literal1.equals(literal3));
    assertFalse(literal1.equals(literal4));

    assertTrue(literal1.toString().contains("value"));
    assertTrue(literal2.toString().contains("lang"));
  }
}
