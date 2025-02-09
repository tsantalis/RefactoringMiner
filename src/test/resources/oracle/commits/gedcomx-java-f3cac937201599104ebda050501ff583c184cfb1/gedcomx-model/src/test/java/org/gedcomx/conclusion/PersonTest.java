package org.gedcomx.conclusion;

import org.gedcomx.common.*;
import org.gedcomx.source.SourceReference;
import org.gedcomx.types.*;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;


/**
 * @author Ryan Heaton
 */
@Test
public class PersonTest {

  /**
   * tests processing a WWW person through xml...
   */
  public void testPersonXml() throws Exception {
    Person person = create();
    person = processThroughXml(person);
    assertPersonEquals(person);
  }

  /**
   * tests processing a WWW person through json...
   */
  public void testPersonJson() throws Exception {
    Person person = create();
    person = processThroughJson(person);
    assertPersonEquals(person);
  }

  public void testDisplayProperties() throws Exception {
    Person person = new Person();
    DisplayProperties display = new DisplayProperties();
    display.setAscendancyNumber("1");
    display.setBirthDate("2");
    display.setBirthPlace("3");
    display.setDeathDate("4");
    display.setDeathPlace("5");
    display.setDescendancyNumber("6");
    display.setGender("7");
    display.setLifespan("8");
    display.setName("9");
    person.setDisplayExtension(display);
    person = processThroughXml(person);
    assertEquals("1", person.getDisplayExtension().getAscendancyNumber());
    assertEquals("2", person.getDisplayExtension().getBirthDate());
    assertEquals("3", person.getDisplayExtension().getBirthPlace());
    assertEquals("4", person.getDisplayExtension().getDeathDate());
    assertEquals("5", person.getDisplayExtension().getDeathPlace());
    assertEquals("6", person.getDisplayExtension().getDescendancyNumber());
    assertEquals("7", person.getDisplayExtension().getGender());
    assertEquals("8", person.getDisplayExtension().getLifespan());
    assertEquals("9", person.getDisplayExtension().getName());

  }

  public void testPersonPersistentIdHelpers() throws Exception {
    Person person = create();
    assertPersonEquals(person);
    assertEquals(2, person.getIdentifiers().size());
    assertEquals("pal", person.getPersistentId().toURI().toString());

    person.setPersistentId(URI.create("urn:pal"));
    assertEquals("urn:pal", person.getPersistentId().toURI().toString());

    person.getIdentifiers().clear();
    assertNull(person.getPersistentId());

    person.setIdentifiers(null);
    assertNull(person.getPersistentId());

    person.setPersistentId(URI.create("urn:pal"));
    assertEquals("urn:pal", person.getPersistentId().toURI().toString());
  }

  public void testPersonGetFirstNameOfType() throws Exception {
    Person person = create();
    assertPersonEquals(person);
    assertEquals("type=FormalName,nameForms[0]=primary form,pref=true", person.getFirstNameOfType(NameType.FormalName).toString());
    assertNull(person.getFirstNameOfType(NameType.BirthName));
    person.setNames(null);
    assertNull(person.getFirstNameOfType(NameType.FormalName));
  }

  public void testPersonGetPreferredName() throws Exception {
    Person person = create();
    assertPersonEquals(person);
    assertEquals("type=FormalName,nameForms[0]=primary form,pref=true", person.getPreferredName().toString());
    person.setNames(null);
    assertNull(person.getPreferredName());
  }

