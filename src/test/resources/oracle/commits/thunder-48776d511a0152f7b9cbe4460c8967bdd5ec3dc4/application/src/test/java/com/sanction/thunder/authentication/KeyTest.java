package com.sanction.thunder.authentication;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class KeyTest {

  @Test
  public void testHashCodeSame() {
    Key keyOne = new Key("name", "secret");
    Key keyTwo = new Key("name", "secret");

    Assertions.assertAll("Assert equal key properties",
        () -> Assertions.assertEquals(keyOne.hashCode(), keyTwo.hashCode()),
        () -> Assertions.assertEquals(keyOne.getName(), keyTwo.getName()),
        () -> Assertions.assertEquals(keyOne.getSecret(), keyTwo.getSecret()));
  }

  @Test
  public void testHashCodeDifferent() {
    Key keyOne = new Key("name", "secret");
    Key keyTwo = new Key("differentName", "differentSecret");

    Assertions.assertAll("Assert unequal key properties",
        () -> Assertions.assertNotEquals(keyOne.hashCode(), keyTwo.hashCode()),
        () -> Assertions.assertNotEquals(keyOne.getName(), keyTwo.getName()),
        () -> Assertions.assertNotEquals(keyOne.getSecret(), keyTwo.getSecret()));
  }

  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "EqualsWithItself"})
  public void testEqualsSameObject() {
    Key keyOne = new Key("name", "secret");

    Assertions.assertTrue(() -> keyOne.equals(keyOne));
  }

  @Test
  @SuppressWarnings("SimplifiableJUnitAssertion")
  public void testEqualsDifferentObject() {
    Key keyOne = new Key("name", "secret");
    Object objectTwo = new Object();

    Assertions.assertFalse(() -> keyOne.equals(objectTwo));
  }

  @Test
  public void testToString() {
    Key key = new Key("testKey", "testSecret");
    String expected = "Key [name=testKey, secret=testSecret]";

    Assertions.assertEquals(expected, key.toString());
  }
}
