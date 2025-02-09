package org.gedcomx.conclusion;

import org.gedcomx.common.Attribution;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.source.SourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.types.RelationshipType;
import org.junit.Test;

import java.util.Arrays;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Ryan Heaton
 */
public class RelationshipTest {

  /**
   * tests processing a relationship through xml...
   */
  @Test
  public void testRelationshipXml() throws Exception {
    Relationship relationship = createTestRelationship();
    relationship = processThroughXml(relationship);
    assertTestRelationship(relationship);
  }

  /**
   * tests processing a relationship through json...
   */
  @Test
  public void testPersonJson() throws Exception {
    Relationship relationship = createTestRelationship();
    relationship = processThroughJson(relationship);
    assertTestRelationship(relationship);
  }

  private Relationship createTestRelationship() {
    Relationship relationship = new Relationship();

    relationship.setId("relationship");
    relationship.addSource(new SourceReference());
    relationship.getSources().get(0).setDescriptionRef(URI.create("urn:sourceDescription1"));

    relationship.setKnownType(RelationshipType.Couple);

    relationship.setPerson1(new ResourceReference(URI.create("urn:person1")));
    relationship.setPerson2(new ResourceReference(URI.create("urn:person2")));

    relationship.addFact(new Fact());
    relationship.getFacts().get(0).setId("fact");
    relationship.addFact(new Fact());
    relationship.getFacts().get(1).setId("event");

    relationship.setIdentifiers(Arrays.asList(new Identifier()));
    relationship.getIdentifiers().get(0).setType(URI.create("urn:identifierType"));
    relationship.getIdentifiers().get(0).setValue(URI.create("urn:identifierValue"));

    relationship.setAttribution(new Attribution());
    relationship.getAttribution().setChangeMessage("explanation");

    return relationship;
  }

  private void assertTestRelationship(Relationship relationship) {
    assertEquals("relationship", relationship.getId());
    assertNotNull(relationship.getSources());
    assertEquals(1, relationship.getSources().size());
    assertEquals(URI.create("urn:sourceDescription1"), relationship.getSources().get(0).getDescriptionRef());

    assertEquals(RelationshipType.Couple, relationship.getKnownType());

    assertEquals(URI.create("urn:person1"), relationship.getPerson1().getResource());
    assertEquals(URI.create("urn:person2"), relationship.getPerson2().getResource());

    assertNotNull(relationship.getFacts());
    assertEquals(2, relationship.getFacts().size());
    assertEquals("fact", relationship.getFacts().get(0).getId());
    assertEquals("event", relationship.getFacts().get(1).getId());

    assertNotNull(relationship.getIdentifiers());
    assertEquals(1, relationship.getIdentifiers().size());
    assertEquals(URI.create("urn:identifierType"), relationship.getIdentifiers().get(0).getType());
    assertEquals(URI.create("urn:identifierValue"), relationship.getIdentifiers().get(0).getValue());

    assertEquals("explanation", relationship.getAttribution().getChangeMessage());
  }
}
