package org.gedcomx.conclusion;

import org.gedcomx.common.Attribution;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.TextValue;
import org.gedcomx.source.SourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.source.CitationField;
import org.gedcomx.source.SourceCitation;
import org.gedcomx.source.SourceDescription;
import org.gedcomx.test.RecipeTest;
import org.gedcomx.test.Snippet;
import org.gedcomx.types.FactType;
import org.gedcomx.types.GenderType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;


/**
 * Recipes for citing evidence in GEDCOM X.
 * @author Ryan Heaton
 */
public class EvidenceRecipesTest extends RecipeTest {

  /**
   * tests citing an online record.
   */
  @Test
  public void testCitingOnlineArtifact() throws Exception {
    createRecipe("Citing an Online Artifact")
      .withDescription("The following example illustrates how to cite an online record. Evidence for Israel Hoyt Heaton is found in the 1920 U.S. Census. The URI to the record is \"https://familysearch.org/pal:/MM9.1.1/M8PT-4GN\". The URI for a description of the record is \"https://api.familysearch.org/platform/sources/GGG-GGGG\".")
      .applicableTo(Person.class);

    Person person = createPersonCitingOnlineArtifact();

    Snippet snippet = new Snippet("The person.");
    Person personThruXml = processThroughXml(person, snippet);
    Person personThruJson = processThroughJson(person, snippet);
    addSnippet(snippet);

    verifyPerson(personThruXml);
    verifyPerson(personThruJson);

    SourceDescription sourceDescription = createDescriptionOfOnlineArtifact();

    snippet = new Snippet("The description of the source. The URI to the description is \"https://api.familysearch.org/platform/sources/GGG-GGGG\".");
    SourceDescription sourceDescriptionThruXml = processThroughXml(sourceDescription, snippet);
    SourceDescription sourceDescriptionThruJson = processThroughJson(sourceDescription, snippet);
    addSnippet(snippet);

    verifySourceDescription(sourceDescriptionThruXml);
    verifySourceDescription(sourceDescriptionThruJson);
  }

  static SourceDescription createDescriptionOfOnlineArtifact() {
    SourceDescription sourceDescription = new SourceDescription();
    sourceDescription.setCitations(Arrays.asList(new SourceCitation()));
    sourceDescription.getCitation().setValue("\"United States Census, 1920,\" index and images, FamilySearch (https://familysearch.org/pal:/MM9.1.1/M8PT-4GN : accessed 31 May 2012), Israel H Heaton, , Kane, Utah.");
    sourceDescription.setTitles(Arrays.asList(new TextValue("\"United States Census, 1920,\" Israel H Heaton, , Kane, Utah")));
    sourceDescription.setAbout(URI.create("https://familysearch.org/pal:/MM9.1.1/M8PT-4GN"));
    return sourceDescription;
  }

  static Person createPersonCitingOnlineArtifact() {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    Fact fact = new Fact();
    fact.setId("123");
    fact.setKnownType(FactType.Birth);

    fact.setDate(new Date());
    fact.getDate().setOriginal("30 January 1880");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Orderville, Utah");

    person.addFact(fact);

    fact = new Fact();
    fact.setId("456");
    fact.setKnownType(FactType.Death);

    fact.setDate(new Date());
    fact.getDate().setOriginal("29 August 1936");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Kanab, Kane, UT");

    person.addFact(fact);

    Name name = new Name();
    name.setId("789");
    name.setPreferred(true);
    name.setNameForms(new ArrayList<NameForm>());
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(0).setFullText("Israel Hoyt Heaton");

    List<Name> names = new ArrayList<Name>();
    names.add(name);
    person.setNames(names);

    ArrayList<SourceReference> sources = new ArrayList<SourceReference>();
    SourceReference attributedSourceReference = new SourceReference();
    Attribution attribution = new Attribution();
    attribution.setContributor(new ResourceReference());
    attribution.getContributor().setResource(URI.create("https://api.familysearch.org/platform/contributors/STV-WXZY"));
    attributedSourceReference.setAttribution(attribution);
    attributedSourceReference.setDescriptionRef(URI.create("https://api.familysearch.org/platform/sources/GGG-GGGG"));
    sources.add(attributedSourceReference);
    person.setSources(sources);

    person.setId("KWCD-QBC");

    return person;
  }

