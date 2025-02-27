package com.sap.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestJPAQueryWhereClause extends TestBase {

  @Test
  public void testFilterOneEquals() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$filter=ID eq '3'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
    assertEquals("3", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneDescriptionEquals() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=LocationName eq 'Deutschland'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
    assertEquals("10", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneDescriptionEqualsFieldNotSelected() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=LocationName eq 'Deutschland'&$select=ID");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
    assertEquals("10", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneEnumEquals() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=ABCClass eq com.sap.olingo.jpa.ABCClassifiaction'A'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
    assertEquals("1", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneEqualsTwoProperties() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=DivisionCode eq CountryCode");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(4, orgs.size());
  }

  @Test
  public void testFilterOneEqualsInvert() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$filter='3' eq ID");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
    assertEquals("3", orgs.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneNotEqual() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$filter=ID ne '3'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(9, orgs.size());
  }

  @Test
  public void testFilterOneEnumNotEqual() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$filter=AccessRights ne com.sap.olingo.jpa.AccessRights'Write'");
    helper.assertStatus(200);

    ArrayNode persons = helper.getValues();
    assertEquals(1, persons.size());
    assertEquals("97", persons.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneEnumEqualMultipleValues() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$filter=AccessRights eq com.sap.olingo.jpa.AccessRights'Read,Delete'");
    helper.assertStatus(200);

    ArrayNode persons = helper.getValues();
    assertEquals(1, persons.size());
    assertEquals("97", persons.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneGreaterEqualsString() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$filter=ID ge '5'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(5, orgs.size()); // '10' is smaller than '5' when comparing strings!
  }

  @Test
  public void testFilterOneLowerThanTwoProperties() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=DivisionCode lt CountryCode");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(244, orgs.size());
  }

  @Test
  public void testFilterOneGreaterThanString() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$filter=ID gt '5'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(4, orgs.size()); // '10' is smaller than '5' when comparing strings!
  }

  @Test
  public void testFilterOneLowerThanString() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$filter=ID lt '5'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(5, orgs.size());
  }

  @Test
  public void testFilterOneLowerEqualsString() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "Organizations?$filter=ID le '5'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(6, orgs.size());
  }

  @Test
  public void testFilterOneGreaterEqualsNumber() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "AdministrativeDivisions?$filter=Area ge 119330610");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(4, orgs.size());
  }

  @Ignore // TODO Clarify if GT, LE .. not supported by OData or "only" by Olingo
  @Test
  public void testFilterOneEnumGreaterThan() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$filter=AccessRights gt com.sap.olingo.jpa.AccessRights'Read'");
    helper.assertStatus(200);

    ArrayNode persons = helper.getValues();
    assertEquals(1, persons.size());
    assertEquals("99", persons.get(0).get("ID").asText());
  }

  @Test
  public void testFilterOneAndEquals() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and CodeID eq 'NUTS2'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(11, orgs.size());
  }

  @Test
  public void testFilterOneOrEquals() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=ID eq '5' or ID eq '10'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterOneNotLower() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=not (Area lt 50000000)");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(24, orgs.size());
  }

  @Test
  public void testFilterTwoAndEquals() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and CodeID eq 'NUTS2' and DivisionCode eq 'BE25'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
    assertEquals("BEL", orgs.get(0).get("CountryCode").asText());
  }

  @Test
  public void testFilterAndOrEqualsParenthesis() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and (DivisionCode eq 'BE25' or  DivisionCode eq 'BE24')&$orderby=DivisionCode desc");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
    assertEquals("BE25", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterAndOrEqualsNoParenthesis() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and DivisionCode eq 'BE25' or  CodeID eq '3166-1'&$orderby=DivisionCode desc");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(5, orgs.size());
    assertEquals("USA", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterAndWithFunction1() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and contains(tolower(DivisionCode),tolower('BE1'))&$orderby=DivisionCode asc");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(3, orgs.size());
    assertEquals("BE1", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterAndWithFunction2() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and contains(DivisionCode,'BE1')&$orderby=DivisionCode asc");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(3, orgs.size());
    assertEquals("BE1", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterAndWithComparisonContainingFunction() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=CodePublisher eq 'Eurostat' and tolower(DivisionCode) eq tolower('BE1')");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
    assertEquals("BE1", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterAddGreater() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Area add 7000000 ge 50000000");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(31, orgs.size());
  }

  @Test
  public void testFilterSubGreater() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Area sub 7000000 ge 60000000");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(15, orgs.size());
  }

  @Test
  public void testFilterDivGreater() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Area gt 0 and Area div Population ge 6000");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(9, orgs.size());
  }

  @Test
  public void testFilterMulGreater() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Area mul Population gt 0");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(64, orgs.size());
  }

  @Test
  public void testFilterMod() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Area gt 0 and Area mod 3578335 eq 0");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterLength() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=length(Name) eq 10");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(11, orgs.size());
  }

  @Test
  public void testFilterNow() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$filter=AdministrativeInformation/Created/At lt now()");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(3, orgs.size());
  }

  @Test
  public void testFilterContains() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=contains(CodeID,'166')");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(110, orgs.size());
  }

  @Test
  public void testFilterEndswith() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=endswith(CodeID,'166-1')");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(4, orgs.size());
  }

  @Test
  public void testFilterStartswith() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=startswith(DivisionCode,'DE-')");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(16, orgs.size());
  }

  @Test
  public void testFilterIndexOf() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=indexof(DivisionCode,'3') eq 4");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(7, orgs.size());
  }

  @Test
  public void testFilterSubstringStartIndex() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,6) eq 'Dakota'");
    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterSubstringStartEndIndex() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,0,5) eq 'North'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterSubstringLengthCalculated() throws IOException, ODataException {
    // substring(CompanyName, 1 add 4, 2 mul 3)
    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,0,1 add 4) eq 'North'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
  }

  @Ignore // Usage of mult currently creates parser error: The types 'Edm.Double' and '[Int64, Int32, Int16, Byte,
          // SByte]' are not compatible.
  @Test
  public void testFilterSubstringStartCalculated() throws IOException, ODataException {
    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and substring(Name,2 mul 3) eq 'Dakota'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterToLower() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and tolower(Name) eq 'brandenburg'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterToUpper() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and toupper(Name) eq 'HESSEN'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterToUpperInvers() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=toupper('nuts1') eq CodeID");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(19, orgs.size());
  }

  @Test
  public void testFilterTrim() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and trim(Name) eq 'Sachsen'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterConcat() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$filter=concat(concat(LastName,','),FirstName) eq 'Mustermann,Max'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAny() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=Roles/any(d:d/RoleCategory eq 'A')");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(3, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToManyValueNotAny() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=not (Roles/any(d:d/RoleCategory eq 'A'))");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(7, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAnyMultiParameter() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$select=ID&$filter=Roles/any(d:d/RoleCategory eq 'A' and d/BusinessPartnerID eq '1')");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAnyNoRestriction() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=Roles/any()");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(4, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAll() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$select=ID&$filter=Roles/all(d:d/RoleCategory eq 'A')");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterCountNavigationProperty() throws IOException, ODataException {
    // https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398301
    // Example 43: return all Categories with less than 10 products
    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$select=ID&$filter=Roles/$count eq 2");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterCountNavigationPropertyMultipleHops() throws IOException, ODataException {
    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$select=ID&$filter=AdministrativeInformation/Created/User/Roles/$count ge 2");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(8, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToOneValue() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Parent/CodeID eq 'NUTS1'");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(11, orgs.size());
  }

  @Test
  public void testFilterNavigationPropertyToOneValueAndEquals() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Parent/CodeID eq 'NUTS1' and DivisionCode eq 'BE34'");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyToOneValueTwoHops() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Parent/Parent/CodeID eq 'NUTS1' and DivisionCode eq 'BE212'");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyToOneValueViaComplexType() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=AdministrativeInformation/Created/User/LastName eq 'Mustermann'");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(8, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyDescriptionViaComplexTypeWOSubselectSelectAll() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=Address/RegionName eq 'Kalifornien'");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(3, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyDescriptionViaComplexTypeWOSubselectSelectId() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=Address/RegionName eq 'Kalifornien'&$select=ID");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(3, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyDescriptionToOneValueViaComplexTypeWSubselect1() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=AdministrativeInformation/Created/User/LocationName eq 'Schweiz'");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyDescriptionToOneValueViaComplexTypeWSubselect2() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=AdministrativeInformation/Created/User/LocationName eq 'Schweiz'&$select=ID");

    helper.assertStatus(200);
    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  };

  @Test
  public void testFilterNavigationPropertyAndExandThatNavigationProperty() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions?$filter=Parent/DivisionCode eq 'BE2'&$expand=Parent");

    helper.assertStatus(200);
    ArrayNode admin = helper.getValues();
    assertEquals(5, admin.size());
    assertNotNull(admin.get(3).findValue("Parent"));
    assertFalse(admin.get(3).findValue("Parent") instanceof NullNode);
    assertEquals("BE2", admin.get(3).findValue("Parent").get("DivisionCode").asText());
  };

  @Test
  public void testFilterNavigationPropertyViaJoinTableSubtype() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$select=ID&$filter=SupportedOrganizations/any()");

    helper.assertStatus(200);
    ArrayNode admin = helper.getValues();
    assertEquals(2, admin.size());
    assertEquals("98", admin.get(0).findValue("ID").asText());

  };

  @Ignore // EclipsLinkProblem see https://bugs.eclipse.org/bugs/show_bug.cgi?id=529565
  @Test
  public void testFilterNavigationPropertyViaJoinTableCountSubType() throws IOException, // NOSONAR
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$select=ID&$filter=SupportedOrganizations/$count gt 1");

    helper.assertStatus(200);
    ArrayNode admin = helper.getValues();
    assertEquals(2, admin.size());
    assertEquals("98", admin.get(0).findValue("ID").asText());

  };

  @Test
  public void testFilterMappedNavigationPropertyViaJoinTableSubtype() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$select=Name1&$filter=SupportEngineers/any(d:d/LastName eq 'Doe')");

    helper.assertStatus(200);
    ArrayNode admin = helper.getValues();
    assertEquals(1, admin.size());
    assertEquals("First Org.", admin.get(0).findValue("Name1").asText());

  };

  @Test
  public void testFilterNavigationPropertyViaJoinTableCount() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$filter=Teams/$count eq 0&$select=ID");

    helper.assertStatus(200);
    ArrayNode admin = helper.getValues();
    assertEquals(1, admin.size());
    assertEquals("98", admin.get(0).findValue("ID").asText());

  };

  @Test
  public void testFilterMappedNavigationPropertyViaJoinTableFilter() throws IOException,
      ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Teams?$select=Name&$filter=Member/any(d:d/LastName eq 'Mustermann')");

    helper.assertStatus(200);
    ArrayNode admin = helper.getValues();
    assertEquals(2, admin.size());
  };

  @Test
  public void testFilterWithAllExpand() throws ODataException, IOException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$filter=Name1 eq 'Third Org.'&$expand=Roles");

    helper.assertStatus(200);
    ArrayNode org = helper.getValues();
    assertNotNull(org);
    assertEquals(1, org.size());
    assertEquals(3, org.get(0).get("Roles").size());
  }

  @Test
  public void testFilterSubstringStartEndIndexToLower() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisionDescriptions?$filter=Language eq 'de' and tolower(substring(Name,0,5)) eq 'north'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(2, orgs.size());
  }

  @Test
  public void testFilterOneHas() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$filter=AccessRights has com.sap.olingo.jpa.AccessRights'Read'");

    helper.assertStatus(200);

    ArrayNode orgs = helper.getValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterNavigationTarget() throws IOException, ODataException {
    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')/Children?$filter=DivisionCode eq 'BE21'");
    helper.assertStatus(200);

    final ObjectNode div = helper.getValue();
    final ObjectNode result = (ObjectNode) div.get("value").get(0);
    assertNotNull(result);
    assertEquals("BE21", result.get("DivisionCode").asText());
  }

  @Test
  public void testFilterCollectionSinplePropertyThrowsError() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$select=ID&$filter=contains(Comment, 'just')");

    helper.assertStatus(400); // Olingo rejects a bunch of functions.
  }

  @Test
  public void testFilterCollectionPropertyAny() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Organizations?$select=ID&$filter=Comment/any(s:contains(s, 'just'))");

    helper.assertStatus(200);
    ArrayNode org = helper.getValues();
    assertNotNull(org);
    assertEquals(1, org.size());
  }

  @Test
  public void testFilterCollectionPropertyDeepSimpleCount() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "CollectionDeeps?$filter=FirstLevel/SecondLevel/Comment/$count eq 2&$select=ID");

    helper.assertStatus(200);
    ArrayNode deep = helper.getValues();
    assertNotNull(deep);
    assertEquals(1, deep.size());
  }

  @Test
  public void testFilterCollectionPropertyDeepComplexCount() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "CollectionDeeps?$filter=FirstLevel/SecondLevel/Address/$count eq 2&$select=ID");

    helper.assertStatus(200);
    ArrayNode deep = helper.getValues();
    assertNotNull(deep);
    assertEquals(1, deep.size());
  }

  @Test
  public void testFilterCollectionPropertyAsPartOfComplexAny() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "CollectionDeeps?$filter=FirstLevel/SecondLevel/Address/any(s:s/TaskID eq 'DEV')");

    helper.assertStatus(200);
    ArrayNode org = helper.getValues();
    assertNotNull(org);
    assertEquals(1, org.size());
  }

  @Test
  public void testFilterCollectionOnPropertyWithNavigation() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons('99')/InhouseAddress?$filter=TaskID eq 'DEV'");

    helper.assertStatus(200);
    ArrayNode addr = helper.getValues();
    assertNotNull(addr);
    assertEquals(1, addr.size());
  }

  @Test
  public void testFilterCollectionPropertyWithOutNavigationThrowsError() throws IOException, ODataException {

    IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "Persons?$select=ID&$filter=InhouseAddress/TaskID eq 'DEV'");

    helper.assertStatus(400); // The URI is malformed
  }
}
