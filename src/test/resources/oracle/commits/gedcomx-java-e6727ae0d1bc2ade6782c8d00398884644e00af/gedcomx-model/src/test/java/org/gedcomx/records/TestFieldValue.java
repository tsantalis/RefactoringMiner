package org.gedcomx.records;

import org.gedcomx.rt.SerializationUtil;
import org.gedcomx.types.FieldValueStatusType;

import javax.xml.bind.JAXBException;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Class for testing the FieldValue class.
 * User: Randy Wilson
 * Date: 11/25/2014
 * Time: 2:55 PM
 */
public class TestFieldValue {

  @Test
  public void testXml() throws JAXBException, UnsupportedEncodingException {
    FieldValue fieldValue = new FieldValue();
    fieldValue.setKnownStatus(FieldValueStatusType.Unreadable);

    fieldValue = SerializationUtil.processThroughXml(fieldValue);

    assertEquals(FieldValueStatusType.Unreadable, fieldValue.getKnownStatus());
  }

}
