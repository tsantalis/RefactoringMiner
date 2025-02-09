package org.gedcomx.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.gedcomx.Gedcomx;
import org.gedcomx.agent.Agent;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.*;
import org.gedcomx.records.RecordDescriptor;
import org.gedcomx.source.SourceDescription;
import org.gedcomx.source.SourceReference;
import org.gedcomx.types.FactType;
import org.gedcomx.types.IdentifierType;
import org.gedcomx.types.RelationshipType;

import java.util.Arrays;

/**
 * Class for testing the DocMap utility class.
 * User: Randy Wilson
 * Date: 6/10/14
 * Time: 12:19 PM
 */
public class TestDocMap extends TestCase {

  public void testStuff() {
    Gedcomx doc = new Gedcomx();
    Person p1 = new Person();
    p1.setId("p1");
    p1.addIdentifier(new Identifier(new URI("http://test.com/person1"), IdentifierType.Primary));
    // Alternate id for the same person resource.
    p1.addIdentifier(new Identifier(new URI("http://alternate.com/oldPerson1"), null));

    PlaceDescription place = new PlaceDescription();
    place.setId("place1");
    place.addIdentifier(new Identifier(new URI("http://placedepot.com/places/1"), IdentifierType.Primary));
    doc.addPlace(place);

    PlaceReference placeReference = new PlaceReference();
    placeReference.setDescriptionRef(new URI("#place1"));
    Date date = new Date();
    date.setOriginal("12 June 1874");
    Fact fact = new Fact(FactType.Birth, date, placeReference);
    p1.addFact(fact);

    doc.addPerson(p1);

    Person p2 = new Person();
    p2.setId("p2");
    p2.addIdentifier(new Identifier(new URI("http://test.com/person2"), IdentifierType.Primary));
    doc.addPerson(p2);

    Relationship relationship = new Relationship();
    relationship.setKnownType(RelationshipType.ParentChild);
    relationship.setPerson1(new ResourceReference(new URI("#p1")));
    relationship.setPerson2(new ResourceReference(new URI("#p2")));
    doc.addRelationship(relationship);

    SourceDescription sd1 = new SourceDescription();
    sd1.setId("sd1");
    sd1.setAbout(new URI("http://test.com/person1"));
    sd1.setIdentifiers(p1.getIdentifiers()); // copy the same list over from person p1.
    doc.setDescriptionRef(new URI("#sd1"));
    doc.addSourceDescription(sd1);

    SourceDescription sd2 = new SourceDescription();
    sd2.setId("sd2");
    sd2.setAbout(new URI("http://test.com/image123"));
    sd2.setIdentifiers(Arrays.asList(new Identifier(new URI("http://test.com/image123"), IdentifierType.Primary)));
    SourceReference sr = new SourceReference();
    sr.setDescriptionRef(new URI("#sd2"));
    sd1.setSources(Arrays.asList(sr));
    doc.addSourceDescription(sd2);

    SourceDescription sd3 = new SourceDescription();
    sd3.setId("sd3");
    sd3.setAbout(new URI("http://test.com/record1"));
    sd3.setIdentifiers(Arrays.asList(new Identifier(new URI("http://test.com/record1"), IdentifierType.Primary)));
    SourceReference componentOf = new SourceReference();
    componentOf.setDescriptionRef(new URI("#sd3"));
    sd1.setComponentOf(componentOf);
    doc.addSourceDescription(sd3);

    Agent agent = new Agent();
    agent.setId("agent1");
    doc.addAgent(agent);

    RecordDescriptor rd = new RecordDescriptor();
    rd.setId("rd1");
    doc.addRecordDescriptor(rd);

    DocMap docMap = new DocMap(doc);

    assertEquals("p1", docMap.getPerson("p1").getId());
    assertEquals("p1", docMap.getPerson("#p1").getId());
    assertEquals("p1", docMap.getPerson("http://test.com/person1").getId());
    assertEquals("p1", docMap.getPerson("http://alternate.com/oldPerson1").getId());
    assertEquals("sd1", docMap.getSourceDescription("sd1").getId());
    assertEquals("sd1", docMap.getSourceDescription("#sd1").getId());
    assertEquals("sd1", docMap.getSourceDescription("http://test.com/person1").getId());
    assertEquals("sd1", docMap.getSourceDescription("http://alternate.com/oldPerson1").getId());
    assertEquals("sd2", docMap.getSourceDescription("#sd2").getId());
    assertEquals("sd1", docMap.getSourceDescription(doc.getDescriptionRef()).getId());
    assertEquals("p1", docMap.getPerson(docMap.getSourceDescription(doc.getDescriptionRef()).getAbout()).getId());
    assertEquals("agent1", docMap.getAgent("agent1").getId());
    assertEquals("agent1", docMap.getAgent("#agent1").getId());
    assertEquals("rd1", docMap.getRecordDescriptor("rd1").getId());
    assertEquals("rd1", docMap.getRecordDescriptor("#rd1").getId());
    assertEquals("rd1", docMap.getRecordDescriptor("https://whatever.com/collections/12345#rd1").getId());
    assertEquals("sd1", docMap.getMainSourceDescription().getId());
    assertEquals("p1", docMap.getMainPerson().getId());
    assertEquals("place1", docMap.getPlaceDescription(docMap.getPerson("p1").getFacts().get(0).getPlace()).getId());
    assertEquals("place1", docMap.getPlaceDescription(docMap.getPerson("p1").getFacts().get(0).getPlace().getDescriptionRef()).getId());
    assertEquals("place1", docMap.getPlaceDescription(docMap.getPerson("p1").getFacts().get(0).getPlace().getDescriptionRef().toString()).getId());
    assertEquals("p1", docMap.getPerson(doc.getRelationships().get(0).getPerson1()).getId());
    assertEquals("p2", docMap.getPerson(doc.getRelationships().get(0).getPerson2()).getId());
  }

}