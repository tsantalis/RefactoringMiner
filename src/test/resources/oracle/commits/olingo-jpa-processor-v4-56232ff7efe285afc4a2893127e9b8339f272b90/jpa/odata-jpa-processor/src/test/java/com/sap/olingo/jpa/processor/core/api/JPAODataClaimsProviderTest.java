package com.sap.olingo.jpa.processor.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class JPAODataClaimsProviderTest {

  private JPAODataClaimsProvider cut;

  @Before
  public void setup() {
    cut = new JPAODataClaimsProvider();
  }

  @Test
  public void checkAddSinglePairReturnsOne() {
    cut.add("Test", new JPAClaimsPair<>("Hugo"));
    List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(1, claims.size());
  }

  @Test
  public void checkAddThreeSinglePairsReturnsThree() {
    cut.add("Test", new JPAClaimsPair<>("Hugo"));
    cut.add("Test", new JPAClaimsPair<>("Willi"));
    cut.add("Test", new JPAClaimsPair<>("Walter"));
    List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(3, claims.size());
  }

  @Test
  public void checkNotProvidedAttributeReturnsEmptyList() {
    List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(0, claims.size());
  }

  @Test
  public void checkAddTwoAttributesSinglePairs() {
    cut.add("Test", new JPAClaimsPair<>("Hugo"));
    cut.add("Dummy", new JPAClaimsPair<>("Willi"));

    List<JPAClaimsPair<?>> claims = cut.get("Test");
    assertNotNull(claims);
    assertEquals(1, claims.size());

    claims = cut.get("Dummy");
    assertNotNull(claims);
    assertEquals(1, claims.size());
  }
}
