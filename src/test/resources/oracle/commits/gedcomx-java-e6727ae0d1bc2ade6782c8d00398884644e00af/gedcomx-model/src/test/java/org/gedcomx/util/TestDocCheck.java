package org.gedcomx.util;

import org.gedcomx.Gedcomx;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.PlaceDescription;
import org.gedcomx.conclusion.Relationship;
import org.gedcomx.records.FieldValue;
import org.gedcomx.source.SourceReference;

import javax.xml.bind.JAXBException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Class for testing DocCheck
 * User: Randy Wilson
 * Date: 10/6/2014
 * Time: 9:51 PM
 */
public class TestDocCheck {

  /**
   * Test the DocCheck on a record (but without fields here).
   * @throws JAXBException
   */
  @Test
  public void testDocCheck() throws JAXBException {
    Gedcomx doc = MarshalUtil.unmarshal(getClass().getClassLoader().getResourceAsStream("gedcomx-record.xml"));
    URI wrongLocalUri = new URI("#wrongId");
    SourceReference wrongSourceReference = new SourceReference();
    wrongSourceReference.setDescriptionRef(wrongLocalUri);

    // No problems with original record
    checkDoc(doc);

    // Break description ref
    URI descriptionRef = doc.getDescriptionRef();
    doc.setDescriptionRef(null);
    checkDoc(doc, 1);
    doc.setDescriptionRef(wrongLocalUri);
    checkDoc(doc, 1);
    doc.setDescriptionRef(descriptionRef); // repair broken ref.

    // Break componentOf
    DocMap docMap = new DocMap(doc);
    SourceReference componentOf = docMap.getMainSourceDescription().getComponentOf();
    docMap.getMainSourceDescription().setComponentOf(wrongSourceReference);
    checkDoc(doc, 2);
    docMap.getMainSourceDescription().setComponentOf(componentOf); // repair

    // Break source
    SourceReference sourceReference = docMap.getMainSourceDescription().getSources().get(0);
    descriptionRef = sourceReference.getDescriptionRef();
    sourceReference.setDescriptionRef(wrongLocalUri);
    checkDoc(doc, 3);
    sourceReference.setDescriptionRef(descriptionRef);
    PlaceDescription place = doc.getPlaces().get(0);
    String temp = place.getId();
    place.setId(null);
    checkDoc(doc, 13);
    place.setId(temp);

    // Break relationship
    Relationship rel = doc.getRelationships().get(0);
    URI person1ref = rel.getPerson1().getResource();
    // Error 4: resourceId but no resource URI
    rel.getPerson1().setResource(null);
    rel.getPerson1().setResourceId(person1ref.toString());
    checkDoc(doc, 4);
    // Error 5: local resourceId that couldn't be found.
    rel.getPerson1().setResource(wrongLocalUri);
    rel.getPerson1().setResourceId(null);
    checkDoc(doc, 5);
    // Error 6: full URI to person that is found in the doc. Should use local #id.
    rel.getPerson1().setResource(docMap.getPerson(person1ref).getPersistentId());
    checkDoc(doc, 6);
    // Error 7: both URIs to persons outside the document.
    URI person2ref = rel.getPerson2().getResource();
    rel.getPerson1().setResource(new URI("https://external.com/123"));
    rel.getPerson2().setResource(new URI("https://external.com/456"));
    checkDoc(doc, 7);

    rel.getPerson1().setResource(person1ref); // repair
    rel.getPerson2().setResource(person2ref); // repair

    // Errors 8, 9, 10, 11, 12: Bad source references on relationship.sources, relationship.media, person.sources, person.media, gender
    rel.addSource(wrongSourceReference);
    rel.addMedia(wrongSourceReference);
    doc.getPersons().get(0).addSource(wrongSourceReference);
    doc.getPersons().get(0).addMedia(wrongSourceReference);
    doc.getPersons().get(1).getGender().addSource(wrongSourceReference);
    checkDoc(doc, 8, 9, 10, 11, 12);
    // Done, so don't bother repairing the above.
  }

  @Test
  public void testDocCheckWithFields() throws JAXBException {
    Gedcomx record = MarshalUtil.unmarshal(getClass().getClassLoader().getResourceAsStream("gedcomx-record.xml"));
    Gedcomx collection = MarshalUtil.unmarshal(getClass().getClassLoader().getResourceAsStream("gedcomx-collection.xml"));

    // Known missing label IDs
    checkFields(record, collection, "IMAGE_ID_NORM", "IMAGE_ARK");

    // Change a field ID, and make sure it isn't found.
    FieldValue fieldValue = record.getPersons().get(0).getFacts().get(0).getDate().getFields().get(0).getValues().get(0);
    String labelId = fieldValue.getLabelId();
    fieldValue.setLabelId(labelId + "_BROKEN");
    checkFields(record, collection, "IMAGE_ID_NORM", "IMAGE_ARK", labelId + "_BROKEN");
  }

  private static final Pattern errorMessagePattern = Pattern.compile(".*(?:Error|Warning) ([0-9]+):.*");
  private void checkDoc(Gedcomx doc, Integer... errorCodes) {
    String errors = DocCheck.checkDocument(doc);
    if (errors == null) {
      assertEquals(errorCodes.length, 0);
    }
    else {
      Set<Integer> actualErrorCodes = new HashSet<Integer>();
      for (String line : errors.split("\n")) {
        Matcher m = errorMessagePattern.matcher(line);
        if (m.matches()) {
          actualErrorCodes.add(Integer.parseInt(m.group(1)));
        }
      }
      assertEquals(errorCodes.length, actualErrorCodes.size());
      for (Integer errorCode : errorCodes) {
        assertTrue("Did not find error code: " + errorCode, actualErrorCodes.contains(errorCode));
      }
    }  }

  private static final Pattern missingLabelIdPattern = Pattern.compile("Error 13:.* labelId '([^']*)' had no .*");

  private void checkFields(Gedcomx record, Gedcomx collection, String... missingLabelIds) {
    String errors = DocCheck.checkDocument(record, collection);
    if (errors == null) {
      assertEquals(missingLabelIds.length, 0);
    }
    else {
      Set<String> actualMissingLabelIds = new HashSet<String>();
      for (String line : errors.split("\n")) {
        Matcher m = missingLabelIdPattern.matcher(line);
        if (m.matches()) {
          actualMissingLabelIds.add(m.group(1));
        }
      }
      assertEquals(missingLabelIds.length, actualMissingLabelIds.size());
      for (String labelId : missingLabelIds) {
        assertTrue("Did not error for labelId '" + labelId + "'", actualMissingLabelIds.contains(labelId));
      }
    }
  }
}
