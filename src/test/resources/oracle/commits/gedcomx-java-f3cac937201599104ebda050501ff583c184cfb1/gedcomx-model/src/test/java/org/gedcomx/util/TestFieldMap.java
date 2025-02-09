package org.gedcomx.util;

import junit.framework.TestCase;
import org.gedcomx.Gedcomx;

import javax.xml.bind.JAXBException;
import java.util.Locale;

/**
 * Class for...
 * User: Randy Wilson
 * Date: 7/31/2014
 * Time: 12:04 PM
 */
public class TestFieldMap extends TestCase {

  public void testRecordFieldMap() throws JAXBException {
    Gedcomx record = MarshalUtil.unmarshal(getClass().getClassLoader().getResourceAsStream("gedcomx-record.xml"));
    Gedcomx collection = MarshalUtil.unmarshal(getClass().getClassLoader().getResourceAsStream("gedcomx-collection.xml"));
    FieldMap fieldMap = new FieldMap(record, collection);
    assertEquals("Maria Johanna Potgieter Van Wyk", fieldMap.getValues("PR_NAME").get(0));
    assertEquals("Name", fieldMap.getDisplayLabel("PR_NAME", "en"));
    assertEquals("Nombre", fieldMap.getDisplayLabel("PR_NAME", "es"));
    assertNull(fieldMap.getDisplayLabel("BATCH_LOCALITY", "en"));
    assertEquals("South Africa", fieldMap.getValues("BATCH_LOCALITY").get(0));
    assertNull(fieldMap.getValues("IMAGE_TYPE")); // empty
    assertNull(fieldMap.getValues("NOT_A_REAL_LABEL_ID")); // doesn't exist
  }

  /**
   * Test an 'image item', which is a small record with no persons or relationships, but only a few "fields" that are
   *   used to tag a group of images to support image browsing.
   * @throws JAXBException
   */
  public void testImageItemFieldMap() throws JAXBException {
    Gedcomx imageItem = MarshalUtil.unmarshal(getClass().getClassLoader().getResourceAsStream("gedcomx-image.xml"));
    Gedcomx collection = MarshalUtil.unmarshal(getClass().getClassLoader().getResourceAsStream("gedcomx-collection.xml"));
    FieldMap fieldMap = new FieldMap(imageItem, collection);
    assertEquals("1962", fieldMap.getValues("YEAR").get(0));
    assertEquals("1116", fieldMap.getValues("FILE_NUMBER").get(0));
    assertEquals("Year", fieldMap.getDisplayLabel("YEAR", Locale.ENGLISH.getLanguage()));
    assertEquals("Año", fieldMap.getDisplayLabel("YEAR", "es"));
    assertEquals("년도", fieldMap.getDisplayLabel("YEAR", "ko"));
  }
}