  public void testFactHelpers() throws Exception {
    Fact fact = new Fact();

    Person person = create();
    assertPersonEquals(person);
    person.addFact(fact);
    assertEquals(3, person.getFacts().size());
    assertEquals(1, person.getFacts(FactType.Adoption).size());
    assertEquals("type=Adoption,value=null,date=Date{original='original date', formal=normalized date},place=PlaceReference{original='original place', descriptionRef='urn:place'}", person.getFacts(FactType.Adoption).get(0).toString());
    assertEquals("type=Adoption,value=null,date=Date{original='original date', formal=normalized date},place=PlaceReference{original='original place', descriptionRef='urn:place'}", person.getFirstFactOfType(FactType.Adoption).toString());
    assertEquals(1, person.getFacts(FactType.Occupation).size());
    assertEquals("type=Occupation,value=fact-value,date=Date{original='original date', formal=formal},place=PlaceReference{original='original place', descriptionRef='urn:place'}", person.getFacts(FactType.Occupation).get(0).toString());
    assertEquals("type=Occupation,value=fact-value,date=Date{original='original date', formal=formal},place=PlaceReference{original='original place', descriptionRef='urn:place'}", person.getFirstFactOfType(FactType.Occupation).toString());

    person.getFacts().clear();
    assertNotNull(person.getFacts());
    assertEquals(0, person.getFacts(FactType.Adoption).size());
    assertEquals(0, person.getFacts(null).size());
    assertNull(person.getFirstFactOfType(FactType.Adoption));
    person.setFacts(null);
    assertNull(person.getFacts());
    assertEquals(0, person.getFacts(FactType.Adoption).size());
    assertNull(person.getFirstFactOfType(FactType.Adoption));

    person.addFact(null);
    assertNull(person.getFacts());
  }

  static Person create() {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
    Identifier identifier = new Identifier();
    identifier.setKnownType(IdentifierType.Deprecated);
    identifier.setValue(URI.create("forward-value"));
    identifiers.add(identifier);
    identifier = new Identifier();
    identifier.setKnownType(IdentifierType.Persistent);
    identifier.setValue(URI.create("pal"));
    identifiers.add(identifier);
    person.setIdentifiers(identifiers);

    Fact fact = new Fact();
    fact.setKnownConfidenceLevel(ConfidenceLevel.High);
    fact.setDate(new Date());
    fact.getDate().setOriginal("original date");
    fact.getDate().setFormal("formal");
    fact.getDate().setNormalizedExtensions(Arrays.asList(new TextValue("normalized date")));
    fact.setId("fact-id");
    fact.setKnownType(FactType.Occupation);
    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("original place");
    fact.getPlace().setDescriptionRef(URI.create("urn:place"));
    fact.setValue("fact-value");
    person.addFact(fact);

    Fact event = new Fact();
    event.setDate(new Date());
    event.getDate().setOriginal("original date");
    event.getDate().setFormal("normalized date");
    event.setId("event-id");
    event.setKnownType(FactType.Adoption);
    event.setPlace(new PlaceReference());
    event.getPlace().setOriginal("original place");
    event.getPlace().setDescriptionRef(URI.create("urn:place"));
    event.setSources(new ArrayList<SourceReference>());
    SourceReference eventSource = new SourceReference();
    eventSource.setDescriptionRef(URI.create("urn:event-source"));
    eventSource.setAttribution(new Attribution());
    event.getSources().add(eventSource);

    List<Fact> facts = person.getFacts();
    facts.add(event);
    person.setFacts(facts);

    Name name = new Name();
    name.setId("name-id");
    name.setPreferred(true);
    name.setKnownType(NameType.FormalName);
    name.setNameForms(new ArrayList<NameForm>());
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(0).setFullText("primary form");
    name.getNameForms().get(0).setParts(new ArrayList<NamePart>());
    name.getNameForms().get(0).getParts().add(new NamePart());
    name.getNameForms().get(0).getParts().get(0).setKnownType(NamePartType.Surname);
    name.getNameForms().get(0).getParts().get(0).setValue("primary surname");
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(1).setFullText("alternate name form");
    name.getNameForms().get(1).setParts(new ArrayList<NamePart>());
    name.getNameForms().get(1).getParts().add(new NamePart());
    name.getNameForms().get(1).getParts().get(0).setKnownType(NamePartType.Given);
    name.getNameForms().get(1).getParts().get(0).setValue("alternate name part");

    List<Name> names = new ArrayList<Name>();
    names.add(name);
    person.setNames(names);

    ArrayList<SourceReference> sources = new ArrayList<SourceReference>();
    SourceReference attributedSourceReference = new SourceReference();
    Attribution attribution = new Attribution();
    attribution.setContributor(new ResourceReference());
    attribution.getContributor().setResource(URI.create("urn:source-reference-attribution"));
    attributedSourceReference.setAttribution(attribution);
    attributedSourceReference.setDescriptionRef(URI.create("urn:source-description"));
    sources.add(attributedSourceReference);
    person.setSources(sources);

    person.setId("pid");
    person.setAttribution(new Attribution());
    person.getAttribution().setChangeMessage("this person existed.");

    person.setLiving(true);

    return person;
  }

