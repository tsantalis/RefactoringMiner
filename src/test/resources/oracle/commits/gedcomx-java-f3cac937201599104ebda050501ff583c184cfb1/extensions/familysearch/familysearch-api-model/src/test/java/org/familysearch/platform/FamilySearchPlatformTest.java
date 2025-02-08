package org.familysearch.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.familysearch.platform.ct.ChildAndParentsRelationship;
import org.familysearch.platform.records.AlternateDate;
import org.familysearch.platform.records.AlternatePlaceReference;
import org.gedcomx.Gedcomx;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.*;
import org.gedcomx.rt.json.GedcomJacksonModule;
import org.gedcomx.types.FactType;
import org.gedcomx.types.RelationshipType;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * Class for testing the FamilySearchPlatform class
 * User: Randy Wilson
 * Date: 20 May 2015
 */
@Test
public class FamilySearchPlatformTest {

  public void testAltDatesPlaces() throws Exception {
    Fact fact = new Fact(FactType.Adoption, "value");
    AlternateDate altDate = new AlternateDate();
    altDate.setOriginal("orig");
    fact.addExtensionElement(altDate);
    AlternatePlaceReference altPlace = new AlternatePlaceReference();
    altPlace.setOriginal("place");
    fact.addExtensionElement(altPlace);
    Gedcomx gx = new Gedcomx().person(new Person().fact(fact));

    ObjectMapper objectMapper = GedcomJacksonModule.createObjectMapper(AlternateDate.class, AlternatePlaceReference.class);
    String value = objectMapper.writeValueAsString(gx);
    //System.out.println(value);
    gx = objectMapper.readValue(value, Gedcomx.class);
    assertEquals("orig", gx.getPerson().getFirstFactOfType(FactType.Adoption).findExtensionOfType(AlternateDate.class).getOriginal());

    //JAXBContext.newInstance(FamilySearchPlatform.class).createMarshaller().marshal(gx, System.out);
  }

  public void testFamily() {
    FamilySearchPlatform g = makeDoc();
    FamilyView family = g.getPerson().getDisplayExtension().getFamiliesAsChild().get(0);

    // dad-mom relationship
    Relationship couple = g.findCoupleRelationship(family);
    assertEquals(RelationshipType.Couple, couple.getKnownType());
    assertEquals("#dad", couple.getPerson1().getResource().toString());
    assertEquals("#mom", couple.getPerson2().getResource().toString());

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

    // Now also look up ChildAndParentsRelationship
    ChildAndParentsRelationship rel = g.findChildAndParentsRelationship(family.getChildren().get(0), family.getParent1(), family.getParent2());
    assertEquals("#dad", rel.getFather().getResource().toString());
    assertEquals("#mom", rel.getMother().getResource().toString());
    assertEquals("#kid1", rel.getChild().getResource().toString());
    assertEquals(FactType.AdoptiveParent, rel.getFatherFacts().get(0).getKnownType());
    assertEquals(FactType.BiologicalParent, rel.getMotherFacts().get(0).getKnownType());

    rel = g.findChildAndParentsRelationship(family.getChildren().get(1), family.getParent1(), family.getParent2());
    assertEquals("#dad", rel.getFather().getResource().toString());
    assertEquals("#mom", rel.getMother().getResource().toString());
    assertEquals("#kid2", rel.getChild().getResource().toString());
    assertNull(rel.getFatherFacts());
    assertNull(rel.getMotherFacts());

    // Test single-parent family
    FamilyView fam2 = g.getPerson().getDisplayExtension().getFamiliesAsChild().get(1);
    rel = g.findChildAndParentsRelationship(fam2.getChildren().get(0), fam2.getParent1(), fam2.getParent2());
    assertEquals("#dad", rel.getFather().getResource().toString());
    assertNull(rel.getMother());
    assertEquals("#kid3", rel.getChild().getResource().toString());

    assertNull(g.findCoupleRelationship(fam2));
  }

  private FamilySearchPlatform makeDoc() {
    FamilySearchPlatform g = new FamilySearchPlatform();
    g.addPerson(makePerson());

    g.addRelationship(makeRel("dad", "mom", RelationshipType.Couple));
    addChild(g, "dad", "mom", "kid1", FactType.AdoptiveParent, FactType.BiologicalParent);
    addChild(g, "dad", "mom", "kid2", null, null);

    // Add single-parent family
    g.getPerson().getDisplayExtension().addFamilyAsChild(makeFam("dad", null, "kid3"));
    addChild(g, "dad", null, "kid3", null, null);

    return g;
  }

  private Person makePerson() {
    Person person = new Person();
    person.setDisplayExtension(new DisplayProperties());
    person.getDisplayExtension().addFamilyAsChild(makeFam("dad", "mom", "kid1", "kid2"));
    return person;
  }

  private static void addChild(FamilySearchPlatform doc, String fatherId, String motherId, String childId,
                               FactType fatherFactType, FactType motherFactType) {
    ChildAndParentsRelationship rel = new ChildAndParentsRelationship();
    if (fatherId != null) {
      doc.addRelationship(kidRel(fatherId, childId, fatherFactType));
      rel.setFather(makeRef(fatherId));
    }
    if (motherId != null) {
      doc.addRelationship(kidRel(motherId, childId, motherFactType));
      rel.setMother(makeRef(motherId));
    }
    rel.setChild(makeRef(childId));
    if (fatherFactType != null) {
      rel.addFatherFact(new Fact(fatherFactType, null));
    }
    if (motherFactType != null) {
      rel.addMotherFact(new Fact(motherFactType, null));
    }
    doc.addChildAndParentsRelationship(rel);
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
