package org.gedcomx.rs.client;

import org.gedcomx.Gedcomx;
import org.gedcomx.atom.Feed;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author Ryan Heaton
 */
@Test( groups = "integration" )
public class GedcomxApplicationStateTest {

//  private GedcomxApplicationState<Feed> initialState;
//
//  @BeforeClass
//  protected void setup() {
//    this.initialState = new GedcomxApplicationState<Feed>(URI.create("https://integration.familysearch.org/.well-known/app-meta"))
//      .authenticateViaOAuth2Password("sdktester", "1234sdkpass", "WCQY-7J1Q-GKVV-7DNM-SQ5M-9Q5H-JX3H-CMJK");
//  }
//
//  public void testGetCurrentUser() {
//    GedcomxApplicationState<? extends Gedcomx> personState = this.initialState.getPersonForCurrentUser(false);
//    assertNotNull(personState.getEntity().getPersons());
//    assertNull(personState.getEntity().getRelationships());
//  }
//
//  public void testGetFullCurrentUser() {
//    GedcomxApplicationState<? extends Gedcomx> personState = this.initialState.getPersonForCurrentUser();
//    assertNotNull(personState.getEntity().getPersons());
//    assertNotNull(personState.getEntity().getRelationships());
//  }
}
