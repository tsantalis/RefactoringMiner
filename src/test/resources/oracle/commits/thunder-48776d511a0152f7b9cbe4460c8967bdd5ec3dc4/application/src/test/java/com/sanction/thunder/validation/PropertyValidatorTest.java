package com.sanction.thunder.validation;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class PropertyValidatorTest {
  private final Map<String, Object> properties = new HashMap<>();
  private List<PropertyValidationRule> validationRules
      = Collections.singletonList(
          new PropertyValidationRule("firstProperty", "string"));

  @Test
  public void testSkipValidation() {
    PropertyValidator validator = new PropertyValidator(null);
    Map<String, Object> properties = Collections.emptyMap();

    Assertions.assertTrue(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testInvalidSize() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.emptyMap();

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testMismatchName() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.singletonMap("myProperty", "value");

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  /* String type */
  @Test
  public void testMismatchTypeString() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.singletonMap("firstProperty", 1);

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulStringValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.singletonMap("firstProperty", "value");

    Assertions.assertTrue(validator.isValidPropertiesMap(properties));
  }

  /* Integer type */
  @Test
  public void testMismatchTypeInteger() {
    validationRules = Arrays.asList(
        new PropertyValidationRule("firstProperty", "string"),
        new PropertyValidationRule("secondProperty", "integer"));

    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", "1");

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulIntegerValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1);

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  /* Boolean type */
  @Test
  public void testMismatchTypeBoolean() {
    validationRules = Arrays.asList(
        new PropertyValidationRule("firstProperty", "string"),
        new PropertyValidationRule("secondProperty", "integer"),
        new PropertyValidationRule("thirdProperty", "boolean"));

    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", "false");

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulBooleanValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false);

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  /* Boolean type */
  @Test
  public void testMismatchTypeDouble() {
    validationRules = Arrays.asList(
        new PropertyValidationRule("firstProperty", "string"),
        new PropertyValidationRule("secondProperty", "integer"),
        new PropertyValidationRule("thirdProperty", "boolean"),
        new PropertyValidationRule("fourthProperty", "double"));

    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false,
        "fourthProperty", 1);

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulDoubleValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false,
        "fourthProperty", 1.0);

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  /* List type */
  @Test
  public void testMismatchTypeList() {
    validationRules = Arrays.asList(
        new PropertyValidationRule("firstProperty", "string"),
        new PropertyValidationRule("secondProperty", "integer"),
        new PropertyValidationRule("thirdProperty", "boolean"),
        new PropertyValidationRule("fourthProperty", "double"),
        new PropertyValidationRule("fifthProperty", "list"));

    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false,
        "fourthProperty", 1.0,
        "fifthProperty", Collections.emptyMap());

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulListValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false,
        "fourthProperty", 1.0,
        "fifthProperty", Collections.emptyList());

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  /* Map type */
  @Test
  public void testMismatchTypeMap() {
    validationRules = Arrays.asList(
        new PropertyValidationRule("firstProperty", "string"),
        new PropertyValidationRule("secondProperty", "integer"),
        new PropertyValidationRule("thirdProperty", "boolean"),
        new PropertyValidationRule("fourthProperty", "double"),
        new PropertyValidationRule("fifthProperty", "list"),
        new PropertyValidationRule("sixthProperty", "map"));

    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false,
        "fourthProperty", 1.0,
        "fifthProperty", Collections.emptyList());

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulMapValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false,
        "fourthProperty", 1.0,
        "fifthProperty", Collections.emptyMap());

    Assertions.assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testGetType() {
    Assertions.assertAll("Assert equal return value from getType PropertyValidator method.",
      () -> Assertions.assertEquals(String.class, PropertyValidator.getType("string")),
      () -> Assertions.assertEquals(Integer.class, PropertyValidator.getType("integer")),
      () -> Assertions.assertEquals(Boolean.class, PropertyValidator.getType("boolean")),
      () -> Assertions.assertEquals(Double.class, PropertyValidator.getType("double")),
      () -> Assertions.assertEquals(List.class, PropertyValidator.getType("list")),
      () -> Assertions.assertEquals(Map.class, PropertyValidator.getType("map")),
      () -> Assertions.assertEquals(Object.class, PropertyValidator.getType("unknown")));
  }
}
