package org.gedcomx.examples;

import org.gedcomx.Gedcomx;
import org.gedcomx.agent.Agent;
import org.gedcomx.common.Attribution;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.TextValue;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.*;
import org.gedcomx.rt.SerializationUtil;
import org.gedcomx.source.SourceCitation;
import org.gedcomx.source.SourceDescription;
import org.gedcomx.source.SourceReference;
import org.gedcomx.types.FactType;
import org.gedcomx.types.GenderType;
import org.gedcomx.types.NamePartType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ryan Heaton
 */
@Test
public class GeorgeMarthaWashingtonExampleTest {

//  public void testEventsOnPlaces() throws Exception {
//    PlaceDescription place = new PlaceDescription().id("P1").name("Family Farm in Scandinavia");
//    Event event1 = new Event().id("E1").place(new PlaceReference().description(place));
//    Event event2 = new Event().id("E2").place(new PlaceReference().description(place));
//    Event event3 = new Event().id("E3").place(new PlaceReference().description(place));
//    Gedcomx gx = new Gedcomx();
//    gx.place(place).event(event1).event(event2).event(event3);
//
//    byte[] bytes = SerializationUtil.toJsonStream(gx);
//    System.out.println(new String(bytes, "UTF-8"));
//  }
//
  public void testExample() throws Exception {
    PlaceDescription popesCreek = createPopesCreek();
    PlaceDescription mountVernon = createMountVernon();
    PlaceDescription chestnutGrove = createChestnutGrove();
    Person george = createGeorge(popesCreek, mountVernon);
    Person martha = createMartha(chestnutGrove, mountVernon);
    Relationship marriage = createMarriage(george, martha);
    List<SourceDescription> sources = citeGeorgeMarthaAndMarriage(george, martha, marriage);
    Agent contributor = createContributor();
    Gedcomx gx = new Gedcomx();
    gx.setPersons(Arrays.asList(george, martha));
    gx.setRelationships(Arrays.asList(marriage));
    gx.setSourceDescriptions(sources);
    gx.setAgents(Arrays.asList(contributor));
    gx.setAttribution(new Attribution());
    gx.getAttribution().setContributor(new ResourceReference());
    gx.getAttribution().getContributor().setResource(URI.create("#" + contributor.getId()));
    gx.setPlaces(Arrays.asList(popesCreek, mountVernon, chestnutGrove));

    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  private PlaceDescription createPopesCreek() {
    PlaceDescription place = new PlaceDescription();
    place.setId("888");
    place.setLatitude(38.192353);
    place.setLongitude(-76.904069);
    place.setNames(Arrays.asList(new TextValue("Pope's Creek, Westmoreland, Virginia, United States")));
    return place;
  }

  private PlaceDescription createMountVernon() {
    PlaceDescription place = new PlaceDescription();
    place.setId("999");
    place.setLatitude(38.721144);
    place.setLongitude(-77.109461);
    place.setNames(Arrays.asList(new TextValue("Mount Vernon, Fairfax County, Virginia, United States")));
    return place;
  }

  private PlaceDescription createChestnutGrove() {
    PlaceDescription place = new PlaceDescription();
    place.setId("KKK");
    place.setLatitude(37.518304);
    place.setLongitude(-76.984148);
    place.setNames(Arrays.asList(new TextValue("Chestnut Grove, New Kent, Virginia, United States")));
    return place;
  }

  private Agent createContributor() {
    Agent agent = new Agent();
    agent.setId("GGG-GGGG");
    agent.setNames(Arrays.asList(new TextValue("Ryan Heaton")));
    return agent;
  }

  private Person createGeorge(PlaceDescription birthPlace, PlaceDescription deathPlace) {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    Fact fact = new Fact();
    fact.setId("123");
    fact.setKnownType(FactType.Birth);

    fact.setDate(new Date());
    fact.getDate().setOriginal("February 22, 1732");
    fact.getDate().setFormal("+1732-02-22");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal(birthPlace.getNames().get(0).getValue().toLowerCase());
    fact.getPlace().setDescriptionRef(URI.create("#" + birthPlace.getId()));

    person.addFact(fact);

    fact = new Fact();
    fact.setId("456");
    fact.setKnownType(FactType.Death);

    fact.setDate(new Date());
    fact.getDate().setOriginal("December 14, 1799");
    fact.getDate().setFormal("+1799-12-14T22:00:00");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal(deathPlace.getNames().get(0).getValue().toLowerCase());
    fact.getPlace().setDescriptionRef(URI.create("#" + deathPlace.getId()));

    person.addFact(fact);

    List<Name> names = new ArrayList<Name>();
    Name name = new Name();
    NameForm nameForm = new NameForm();
    nameForm.setFullText("George Washington");
    ArrayList<NamePart> parts = new ArrayList<NamePart>();
    NamePart part = new NamePart();
    part.setKnownType(NamePartType.Given);
    part.setValue("George");
    parts.add(part);
    part = new NamePart();
    part.setKnownType(NamePartType.Surname);
    part.setValue("Washington");
    parts.add(part);
    nameForm.setParts(parts);
    name.setNameForms(Arrays.asList(nameForm));
    name.setId("789");
    names.add(name);
    person.setNames(names);

    person.setId("BBB-BBBB");

    return person;
  }

  private Person createMartha(PlaceDescription birthPlace, PlaceDescription deathPlace) {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    Fact fact = new Fact();
    fact.setId("321");
    fact.setKnownType(FactType.Birth);

    fact.setDate(new Date());
    fact.getDate().setOriginal("June 2, 1731");
    fact.getDate().setFormal("+1731-06-02");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal(birthPlace.getNames().get(0).getValue().toLowerCase());
    fact.getPlace().setDescriptionRef(URI.create("#" + birthPlace.getId()));

    person.addFact(fact);

    fact = new Fact();
    fact.setId("654");
    fact.setKnownType(FactType.Death);

    fact.setDate(new Date());
    fact.getDate().setOriginal("May 22, 1802");
    fact.getDate().setFormal("+1802-05-22");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal(deathPlace.getNames().get(0).getValue().toLowerCase());
    fact.getPlace().setDescriptionRef(URI.create("#" + deathPlace.getId()));

    person.addFact(fact);

    List<Name> names = new ArrayList<Name>();
    Name name = new Name();
    NameForm nameForm = new NameForm();
    nameForm.setFullText("Martha Dandridge Custis");
    ArrayList<NamePart> parts = new ArrayList<NamePart>();
    NamePart part = new NamePart();
    part.setKnownType(NamePartType.Given);
    part.setValue("Martha Dandridge");
    parts.add(part);
    part = new NamePart();
    part.setKnownType(NamePartType.Surname);
    part.setValue("Custis");
    parts.add(part);
    nameForm.setParts(parts);
    name.setNameForms(Arrays.asList(nameForm));
    name.setId("987");
    names.add(name);
    person.setNames(names);

    person.setId("CCC-CCCC");

    return person;
  }

  private Relationship createMarriage(Person george, Person martha) {
    Relationship relationship = new Relationship();
    relationship.setId("DDD-DDDD");
    relationship.setPerson1(new ResourceReference(URI.create("#" + george.getId())));
    relationship.setPerson2(new ResourceReference(URI.create("#" + martha.getId())));
    Fact marriage = new Fact();
    marriage.setDate(new Date());
    marriage.getDate().setOriginal("January 6, 1759");
    marriage.getDate().setFormal("+01-06-1759");
    marriage.setPlace(new PlaceReference());
    marriage.getPlace().setOriginal("White House Plantation");
    relationship.setFacts(Arrays.asList(marriage));
    return relationship;
  }

  private List<SourceDescription> citeGeorgeMarthaAndMarriage(Person george, Person martha, Relationship relationship) {
    SourceDescription georgeSource = new SourceDescription();
    georgeSource.setId("EEE-EEEE");
    georgeSource.setAbout(URI.create("http://en.wikipedia.org/wiki/George_washington"));
    SourceCitation georgeCitation = new SourceCitation();
    georgeCitation.setValue("\"George Washington.\" Wikipedia, The Free Encyclopedia. Wikimedia Foundation, Inc. 24 October 2012.");
    georgeSource.setCitations(Arrays.asList(georgeCitation));

    SourceDescription marthaSource = new SourceDescription();
    marthaSource.setId("FFF-FFFF");
    marthaSource.setAbout(URI.create("http://en.wikipedia.org/wiki/Martha_washington"));
    SourceCitation marthaCitation = new SourceCitation();
    marthaCitation.setValue("\"Martha Washington.\" Wikipedia, The Free Encyclopedia. Wikimedia Foundation, Inc. 24 October 2012.");
    marthaSource.setCitations(Arrays.asList(marthaCitation));

    SourceReference reference = new SourceReference();
    reference.setDescriptionRef(URI.create("#" + georgeSource.getId()));
    george.setSources(Arrays.asList(reference));

    reference = new SourceReference();
    reference.setDescriptionRef(URI.create("#" + marthaSource.getId()));
    martha.setSources(Arrays.asList(reference));

    relationship.setSources(Arrays.asList(reference));

    return Arrays.asList(georgeSource, marthaSource);
  }


}
