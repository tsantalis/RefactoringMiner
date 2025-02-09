package org.gedcomx.conclusion;

import org.gedcomx.test.RecipeTest;
import org.gedcomx.test.Snippet;
import org.gedcomx.types.FactType;
import org.junit.Test;

import java.util.ArrayList;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;

/**
 * @author Ryan Heaton
 */
public class DateRecipesTest extends RecipeTest {

  @Test
  public void testDateFormalValueIso8601() throws Exception {
    createRecipe("Standardized Date Using ISO 8601")
      .withDescription("Simple recipe for creating a standardized date using the ISO 8601 standard. The example shows a person born on July 1, 1980.")
      .applicableTo(Date.class);


    Person person = new Person();
    person.setId("12345");
    person.setFacts(new ArrayList<Fact>());
    Fact fact = new Fact();
    fact.setKnownType(FactType.Birth);
    person.getFacts().add(fact);
    Date date = new Date();
    date.setOriginal("1 July 1980");
    date.setFormal("+1980-07-01");
    fact.setDate(date);

    Snippet snippet = new Snippet();
    person = processThroughXml(person, snippet);
    //todo: verify person.
    person = processThroughJson(person, snippet);
    //todo: verify person.
    addSnippet(snippet);

  }


  @Test
  public void testDateFormalValueGedcom55() throws Exception {
    createRecipe("Standardized Date Using GEDCOM 5.5")
      .withDescription("Simple recipe for creating a standardized date using the GEDCOM 5.5 standard. The example shows a person born about July 1, 1980.")
      .applicableTo(Date.class);


    Person person = new Person();
    person.setId("12345");
    person.setFacts(new ArrayList<Fact>());
    Fact fact = new Fact();
    fact.setKnownType(FactType.Birth);
    person.getFacts().add(fact);
    Date date = new Date();
    date.setOriginal("About July 1, 1980");
    date.setFormal("A+1980-07-01");
    fact.setDate(date);

    Snippet snippet = new Snippet();
    person = processThroughXml(person, snippet);
    //todo: verify person.
    person = processThroughJson(person, snippet);
    //todo: verify person.
    addSnippet(snippet);

  }

}
