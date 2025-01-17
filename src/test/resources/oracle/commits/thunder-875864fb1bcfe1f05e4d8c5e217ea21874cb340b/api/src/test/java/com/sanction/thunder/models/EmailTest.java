package com.sanction.thunder.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;

import java.util.StringJoiner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class EmailTest {
  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  private final Email email = new Email("test@test.com", true, "token");

  @Test
  public void testToJson() throws Exception {
    String expected = MAPPER.writeValueAsString(
        MAPPER.readValue(FixtureHelpers.fixture("fixtures/email.json"), Email.class));

    assertEquals(expected, MAPPER.writeValueAsString(email));
  }

  @Test
  public void testFromJson() throws Exception {
    Email fromJson = MAPPER.readValue(FixtureHelpers.fixture("fixtures/email.json"), Email.class);

    assertEquals(email, fromJson);
  }

  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "EqualsWithItself"})
  public void testEqualsSameObject() {
    assertTrue(email.equals(email));
  }

  @Test
  @SuppressWarnings("SimplifiableJUnitAssertion")
  public void testEqualsDifferentObjectType() {
    Object objectTwo = new Object();

    assertFalse(email.equals(objectTwo));
  }

  @Test
  public void testHashCodeSame() {
    Email emailOne = new Email("test@test.com", true, "token");
    Email emailTwo = new Email("test@test.com", true, "token");

    assertEquals(emailOne.hashCode(), emailTwo.hashCode());
  }

  @Test
  public void testHashCodeDifferent() {
    Email emailOne = new Email("test@test.com", true, "token");
    Email emailTwo = new Email("differentTest@test.com", true, "token");

    assertNotEquals(emailOne.hashCode(), emailTwo.hashCode());
  }

  @Test
  public void testToString() {
    String expected = new StringJoiner(", ", "Email [", "]")
        .add(String.format("address=%s", "test@test.com"))
        .add(String.format("verified=%b", "true"))
        .add(String.format("verificationToken=%s", "token"))
        .toString();

    assertEquals(expected, email.toString());
  }
}
