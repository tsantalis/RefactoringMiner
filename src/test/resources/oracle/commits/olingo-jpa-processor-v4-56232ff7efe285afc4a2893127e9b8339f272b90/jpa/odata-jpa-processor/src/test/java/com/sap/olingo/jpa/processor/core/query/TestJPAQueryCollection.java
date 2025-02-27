package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestJPAQueryCollection extends TestBase {

  @Test
  public void testSelectPropertyAndCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$select=ID,Comment&orderby=ID");
    helper.assertStatus(200);

    final ArrayNode orgs = helper.getValues();
    ObjectNode org = (ObjectNode) orgs.get(0);
    assertNotNull(org.get("ID"));
    ArrayNode comment = (ArrayNode) org.get("Comment");
    assertEquals(2, comment.size());
  }

  // @Ignore // See https://issues.apache.org/jira/browse/OLINGO-1231
  @Test
  public void testSelectPropertyOfCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons('99')/InhouseAddress?$select=Building");
    helper.assertStatus(200);

    final ArrayNode buildings = helper.getValues();
    assertEquals(2, buildings.size());
    ObjectNode building = (ObjectNode) buildings.get(0);
    TextNode number = (TextNode) building.get("Building");
    assertNotNull(number);
  }

  @Test
  public void testSelectAllWithComplexCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons('99')?$select=*");
    helper.assertStatus(200);

    final ObjectNode person = helper.getValue();
    ArrayNode comment = (ArrayNode) person.get("InhouseAddress");
    assertEquals(2, comment.size());
  }

  @Test
  public void testSelectAllWithPrimitiveCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('1')?$select=*");
    helper.assertStatus(200);

    final ObjectNode person = helper.getValue();
    ArrayNode comment = (ArrayNode) person.get("Comment");
    assertEquals(2, comment.size());
  }

  @Test
  public void testSelectWithNestedComplexCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Collections('504')?$select=Nested");
    helper.assertStatus(200);

    final ObjectNode collection = helper.getValue();
    ArrayNode nested = (ArrayNode) collection.get("Nested");
    assertEquals(1, nested.size());
    ObjectNode n = (ObjectNode) nested.get(0);
    assertEquals(1L, n.get("Number").asLong());
    assertFalse(n.get("Inner") instanceof NullNode);
    assertEquals(6L, n.get("Inner").get("Figure3").asLong());
  }

  @Test
  public void testSelectComplexContainingCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Collections('502')?$select=Complex");
    helper.assertStatus(200);

    final ObjectNode collection = helper.getValue();
    ObjectNode complex = (ObjectNode) collection.get("Complex");
    assertEquals(32L, complex.get("Number").asLong());
    assertFalse(complex.get("Address") instanceof NullNode);
    assertEquals(2, complex.get("Address").size());
    assertEquals("DEV", complex.get("Address").get(0).get("TaskID").asText());
  }

  @Test
  public void testSelectComplexContainingTwoCollections() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Collections('501')?$select=Complex");
    helper.assertStatus(200);

    final ObjectNode collection = helper.getValue();
    ObjectNode complex = (ObjectNode) collection.get("Complex");
    assertEquals(-1L, complex.get("Number").asLong());
    assertFalse(complex.get("Address") instanceof NullNode);
    assertEquals(1, complex.get("Address").size());
    assertEquals("MAIN", complex.get("Address").get(0).get("TaskID").asText());
    assertFalse(complex.get("Comment") instanceof NullNode);
    assertEquals(1, complex.get("Comment").size());
    assertEquals("This is another test", complex.get("Comment").get(0).asText());
  }

  @Test
  public void testSelectAllWithComplexContainingCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Collections('502')");
    helper.assertStatus(200);

    final ObjectNode collection = helper.getValue();
    ObjectNode complex = (ObjectNode) collection.get("Complex");
    assertEquals(32L, complex.get("Number").asLong());
    assertFalse(complex.get("Address") instanceof NullNode);
    assertEquals(2, complex.get("Address").size());
    assertEquals("DEV", complex.get("Address").get(0).get("TaskID").asText());
  }

  @Test
  public void testSelectAllDeepComplexContainingCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "CollectionDeeps('501')");
    helper.assertStatus(200);

    final ObjectNode collection = helper.getValue();
    ObjectNode complex = (ObjectNode) collection.get("FirstLevel");
    assertEquals(1, complex.get("LevelID").asInt());
    assertFalse(complex.get("SecondLevel") instanceof NullNode);
    ObjectNode second = (ObjectNode) complex.get("SecondLevel");
    ArrayNode address = (ArrayNode) second.get("Address");
    assertEquals(32, address.get(0).get("RoomNumber").asInt());
  }

}