  /**
   * tests citing an online record.
   */
  @Test
  public void testCitingPhysicalArtifact() throws Exception {
    createRecipe("Citing a Physical Artifact")
      .withDescription("The following example illustrates how to cite a physical artifact, such as a book. Evidence for Asa Phillips is found in a book authored by Helen Kelly Brink. The book doesn't have a URI, but the URI for a description of the book is \"https://api.familysearch.org/platform/sources/KKK-KKKK\".")
      .applicableTo(Person.class);

    Person person = createPersonCitingPhysicalArtifact();

    Snippet snippet = new Snippet("The person.");
    Person personThruXml = processThroughXml(person, snippet);
    Person personThruJson = processThroughJson(person, snippet);
    addSnippet(snippet);

    verifyPerson(personThruXml);
    verifyPerson(personThruJson);

    SourceDescription description = createDescriptionOfPhysicalArtifact();

    snippet = new Snippet("The description of the source. The URI to the description is \"https://api.familysearch.org/platform/sources/KKK-KKKK\".");
    SourceDescription descriptionThruXml = processThroughXml(description, snippet);
    SourceDescription descriptionThruJson = processThroughJson(description, snippet);
    addSnippet(snippet);

    verifySourceDescription(descriptionThruXml);
    verifySourceDescription(descriptionThruJson);
  }

  static SourceDescription createDescriptionOfPhysicalArtifact() {
    SourceDescription sourceDescription = new SourceDescription();
    sourceDescription.setId("KKK-KKKK");
    sourceDescription.setCitations(Arrays.asList(new SourceCitation()));
    sourceDescription.getCitation().setValue("Helen Kelly Brink, Some of the Descendants of Asa Phillips (1793-1844); Who were Born in Vermont and Who Settled in Steuben County, New York in 1802, (Marco Island, Florida, By the Author, 1992) p.34");
    //sourceDescription.getCitation().setFields(new ArrayList<CitationField>());
    //sourceDescription.getCitation().getFields().add(new CitationField("title", "Some of the Descendants of Asa Phillips (1793-1844); Who were Born in Vermont and Who Settled in Steuben County, New York in 1802"));
    //sourceDescription.getCitation().getFields().add(new CitationField("author", "Helen Kelly Brink"));
    //sourceDescription.getCitation().getFields().add(new CitationField("publisher", "Helen Kelly Brink"));
    //sourceDescription.getCitation().getFields().add(new CitationField("publisher-locality", "Steuben County, New York"));
    //sourceDescription.getCitation().getFields().add(new CitationField("publish-date", "1802"));
    return sourceDescription;
  }

  static Person createPersonCitingPhysicalArtifact() {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    Fact fact = new Fact();
    fact.setId("123");
    fact.setKnownType(FactType.Birth);

    fact.setDate(new Date());
    fact.getDate().setOriginal("1793");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Vermont");

    person.addFact(fact);

    fact = new Fact();
    fact.setId("456");
    fact.setKnownType(FactType.Death);

    fact.setDate(new Date());
    fact.getDate().setOriginal("1844");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Steuben County, New York");

    person.addFact(fact);

    Name name = new Name();
    name.setId("789");
    name.setPreferred(true);
    name.setNameForms(new ArrayList<NameForm>());
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(0).setFullText("Asa Phillips");

    List<Name> names = new ArrayList<Name>();
    names.add(name);
    person.setNames(names);

    ArrayList<SourceReference> sources = new ArrayList<SourceReference>();
    SourceReference attributedSourceReference = new SourceReference();
    Attribution attribution = new Attribution();
    attribution.setContributor(new ResourceReference());
    attribution.getContributor().setResource(URI.create("https://api.familysearch.org/platform/contributors/STV-WXZY"));
    attributedSourceReference.setAttribution(attribution);
    attributedSourceReference.setDescriptionRef(URI.create("https://api.familysearch.org/platform/sources/KKK-KKKK"));
    sources.add(attributedSourceReference);
    person.setSources(sources);

    person.setId("K2VD-4P2");

    return person;
  }


