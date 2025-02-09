package org.gedcomx.conclusion;

import org.gedcomx.common.Qualifier;
import org.gedcomx.common.URI;
import org.gedcomx.rt.json.GedcomJacksonModule;
import org.gedcomx.test.RecipeTest;
import org.gedcomx.test.Snippet;
import org.gedcomx.types.FactType;
import org.gedcomx.types.GenderType;
import org.gedcomx.types.NamePartQualifierType;
import org.gedcomx.types.NamePartType;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;


/**
 * @author Ryan Heaton
 */
@Test
public class PersonRecipesTest extends RecipeTest {

  @XmlRootElement ( namespace = "http://familysearch.org/v1/" )
  public static class CustomMarker {

    private boolean userProvided;

    public CustomMarker() {
      userProvided = true;
    }

    @XmlAttribute ( name = "userProvided" )
    public boolean isUserProvided() {
      return userProvided;
    }

    public void setUserProvided(boolean userProvided) {
      this.userProvided = userProvided;
    }
  }

  /**
   * tests processing a WWW person through xml...
   */
  public void testStandardPerson() throws Exception {
    createRecipe("Simple Person")
      .withDescription("Simple example for a person.")
      .applicableTo(Person.class);

    Person person = create();

    Snippet snippet = new Snippet();
    Person personThurXml = processThroughXml(person, Person.class, JAXBContext.newInstance(Person.class, CustomMarker.class), snippet);
    Person personThurJson = processThroughJson(person, Person.class, GedcomJacksonModule.createObjectMapper(Person.class, CustomMarker.class), snippet);
    addSnippet(snippet);

    verifyPerson(personThurXml);
    verifyPerson(personThurJson);
  }

  public void testMarriageWithNoSpouse() throws Exception {
    createRecipe("Marriage Fact With No Spouse Provided")
      .withDescription("How to model a marriage (or divorce) event for which the spouse is not available or otherwise not provided.")
      .applicableTo(Person.class);

    Person person = new Person();
    Fact fact = new Fact();
    fact.setKnownType(FactType.Marriage);
    fact.setDate(new Date());
    fact.getDate().setOriginal("January 6, 1759");
    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("New Kent, Virginia");
    person.addFact(fact);

    Snippet snippet = new Snippet("Note that the recommendation is to add a marriage fact directly to the person. It is not recommended to create a relationship with only one person.");
    Person personThruXml = processThroughXml(person, snippet);
    Person personThruJson = processThroughJson(person, snippet);
    addSnippet(snippet);

    verifyPerson(personThruXml);
    verifyPerson(personThruJson);
  }


  static Person create() {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    Fact fact = new Fact();
    fact.setId("123");
    fact.setKnownType(FactType.Birth);

    fact.setDate(new Date());
    fact.getDate().setOriginal("March 18, 1844");
    fact.getDate().setFormal("+1844-03-18");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Tikhvin, Leningradskaya Oblast', Russia");
    fact.getPlace().setDescriptionRef(URI.create("#tikhvinDesc1"));
    // Tikhvin, Leningradskaya Oblast', Russia
    // https://labs.familysearch.org/stdfinder/PlaceDetail.jsp?placeId=3262902

    person.addFact(fact);

    fact = new Fact();
    fact.setId("456");
    fact.setKnownType(FactType.Death);

    fact.setDate(new Date());
    fact.getDate().setOriginal("June 21, 1908");
    fact.getDate().setFormal("+1908-06-21T12:34:56");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Luga, Russia");
    fact.getPlace().setDescriptionRef(URI.create("#lugaDesc1"));
    // Luga, Leningradskaya Oblast', Russia
    // https://labs.familysearch.org/stdfinder/PlaceDetail.jsp?placeId=3314013

    person.addFact(fact);

    Name name = new Name();
    name.setId("789");
    name.setPreferred(true);
    name.setNameForms(new ArrayList<NameForm>());
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(0).setLang("ru-Cyrl");
    name.getNameForms().get(0).setFullText("Никола́й Андре́евич Ри́мский-Ко́рсаков");
    name.getNameForms().get(0).setParts(new ArrayList<NamePart>());
    name.getNameForms().get(0).getParts().add(new NamePart());
    name.getNameForms().get(0).getParts().get(0).setKnownType(NamePartType.Given);
    name.getNameForms().get(0).getParts().get(0).setValue("Никола́й");
    name.getNameForms().get(0).getParts().get(0).setQualifiers(new ArrayList<Qualifier>());
    name.getNameForms().get(0).getParts().get(0).getQualifiers().add(new Qualifier(NamePartQualifierType.Primary));
    name.getNameForms().get(0).getParts().add(new NamePart());
    name.getNameForms().get(0).getParts().get(1).setKnownType(NamePartType.Given);
    name.getNameForms().get(0).getParts().get(1).setValue("Андре́евич");
    name.getNameForms().get(0).getParts().get(1).setQualifiers(new ArrayList<Qualifier>());
    name.getNameForms().get(0).getParts().get(1).getQualifiers().add(new Qualifier(NamePartQualifierType.Secondary));
    name.getNameForms().get(0).getParts().add(new NamePart());
    name.getNameForms().get(0).getParts().get(2).setKnownType(NamePartType.Surname);
    name.getNameForms().get(0).getParts().get(2).setValue("Ри́мский-Ко́рсаков");
    name.getNameForms().get(0).addExtensionElement(new CustomMarker());
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(1).setLang("ru-Latn");
    name.getNameForms().get(1).setFullText("Nikolai Andreyevich Rimsky-Korsakov");
    name.getNameForms().get(1).setParts(new ArrayList<NamePart>());
    name.getNameForms().get(1).getParts().add(new NamePart());
    name.getNameForms().get(1).getParts().get(0).setKnownType(NamePartType.Given);
    name.getNameForms().get(1).getParts().get(0).setValue("Nikolai");
    name.getNameForms().get(1).getParts().get(0).setQualifiers(new ArrayList<Qualifier>());
    name.getNameForms().get(1).getParts().get(0).getQualifiers().add(new Qualifier(NamePartQualifierType.Primary));
    name.getNameForms().get(1).getParts().add(new NamePart());
    name.getNameForms().get(1).getParts().get(1).setKnownType(NamePartType.Given);
    name.getNameForms().get(1).getParts().get(1).setValue("Andreyevich");
    name.getNameForms().get(1).getParts().get(1).setQualifiers(new ArrayList<Qualifier>());
    name.getNameForms().get(1).getParts().get(1).getQualifiers().add(new Qualifier(NamePartQualifierType.Secondary));
    name.getNameForms().get(1).getParts().add(new NamePart());
    name.getNameForms().get(1).getParts().get(2).setKnownType(NamePartType.Surname);
    name.getNameForms().get(1).getParts().get(2).setValue("Rimsky-Korsakov");

    person.setNames(new ArrayList<Name>());
    person.getNames().add(name);

    person.setId("BBB-BBBB");

    return person;
  }

  static void verifyPerson(Person person) {
    //TODO: verify contents of person
  }
}
