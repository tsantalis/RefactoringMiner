package org.gedcomx.util;

import junit.framework.TestCase;
import org.gedcomx.Gedcomx;
import org.gedcomx.conclusion.Name;
import org.gedcomx.conclusion.NameForm;
import org.gedcomx.conclusion.NamePart;
import org.gedcomx.conclusion.Person;
import org.gedcomx.records.RecordSet;
import org.gedcomx.types.NamePartType;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Class for testing the CleanXmlStreamWriter.
 * User: Randy Wilson
 * Date: 17 September 2015
 */
public class TestCleanXmlStreamWriter extends TestCase {
  public void testEscape() {
    String orig = "\uD850\uDDAC成功";
    String escaped = CleanXMLStreamWriter.escapeCharacters(orig);
    assertEquals(orig, escaped);

    orig = "\uDBC0\uDCDF am 25 April 1868";
    escaped = CleanXMLStreamWriter.escapeCharacters(orig);
    assertEquals(orig, escaped);

    orig = "蘇\uD849\uDF67";
    escaped = CleanXMLStreamWriter.escapeCharacters(orig);
    assertEquals(orig, escaped);
  }

  public void testVerticalTab() {
    String orig = "Vertical\u000BTab";
    String escaped = CleanXMLStreamWriter.escapeCharacters(orig);
    // Make sure the vertical tab got replaced with a Unicode "Replacement Character" (0xFFFD)
    assertEquals("Vertical\uFFFDTab", escaped);
  }

  public void testSurrogatePairs() throws JAXBException, IOException {
    testName("\uD850\uDDAC成功");
    testName("岩\uD842\uDFB7");
  }

  private void testName(String orig) throws JAXBException, IOException {
    Gedcomx doc = new Gedcomx();
    Person person = new Person();
    doc.addPerson(person);
    Name name = new Name(orig);
    person.addName(name);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    RecordSetWriter cleanWriter = new RecordSetWriter(bos, false, Gedcomx.class, RecordSetWriter.class);
    cleanWriter.writeRecord(doc);
    cleanWriter.close();

    RecordSetIterator recordSetIterator = new XmlRecordSetIterator(new ByteArrayInputStream(bos.toByteArray()), false);
    Gedcomx doc1 = recordSetIterator.next();
    assertEquals(orig, doc1.getPerson().getName().getNameForm().getFullText());

    bos = new ByteArrayOutputStream();
    RecordSetWriter cleanWriter2 = new RecordSetWriter(bos, true, Gedcomx.class, RecordSetWriter.class);
    cleanWriter2.writeRecord(doc);
    cleanWriter2.close();
    recordSetIterator = new XmlRecordSetIterator(new ByteArrayInputStream(bos.toByteArray()), false);
    Gedcomx doc2 = recordSetIterator.next();
    String actual = doc2.getPerson().getName().getNameForm().getFullText();
    assertEquals(orig, actual);
  }

  /**
   * XMLStreamWriter in the standard Java package has a bug in which Unicode surrogate pairs that appear in
   *   attributes remain in a StringBuffer that gets re-used on every subsequent surrogate pair that appears in
   *   any other attribute, causing those to get longer and longer (!).
   * To solve the problem, we switched to using Woodstox's XMLStreamWriter instead.
   * This test validated the problem before the switch, and made sure it worked afterwards.
   */
  public void testSurrogateAttributes() throws Exception {
    String surname = "\uD850\uDDAC";
    String given = "\uD842\uDFB7";
    String full = surname + given;
    Gedcomx doc = new Gedcomx();
    Name name = new Name(full, new NamePart(NamePartType.Surname, surname),
                         new NamePart(NamePartType.Given, given));
    doc.addPerson(new Person().id("p." + full).name(name));

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    RecordSetWriter cleanWriter = new RecordSetWriter(bos, false, Gedcomx.class, RecordSet.class);
    cleanWriter.writeRecord(doc);
    cleanWriter.close();

    RecordSetIterator recordSetIterator = new XmlRecordSetIterator(new ByteArrayInputStream(bos.toByteArray()), false);
    Gedcomx doc1 = recordSetIterator.next();
    //System.out.println("Orig: =====\n" + MarshalUtil.toXml(doc));
    //System.out.println("Then: =====\n" + MarshalUtil.toXml(doc1));
    NameForm nameForm = doc1.getPerson().getName().getNameForm();
    assertEquals(full, nameForm.getFullText());
    assertEquals(surname, nameForm.getParts().get(0).getValue());
    // This is where the standard XMLStreamReader causes the value to be surname + given instead of just given...
    assertEquals(given, nameForm.getParts().get(1).getValue());
  }
}
