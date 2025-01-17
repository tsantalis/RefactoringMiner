package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.api.JPAODataContextAccessDouble;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;
import com.sap.olingo.jpa.processor.core.util.TestBase;
import com.sap.olingo.jpa.processor.core.util.TestHelper;

public class TestJPAQueryFromClause extends TestBase {
  private JPAAbstractJoinQuery cut;
  private JPAEntityType jpaEntityType;

  @Before
  public void setup() throws ODataException {
    final UriInfo uriInfo = Mockito.mock(UriInfo.class);
    final EdmEntitySet odataEs = Mockito.mock(EdmEntitySet.class);
    final EdmType odataType = Mockito.mock(EdmEntityType.class);
    final List<UriResource> resources = new ArrayList<>();
    final UriResourceEntitySet esResource = Mockito.mock(UriResourceEntitySet.class);
    Mockito.when(uriInfo.getUriResourceParts()).thenReturn(resources);
    Mockito.when(esResource.getKeyPredicates()).thenReturn(new ArrayList<>(1));
    Mockito.when(esResource.getEntitySet()).thenReturn(odataEs);
    Mockito.when(esResource.getKind()).thenReturn(UriResourceKind.entitySet);
    Mockito.when(esResource.getType()).thenReturn(odataType);
    Mockito.when(odataEs.getName()).thenReturn("Organizations");
    Mockito.when(odataType.getNamespace()).thenReturn(PUNIT_NAME);
    Mockito.when(odataType.getName()).thenReturn("Organization");
    resources.add(esResource);

    helper = new TestHelper(emf, PUNIT_NAME);
    jpaEntityType = helper.getJPAEntityType("Organizations");
    JPAODataSessionContextAccess context = new JPAODataContextAccessDouble(new JPAEdmProvider(PUNIT_NAME, emf, null,
        TestBase.enumPackages), ds, null);
    createHeaders();

    cut = new JPAJoinQuery(null, context, emf.createEntityManager(), headers, uriInfo);
  }

  @Test
  public void checkFromListContainsRoot() throws ODataApplicationException {

    Map<String, From<?, ?>> act = cut.createFromClause(new ArrayList<JPAAssociationPath>(1),
        new ArrayList<JPAPath>(), cut.cq);
    assertNotNull(act.get(jpaEntityType.getExternalFQN().getFullQualifiedNameAsString()));
  }

  @Test
  public void checkFromListOrderByContainsOne() throws ODataJPAModelException, ODataApplicationException {
    final List<JPAAssociationPath> orderBy = new ArrayList<>();
    final JPAAssociationPath exp = buildRoleAssociationPath(orderBy);

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, new ArrayList<JPAPath>(), cut.cq);
    assertNotNull(act.get(exp.getAlias()));
  }

  @Test
  public void checkFromListOrderByOuterJoinOne() throws ODataJPAModelException, ODataApplicationException {
    final List<JPAAssociationPath> orderBy = new ArrayList<>();
    buildRoleAssociationPath(orderBy);

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, new ArrayList<JPAPath>(), cut.cq);

    @SuppressWarnings("unchecked")
    Root<Organization> root = (Root<Organization>) act.get(jpaEntityType.getExternalFQN()
        .getFullQualifiedNameAsString());
    Set<Join<Organization, ?>> joins = root.getJoins();
    assertEquals(1, joins.size());

    for (Join<Organization, ?> join : joins) {
      assertEquals(JoinType.LEFT, join.getJoinType());
    }
  }

  @Test
  public void checkFromListOrderByOuterJoinOnConditionOne() throws ODataJPAModelException, ODataApplicationException {
    final List<JPAAssociationPath> orderBy = new ArrayList<>();
    buildRoleAssociationPath(orderBy);

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, new ArrayList<JPAPath>(), cut.cq);

    @SuppressWarnings("unchecked")
    Root<Organization> root = (Root<Organization>) act.get(jpaEntityType.getExternalFQN()
        .getFullQualifiedNameAsString());
    Set<Join<Organization, ?>> joins = root.getJoins();
    assertEquals(1, joins.size());

    for (Join<Organization, ?> join : joins) {
      assertNull(join.getOn());
    }
  }

  @Test
  public void checkFromListDescriptionAssozationAllFields() throws ODataApplicationException, ODataJPAModelException {
    List<JPAAssociationPath> orderBy = new ArrayList<>();
    List<JPAPath> descriptionPathList = new ArrayList<>();
    JPAEntityType entity = helper.getJPAEntityType("Organizations");
    descriptionPathList.add(entity.getPath("Address/CountryName"));

    JPAAttribute attri = helper.getJPAAttribute("Organizations", "address");
    JPAAttribute exp = attri.getStructuredType().getAttribute("countryName");

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, descriptionPathList, cut.cq);
    assertEquals(2, act.size());
    assertNotNull(act.get(exp.getInternalName()));
  }

  @Test
  public void checkFromListDescriptionAssozationAllFields2() throws ODataApplicationException, ODataJPAModelException {
    List<JPAAssociationPath> orderBy = new ArrayList<>();
    List<JPAPath> descriptionPathList = new ArrayList<>();
    JPAEntityType entity = helper.getJPAEntityType("Organizations");
    descriptionPathList.add(entity.getPath("Address/RegionName"));

    JPAAttribute attri = helper.getJPAAttribute("Organizations", "address");
    JPAAttribute exp = attri.getStructuredType().getAttribute("regionName");

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, descriptionPathList, cut.cq);
    assertEquals(2, act.size());
    assertNotNull(act.get(exp.getInternalName()));
  }

  private JPAAssociationPath buildRoleAssociationPath(final List<JPAAssociationPath> orderBy)
      throws ODataJPAModelException {
    JPAAssociationPath exp = helper.getJPAAssociationPath("Organizations", "Roles");
    orderBy.add(exp);
    return exp;
  }

}
