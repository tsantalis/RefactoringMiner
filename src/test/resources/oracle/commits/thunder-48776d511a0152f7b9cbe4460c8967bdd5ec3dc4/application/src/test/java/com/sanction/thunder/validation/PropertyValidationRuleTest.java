package com.sanction.thunder.validation;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class PropertyValidationRuleTest {

  @Test
  public void testHashCodeSame() {
    PropertyValidationRule ruleOne = new PropertyValidationRule("name", "string");
    PropertyValidationRule ruleTwo = new PropertyValidationRule("name", "string");

    Assertions.assertAll("Assert equal PropertyValidationRule properties.",
      () -> Assertions.assertEquals(ruleOne.hashCode(), ruleTwo.hashCode()),
      () -> Assertions.assertEquals(ruleOne.getName(), ruleTwo.getName()),
      () -> Assertions.assertEquals(ruleOne.getType(), ruleTwo.getType()));
  }

  @Test
  public void testHashCodeDifferent() {
    PropertyValidationRule ruleOne = new PropertyValidationRule("name", "string");
    PropertyValidationRule ruleTwo = new PropertyValidationRule("differentName", "integer");

    Assertions.assertAll("Assert unequal PropertyValidationRule properties.",
      () -> Assertions.assertNotEquals(ruleOne.hashCode(), ruleTwo.hashCode()),
      () -> Assertions.assertNotEquals(ruleOne.getName(), ruleTwo.getName()),
      () -> Assertions.assertNotEquals(ruleOne.getType(), ruleTwo.getType()));
  }

  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "EqualsWithItself"})
  public void testEqualsSameObject() {
    PropertyValidationRule ruleOne = new PropertyValidationRule("name", "list");

    Assertions.assertTrue(ruleOne.equals(ruleOne));
  }

  @Test
  @SuppressWarnings("SimplifiableJUnitAssertion")
  public void testEqualsDifferentObject() {
    PropertyValidationRule ruleOne = new PropertyValidationRule("name", "map");
    Object objectTwo = new Object();

    Assertions.assertFalse(ruleOne.equals(objectTwo));
  }

  @Test
  public void testToString() {
    PropertyValidationRule rule = new PropertyValidationRule("testName", "string");
    String expected = "PropertyValidationRule [name=testName, type=class java.lang.String]";

    Assertions.assertEquals(expected, rule.toString());
  }
}
