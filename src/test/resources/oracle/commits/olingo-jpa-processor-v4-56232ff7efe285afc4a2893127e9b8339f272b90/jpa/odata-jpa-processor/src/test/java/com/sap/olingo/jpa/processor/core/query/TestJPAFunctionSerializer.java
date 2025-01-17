package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestHelper;

public class TestJPAFunctionSerializer {
  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";
  protected static EntityManagerFactory emf;
  protected static DataSource ds;

  protected TestHelper helper;
  protected Map<String, List<String>> headers;
  protected static JPAEdmNameBuilder nameBuilder;

  @Before
  public void setup() {
    ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB);
    Map<String, Object> properties = new HashMap<>();
    properties.put("javax.persistence.nonJtaDataSource", ds);
    emf = Persistence.createEntityManagerFactory(PUNIT_NAME, properties);
    emf.getProperties();
  }

  @Test
  public void testFunctionReturnsEntityType() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "EntityType(A=1250)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ObjectNode r = helper.getValue();
    assertNotNull(r.get("Area").asInt());
    assertEquals(1250, r.get("Area").asInt());
  }

  @Test
  public void testFunctionReturnsEntityTypeNull() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "EntityType(A=0)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(204);
  }

  @Test
  public void testFunctionReturnsEntityTypeCollection() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "ListOfEntityType(A=1250)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ObjectNode r = helper.getValue();
    ArrayNode values = (ArrayNode) r.get("value");
    assertNotNull(values.get(0));
    assertNotNull(values.get(0).get("Area").asInt());
    assertEquals(1250, values.get(0).get("Area").asInt());
    assertNotNull(values.get(1));
    assertNotNull(values.get(1).get("Area").asInt());
    assertEquals(625, values.get(1).get("Area").asInt());
  }

  @Test
  public void testFunctionReturnsPrimitiveType() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "PrimitiveValue(A=124)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ObjectNode r = helper.getValue();
    assertNotNull(r);
    assertNotNull(r.get("value"));
    assertEquals(124, r.get("value").asInt());
  }

  @Test
  public void testFunctionReturnsPrimitiveTypeNull() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "PrimitiveValue(A=0)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(204);
  }

  @Test
  public void testFunctionReturnsPrimitiveTypeCollection() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "ListOfPrimitiveValues(A=124)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ArrayNode r = helper.getValues();
    assertNotNull(r);
    assertNotNull(r.get(0));
    assertEquals(124, r.get(0).asInt());
    assertNotNull(r.get(1));
    assertEquals(62, r.get(1).asInt());
  }

  @Test
  public void testFunctionReturnsComplexType() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "ComplexType(A=124)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ObjectNode r = helper.getValue();
    assertNotNull(r);
    assertNotNull(r.get("LandlinePhoneNumber"));
    assertEquals(124, r.get("LandlinePhoneNumber").asInt());
  }

  @Test
  public void testFunctionReturnsComplexTypeNull() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "ComplexType(A=0)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(204);
  }

  @Test
  public void testFunctionReturnsComplexTypeCollection() throws IOException, ODataException, SQLException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "ListOfComplexType(A='Willi')",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ArrayNode r = helper.getValues();
    assertNotNull(r);
    assertNotNull(r.get(0));
    assertNotNull(r.get(0).get("Created"));
  }

  @Test
  public void testUsesConverter() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "ConvertBirthday()",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
  }

  @Test
  public void testFunctionReturnsEntityTypeWithCollection() throws IOException, ODataException {
    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "ListOfEntityTypeWithCollction(A=1250)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ObjectNode r = helper.getValue();
    assertNotNull(r.get("value"));
    ObjectNode person = (ObjectNode) r.get("value").get(0);
    ArrayNode addr = (ArrayNode) person.get("InhouseAddress");
    assertEquals(2, addr.size());
  }

  @Test
  public void testFunctionReturnsEntityTypeWithDeepCollection() throws IOException, ODataException {
    IntegrationTestHelper helper = new IntegrationTestHelper(emf, ds, "EntityTypeWithDeepCollction(A=1250)",
        "com.sap.olingo.jpa.processor.core.query");
    helper.assertStatus(200);
    ObjectNode r = helper.getValue();
    assertNotNull(r.get("FirstLevel"));
    ObjectNode first = (ObjectNode) r.get("FirstLevel");
    assertEquals(10, first.get("LevelID").asInt());

    assertNotNull(first.get("SecondLevel"));
    ObjectNode second = (ObjectNode) first.get("SecondLevel");
    assertEquals(5L, second.get("Number").asLong());
    ArrayNode addr = (ArrayNode) second.get("Address");
    assertEquals(2, addr.size());
    assertEquals("ADMIN", addr.get(1).get("TaskID").asText());

    ArrayNode comment = (ArrayNode) second.get("Comment");
    assertEquals(3, comment.size());
    assertEquals("Three", comment.get(2).asText());
  }
}
