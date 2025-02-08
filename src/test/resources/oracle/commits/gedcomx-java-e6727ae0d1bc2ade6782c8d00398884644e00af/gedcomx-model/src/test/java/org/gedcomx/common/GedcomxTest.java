package org.gedcomx.common;

import org.gedcomx.Gedcomx;
import org.gedcomx.agent.Agent;
import org.gedcomx.conclusion.*;
import org.gedcomx.types.FactType;
import org.gedcomx.types.RelationshipType;
import org.junit.Test;

import java.util.ArrayList;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.*;

/**
 * @author Ryan Heaton
 */
public class GedcomxTest {

  /**
   * tests id xml
   */
  @Test
  public void testResourceSetXml() throws Exception {
    Gedcomx meta = createData();
    meta = processThroughXml(meta);
    assertData(meta);
  }

  /**
   * tests id json
   */
  @Test
  public void testResourceSetJson() throws Exception {
    Gedcomx meta = createData();
    meta = processThroughJson(meta);
    assertData(meta);
  }

  private void assertData(Gedcomx meta) {
    assertEquals("id", meta.getId());
    assertNotNull(meta.getAttribution());
    assertEquals("en", meta.getLang());
    assertEquals(1, meta.getAgents().size());
    assertEquals(1, meta.getDocuments().size());
    assertEquals(1, meta.getEvents().size());
    assertEquals(1, meta.getPersons().size());
    assertEquals(1, meta.getPersons().get(0).getDisplayExtension().getFamiliesAsParent().size());
    assertEquals(1, meta.getRelationships().size());
    assertEquals(1, meta.getExtensionElements().size());
  }

  private Gedcomx createData() {
    Gedcomx meta = new Gedcomx();
    meta.setId("id");
    meta.setAttribution(new Attribution());
    meta.setLang("en");
    meta.setAgents(new ArrayList<Agent>());
    meta.getAgents().add(new Agent());
    meta.setDocuments(new ArrayList<Document>());
    meta.getDocuments().add(new Document());
    meta.setEvents(new ArrayList<Event>());
    meta.getEvents().add(new Event());
    meta.setPersons(new ArrayList<Person>());
    meta.getPersons().add(makePerson());
    meta.setRelationships(new ArrayList<Relationship>());
    meta.getRelationships().add(new Relationship());
    meta.setExtensionElements(new ArrayList<Object>());
    meta.getExtensionElements().add(new Gedcomx());
    return meta;
  }

  private Person makePerson() {
    Person person = new Person();
    person.setDisplayExtension(new DisplayProperties());
    person.getDisplayExtension().addFamilyAsParent(makeFam("dad", "mom", "kid1", "kid2"));
    return person;
  }

  @Test
  public void testFamily() {
    Gedcomx g = makeDoc();
    FamilyView family = g.getPersons().get(0).getDisplayExtension().getFamiliesAsParent().get(0);

    // dad-mom relationship
    Relationship couple = g.findCoupleRelationship(family);
    assertEquals(RelationshipType.Couple, couple.getKnownType());
    assertEquals("#dad", couple.getPerson1().getResource().toString());
    assertEquals("#mom", couple.getPerson2().getResource().toString());
    assertEquals(couple, g.findCoupleRelationship(family.getParent1(), family.getParent2()));

    // dad-kid1 relationship
    Relationship pcRel = g.findParentChildRelationship(family.getParent1(), family.getChildren().get(0));
    assertEquals(RelationshipType.ParentChild, pcRel.getKnownType());
    assertEquals("#dad", pcRel.getPerson1().getResource().toString());
    assertEquals("#kid1", pcRel.getPerson2().getResource().toString());
    assertEquals(FactType.AdoptiveParent, pcRel.getFacts().get(0).getKnownType());
    assertNull(pcRel.getFacts().get(0).getValue());

    // mom-kid1 relationship
    pcRel = g.findParentChildRelationship(family.getParent2(), family.getChildren().get(0));
    assertEquals(RelationshipType.ParentChild, pcRel.getKnownType());
    assertEquals("#mom", pcRel.getPerson1().getResource().toString());
    assertEquals("#kid1", pcRel.getPerson2().getResource().toString());
    assertEquals(FactType.BiologicalParent, pcRel.getFacts().get(0).getKnownType());

    // mom-kid2 relationship
    pcRel = g.findParentChildRelationship(family.getParent2(), family.getChildren().get(1));
    assertEquals(RelationshipType.ParentChild, pcRel.getKnownType());
    assertEquals("#mom", pcRel.getPerson1().getResource().toString());
    assertEquals("#kid2", pcRel.getPerson2().getResource().toString());
    assertNull(pcRel.getFacts());
  }

  private Gedcomx makeDoc() {
    Gedcomx g = new Gedcomx();

    g.addPerson(makePerson());

    g.addRelationship(makeRel("dad", "mom", RelationshipType.Couple));
    g.addRelationship(kidRel("dad", "kid1", FactType.AdoptiveParent));
    g.addRelationship(kidRel("mom", "kid1", FactType.BiologicalParent));
    g.addRelationship(kidRel("dad", "kid2", null));
    g.addRelationship(kidRel("mom", "kid2", null));

    return g;
  }

  private static FamilyView makeFam(String fatherId, String motherId, String... kidIds) {
    FamilyView family = new FamilyView();
    family.setParent1(makeRef(fatherId));
    family.setParent2(makeRef(motherId));
    if (kidIds != null) {
      for (String kidId : kidIds) {
        family.addChild(makeRef(kidId));
      }
    }
    return family;
  }

  protected static Relationship kidRel(String parentId, String kidId, FactType lineageType) {
    Relationship relationship = makeRel(parentId, kidId, RelationshipType.ParentChild);
    if (lineageType != null) {
      relationship.addFact(new Fact(lineageType, null));
    }
    return relationship;
  }

  protected static Relationship makeRel(String id1, String id2, RelationshipType relationshipType) {
    Relationship relationship = new Relationship();
    relationship.setKnownType(relationshipType);
    relationship.setPerson1(makeRef(id1));
    relationship.setPerson2(makeRef(id2));
    return relationship;
  }

  protected static ResourceReference makeRef(String id) {
    return id == null ? null : new ResourceReference(new URI("#" + id));
  }
}
