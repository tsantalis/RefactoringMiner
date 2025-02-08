package org.gedcomx.examples;

import org.gedcomx.Gedcomx;
import org.gedcomx.agent.Agent;
import org.gedcomx.common.Attribution;
import org.gedcomx.conclusion.*;
import org.gedcomx.rt.SerializationUtil;
import org.gedcomx.source.SourceCitation;
import org.gedcomx.source.SourceDescription;
import org.gedcomx.types.FactType;
import org.gedcomx.types.GenderType;
import org.gedcomx.types.RelationshipType;
import org.gedcomx.types.ResourceType;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author Ryan Heaton
 */
@Test
public class EmmaBocockExampleTest {

  public void testExample() throws Exception {
    Agent contributor = new Agent().id("A-1").name("Jane Doe").email("example@example.org");
    Agent repository = new Agent().id("A-2").name("General Registry Office, Southport");
    Attribution attribution = new Attribution().contributor(contributor).modified(parse("2014-03-07")).changeMessage("change message example");
    SourceDescription sourceDescription = new SourceDescription().id("S-1")
      .title("Birth Certificate of Emma Bocock, 23 July 1843, General Registry Office")
      .citation(new SourceCitation().value("England, birth certificate for Emma Bocock, born 23 July 1843; citing 1843 Birth in District and Sub-district of Ecclesall-Bierlow in the County of York, 303; General Registry Office, Southport."))
      .resourceType(ResourceType.PhysicalArtifact)
      .created(parse("1843-07-27"))
      .repository(repository);

    Fact birth = new Fact()
      .type(FactType.Birth)
      .date(new Date().original("23 June 1843"))
      .place(new PlaceReference().original("Broadfield Bar, Abbeydale Road, Ecclesall-Bierlow, York, England, United Kingdom"));

    Person emma = new Person().id("P-1").extracted(true).source(sourceDescription).name("Emma Bocock").gender(GenderType.Female).fact(birth);

    Person father = new Person().id("P-2").extracted(true).source(sourceDescription).name("William Bocock").fact(new Fact().type(FactType.Occupation).value("Toll Collector"));

    Person mother = new Person().id("P-3").extracted(true).source(sourceDescription).name("Sarah Bocock formerly Brough");

    Relationship fatherRelationship = new Relationship().type(RelationshipType.ParentChild).person1(father).person2(emma);

    Relationship motherRelationship = new Relationship().type(RelationshipType.ParentChild).person1(mother).person2(emma);

    Document analysis = new Document().id("D-1").text("...Jane Doe's analysis document...");

    Person emmaConclusion = new Person().id("C-1").evidence(emma).analysis(analysis);

    Gedcomx gx = new Gedcomx()
      .agent(contributor)
      .agent(repository)
      .attribution(attribution)
      .sourceDescription(sourceDescription)
      .person(emma)
      .person(father)
      .person(mother)
      .relationship(fatherRelationship)
      .relationship(motherRelationship)
      .document(analysis)
      .person(emmaConclusion);

    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  private java.util.Date parse(String date) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd").parse(date);
  }

}
