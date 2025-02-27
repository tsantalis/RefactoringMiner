package org.familysearch.platform.ct;

import org.junit.Test;


import java.util.Collection;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceReferenceTagTypeTest {

  private Collection<SourceReferenceTagType> typesTested;
  private Collection<String> typeStrings;

  @Test
  public void testIt() {
    typesTested = new LinkedList<SourceReferenceTagType>();
    typeStrings = new LinkedList<String>();

    // test the contract that the @XmlEnumValue is unique and does not change its value
    testType("http://gedcomx.org/Name", SourceReferenceTagType.Name);
    testType("http://gedcomx.org/Gender", SourceReferenceTagType.Gender);

    // make sure all are tested
    for (SourceReferenceTagType type : SourceReferenceTagType.values()) {
      if ((!typesTested.contains(type)) && (!SourceReferenceTagType.OTHER.equals(type))) {
        assertTrue("Untested SourceReferenceTagType: " + type.name(), false);
      }
    }
  }

  private void testType(String enumStr, SourceReferenceTagType srcRefTagType) {
    assertEquals(SourceReferenceTagType.fromQNameURI(srcRefTagType.toQNameURI()).toQNameURI().toString(), enumStr);
    typesTested.add( srcRefTagType );

    // make sure enum string is unique
    if ( typeStrings.contains(enumStr) ) {
      assertTrue("Duplicate SourceReferenceTagType value: " + enumStr, false);
    }
    typeStrings.add( enumStr );
  }
}
