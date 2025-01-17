package com.sanction.thunder.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class UserTest {
  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  private static final Email EMAIL = new Email("test@test.com", true, "hashToken");
  private static final String PASSWORD = "12345";
  private static final Map<String, Object> MULTIPLE_PROPERTY_MAP = new TreeMap<>();

  private final User emptyPropertiesUser = new User(EMAIL, PASSWORD, Collections.emptyMap());
  private final User multiplePropertiesUser = new User(EMAIL, PASSWORD, MULTIPLE_PROPERTY_MAP);

  @BeforeClass
  public static void setup() {
    MULTIPLE_PROPERTY_MAP.put("customString", "value");
    MULTIPLE_PROPERTY_MAP.put("customInt", 1);
    MULTIPLE_PROPERTY_MAP.put("customDouble", 1.2);
    MULTIPLE_PROPERTY_MAP.put("customBoolean", true);
    MULTIPLE_PROPERTY_MAP.put("customList", Arrays.asList("hello", "world"));
  }

  @Test
  public void testToJsonNoProperties() throws Exception {
    String expected = MAPPER.writeValueAsString(MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/no_properties_user.json"), User.class));

    assertEquals(expected, MAPPER.writeValueAsString(emptyPropertiesUser));
  }

  @Test
  public void testFromJsonNoProperties() throws Exception {
    User fromJson = MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/no_properties_user.json"), User.class);

    assertEquals(emptyPropertiesUser, fromJson);
  }

  @Test
  public void testToJsonEmptyProperties() throws Exception {
    String expected = MAPPER.writeValueAsString(MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/empty_properties_user.json"),User.class));

    assertEquals(expected, MAPPER.writeValueAsString(emptyPropertiesUser));
  }

  @Test
  public void testFromJsonEmptyProperties() throws Exception {
    User fromJson = MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/empty_properties_user.json"), User.class);

    assertEquals(emptyPropertiesUser, fromJson);
  }

  @Test
  public void testToJsonMultipleProperties() throws Exception {
    String expected = MAPPER.writeValueAsString(MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/multiple_properties_user.json"), User.class));

    assertEquals(expected, MAPPER.writeValueAsString(multiplePropertiesUser));
  }

  @Test
  public void testFromJsonMultipleProperties() throws Exception {
    User fromJson = MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/multiple_properties_user.json"), User.class);

    assertEquals(multiplePropertiesUser, fromJson);
  }

  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "EqualsWithItself"})
  public void testEqualsSameObject() {
    assertTrue(multiplePropertiesUser.equals(multiplePropertiesUser));
  }

  @Test
  @SuppressWarnings("SimplifiableJUnitAssertion")
  public void testEqualsDifferentObjectType() {
    Object objectTwo = new Object();

    assertFalse(multiplePropertiesUser.equals(objectTwo));
  }

  @Test
  public void testHashCodeSame() {
    User userOne = new User(EMAIL, PASSWORD, Collections.singletonMap("customKey", 1));
    User userTwo = new User(EMAIL, PASSWORD, Collections.singletonMap("customKey", 1));

    assertEquals(userOne.hashCode(), userTwo.hashCode());
  }

  @Test
  public void testHashCodeDifferent() {
    User userOne = new User(EMAIL, PASSWORD, Collections.singletonMap("customKey", 1));
    User userTwo = new User(EMAIL, PASSWORD, Collections.singletonMap("customKey", 2));

    assertNotEquals(userOne.hashCode(), userTwo.hashCode());
  }

  @Test
  public void testToString() {
    String expected = new StringJoiner(", ", "User [", "]")
            .add(String.format("email=%s", EMAIL))
            .add(String.format("password=%s", PASSWORD))
            .add(String.format("properties=%s", MULTIPLE_PROPERTY_MAP))
            .toString();

    assertEquals(expected, multiplePropertiesUser.toString());
  }
}
