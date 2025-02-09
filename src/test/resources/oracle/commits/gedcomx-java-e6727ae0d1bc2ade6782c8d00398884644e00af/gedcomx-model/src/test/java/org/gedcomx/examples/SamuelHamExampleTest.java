package org.gedcomx.examples;

import org.gedcomx.Gedcomx;
import org.gedcomx.agent.Address;
import org.gedcomx.agent.Agent;
import org.gedcomx.common.Attribution;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.*;
import org.gedcomx.rt.SerializationUtil;
import org.gedcomx.source.SourceCitation;
import org.gedcomx.source.SourceDescription;
import org.gedcomx.source.SourceReference;
import org.gedcomx.types.*;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author Ryan Heaton
 */
public class SamuelHamExampleTest {

  @Test
  public void testExample() throws Exception {
    //Jane Doe, the researcher.
    Agent janeDoe = new Agent().id("A-1").name("Jane Doe").email("example@example.org");

    //Lin Yee Chung Cemetery
    Agent fhl = new Agent().id("A-2").name("Family History Library").address(new Address().city("Salt Lake City").stateOrProvince("Utah"));

    //The attribution for this research.
    Attribution researchAttribution = new Attribution().contributor(janeDoe).modified(parse("2014-04-25"));

    //The parish register.
    SourceDescription recordDescription = new SourceDescription().id("S-1")
      .title("Marriage entry for Samuel Ham and Elizabeth Spiller, Parish Register, Wilton, Somerset, England")
      .description("Marriage entry for Samuel Ham and Elizabeth in a copy of the registers of the baptisms, marriages, and burials at the church of St. George in the parish of Wilton : adjoining Taunton, in the county of Somerset from A.D. 1558 to A.D. 1837.")
      .citation(new SourceCitation().value("Joseph Houghton Spencer, transcriber, Church of England, Parish Church of Wilton (Somerset). <cite>A copy of the registers of the baptisms, marriages, and burials at the church of St. George in the parish of Wilton : adjoining Taunton, in the county of Somerset from A.D. 1558 to A.D. 1837</cite>; Marriage entry for Samuel Ham and Elizabeth Spiller (3 November 1828), (Taunton: Barnicott, 1890), p. 224, No. 86."))
      .resourceType(ResourceType.PhysicalArtifact)
      .repository(fhl);

    //The transcription of the grave stone.
    Document transcription = new Document().id("D-1")
      .lang("en")
      .type(DocumentType.Transcription)
      .text("Samuel Ham of the parish of Honiton and Elizabeth Spiller\n" +
              "were married this 3rd day of November 1828 by David Smith\n" +
              "Stone, Pl Curate,\n" +
              "In the Presence of\n" +
              "Jno Pain.\n" +
              "R.G. Halls.  Peggy Hammet.\n" +
              "No. 86.")
      .source(recordDescription);

    //The transcription described as a source.
    SourceDescription transcriptionDescription = new SourceDescription().id("S-2")
      .about(URI.create("#" + transcription.getId()))
      .title("Transcription of marriage entry for Samuel Ham and Elizabeth Spiller, Parish Register, Wilton, Somerset, England")
      .description("Transcription of marriage entry for Samuel Ham and Elizabeth in a copy of the registers of the baptisms, marriages, and burials at the church of St. George in the parish of Wilton : adjoining Taunton, in the county of Somerset from A.D. 1558 to A.D. 1837.")
      .citation(new SourceCitation().value("Joseph Houghton Spencer, transcriber, Church of England, Parish Church of Wilton (Somerset). <cite>A copy of the registers of the baptisms, marriages, and burials at the church of St. George in the parish of Wilton : adjoining Taunton, in the county of Somerset from A.D. 1558 to A.D. 1837</cite>; Marriage entry for Samuel Ham and Elizabeth Spiller (3 November 1828), (Taunton: Barnicott, 1890), p. 224, No. 86."))
      .resourceType(ResourceType.DigitalArtifact)
      .source(new SourceReference().description(recordDescription));

    //the marriage fact.
    Fact marriage = new Fact()
      .type(FactType.Marriage)
      .date(new Date().original("3 November 1828").formal("+1828-11-03"))
      .place(new PlaceReference().original("Wilton St George, Wilton, Somerset, England"));

    //the groom's residence.
    Fact samsResidence = new Fact()
      .type(FactType.Residence)
      .date(new Date().original("3 November 1828").formal("+1828-11-03"))
      .place(new PlaceReference().original("parish of Honiton, Devon, England"));

    //the groom's residence.
    Fact lizsResidence = new Fact()
      .type(FactType.Residence)
      .date(new Date().original("3 November 1828").formal("+1828-11-03"))
      .place(new PlaceReference().original("parish of Wilton, Somerset, England"));

    //the groom
    Person sam = new Person().id("P-1").extracted(true).source(transcriptionDescription).name("Samuel Ham").gender(GenderType.Male).fact(samsResidence);

    //the bride.
    Person liz = new Person().id("P-2").extracted(true).source(transcriptionDescription).name("Elizabeth Spiller").gender(GenderType.Female).fact(lizsResidence);

    //witnesses
    Person witness1 = new Person().id("P-3").extracted(true).source(transcriptionDescription).name("Jno. Pain");
    Person witness2 = new Person().id("P-4").extracted(true).source(transcriptionDescription).name("R.G. Halls");
    Person witness3 = new Person().id("P-5").extracted(true).source(transcriptionDescription).name("Peggy Hammet");

    //officiator
    Person officiator = new Person().id("P-6").extracted(true).source(transcriptionDescription).name("David Smith Stone");

    //the relationship.
    Relationship marriageRelationship = new Relationship().extracted(true).type(RelationshipType.Couple).person1(sam).person2(liz).fact(marriage);

    //the marriage event
    Event marriageEvent = new Event(EventType.Marriage).id("E-1").extracted(true)
      .date(new Date().original("3 November 1828").formal("+1828-11-03"))
      .place(new PlaceReference().original("Wilton St George, Wilton, Somerset, England"))
      .role(new EventRole().person(sam).type(EventRoleType.Principal))
      .role(new EventRole().person(liz).type(EventRoleType.Principal))
      .role(new EventRole().person(witness1).type(EventRoleType.Witness))
      .role(new EventRole().person(witness2).type(EventRoleType.Witness))
      .role(new EventRole().person(witness3).type(EventRoleType.Witness))
      .role(new EventRole().person(officiator).type(EventRoleType.Official));

    //Jane Doe's analysis.
    Document analysis = new Document().id("D-2").text("...Jane Doe's analysis document...");

    //Jane Doe's conclusions about a person.
    Person samConclusion = new Person().id("C-1").evidence(sam).analysis(analysis);

    Gedcomx gx = new Gedcomx()
      .agent(janeDoe)
      .agent(fhl)
      .attribution(researchAttribution)
      .sourceDescription(recordDescription)
      .document(transcription)
      .sourceDescription(transcriptionDescription)
      .person(sam)
      .person(liz)
      .person(witness1)
      .person(witness2)
      .person(witness3)
      .person(officiator)
      .relationship(marriageRelationship)
      .event(marriageEvent)
      .document(analysis)
      .person(samConclusion);

    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  private java.util.Date parse(String date) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd").parse(date);
  }

}
