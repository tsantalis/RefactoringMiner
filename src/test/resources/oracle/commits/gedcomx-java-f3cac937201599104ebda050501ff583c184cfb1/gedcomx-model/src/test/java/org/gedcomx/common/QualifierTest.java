package org.gedcomx.common;

import org.gedcomx.source.SourceReference;
import org.gedcomx.types.SourceReferenceQualifierType;
import org.testng.annotations.Test;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.testng.AssertJUnit.*;


/**
 * @author Ryan Heaton
 */
@Test
public class QualifierTest {

  /**
   * tests qualifier xml
   */
  public void testQualifierXml() throws Exception {
    SourceReference sourceReference = new SourceReference();
    sourceReference.qualifier(new Qualifier(SourceReferenceQualifierType.RectangleRegion.toQNameURI(), "1,2,3,4"));
    Qualifier qualifier = ((SourceReference)processThroughXml(sourceReference)).getQualifiers().get(0);
    assertEquals(SourceReferenceQualifierType.RectangleRegion.toQNameURI(), qualifier.getName());
    assertEquals("1,2,3,4", qualifier.getValue());
  }

  /**
   * tests qualifier json
   */
  public void testQualifierJson() throws Exception {
    SourceReference sourceReference = new SourceReference();
    sourceReference.qualifier(new Qualifier(SourceReferenceQualifierType.RectangleRegion.toQNameURI(), "1,2,3,4"));
    Qualifier qualifier = ((SourceReference)processThroughJson(sourceReference)).getQualifiers().get(0);
    assertEquals(SourceReferenceQualifierType.RectangleRegion.toQNameURI(), qualifier.getName());
    assertEquals("1,2,3,4", qualifier.getValue());
  }

}
