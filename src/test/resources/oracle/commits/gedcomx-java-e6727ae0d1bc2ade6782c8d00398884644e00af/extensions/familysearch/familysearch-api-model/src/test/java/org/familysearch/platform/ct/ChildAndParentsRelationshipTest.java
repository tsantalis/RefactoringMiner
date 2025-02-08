/**
 * Copyright 2011 Intellectual Reserve, Inc. All Rights reserved.
 */
package org.familysearch.platform.ct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import org.gedcomx.common.Note;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.Fact;
import org.gedcomx.rt.json.GedcomJacksonModule;
import org.gedcomx.source.SourceReference;
import org.gedcomx.types.FactType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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


//  @Test
//  public void testMarshalling() {
////    ChildAndParentsRelationship childAndParentsRelationship = new ChildAndParentsRelationship();
//
//    ResourceReference childResourceReference = new ResourceReference(URI.create("urn:child"), "childId");
//    ResourceReference fatherResourceReference = new ResourceReference(URI.create("urn:father"), "fatherId");
//    ResourceReference motherResourceReference = new ResourceReference(URI.create("urn:mother"), "motherId");
//
//    ChildAndParentsRelationship origChildAndParentsRelationship = new ChildAndParentsRelationship() //create a child-and-parents relationship
//        .child(childResourceReference)    //between a child
//        .father(fatherResourceReference)  //a father
//        .mother(motherResourceReference); //and a mother
//
//
//    ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024); //figure out where you want to write the XML
//
//
//    try {
//      JAXBContext context = JAXBContext.newInstance(ChildAndParentsRelationship.class);
//      Marshaller marshaller = context.createMarshaller();
//      marshaller.marshal(origChildAndParentsRelationship, outStream);
//      //marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
//
//      ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
//      Unmarshaller unmarshaller = context.createUnmarshaller();
//      ChildAndParentsRelationship roundTrippedChildAndParentsRelationship = (ChildAndParentsRelationship)unmarshaller.unmarshal(inStream);
//
//    }
//    catch (JAXBException e) {
//      fail("Failed to marshall XML " + e.toString());
//    }
//
//
//    outStream.reset();
//
//    try {
//      ObjectMapper mapper = GedcomJacksonModule.createObjectMapper(ChildAndParentsRelationship.class);
//      mapper.writeValue(outStream, origChildAndParentsRelationship);
//
//      ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
//      ChildAndParentsRelationship roundTrippedChildAndParentsRelationship = mapper.readValue(inStream, ChildAndParentsRelationship.class);
//
//    }
//    catch (IOException e) {
//      fail("Failed to marshall Json " + e.toString());
//    }
//
//
//  }
//
//  private void assertRoundTrip(ChildAndParentsRelationship orig, ChildAndParentsRelationship roundTripped) {
////    assertThat
//  }


}
