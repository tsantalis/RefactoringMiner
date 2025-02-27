package org.gedcomx.rt;

import org.gedcomx.Gedcomx;
import org.gedcomx.agent.Agent;
import org.gedcomx.common.Note;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.*;
import org.gedcomx.source.SourceCitation;
import org.gedcomx.source.SourceDescription;
import org.gedcomx.source.SourceReference;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class GedcomxModelVisitorBaseTest {
  @Test (expected = NullPointerException.class)
  public void testNullVisitor() throws Exception {
    Gedcomx gedcomxDocument = new Gedcomx();
    gedcomxDocument.accept(null);
  }

  @Test
  public void testVisitGedcomx() throws Exception {
    GedcomxModelVisitorBase visitor = new GedcomxModelVisitorBase();
    assertNotNull(visitor.getContextStack());
    assertEquals(visitor.getContextStack().size(), 0);

    Gedcomx gedcomxDocument = new Gedcomx();

    // visit empty document
    gedcomxDocument.accept(visitor);

    // re-visit document that now has empty lists (except for extension elements)
    gedcomxDocument.setAgents(new ArrayList<Agent>());
    gedcomxDocument.setDocuments(new ArrayList<Document>());
    gedcomxDocument.setEvents(new ArrayList<Event>());
    gedcomxDocument.setPersons(new ArrayList<Person>());
    gedcomxDocument.setPlaces(new ArrayList<PlaceDescription>());
    gedcomxDocument.setRelationships(new ArrayList<Relationship>());
    gedcomxDocument.setSourceDescriptions(new ArrayList<SourceDescription>());
    gedcomxDocument.addLink("junkRel", URI.create("urn:junkUri"));
    gedcomxDocument.addExtensionElement("junkExtensionElement");
    gedcomxDocument.accept(visitor);

    // re-visit document that now has lists with null elements
    gedcomxDocument.getAgents().add(null);
    gedcomxDocument.getDocuments().add(null);
    gedcomxDocument.getEvents().add(null);
    gedcomxDocument.getPersons().add(null);
    gedcomxDocument.getPlaces().add(null);
    gedcomxDocument.getRelationships().add(null);
    gedcomxDocument.getSourceDescriptions().add(null);
    gedcomxDocument.accept(visitor);

    // clear lists
    gedcomxDocument.getAgents().clear();
    gedcomxDocument.getDocuments().clear();
    gedcomxDocument.getEvents().clear();
    gedcomxDocument.getPersons().clear();
    gedcomxDocument.getPlaces().clear();
    gedcomxDocument.getRelationships().clear();
    gedcomxDocument.getSourceDescriptions().clear();

    // re-visit document that now has single element lists -- the elements are newly constructed and otherwise uninitialized
    gedcomxDocument.getAgents().add(new Agent());
    gedcomxDocument.getDocuments().add(new Document());
    gedcomxDocument.getEvents().add(new Event());
    gedcomxDocument.getPersons().add(new Person());
    gedcomxDocument.getPlaces().add(new PlaceDescription());
    gedcomxDocument.getRelationships().add(new Relationship());
    gedcomxDocument.getSourceDescriptions().add(new SourceDescription());
    gedcomxDocument.accept(visitor);

    ArrayList<SourceReference> sourceReferences;
    ArrayList<Note> notes;

    // re-visit document but add empty source and notes lists
    sourceReferences = new ArrayList<SourceReference>();
    notes = new ArrayList<Note>();
    gedcomxDocument.getDocuments().get(0).setSources(sourceReferences);
    gedcomxDocument.getDocuments().get(0).setNotes(notes);
    gedcomxDocument.getEvents().get(0).setSources(sourceReferences);
    gedcomxDocument.getEvents().get(0).setNotes(notes);
    gedcomxDocument.getPersons().get(0).setSources(sourceReferences);
    gedcomxDocument.getPersons().get(0).setNotes(notes);
    gedcomxDocument.getPlaces().get(0).setSources(sourceReferences);
    gedcomxDocument.getPlaces().get(0).setNotes(notes);
    gedcomxDocument.getRelationships().get(0).setSources(sourceReferences);
    gedcomxDocument.getRelationships().get(0).setNotes(notes);
    gedcomxDocument.getSourceDescriptions().get(0).setSources(sourceReferences);
    gedcomxDocument.getSourceDescriptions().get(0).setNotes(notes);
    gedcomxDocument.accept(visitor);

    // re-visit document but source and note lists now has single element
    sourceReferences.add(new SourceReference());
    notes.add(new Note());
    gedcomxDocument.accept(visitor);

    // re-visit document with event now initialized with a date, place reference, and empty roles list
    gedcomxDocument.getEvents().get(0).setDate(new Date());
    gedcomxDocument.getEvents().get(0).setPlace(new PlaceReference());
    gedcomxDocument.getEvents().get(0).setRoles(new ArrayList<EventRole>());
    gedcomxDocument.accept(visitor);

    // re-visit document with event now initialized with a date, place reference, and single element roles list
    gedcomxDocument.getEvents().get(0).getRoles().add(new EventRole());
    gedcomxDocument.accept(visitor);

    // re-visit document, but source description now has an empty citation list
    gedcomxDocument.getSourceDescriptions().get(0).setCitations(new ArrayList<SourceCitation>());
    gedcomxDocument.accept(visitor);

    // re-visit document, but source description now has a single-element citation list
    gedcomxDocument.getSourceDescriptions().get(0).getCitations().add(new SourceCitation());
    gedcomxDocument.accept(visitor);

    ArrayList<Fact> facts;
    ArrayList<Name> names;

    // re-visit document, but initialize person (names, gender, facts) and relationship (facts)
    names = new ArrayList<Name>();
    facts = new ArrayList<Fact>();
    gedcomxDocument.getPersons().get(0).setNames(names);
    gedcomxDocument.getPersons().get(0).setGender(new Gender());
    gedcomxDocument.getPersons().get(0).setFacts(facts);
    gedcomxDocument.getRelationships().get(0).setFacts(facts);
    gedcomxDocument.accept(visitor);

    // re-visit document, now name and fact lists are single element lists
    names.add(new Name());
    facts.add(new Fact());
    gedcomxDocument.accept(visitor);

    // re-visit document, now name has empty name form list; and fact element date and place reference
    names.get(0).setNameForms(new ArrayList<NameForm>());
    facts.get(0).setDate(new Date());
    facts.get(0).setPlace(new PlaceReference());
    gedcomxDocument.accept(visitor);

    // re-visit document, now name has single-element name form list -- name form is uninitialized
    names.get(0).getNameForms().add(new NameForm());
    gedcomxDocument.accept(visitor);

    // re-visit document, now name form element has empty name part list
    names.get(0).getNameForms().get(0).setParts(new ArrayList<NamePart>());
    gedcomxDocument.accept(visitor);

    // re-visit document, now name form element has single-element name part list
    names.get(0).getNameForms().get(0).getParts().add(new NamePart());
    gedcomxDocument.accept(visitor);

    assertNotNull(visitor.getContextStack());
    assertEquals(visitor.getContextStack().size(), 0);
  }
}
