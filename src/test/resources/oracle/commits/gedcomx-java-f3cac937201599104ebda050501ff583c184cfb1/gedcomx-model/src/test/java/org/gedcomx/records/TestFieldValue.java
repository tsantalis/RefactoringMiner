package org.gedcomx.records;

import junit.framework.TestCase;
import org.gedcomx.rt.SerializationUtil;
import org.gedcomx.types.FieldValueStatusType;

import javax.xml.bind.JAXBException;
import java.io.UnsupportedEncodingException;

/**
 * Class for testing the FieldValue class.
 * User: Randy Wilson
 * Date: 11/25/2014
 * Time: 2:55 PM
 */
public class TestFieldValue extends TestCase {

  public void testXml() throws JAXBException, UnsupportedEncodingException {
    FieldValue fieldValue = new FieldValue();
    fieldValue.setKnownStatus(FieldValueStatusType.Unreadable);

    fieldValue = SerializationUtil.processThroughXml(fieldValue);

    assertEquals(FieldValueStatusType.Unreadable, fieldValue.getKnownStatus());
  }

}