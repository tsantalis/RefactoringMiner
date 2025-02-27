package com.sanction.thunder.authentication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class KeyTest {

  @Test
  public void testHashCodeSame() {
    Key keyOne = new Key("name", "secret");
    Key keyTwo = new Key("name", "secret");

    assertEquals(keyOne.hashCode(), keyTwo.hashCode());
    assertEquals(keyOne.getName(), keyTwo.getName());
    assertEquals(keyOne.getSecret(), keyTwo.getSecret());
  }

  @Test
  public void testHashCodeDifferent() {
    Key keyOne = new Key("name", "secret");
    Key keyTwo = new Key("differentName", "differentSecret");

    assertNotEquals(keyOne.hashCode(), keyTwo.hashCode());
    assertNotEquals(keyOne.getName(), keyTwo.getName());
    assertNotEquals(keyOne.getSecret(), keyTwo.getSecret());
  }

  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "EqualsWithItself"})
  public void testEqualsSameObject() {
    Key keyOne = new Key("name", "secret");

    assertTrue(keyOne.equals(keyOne));
  }

  @Test
  @SuppressWarnings("SimplifiableJUnitAssertion")
  public void testEqualsDifferentObject() {
    Key keyOne = new Key("name", "secret");
    Object objectTwo = new Object();

    assertFalse(keyOne.equals(objectTwo));
  }

  @Test
  public void testToString() {
    Key key = new Key("testKey", "testSecret");
    String expected = "Key [name=testKey, secret=testSecret]";

    assertEquals(expected, key.toString());
  }
}
