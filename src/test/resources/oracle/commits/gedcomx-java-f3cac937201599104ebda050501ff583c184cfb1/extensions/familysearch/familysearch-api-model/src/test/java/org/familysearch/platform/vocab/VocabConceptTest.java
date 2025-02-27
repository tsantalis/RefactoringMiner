package org.familysearch.platform.vocab;

import java.util.Collections;
import java.util.List;

import org.gedcomx.common.ExtensibleData;
import org.gedcomx.common.TextValue;
import org.gedcomx.common.URI;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VocabConceptTest {

  @Test
  public void testId() {
    final String id = "testId";

    // Test using setter
    ExtensibleData classUnderTest = new VocabConcept();
    classUnderTest.setId(id);

    assertEquals(classUnderTest.getId(), id);

    // Test using builder pattern method
    classUnderTest = new VocabConcept().id(id);

    assertEquals(classUnderTest.getId(), id);
  }

  @Test
  public void testDescription() {
    final String description = "testDescription";

    // Test using setter
    VocabConcept classUnderTest = new VocabConcept();
    classUnderTest.setDescription(description);

    assertEquals(classUnderTest.getDescription(), description);

    // Test using builder pattern method
    classUnderTest = new VocabConcept().description(description);

    assertEquals(classUnderTest.getDescription(), description);
  }

  @Test
  public void testNote() {
    final String note = "testNote";

    // Test using setter
    VocabConcept classUnderTest = new VocabConcept();
    classUnderTest.setNote(note);

    assertEquals(classUnderTest.getNote(), note);

    // Test using builder pattern method
    classUnderTest = new VocabConcept().note(note);

    assertEquals(classUnderTest.getNote(), note);
  }

  @Test
  public void testTerms() {
    final String uriBase = "http://some.valid.uri/";
    final TextValue testTextValue = new TextValue("testValue");
    testTextValue.setLang("testLang");
    final VocabTerm testVocabTerm = (VocabTerm) new VocabTerm()
        .vocabConcept(new URI(uriBase + "vocabConcept/testVocabConceptUri"))
        .sublist(new URI(uriBase + "vocabTerm/testVocabTermUri"))
        .sublistPosition(4736)
        .type(new URI(uriBase + "type/testType"))
        .values(Collections.singletonList(testTextValue))
        .id("testId");
    List<VocabTerm> testVocabTerms = Collections.singletonList(testVocabTerm);

    // Test using setter
    VocabConcept classUnderTest = new VocabConcept();
    classUnderTest.setVocabTerms(testVocabTerms);

    assertEquals(classUnderTest.getVocabTerms(), testVocabTerms);

    // Test using builder pattern method
    classUnderTest = new VocabConcept().vocabTerms(testVocabTerms);

    assertEquals(classUnderTest.getVocabTerms(), testVocabTerms);
  }

  @Test
  public void testAttributes() {
    final VocabConceptAttribute testVocabConceptAttribute = new VocabConceptAttribute()
        .id("testId")
        .name("testName")
        .value("testValue");
    List<VocabConceptAttribute> testVocabConceptAttributes = Collections.singletonList(testVocabConceptAttribute);

    // Test using setter
    VocabConcept classUnderTest = new VocabConcept();
    classUnderTest.setAttributes(testVocabConceptAttributes);

    assertEquals(classUnderTest.getAttributes(), testVocabConceptAttributes);

    // Test using builder pattern method
    classUnderTest = new VocabConcept().attributes(testVocabConceptAttributes);

    assertEquals(classUnderTest.getAttributes(), testVocabConceptAttributes);
  }

  @Test
  public void testGedcomxUri() {
    final URI uri = new URI("http://some.valid.url/test");

    // Test using setter
    VocabConcept classUnderTest = new VocabConcept();
    classUnderTest.setGedcomxUri(uri);

    assertEquals(classUnderTest.getGedcomxUri(), uri);

    // Test using builder pattern method
    classUnderTest = new VocabConcept().gedcomxUri(uri);

    assertEquals(classUnderTest.getGedcomxUri(), uri);
  }

}
