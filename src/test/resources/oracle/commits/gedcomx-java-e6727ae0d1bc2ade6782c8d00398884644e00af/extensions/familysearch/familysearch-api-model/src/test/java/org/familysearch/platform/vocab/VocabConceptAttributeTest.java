package org.familysearch.platform.vocab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VocabConceptAttributeTest {

  @Test
  public void testId() {
    final String id = "testId";

    // Test using setter
    VocabConceptAttribute classUnderTest = new VocabConceptAttribute();
    classUnderTest.setId(id);

    assertEquals(classUnderTest.getId(), id);

    // Test using builder pattern method
    classUnderTest = new VocabConceptAttribute().id(id);

    assertEquals(classUnderTest.getId(), id);
  }

  @Test
  public void testName() {
    final String name = "testName";

    // Test using setter
    VocabConceptAttribute classUnderTest = new VocabConceptAttribute();
    classUnderTest.setName(name);

    assertEquals(classUnderTest.getName(), name);

    // Test using builder pattern method
    classUnderTest = new VocabConceptAttribute().name(name);

    assertEquals(classUnderTest.getName(), name);
  }

  @Test
  public void testValue() {
    final String value = "testValue";

    // Test using setter
    VocabConceptAttribute classUnderTest = new VocabConceptAttribute();
    classUnderTest.setValue(value);

    assertEquals(classUnderTest.getValue(), value);

    // Test using builder pattern method
    classUnderTest = new VocabConceptAttribute().value(value);

    assertEquals(classUnderTest.getValue(), value);
  }

}
