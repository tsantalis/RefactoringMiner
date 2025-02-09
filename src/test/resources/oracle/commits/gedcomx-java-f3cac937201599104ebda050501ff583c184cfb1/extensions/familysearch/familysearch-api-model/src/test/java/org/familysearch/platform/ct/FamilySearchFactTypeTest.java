package org.familysearch.platform.ct;

import org.testng.annotations.Test;


import java.util.Collection;
import java.util.LinkedList;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class FamilySearchFactTypeTest {

  private Collection<FamilySearchFactType> typesTested;
  private Collection<String> typeStrings;

  @Test
  public void testIt() {
    typesTested = new LinkedList<FamilySearchFactType>();
    typeStrings = new LinkedList<String>();

    // test the contract that the @XmlEnumValue is unique and does not change its value
    testType("http://familysearch.org/v1/Affiliation", FamilySearchFactType.Affiliation);
    testType("http://familysearch.org/v1/BirthOrder", FamilySearchFactType.BirthOrder);
    testType("http://familysearch.org/v1/DiedBeforeEight", FamilySearchFactType.DiedBeforeEight);
    testType("http://familysearch.org/v1/LifeSketch", FamilySearchFactType.LifeSketch);
    testType("http://familysearch.org/v1/NeverMarried", FamilySearchFactType.NeverMarried);
    testType("http://familysearch.org/v1/NoChildren", FamilySearchFactType.NoChildren);
    testType("http://familysearch.org/v1/TitleOfNobility", FamilySearchFactType.TitleOfNobility);
    testType("http://familysearch.org/v1/TribeName", FamilySearchFactType.TribeName);

    // make sure all are tested
    for (FamilySearchFactType type : FamilySearchFactType.values()) {
      if ((!typesTested.contains(type)) && (!FamilySearchFactType.OTHER.equals(type))) {
        assertTrue("Untested FamilySearchFactType: " + type.name(), false);
      }
    }
  }

  private void testType(String enumStr, FamilySearchFactType srcRefTagType) {
    assertEquals(FamilySearchFactType.fromQNameURI(srcRefTagType.toQNameURI()).toQNameURI().toString(), enumStr);
    typesTested.add( srcRefTagType );

    // make sure enum string is unique
    if ( typeStrings.contains(enumStr) ) {
      assertTrue("Duplicate FamilySearchFactType value: " + enumStr, false);
    }
    typeStrings.add( enumStr );
  }
}
