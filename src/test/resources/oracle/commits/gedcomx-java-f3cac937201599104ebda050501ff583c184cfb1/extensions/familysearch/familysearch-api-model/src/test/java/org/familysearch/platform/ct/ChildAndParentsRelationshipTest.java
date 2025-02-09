/**
 * Copyright 2011 Intellectual Reserve, Inc. All Rights reserved.
 */
package org.familysearch.platform.ct;

import org.gedcomx.common.Note;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.Fact;
import org.gedcomx.source.SourceReference;
import org.gedcomx.types.FactType;
import org.testng.annotations.Test;


import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 */
public class ChildAndParentsRelationshipTest {
  @Test
  public void testModel() {
    ArrayList<SourceReference> sources = new ArrayList<SourceReference>();
    ArrayList<Note> notes = new ArrayList<Note>();
    ChildAndParentsRelationship rel = new ChildAndParentsRelationship();
    rel.setFather(new ResourceReference(URI.create("urn:father")));
    rel.setMother(new ResourceReference(URI.create("urn:mother")));
    rel.setChild(new ResourceReference(URI.create("urn:child")));
    rel.setSources(sources);
    rel.setNotes(notes);

    assertEquals(URI.create("urn:father"), rel.getFather().getResource());
    assertEquals(URI.create("urn:mother"), rel.getMother().getResource());
    assertEquals(URI.create("urn:child"), rel.getChild().getResource());
    assertEquals(sources, rel.getSources());
    assertEquals(notes,rel.getNotes());

    rel.addFatherFact(null);
    assertNull(rel.getFatherFacts());
    rel.addFatherFact(new Fact(FactType.Birth, "origBirthValue"));
    rel.addFatherFact(new Fact(FactType.Death, "origDeathValue"));
    assertNotNull(rel.getFatherFacts());
    assertEquals(rel.getFatherFacts().size(), 2);
    assertEquals(rel.getFatherFacts().get(0).getValue(), "origBirthValue");
    assertEquals(rel.getFatherFacts().get(1).getValue(), "origDeathValue");

    rel.addMotherFact(null);
    assertNull(rel.getMotherFacts());
    rel.addMotherFact(new Fact(FactType.Birth, "origBirthValue"));
    rel.addMotherFact(new Fact(FactType.Death, "origDeathValue"));
    assertNotNull(rel.getMotherFacts());
    assertEquals(rel.getMotherFacts().size(), 2);
    assertEquals(rel.getMotherFacts().get(0).getValue(), "origBirthValue");
    assertEquals(rel.getMotherFacts().get(1).getValue(), "origDeathValue");
  }
}
