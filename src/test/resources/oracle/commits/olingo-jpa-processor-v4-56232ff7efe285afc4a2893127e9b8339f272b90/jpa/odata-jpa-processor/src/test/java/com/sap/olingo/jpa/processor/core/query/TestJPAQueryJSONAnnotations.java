package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestJPAQueryJSONAnnotations extends TestBase {

  @Test
  public void testEntityWithMetadataFullContainNavigationLink() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')?$format=application/json;odata.metadata=full");
    helper.assertStatus(200);

    ObjectNode org = helper.getValue();
    assertNotNull(org.get("Roles@odata.navigationLink"));
    assertEquals("Organizations('3')/Roles", org.get("Roles@odata.navigationLink").asText());
  }

  @Test
  public void testEntityWithMetadataMinimalWithoutNavigationLink() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')?$format=application/json;odata.metadata=minimal");
    helper.assertStatus(200);

    ObjectNode org = helper.getValue();
    assertNull(org.get("Roles@odata.navigationLink"));
  }

  @Test
  public void testEntityWithMetadataNoneWithoutNavigationLink() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')?$format=application/json;odata.metadata=none");
    helper.assertStatus(200);

    ObjectNode org = helper.getValue();
    assertNull(org.get("Roles@odata.navigationLink"));
  }

  @Test
  public void testEntityExpandWithMetadataFullContainNavigationLink() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')?$expand=Roles&$format=application/json;odata.metadata=full");
    helper.assertStatus(200);

    ObjectNode org = helper.getValue();
    assertNotNull(org.get("Roles@odata.navigationLink"));
    assertEquals("Organizations('3')/Roles", org.get("Roles@odata.navigationLink").asText());
  }

  @Ignore // See https://issues.apache.org/jira/browse/OLINGO-1248
  @Test
  public void testEntityWithMetadataFullContainNavigationLinkOfComplex() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')?$format=application/json;odata.metadata=full");
    helper.assertStatus(200);

    ObjectNode org = helper.getValue();
    ObjectNode admin = (ObjectNode) org.get("AdministrativeInformation");
    ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User@odata.navigationLink"));
    assertEquals("Organizations('3')/AdministrativeInformation/Created/User", created.get("User@odata.navigationLink")
        .asText());
  }

}
