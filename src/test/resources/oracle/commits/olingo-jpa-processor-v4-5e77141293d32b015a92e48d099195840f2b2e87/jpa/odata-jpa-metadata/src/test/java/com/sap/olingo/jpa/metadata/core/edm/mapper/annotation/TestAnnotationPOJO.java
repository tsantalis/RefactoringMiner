package com.sap.olingo.jpa.metadata.core.edm.mapper.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class TestAnnotationPOJO {
  private String terms =
      "<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" Version=\"4.0\">"
          + "<edmx:Reference Uri=\"http://docs.oasis-open.org/odata/odata/v4.0/os/vocabularies/Org.OData.Core.V1.xml\">"
          + "<edmx:Include Alias=\"Core\" Namespace=\"Org.OData.Core.V1\" />"
          + "</edmx:Reference>"
          + "<edmx:DataServices>"
          + "<Schema xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Namespace=\"Org.OData.Measures.V1\" Alias=\"Measures\"> "
          + "<Annotation Term=\"Core.Description\"> "
          + "<String>Terms describing monetary amounts and measured quantities</String> "
          + "</Annotation> "
          + "<Term Name=\"ISOCurrency\" Type=\"Edm.String\" AppliesTo=\"Property\"> "
          + "<Annotation Term=\"Core.Description\" String=\"The currency for this monetary amount as an ISO 4217 currency code\" /> "
          + "</Term> "
          + "<Term Name=\"Scale\" Type=\"Edm.Byte\" AppliesTo=\"Property\"> "
          + "<Annotation Term=\"Core.Description\" "
          + "String=\"The number of significant decimal places in the scale part (less than or equal to the number declared in the Scale facet)\" /> "
          + "<Annotation Term=\"Core.RequiresType\" String=\"Edm.Decimal\" /> "
          + "</Term> "
          + "<Term Name=\"Unit\" Type=\"Edm.String\" AppliesTo=\"Property\"> "
          + "<Annotation Term=\"Core.Description\" String=\"The unit of measure for this measured quantity, e.g. cm for centimeters or % for percentages\" /> "
          + "</Term> "
          + "</Schema>   "
          + "</edmx:DataServices>"
          + "</edmx:Edmx>";

  @Test
  public void TestSimpleXMLConverted() throws JsonParseException, JsonMappingException, IOException {
    JacksonXmlModule module = new JacksonXmlModule();
    module.setDefaultUseWrapper(false);
    XmlMapper xmlMapper = new XmlMapper(module);

    Edmx act = xmlMapper.readValue(terms, Edmx.class);
    assertNotNull(act.getDataService());

    Schema[] actSchemas = act.getDataService().getSchemas();
    assertEquals(1, actSchemas.length);

    List<Term> actTerms = actSchemas[0].getTerms();
    assertEquals(3, actTerms.size());
  }
}
