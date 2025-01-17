package com.sap.olingo.jpa.processor.core.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate.BooleanOperator;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.eclipse.persistence.internal.jpa.querydef.CompoundExpressionImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAProtectionInfo;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAServiceDocument;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.api.JPAClaimsPair;
import com.sap.olingo.jpa.processor.core.api.JPAODataClaimsProvider;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.JPAEntityTypeDouble;
import com.sap.olingo.jpa.processor.core.util.TestQueryBase;

public class TestJPAQueryWithProtection extends TestQueryBase {
  private JPAODataSessionContextAccess contextSpy;
  private JPAServiceDocument sdSpy;
  private EdmType odataType;
  private List<JPAAttribute> attributes;
  private Set<String> claimNames;
  private List<String> pathList;
  private JPAEntityType etSpy;
  private List<JPAProtectionInfo> protections;

  @Override
  @BeforeEach
  public void setup() throws ODataException {
    super.setup();
    contextSpy = Mockito.spy(context);
    JPAEdmProvider providerSpy = Mockito.spy(context.getEdmProvider());
    sdSpy = Mockito.spy(context.getEdmProvider().getServiceDocument());
    when(contextSpy.getEdmProvider()).thenReturn(providerSpy);
    when(providerSpy.getServiceDocument()).thenReturn(sdSpy);

  }

  @Test
  public void testRestrictOnePropertyOneValue() throws IOException, ODataException {
    JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add("UserId", new JPAClaimsPair<>("Willi"));
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "BusinessPartnerProtecteds?$select=ID,Name1,Country", claims);
    helper.assertStatus(200);