  static void assertPersonEquals(Person person) {
    Fact fact;
    Fact event;
    Name name;
    SourceReference sr;
    assertEquals(GenderType.Male, person.getGender().getKnownType());

    assertEquals(2, person.getIdentifiers().size());
    Identifier identifier1 = person.getIdentifiers().get(0);
    Identifier identifier2 = person.getIdentifiers().get(1);
    Identifier deprecatedIdentifier = identifier1.getKnownType() == IdentifierType.Deprecated ? identifier1 : identifier2;
    Identifier persistentIdentifier = identifier1.getKnownType() == IdentifierType.Persistent ? identifier1 : identifier2;

    assertEquals(IdentifierType.Deprecated, deprecatedIdentifier.getKnownType());
    assertEquals("forward-value", deprecatedIdentifier.getValue().toString());
    assertEquals(IdentifierType.Persistent, persistentIdentifier.getKnownType());
    assertEquals("pal", persistentIdentifier.getValue().toString());

    assertEquals(2, person.getFacts().size());
    fact = person.getFirstFactOfType(FactType.Occupation);
    assertEquals(ConfidenceLevel.High, fact.getKnownConfidenceLevel());
    assertEquals("original date", fact.getDate().getOriginal());
    assertEquals("formal", fact.getDate().getFormal());
    assertEquals("normalized date", fact.getDate().getNormalizedExtensions().get(0).getValue());
    assertEquals("fact-id", fact.getId());
    assertEquals(FactType.Occupation, fact.getKnownType());
    assertEquals("original place", fact.getPlace().getOriginal());
    assertEquals("urn:place", fact.getPlace().getDescriptionRef().toURI().toString());
    assertEquals("fact-value", fact.getValue());

    event = person.getFirstFactOfType(FactType.Adoption);
    assertEquals("original date", event.getDate().getOriginal());
    assertEquals("normalized date", event.getDate().getFormal());
    assertEquals("event-id", event.getId());
    assertEquals(FactType.Adoption, event.getKnownType());
    assertEquals("original place", event.getPlace().getOriginal());
    assertEquals("urn:place", event.getPlace().getDescriptionRef().toURI().toString());

    assertEquals(1, person.getNames().size());
    name = person.getNames().iterator().next();
    assertTrue(name.getPreferred());
    assertEquals(2, name.getNameForms().size());
    assertEquals("alternate name form", name.getNameForms().get(1).getFullText());
    assertEquals(1, name.getNameForms().get(1).getParts().size());
    assertEquals("alternate name part", name.getNameForms().get(1).getParts().get(0).getValue());
    assertEquals(NamePartType.Given, name.getNameForms().get(1).getParts().get(0).getKnownType());
    assertEquals("name-id", name.getId());
    assertEquals(NameType.FormalName, name.getKnownType());
    assertEquals("primary form", name.getNameForms().get(0).getFullText());
    assertEquals(1, name.getNameForms().get(0).getParts().size());
    assertEquals("primary surname", name.getNameForms().get(0).getParts().get(0).getValue());
    assertEquals(NamePartType.Surname, name.getNameForms().get(0).getParts().get(0).getKnownType());

    assertEquals("pal", person.getPersistentId().toString());

    assertEquals(1, person.getSources().size());
    sr = person.getSources().iterator().next();
    assertEquals("urn:source-reference-attribution", sr.getAttribution().getContributor().getResource().toString());
    assertEquals("urn:source-description", sr.getDescriptionRef().toString());

    assertEquals("pid", person.getId());
    assertEquals("this person existed.", person.getAttribution().getChangeMessage());

    assertTrue(person.getLiving());
  }

}
