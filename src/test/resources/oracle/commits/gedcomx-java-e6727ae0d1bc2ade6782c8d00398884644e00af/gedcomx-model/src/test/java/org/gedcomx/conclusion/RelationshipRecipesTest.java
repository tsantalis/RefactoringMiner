package org.gedcomx.conclusion;

import org.gedcomx.common.*;
import org.gedcomx.source.SourceReference;
import org.gedcomx.test.RecipeTest;
import org.gedcomx.test.Snippet;
import org.gedcomx.types.FactType;
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
public class RelationshipRecipesTest extends RecipeTest {

  @Test
  public void testRelationship() throws Exception {
    createRecipe("Simple Relationship")
      .withDescription("Simple example for a relationship.")
      .applicableTo(Relationship.class);

    Relationship relationship = createTestRelationship();

    Snippet snippet = new Snippet();
    Relationship relationshipThruXml = processThroughXml(relationship, snippet);
    Relationship relationshipThruJson = processThroughJson(relationship, snippet);
    addSnippet(snippet);

    verifyRelationship(relationshipThruXml);
    verifyRelationship(relationshipThruJson);
  }

  private Relationship createTestRelationship() {
    Relationship relationship = new Relationship();

    relationship.setId("CCC-CCCC");
    relationship.addSource(new SourceReference());
    relationship.getSources().get(0).setDescriptionRef(URI.create("urn:srcDescId"));

    relationship.setKnownType(RelationshipType.ParentChild);

    relationship.setPerson1(new ResourceReference(URI.create("https://api.familysearch.org/platform/persons/DDD-D001")));
    relationship.setPerson2(new ResourceReference(URI.create("https://api.familysearch.org/platform/persons/DDD-D002")));

    relationship.addFact(new Fact());
    relationship.getFacts().get(0).setId("F123");
    relationship.getFacts().get(0).setKnownType(FactType.AdoptiveParent);
    relationship.getFacts().get(0).setDate(new Date());
    relationship.getFacts().get(0).getDate().setOriginal("January 6, 1759");
    relationship.getFacts().get(0).getDate().setFormal("+1759-01-06");

    relationship.setIdentifiers(Arrays.asList(new Identifier()));
    relationship.getIdentifiers().get(0).setType(URI.create("http://familysearch.org/v1/ParentPairing"));
    relationship.getIdentifiers().get(0).setValue(URI.create("https://api.familysearch.org/platform/parent-relationships/FFF-FFFF"));

    relationship.setAttribution(new Attribution());
    relationship.getAttribution().setChangeMessage("(justification here)");
    relationship.getAttribution().setContributor(new ResourceReference(URI.create("https://api.familysearch.org/platform/contributors/BCD-FGHJ")));

    return relationship;
  }

  private void verifyRelationship(Relationship relationship) {
    assertEquals("CCC-CCCC", relationship.getId());
    assertNotNull(relationship.getSources());
    assertEquals(1, relationship.getSources().size());
    assertEquals(URI.create("urn:srcDescId"), relationship.getSources().get(0).getDescriptionRef());

    assertEquals(RelationshipType.ParentChild, relationship.getKnownType());

    assertEquals(URI.create("https://api.familysearch.org/platform/persons/DDD-D001"), relationship.getPerson1().getResource());
    assertEquals(URI.create("https://api.familysearch.org/platform/persons/DDD-D002"), relationship.getPerson2().getResource());

    assertNotNull(relationship.getFacts());
    assertEquals(1, relationship.getFacts().size());
    assertEquals("F123", relationship.getFacts().get(0).getId());
    assertEquals(FactType.AdoptiveParent, relationship.getFacts().get(0).getKnownType());
    assertNotNull(relationship.getFacts().get(0).getDate());
    assertNotNull("January 6, 1759", relationship.getFacts().get(0).getDate().getOriginal());
    assertNotNull("+1759-01-06", relationship.getFacts().get(0).getDate().getFormal());

    assertNotNull(relationship.getIdentifiers());
    assertEquals(1, relationship.getIdentifiers().size());
    assertEquals(URI.create("http://familysearch.org/v1/ParentPairing"), relationship.getIdentifiers().get(0).getType());
    assertEquals(URI.create("https://api.familysearch.org/platform/parent-relationships/FFF-FFFF"), relationship.getIdentifiers().get(0).getValue());

    assertNotNull(relationship.getAttribution());
    assertEquals("(justification here)", relationship.getAttribution().getChangeMessage());
    assertEquals(URI.create("https://api.familysearch.org/platform/contributors/BCD-FGHJ"), relationship.getAttribution().getContributor().getResource());
  }

}
