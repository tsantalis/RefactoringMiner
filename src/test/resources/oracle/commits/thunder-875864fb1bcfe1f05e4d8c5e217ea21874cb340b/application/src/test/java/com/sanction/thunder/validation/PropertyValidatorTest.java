package com.sanction.thunder.validation;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyValidatorTest {
  private final Map<String, Object> properties = new HashMap<>();
  private List<PropertyValidationRule> validationRules
      = Collections.singletonList(
          new PropertyValidationRule("firstProperty", "string"));

  @Test
  public void testSkipValidation() {
    PropertyValidator validator = new PropertyValidator(null);
    Map<String, Object> properties = Collections.emptyMap();

    assertTrue(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testInvalidSize() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.emptyMap();

    assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testMismatchName() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.singletonMap("myProperty", "value");

    assertFalse(validator.isValidPropertiesMap(properties));
  }

  /* String type */
  @Test
  public void testMismatchTypeString() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.singletonMap("firstProperty", 1);

    assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulStringValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = Collections.singletonMap("firstProperty", "value");

    assertTrue(validator.isValidPropertiesMap(properties));
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

    assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulIntegerValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1);

    assertFalse(validator.isValidPropertiesMap(properties));
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

    assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulBooleanValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false);

    assertFalse(validator.isValidPropertiesMap(properties));
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

    assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testSuccessfulDoubleValidation() {
    PropertyValidator validator = new PropertyValidator(validationRules);
    Map<String, Object> properties = ImmutableMap.of(
        "firstProperty", "value",
        "secondProperty", 1,
        "thirdProperty", false,
        "fourthProperty", 1.0);

    assertFalse(validator.isValidPropertiesMap(properties));
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

    assertFalse(validator.isValidPropertiesMap(properties));
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

    assertFalse(validator.isValidPropertiesMap(properties));
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

    assertFalse(validator.isValidPropertiesMap(properties));
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

    assertFalse(validator.isValidPropertiesMap(properties));
  }

  @Test
  public void testGetType() {
    assertEquals(String.class, PropertyValidator.getType("string"));
    assertEquals(Integer.class, PropertyValidator.getType("integer"));
    assertEquals(Boolean.class, PropertyValidator.getType("boolean"));
    assertEquals(Double.class, PropertyValidator.getType("double"));
    assertEquals(List.class, PropertyValidator.getType("list"));
    assertEquals(Map.class, PropertyValidator.getType("map"));
    assertEquals(Object.class, PropertyValidator.getType("unknown"));
  }
}
