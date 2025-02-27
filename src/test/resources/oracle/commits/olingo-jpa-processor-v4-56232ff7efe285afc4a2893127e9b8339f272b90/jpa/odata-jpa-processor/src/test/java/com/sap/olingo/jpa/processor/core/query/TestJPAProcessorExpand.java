package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestJPAProcessorExpand extends TestBase {

  @Test
  public void testExpandEntitySet() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$orderby=ID&$expand=Roles");
    helper.assertStatus(200);

    final ArrayNode orgs = helper.getValues();
    ObjectNode org = (ObjectNode) orgs.get(0);
    ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(1, roles.size());

    org = (ObjectNode) orgs.get(3);
    roles = (ArrayNode) org.get("Roles");
    assertEquals(3, roles.size());
  }

  @Test
  public void testExpandOneEntity() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations('2')?$expand=Roles");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(2, roles.size());
    int found = 0;
    for (final JsonNode role : roles) {
      final String id = role.get("BusinessPartnerID").asText();
      final String code = role.get("RoleCategory").asText();
      if (id.equals("2") && (code.equals("A") || code.equals("C")))
        found++;
    }
    assertEquals("Not all expected results found", 2, found);
  }

  @Test
  public void testExpandOneEntityCompoundKey() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Parent");
    helper.assertStatus(200);

    final ObjectNode divsion = helper.getValue();
    final ObjectNode parent = (ObjectNode) divsion.get("Parent");
    assertEquals("BE2", parent.get("DivisionCode").asText());

  }

  @Test
  public void testExpandOneEntityCompoundKeyCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Children($orderby=DivisionCode asc)");
    helper.assertStatus(200);

    final ObjectNode divsion = helper.getValue();
    final ArrayNode parent = (ArrayNode) divsion.get("Children");
    assertEquals(8, parent.size());
    assertEquals("BE251", parent.get(0).get("DivisionCode").asText());

  }

  @Test
  public void testExpandEntitySetWithOutParentKeySelection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$orderby=Name1&$select=Name1&$expand=Roles");
    helper.assertStatus(200);

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(9);
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(3, roles.size());

  }

  @Test
  public void testExpandEntitySetViaNonKeyField_FieldNotSelected() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')/AdministrativeInformation/Created?$select=At&$expand=User");
    helper.assertStatus(200);

    final ObjectNode created = helper.getValue();
    assertNotNull(created.get("User"));
  }

  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi2Hops() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')/AdministrativeInformation/Created?$expand=User");
    helper.assertStatus(200);

    final ObjectNode created = helper.getValue();
    @SuppressWarnings("unused")
    final ObjectNode user = (ObjectNode) created.get("User");
  }

  @Test
  public void testExpandEntityViaComplexProperty() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')/Address?$expand=AdministrativeDivision");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    final ObjectNode created = (ObjectNode) org.get("AdministrativeDivision");
    assertEquals("USA", created.get("ParentDivisionCode").asText());
  }

  @Ignore // Version 4.4.0 of olingo does not path the expand correctly
  // org.apache.olingo.server.core.uri.parser.ExpandParser -> parseExpandPath
  // see https://issues.apache.org/jira/browse/OLINGO-1143
  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi0Hops() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')?$expand=AdministrativeInformation/Created/User");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    final ObjectNode admin = (ObjectNode) org.get("AdministrativeInformation");
    final ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User"));

  }

  @Ignore // Version 4.4.0 of olingo does not path the expand correctly
  // org.apache.olingo.server.core.uri.parser.ExpandParser -> parseExpandPath
  // see https://issues.apache.org/jira/browse/OLINGO-1143
  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi1Hop() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')/AdministrativeInformation?$expand=Created/User");
    helper.assertStatus(200);

    final ObjectNode admin = helper.getValue();
    final ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User"));
  }

  @Test
  public void testNestedExpandNestedExpand2LevelsSelf() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE253',CodeID='NUTS3',CodePublisher='Eurostat')?$expand=Parent($expand=Children)");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("Children"));
    final ArrayNode children = (ArrayNode) parent.get("Children");
    assertEquals(8, children.size());
    assertEquals("NUTS3", children.get(0).get("CodeID").asText());
  }

  @Test
  public void testNestedExpandNestedExpand3LevelsSelf() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='33016',CodeID='LAU2',CodePublisher='Eurostat')?$expand=Parent($expand=Parent($expand=Parent))");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("Parent"));
    assertNotNull(parent.get("Parent").get("CodeID"));
    assertEquals("NUTS3", parent.get("CodeID").asText());
    final ObjectNode grandParent = (ObjectNode) parent.get("Parent");
    assertNotNull(grandParent);
    assertNotNull(grandParent.get("CodeID"));
    assertEquals("NUTS2", grandParent.get("CodeID").asText());
    final ObjectNode greateGrandParent = (ObjectNode) grandParent.get("Parent");
    assertNotNull(greateGrandParent);
    assertNotNull(greateGrandParent.get("CodeID"));
    assertEquals("NUTS1", greateGrandParent.get("CodeID").asText());
  }

  @Test
  public void testNestedExpandNestedExpand3Only1PossibleLevelsSelf() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Parent($expand=Parent($expand=Parent))");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("CodeID"));
    assertEquals("NUTS1", parent.get("CodeID").asText());
  }

  @Test
  public void testNestedExpandNestedExpand2LevelsMixed() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('3')/Address?$select=Country&$expand=AdministrativeDivision($expand=Parent)");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ObjectNode admin = (ObjectNode) div.get("AdministrativeDivision");
    assertNotNull(admin);
    final ObjectNode parent = (ObjectNode) admin.get("Parent");
    assertEquals("3166-1", parent.get("CodeID").asText());
  }

  @Ignore // TODO check how the result should look like
  @Test
  public void testExpandWithNavigationToEntity() throws IOException, ODataException {
    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE253',CodeID='3',CodePublisher='NUTS')?$expand=Parent/Parent");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("Parent").get("CodeID"));
    assertEquals("1", parent.get("Parent").get("CodeID").asText());
  }

  @Ignore // TODO check with Olingo looks like OData does not support this
  @Test
  public void testExpandWithNavigationToProperty() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE253',CodeID='NUTS3',CodePublisher='Eurostat')?$expand=Parent/CodeID");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("CodeID"));
    assertEquals("NUTS2", parent.get("CodeID").asText());
    // TODO: Check how to create the response correctly
    // assertEquals(1, parent.size());
  }

  @Test
  public void testExpandAfterNavigationToEntity() throws IOException, ODataException {
    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')/Children?$filter=DivisionCode eq 'BE21'&$expand=Children($orderby=DivisionCode)");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("value").get(0).get("Children");
    assertNotNull(children);
    assertEquals(3, children.size());
    assertEquals("BE211", children.get(0).get("DivisionCode").asText());
    assertEquals("BE212", children.get(1).get("DivisionCode").asText());
    assertEquals("BE213", children.get(2).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByDesc() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($orderby=DivisionCode desc)");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(5, children.size());
    assertEquals("BE25", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByAsc() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($orderby=DivisionCode asc)");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(5, children.size());
    assertEquals("BE21", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByDescTop() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($top=2;$orderby=DivisionCode desc)");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(2, children.size());
    assertEquals("BE25", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByDescTopSkip() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($top=2;$skip=2;$orderby=DivisionCode desc)");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(2, children.size());
    assertEquals("BE23", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithCount() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$count=true&$expand=Roles($count=true)&$orderby=Roles/$count desc");
    helper.assertStatus(200);

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertNotNull(org.get("Roles"));
    ArrayNode roles = (ArrayNode) org.get("Roles");
    assertNotNull(org.get("Roles@odata.count"));
    assertEquals(roles.size(), org.get("Roles@odata.count").asInt());
  }

  @Test
  public void testExpandWithCountPath() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('2')?$expand=Roles/$count");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Roles@odata.count"));
    assertEquals(2, org.get("Roles@odata.count").asInt());
  }

  @Ignore // ODataJsonSerializer.writeExpandedNavigationProperty does not write a "@odata.count" for to 1 relations
  @Test
  public void testExpandOppositeDirectionWithCount() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "BusinessPartnerRoles(BusinessPartnerID='1',RoleCategory='A')?$expand=Organization/$count");
    helper.assertStatus(200);

    final ObjectNode role = helper.getValue();
    assertNotNull(role.get("Organization"));
    assertNotNull(role.get("Organization@odata.count"));
    assertEquals(1, role.get("Organization@odata.count").asText());
  }

  @Test
  public void testExpandWithCountAndTop() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$count=true&$expand=Roles($count=true;$top=1)&$orderby=Roles/$count desc");
    helper.assertStatus(200);

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertNotNull(org.get("Roles"));
    assertNotNull(org.get("Roles@odata.count"));
    assertEquals(3, org.get("Roles@odata.count").asInt());
  }

  @Test
  public void testExpandWithOrderByDescTopSkipAndExternalOrderBy() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$count=true&$expand=Roles($orderby=RoleCategory desc)&$orderby=Roles/$count desc");

    helper.assertStatus(200);

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertEquals("3", org.get("ID").asText());
    assertNotNull(org.get("Roles"));
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(3, roles.size());
    final ObjectNode firstRole = (ObjectNode) roles.get(0);
    assertEquals("C", firstRole.get("RoleCategory").asText());
  }

  @Test
  public void testExpandWithFilter() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Children($filter=DivisionCode eq 'BE252')");

    helper.assertStatus(200);

    final ObjectNode division = helper.getValue();
    assertEquals("BE25", division.get("DivisionCode").asText());
    assertNotNull(division.get("Children"));
    final ArrayNode children = (ArrayNode) division.get("Children");
    assertEquals(1, children.size());
    final ObjectNode firstChild = (ObjectNode) children.get(0);
    assertEquals("BE252", firstChild.get("DivisionCode").asText());
  }

  @Test
  public void testFilterExpandWithFilter() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=DivisionCode eq 'BE25' and CodeID eq 'NUTS2'&$expand=Children($filter=DivisionCode eq 'BE252')");

    helper.assertStatus(200);

    final JsonNode value = helper.getValue().get("value");
    assertNotNull(value.get(0));
    final ObjectNode division = (ObjectNode) value.get(0);

    assertEquals("BE25", division.get("DivisionCode").asText());
    ArrayNode children = (ArrayNode) division.get("Children");
    assertEquals(1, children.size());
    final ObjectNode firstChild = (ObjectNode) children.get(0);
    assertEquals("BE252", firstChild.get("DivisionCode").asText());
  }

  @Test
  public void testFilterExpandWithFilterOnParentDescription() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$select=ID&$filter=LocationName eq 'Deutschland'&$expand=Roles&$orderby=ID asc");

    helper.assertStatus(200);

    final JsonNode value = helper.getValue().get("value");
    assertNotNull(value.get(0));
    final ArrayNode roles = (ArrayNode) value.get(0).get("Roles");

    assertEquals(1, roles.size());
  }

  @Test
  public void testExpandCompleteEntitySet() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$expand=Roles&orderby=ID");

    helper.assertStatus(200);

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertEquals("1", org.get("ID").asText());
    assertNotNull(org.get("Roles"));
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(1, roles.size());
    final ObjectNode firstRole = (ObjectNode) roles.get(0);
    assertEquals("A", firstRole.get("RoleCategory").asText());
  }

  @Test
  public void testExpandTwoNavigationPath() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE32',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Parent,Children");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Test
  public void testExpandAllNavigationPath() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE32',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=*");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Test
  public void testExpandAllNavigationPathOfPath() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE32',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=*");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Test
  public void testExpandLevel1() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='38025',CodeID='LAU2',CodePublisher='Eurostat')?$expand=Parent($levels=1)");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final TextNode divCode = (TextNode) parent.get("DivisionCode");
    assertEquals("BE258", divCode.asText());
  }

  @Test
  public void testExpandLevel2() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='38025',CodeID='LAU2',CodePublisher='Eurostat')?$expand=Parent($levels=2)");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertFalse(org.get("Parent") instanceof NullNode);
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    final TextNode parentDivCode = (TextNode) parent.get("DivisionCode");
    assertEquals("BE258", parentDivCode.asText());

    assertFalse(parent.get("Parent") instanceof NullNode);
    final ObjectNode grandParent = (ObjectNode) parent.get("Parent");
    assertNotNull(grandParent.get("DivisionCode"));
    final TextNode grandparentDivCode = (TextNode) grandParent.get("DivisionCode");
    assertEquals("BE25", grandparentDivCode.asText());
  }

  @Test
  public void testExpandLevelMax() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE241',CodeID='NUTS3',CodePublisher='Eurostat')?$expand=Parent($levels=max)");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertFalse(org.get("Parent") instanceof NullNode);
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    final TextNode parentDivCode = (TextNode) parent.get("DivisionCode");
    assertEquals("BE24", parentDivCode.asText());

    assertFalse(parent.get("Parent") instanceof NullNode);
    final ObjectNode grandParent = (ObjectNode) parent.get("Parent");
    assertNotNull(grandParent.get("DivisionCode"));
    final TextNode grandparentDivCode = (TextNode) grandParent.get("DivisionCode");
    assertEquals("BE2", grandparentDivCode.asText());
  }

  @Test
  public void testExpandAllNavigationPathWithComplex() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations('3')?$expand=*");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Roles"));

  }

  @Test
  public void testExpandCompleteEntitySet2() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "AdministrativeDivisions?$expand=Parent");

    helper.assertStatus(200);
  }

  @Test
  public void testExpandViaJoinTable() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('2')?$select=Name1&$expand=SupportEngineers($select=FirstName,LastName)");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("SupportEngineers"));

  }

  @Test
  public void testExpandViaJoinTable2Levels() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations('1')?$select=Name1&$expand=SupportEngineers($select=FirstName,LastName;$expand=SupportedOrganizations)");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("SupportEngineers"));
    ArrayNode supportOrgs = (ArrayNode) org.get("SupportEngineers").get(0).get("SupportedOrganizations");
    assertNotNull(supportOrgs);
    assertEquals(1, supportOrgs.size());

    supportOrgs = (ArrayNode) org.get("SupportEngineers").get(1).get("SupportedOrganizations");
    assertNotNull(supportOrgs);
    assertEquals(2, supportOrgs.size());
  }

  @Test
  public void testExpandViaJoinTable1LevelNoSubType() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$select=LastName&$expand=Teams");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org);
  }

  @Test
  public void testExpandViaJoinTable1LevelNoMapped() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "JoinSources(1)?$expand=OneToMany");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("OneToMany"));
    ArrayNode oneToMany = (ArrayNode) org.get("OneToMany");
    assertEquals(2, oneToMany.size());
  }

  @Test
  public void testExpandViaJoinTable1LevelNoMappedHidden() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "JoinSources(2)?$expand=OneToManyHidden");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("OneToManyHidden"));
    ArrayNode oneToMany = (ArrayNode) org.get("OneToManyHidden");
    assertEquals(2, oneToMany.size());
  }

  @Test
  public void testExpandViaJoinTableComplex() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "JoinSources(1)/Complex?$expand=OneToManyComplex");
    helper.assertStatus(200);

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("OneToManyComplex"));
    ArrayNode oneToMany = (ArrayNode) org.get("OneToManyComplex");
    assertEquals(2, oneToMany.size());
  }
}
