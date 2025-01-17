package com.sap.olingo.jpa.metadata.core.edm.mapper.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.olingo.commons.api.edm.provider.CsdlTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class TestTermReader {
  private TermReader cut;

  @BeforeEach
  public void setup() {
    cut = new TermReader();
  }

  @Test
  public void TestReadFromResource() throws JsonParseException, JsonMappingException, IOException {
    Edmx actEdmx = cut.readFromResource("annotations/Org.OData.Measures.V1.xml");
    assertNotNull(actEdmx);
    assertNotNull(actEdmx.getDataService());

    Schema[] actSchemas = actEdmx.getDataService().getSchemas();
    assertEquals(1, actSchemas.length);
    assertEquals("Org.OData.Measures.V1", actSchemas[0].getNamespace());
  }

  @Test
  public void TestGetTermsOneSchemaFromPath() throws JsonParseException, JsonMappingException, IOException {
    Map<String, Map<String, CsdlTerm>> act;
    act = cut.getTerms("annotations/Org.OData.Core.V1.xml");
    assertNotNull(act.get("Org.OData.Core.V1"));
    Map<String, CsdlTerm> terms = act.get("Org.OData.Core.V1");
    assertEquals(15, terms.size());
  }

  @Test
  public void TestGetAppliesTo() throws JsonParseException, JsonMappingException, IOException {
    Map<String, Map<String, CsdlTerm>> act;
    act = cut.getTerms("annotations/Org.OData.Core.V1.xml");
    assertNotNull(act.get("Org.OData.Core.V1"));
    Map<String, CsdlTerm> terms = act.get("Org.OData.Core.V1");
    CsdlTerm term = terms.get("IsLanguageDependent");
    assertEquals(2, term.getAppliesTo().size());
    assertTrue("Term".equals(term.getAppliesTo().get(0)) || "Term".equals(term.getAppliesTo().get(1)));
    assertTrue("Property".equals(term.getAppliesTo().get(0)) || "Property".equals(term.getAppliesTo().get(1)));
  }

  @Test
  public void TestGetTermsTwoSchemaFromPath() throws JsonParseException, JsonMappingException, IOException {
    Map<String, Map<String, CsdlTerm>> act;
    act = cut.getTerms("annotations/Org.Olingo.Test.V1.xml");
    assertNotNull(act.get("Org.OData.Measures.V1"));
    assertNotNull(act.get("Org.OData.Capabilities.V1"));
  }

  // TODO This test may not run because of proxy setting problems!! -> find alternative for Integration tests
  @Disabled
  @Test
  public void TestReadFromURI() throws URISyntaxException, JsonParseException, JsonMappingException,
      MalformedURLException, IOException {
    URI uri = new URI("http://docs.oasis-open.org/odata/odata/v4.0/os/vocabularies/Org.OData.Core.V1.xml");
    Edmx actEdmx = cut.readFromURI(uri);
    assertNotNull(actEdmx);
    assertNotNull(actEdmx.getDataService());

    Schema[] actSchemas = actEdmx.getDataService().getSchemas();
    assertEquals(1, actSchemas.length);
    assertEquals("Org.OData.Core.V1", actSchemas[0].getNamespace());
  }

  // TODO This test may not run because of proxy setting problems!! -> find alternative for Integration tests
  @Disabled
  @Test
  public void TestGetTermsOneSchemaFromURI() throws URISyntaxException, JsonParseException, JsonMappingException,
      MalformedURLException, IOException {
    URI uri = new URI("http://docs.oasis-open.org/odata/odata/v4.0/os/vocabularies/Org.OData.Core.V1.xml");
    Map<String, Map<String, CsdlTerm>> act;
    act = cut.getTerms(uri);
    assertNotNull(act.get("Org.OData.Core.V1"));
  }

}