    final ArrayNode bupa = helper.getValues();
    assertEquals(3, bupa.size());
  }

  @Test
  public void testRestrictOnePropertyTwoValues() throws IOException, ODataException {
    JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add("UserId", new JPAClaimsPair<>("Willi"));
    claims.add("UserId", new JPAClaimsPair<>("Marvin"));
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "BusinessPartnerProtecteds?$select=ID,Name1,Country", claims);
    helper.assertStatus(200);

    final ArrayNode bupa = helper.getValues();
    assertEquals(16, bupa.size());
  }

  @Test
  public void testRestrictOnePropertyNoProvider() throws IOException, ODataException {
    JPAODataClaimsProvider claims = null;

    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "BusinessPartnerProtecteds?$select=ID,Name1,Country", claims);
    helper.assertStatus(403);
  }

  @Test
  public void testRestrictOnePropertyNoValue() throws IOException, ODataException {
    JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "BusinessPartnerProtecteds?$select=ID,Name1,Country", claims);
    helper.assertStatus(403);
  }

  @Test
  public void testRestrictOnePropertyBetweenValues() throws IOException, ODataException {
    JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add("UserId", new JPAClaimsPair<>("Marvin", "Willi"));
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "BusinessPartnerProtecteds?$select=ID,Name1,Country", claims);
    helper.assertStatus(200);

    final ArrayNode bupa = helper.getValues();
    assertEquals(16, bupa.size());
  }

  @Test
  public void testRestrictOnePropertyOneValueWithNavigationToRoles() throws IOException, ODataException {
    JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add("UserId", new JPAClaimsPair<>("Willi"));
    final IntegrationTestHelper helper = new IntegrationTestHelper(emf,
        "BusinessPartnerProtecteds('99')/Roles", claims);
    helper.assertStatus(200);

    final ArrayNode bupa = helper.getValues();
    assertEquals(2, bupa.size());
  }

  @Test
  public void testRestrictComplexOnePropertyOnValue() throws ODataException {
    prepareTest();
    prepareComplexAttributeCreateUser("UserId");

    claimNames.add("UserId");
    final JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add("UserId", new JPAClaimsPair<>("Marvin"));

    JPAAttribute aSpy = Mockito.spy(etSpy.getAttribute("administrativeInformation"));
    doReturn(true).when(aSpy).hasProtection();
    doReturn(claimNames).when(aSpy).getProtectionClaimNames();
    doReturn(pathList).when(aSpy).getProtectionPath("UserId");
    attributes.add(aSpy);

    final Expression<Boolean> act = ((JPAJoinQuery) cut).createProtectionWhere(Optional.of(claims));
    assertEqual(act);
  }

  @Test
  public void testRestrictComplexOnePropertyUpperLowerValues() throws ODataException {
    final String claimName = "UserId";
    prepareTest();
    prepareComplexAttributeCreateUser(claimName);

    claimNames.add(claimName);
    final JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add(claimName, new JPAClaimsPair<>("Marvin", "Willi"));

    JPAAttribute aSpy = Mockito.spy(etSpy.getAttribute("administrativeInformation"));
    doReturn(true).when(aSpy).hasProtection();
    doReturn(claimNames).when(aSpy).getProtectionClaimNames();
    doReturn(pathList).when(aSpy).getProtectionPath("UserId");
    attributes.add(aSpy);

    final Expression<Boolean> act = ((JPAJoinQuery) cut).createProtectionWhere(Optional.of(claims));
    assertBetween(act);
  }

  @Test
  public void testRestrictComplexOnePropertyTwoValues() throws ODataException {
    final String claimName = "UserId";
    prepareTest();
    prepareComplexAttributeCreateUser(claimName);

    claimNames.add(claimName);
    final JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add(claimName, new JPAClaimsPair<>("Marvin"));
    claims.add(claimName, new JPAClaimsPair<>("Willi"));

    JPAAttribute aSpy = Mockito.spy(etSpy.getAttribute("administrativeInformation"));
    doReturn(true).when(aSpy).hasProtection();
    doReturn(claimNames).when(aSpy).getProtectionClaimNames();
    doReturn(pathList).when(aSpy).getProtectionPath(claimName);
    attributes.add(aSpy);

    final Expression<Boolean> act = ((JPAJoinQuery) cut).createProtectionWhere(Optional.of(claims));
    assertEquals(BooleanOperator.OR, ((CompoundExpressionImpl) act).getOperator());
    for (Expression<?> part : ((CompoundExpressionImpl) act).getChildExpressions())
      assertEqual(part);
  }

  @Test
  public void testRestrictComplexOnePropertyOneValuesDate() throws ODataException {
    final String claimName = "CreationDate";
    prepareTest();
    prepareComplexAttributeDate(claimName);

    claimNames.add(claimName);
    final JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add(claimName, new JPAClaimsPair<>(Date.valueOf("2010-01-01")));

    JPAAttribute aSpy = Mockito.spy(etSpy.getAttribute("administrativeInformation"));
    doReturn(true).when(aSpy).hasProtection();
    doReturn(claimNames).when(aSpy).getProtectionClaimNames();
    doReturn(pathList).when(aSpy).getProtectionPath(claimName);
    attributes.add(aSpy);

    final Expression<Boolean> act = ((JPAJoinQuery) cut).createProtectionWhere(Optional.of(claims));
    assertEqual(act);
  }

  @Test
  public void testRestrictComplexOnePropertyUpperLowerValuesDate() throws ODataException {
    final String claimName = "CreationDate";
    prepareTest();
    prepareComplexAttributeDate(claimName);

    claimNames.add(claimName);
    final JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add(claimName, new JPAClaimsPair<>(Date.valueOf("2010-01-01"), Date.valueOf("9999-12-30")));

    JPAAttribute aSpy = Mockito.spy(etSpy.getAttribute("administrativeInformation"));
    doReturn(true).when(aSpy).hasProtection();
    doReturn(claimNames).when(aSpy).getProtectionClaimNames();
    doReturn(pathList).when(aSpy).getProtectionPath(claimName);
    attributes.add(aSpy);

    final Expression<Boolean> act = ((JPAJoinQuery) cut).createProtectionWhere(Optional.of(claims));
    assertBetween(act);
  }

  @Test
  public void testRestrictComplexTwoPropertyOneValuesOperatorAND() throws ODataException {
    final String claimName = "UserId";
    prepareTest();
    prepareComplexAttributeCreateUser(claimName);
    prepareComplexAttributeUpdateUser(claimName);

    claimNames.add(claimName);
    final JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add(claimName, new JPAClaimsPair<>("Marvin"));

    JPAAttribute aSpy = Mockito.spy(etSpy.getAttribute("administrativeInformation"));
    doReturn(true).when(aSpy).hasProtection();
    doReturn(claimNames).when(aSpy).getProtectionClaimNames();
    doReturn(pathList).when(aSpy).getProtectionPath(claimName);
    attributes.add(aSpy);

    final Expression<Boolean> act = ((JPAJoinQuery) cut).createProtectionWhere(Optional.of(claims));
    assertEquals(BooleanOperator.AND, ((CompoundExpressionImpl) act).getOperator());
    for (Expression<?> part : ((CompoundExpressionImpl) act).getChildExpressions())
      assertEqual(part);
  }

  @Test
  public void testRestrictTwoPropertiesOneValuesOperatorAND() throws ODataException {
    final String claimName = "UserId";
    prepareTest();
    prepareComplexAttributeCreateUser(claimName);
    prepareComplexAttributeUpdateUser(claimName);

    claimNames.add(claimName);
    final JPAODataClaimsProvider claims = new JPAODataClaimsProvider();
    claims.add("UserId", new JPAClaimsPair<>("Marvin"));

    JPAAttribute aSpy = Mockito.spy(etSpy.getAttribute("administrativeInformation"));
    doReturn(true).when(aSpy).hasProtection();
    doReturn(claimNames).when(aSpy).getProtectionClaimNames();
    doReturn(pathList).when(aSpy).getProtectionPath(claimName);
    attributes.add(aSpy);

    final Expression<Boolean> act = ((JPAJoinQuery) cut).createProtectionWhere(Optional.of(claims));
    assertEquals(BooleanOperator.AND, ((CompoundExpressionImpl) act).getOperator());
    for (Expression<?> part : ((CompoundExpressionImpl) act).getChildExpressions())
      assertEqual(part);
  }

  private void assertBetween(Expression<Boolean> act) {
    assertExpression(act, "between", 3);
  }

  private void assertEqual(Expression<?> act) {
    assertExpression(act, "equal", 2);
  }

  private void assertExpression(Expression<?> act, String operator, int size) {
    assertNotNull(act);
    final List<Expression<?>> actChildren = ((CompoundExpressionImpl) act).getChildExpressions();
    assertEquals(size, actChildren.size());
    assertEquals(operator, ((CompoundExpressionImpl) act).getOperation());
    assertEquals("Path", actChildren.get(0).getClass().getInterfaces()[0].getSimpleName());
  }

  private void prepareComplexAttributeUser(final String claimName, final String pathName,
      final String intermediateElement) throws ODataJPAModelException {

    final JPAProtectionInfo protection = Mockito.mock(JPAProtectionInfo.class);
    protections.add(protection);

    final String path = pathName;
    pathList.add(path);
    final JPAPath jpaPath = Mockito.mock(JPAPath.class);
    final JPAElement adminAttri = Mockito.mock(JPAElement.class);
    final JPAElement complexAttri = Mockito.mock(JPAElement.class);
    final JPAAttribute simpleAttri = Mockito.mock(JPAAttribute.class);
    final List<JPAElement> pathElements = Arrays.asList(new JPAElement[] { adminAttri, complexAttri, simpleAttri });
    doReturn(pathElements).when(jpaPath).getPath();
    doReturn("administrativeInformation").when(adminAttri).getInternalName();
    doReturn(intermediateElement).when(complexAttri).getInternalName();
    doReturn("by").when(simpleAttri).getInternalName();
    doReturn(String.class).when(simpleAttri).getType();
    doReturn(simpleAttri).when(jpaPath).getLeaf();
    doReturn(jpaPath).when(etSpy).getPath(path);

    doReturn(simpleAttri).when(protection).getAttribute();
    doReturn(jpaPath).when(protection).getPath();
    doReturn(claimName).when(protection).getClaimName();
  }

  private void prepareComplexAttributeCreateUser(final String claimName) throws ODataJPAModelException {
    prepareComplexAttributeUser(claimName, "AdministrativeInformation/Created/By", "created");
  }

  private void prepareComplexAttributeUpdateUser(final String claimName) throws ODataJPAModelException {
    prepareComplexAttributeUser(claimName, "AdministrativeInformation/Updated/By", "updated");
  }

  private void prepareComplexAttributeDate(final String claimName) throws ODataJPAModelException {

    final JPAProtectionInfo protection = Mockito.mock(JPAProtectionInfo.class);
    protections.add(protection);

    final String path = "AdministrativeInformation/Created/At";
    pathList.add(path);
    final JPAPath jpaPath = Mockito.mock(JPAPath.class);
    final JPAElement adminAttri = Mockito.mock(JPAElement.class);
    final JPAElement complexAttri = Mockito.mock(JPAElement.class);
    final JPAAttribute simpleAttri = Mockito.mock(JPAAttribute.class);
    final List<JPAElement> pathElements = Arrays.asList(new JPAElement[] { adminAttri, complexAttri, simpleAttri });
    doReturn(pathElements).when(jpaPath).getPath();
    doReturn("administrativeInformation").when(adminAttri).getInternalName();
    doReturn("created").when(complexAttri).getInternalName();
    doReturn("at").when(simpleAttri).getInternalName();
    doReturn(Date.class).when(simpleAttri).getType();
    doReturn(simpleAttri).when(jpaPath).getLeaf();
    doReturn(jpaPath).when(etSpy).getPath(path);

    doReturn(simpleAttri).when(protection).getAttribute();
    doReturn(jpaPath).when(protection).getPath();
    doReturn(claimName).when(protection).getClaimName();

  }

  private void prepareTest() throws ODataException {
    buildUriInfo("BusinessPartnerProtecteds", "BusinessPartnerProtected");
    odataType = ((UriResourceEntitySet) uriInfo.getUriResourceParts().get(0)).getType();
    attributes = new ArrayList<>();
    claimNames = new HashSet<>();
    pathList = new ArrayList<>();
    protections = new ArrayList<>();

    etSpy = Mockito.spy(new JPAEntityTypeDouble(sdSpy.getEntity("BusinessPartnerProtecteds")));
    doReturn(attributes).when(etSpy).getAttributes();
    doReturn(protections).when(etSpy).getProtections();
    doReturn(etSpy).when(sdSpy).getEntity("BusinessPartnerProtecteds");
    doReturn(etSpy).when(sdSpy).getEntity(odataType);
    cut = new JPAJoinQuery(null, contextSpy, emf.createEntityManager(), headers, uriInfo);
    cut.createFromClause(new ArrayList<JPAAssociationPath>(1), new ArrayList<JPAPath>(), cut.cq);

  }
}