  /**
   * tests citing an online record.
   */
  @Test
  public void testConclusionsCitingOtherConclusions() throws Exception {
    createRecipe("Citing Other Conclusions")
      .withDescription("The following example illustrates how to cite another conclusion. Evidence for Israel Heaton is found in another conclusion person that (presumably) describes the same person. The URI to the person being cited is \"https://api.familysearch.org/platform/persons/NNN-NNNN\".")
      .applicableTo(Person.class);

    Person person = createPersonCitingOtherConclusion();
    Snippet snippet = new Snippet("The person.");
    processThroughXml(person, snippet);
    processThroughJson(person, snippet);
    addSnippet(snippet);

    snippet = new Snippet("The person being cited. The URI to this conclusion person is \"https://api.familysearch.org/platform/sources/NNN-NNNN\".");
    Person description = createPersonBeingCitedByAnotherPerson();
    Person descriptionThruXml = processThroughXml(description, snippet);
    Person descriptionThruJson = processThroughJson(description, snippet);
    addSnippet(snippet);

    verifyPerson(descriptionThruXml);
    verifyPerson(descriptionThruJson);
  }

  static Person createPersonCitingOtherConclusion() {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    Fact fact = new Fact();
    fact.setId("123");
    fact.setKnownType(FactType.Birth);

    fact.setDate(new Date());
    fact.getDate().setOriginal("30 January 1880");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Orderville, Utah");

    person.addFact(fact);

    fact = new Fact();
    fact.setId("456");
    fact.setKnownType(FactType.Death);

    fact.setDate(new Date());
    fact.getDate().setOriginal("29 August 1936");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Kanab, Kane, UT");

    person.addFact(fact);

    Name name = new Name();
    name.setId("789");
    name.setPreferred(true);
    name.setNameForms(new ArrayList<NameForm>());
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(0).setFullText("Israel Hoyt Heaton");

    List<Name> names = new ArrayList<Name>();
    names.add(name);
    person.setNames(names);

    person.setId("KWCD-QBC");

    return person;
  }

  static Person createPersonBeingCitedByAnotherPerson() {
    Person person = new Person();
    person.setGender(new Gender(GenderType.Male));

    Fact fact = new Fact();
    fact.setId("123");
    fact.setKnownType(FactType.Birth);

    fact.setDate(new Date());
    fact.getDate().setOriginal("1880");

    fact.setPlace(new PlaceReference());
    fact.getPlace().setOriginal("Orderville, Utah");

    person.addFact(fact);

    Name name = new Name();
    name.setId("789");
    name.setPreferred(true);
    name.setNameForms(new ArrayList<NameForm>());
    name.getNameForms().add(new NameForm());
    name.getNameForms().get(0).setFullText("Israel H. Heaton");

    List<Name> names = new ArrayList<Name>();
    names.add(name);
    person.setNames(names);

    ArrayList<SourceReference> sources = new ArrayList<SourceReference>();
    SourceReference attributedSourceReference = new SourceReference();
    Attribution attribution = new Attribution();
    attribution.setContributor(new ResourceReference());
    attribution.getContributor().setResource(URI.create("https://api.familysearch.org/platform/contributors/ZZZ-ZZZZ"));
    attributedSourceReference.setAttribution(attribution);
    attributedSourceReference.setDescriptionRef(URI.create("https://api.familysearch.org/platform/sources/JJJ-JJJJ"));
    sources.add(attributedSourceReference);
    person.setSources(sources);

    person.setId("KWCD-QBC");

    return person;
  }

  static void verifySourceDescription(SourceDescription sourceDescription) {
    //TODO: verify contents of source description
  }

  static void verifyPerson(Person person) {
    //TODO: verify contents of person
  }
}
