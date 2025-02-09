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
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author Ryan Heaton
 */
@Test
public class WongAloiauExampleTest {

  public void testExample() throws Exception {
    //Jane Doe, the researcher.
    Agent janeDoe = new Agent().id("A-1").name("Jane Doe").email("example@example.org");

    //Lin Yee Chung Cemetery
    Agent cemetery = new Agent().id("A-2").name("Lin Yee Chung Cemetery").address(new Address().city("Honolulu").stateOrProvince("Hawaii"));

    //Hanyu Pinyin, the translator.
    Agent hanyuPinyin = new Agent().id("A-3").name("HANYU Pinyin 王大年").email("example@example.org");

    //The attribution for this research.
    Attribution researchAttribution = new Attribution().contributor(janeDoe).modified(parse("2014-03-27"));

    //The attribution for the translation.
    Attribution translationAttribution = new Attribution().contributor(hanyuPinyin).modified(parse("2014-03-27"));

    //The grave stone.
    SourceDescription gravestoneDescription = new SourceDescription().id("S-1")
      .title("Grave Marker of WONG Aloiau, Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii")
      .citation(new SourceCitation().value("WONG Aloiau gravestone, Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii; visited May 1975 by Jane Doe."))
      .resourceType(ResourceType.PhysicalArtifact)
      .repository(cemetery);

    //The image of the grave stone.
    SourceDescription gravestoneImageDescription = new SourceDescription().id("S-2")
      .title("Grave Marker of WONG Aloiau, Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii")
      .citation(new SourceCitation().value("WONG Aloiau gravestone (digital photograph), Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii; visited May 1975 by Jane Doe."))
      .resourceType(ResourceType.DigitalArtifact)
      .source(new SourceReference().description(gravestoneDescription));

    //The transcription of the grave stone.
    Document transcription = new Document().id("D-1")
      .lang("zh")
      .text("WONG ALOIAU\n" +
              "NOV. 22, 1848 – AUG. 3, 1920\n" +
              "中山  大字都  泮沙鄉\n" +
              "生  於  前  清 戊申 年 十一 月 廿二（日）子   時\n" +
              "終  於  民國  庚申 年     七月    十二 (日)    午    時\n" +
              "先考  諱 羅有  字 容康 王 府 君 之 墓")
      .source(gravestoneImageDescription);

    //The transcription described as a source.
    SourceDescription transcriptionDescription = new SourceDescription().id("S-3")
      .about(URI.create("#" + transcription.getId()))
      .title("Transcription of Grave Marker of WONG Aloiau, Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii")
      .citation(new SourceCitation().value("WONG Aloiau gravestone (transcription), Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii; visited May 1975 by Jane Doe."))
      .resourceType(ResourceType.DigitalArtifact)
      .source(new SourceReference().description(gravestoneImageDescription));

    //The translation of the grave stone.
    Document translation = new Document().id("D-2")
      .text("WONG ALOIAU\n" +
              "NOV. 22, 1848 – AUG. 3, 1920 [lunar dates]\n" +
              "[Birthplace] [China, Guandong, ]Chung Shan, See Dai Doo, Pun Sha village\n" +
              "[Date of birth] Born at former Qing 1848 year 11th month 22nd day 23-1 hour.\n" +
              "[Life] ended at Republic of China year 1920 year 7th mo. 12th day 11-13 hour.\n" +
              "Deceased father avoid [mention of] Lo Yau also known as Young Hong Wong [noble]residence ruler’s grave.")
      .source(transcriptionDescription);

    //The translation described as a source.
    SourceDescription translationDescription = new SourceDescription().id("S-4")
      .about(URI.create("#" + translation.getId()))
      .title("Translation of Grave Marker of WONG Aloiau, Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii")
      .citation(new SourceCitation().value("WONG Aloiau gravestone, Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii; visited May 1975 by Jane Doe. Translation by HANYU Pinyin 王大年."))
      .attribution(translationAttribution)
      .resourceType(ResourceType.DigitalArtifact)
      .source(new SourceReference().description(transcriptionDescription));

    //the birth.
    Fact birth = new Fact()
      .type(FactType.Birth)
      .date(new Date().original("former Qing 1848 year 11th month 22nd day 23-1 hour").formal("+1848-11-22"))
      .place(new PlaceReference().original("Pun Sha Village, See Dai Doo, Chung Shan, Guangdong, China"));

    //the death.
    Fact death = new Fact()
      .type(FactType.Death)
      .date(new Date().original("Republic of China year 1920 year 7th mo. 12th day 11-13 hour").formal("+1920-08-03"));

    //the burial.
    Fact burial = new Fact()
      .type(FactType.Burial)
      .place(new PlaceReference().original("Lin Yee Chung Cemetery, Honolulu, Oahu, Hawaii"));

    //the principal person
    Person aloiau = new Person().id("P-1").extracted(true).source(translationDescription).name("WONG Aloiau").gender(GenderType.Male).fact(birth).fact(death).fact(burial);

    //the father of the principal (with an aka name).
    Person father = new Person().id("P-2").extracted(true).source(translationDescription).name("Lo Yau").name(new Name().type(NameType.AlsoKnownAs).nameForm(new NameForm().fullText("Young Hong Wong")));

    //the relationship.
    Relationship fatherRelationship = new Relationship().type(RelationshipType.ParentChild).person1(father).person2(aloiau);

    //Jane Doe's analysis.
    Document analysis = new Document().id("D-3").text("...Jane Doe's analysis document...");

    //Jane Doe's conclusions about a person.
    Person aloiauConclusion = new Person().id("C-1").evidence(aloiau).analysis(analysis);

    Gedcomx gx = new Gedcomx()
      .agent(janeDoe)
      .agent(cemetery)
      .agent(hanyuPinyin)
      .attribution(researchAttribution)
      .sourceDescription(gravestoneDescription)
      .sourceDescription(gravestoneImageDescription)
      .document(transcription)
      .sourceDescription(transcriptionDescription)
      .document(translation)
      .sourceDescription(translationDescription)
      .person(aloiau)
      .person(father)
      .relationship(fatherRelationship)
      .document(analysis)
      .person(aloiauConclusion);

    SerializationUtil.processThroughXml(gx);
    SerializationUtil.processThroughJson(gx);
  }

  private java.util.Date parse(String date) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd").parse(date);
  }

}
