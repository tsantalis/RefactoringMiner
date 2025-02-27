package com.sap.olingo.jpa.processor.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmParameter;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceCount;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Equals;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPADataBaseFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAParameter;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;

public class TestJPA_HSQLDB_DatabaseProcessor {
  private JPAODataDatabaseProcessor cut;
  private EntityManager em;
  private UriResourceEntitySet uriEntitySet;
  private JPADataBaseFunction jpaFunction;
  private UriResourceFunction uriFunction;
  private EdmEntityType edmEntityType;
  private EdmFunction edmFunction;
  private EdmParameter edmElement;
  private List<UriResource> uriResourceParts;
  private List<UriParameter> uriParameters;
  private JPAOperationResultParameter returnParameter;
  private List<JPAParameter> parameterList;
  private JPAParameter firstParameter;
  private UriParameter firstUriParameter;
  private Query functionQuery;

  @BeforeEach
  public void steup() {
    em = mock(EntityManager.class);
    functionQuery = mock(Query.class);
    uriResourceParts = new ArrayList<>();
    uriFunction = mock(UriResourceFunction.class);
    uriEntitySet = mock(UriResourceEntitySet.class);
    edmFunction = mock(EdmFunction.class);
    edmElement = mock(EdmParameter.class);
    edmEntityType = mock(EdmEntityType.class);
    uriResourceParts.add(uriFunction);
    uriParameters = new ArrayList<>();
    firstUriParameter = mock(UriParameter.class);

    jpaFunction = mock(JPADataBaseFunction.class);
    returnParameter = mock(JPAOperationResultParameter.class);
    parameterList = new ArrayList<>();
    firstParameter = mock(JPAParameter.class);

    when(em.createNativeQuery(any(), eq(BusinessPartner.class))).thenReturn(functionQuery);
    when(em.createNativeQuery(any())).thenReturn(functionQuery);
    when(uriEntitySet.getEntityType()).thenReturn(edmEntityType);
    when(uriEntitySet.getKind()).thenReturn(UriResourceKind.entitySet);
    when(uriEntitySet.getKeyPredicates()).thenReturn(uriParameters);
    when(uriFunction.getParameters()).thenReturn(uriParameters);
    when(jpaFunction.getResultParameter()).thenReturn(returnParameter);
    when(uriFunction.getFunction()).thenReturn(edmFunction);
    when(uriFunction.getKind()).thenReturn(UriResourceKind.function);
    when(edmFunction.getParameter(firstParameter.getName())).thenReturn(edmElement);

    cut = new JPA_HSQLDB_DatabaseProcessor();
  }

  @Test
  public void testUnboundFunctionWithOneParameterReturnsBuPas() throws ODataApplicationException,
      ODataJPAModelException {

    createFunctionWithOneParameter();

    final List<BusinessPartner> act = cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    verify(em, times(1)).createNativeQuery((String) argThat(new Equals("SELECT * FROM TABLE (Example(?1))")), eq(
        BusinessPartner.class));
    verify(functionQuery, times(1)).setParameter(1, "5");
    assertNotNull(act);
    assertEquals(2, act.size());
  }

  @Test
  public void testUnboundFunctionWithTwoParameterReturnsBuPas() throws ODataApplicationException,
      ODataJPAModelException {

    createFunctionWithOneParameter();
    addSecondParameter();

    final List<BusinessPartner> act = cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    verify(em, times(1)).createNativeQuery((String) argThat(new Equals("SELECT * FROM TABLE (Example(?1,?2))")), eq(
        BusinessPartner.class));
    verify(functionQuery, times(1)).setParameter(1, "5");
    verify(functionQuery, times(1)).setParameter(2, "3");
    assertNotNull(act);
    assertEquals(2, act.size());
  }

  @Test
  public void testUnboundFunctionWithOneParameterCount() throws ODataApplicationException,
      ODataJPAModelException {

    createFunctionWithOneParameter();

    final UriResourceCount uriResourceCount = mock(UriResourceCount.class);
    uriResourceParts.add(uriResourceCount);
    when(uriResourceCount.getKind()).thenReturn(UriResourceKind.count);
    when(functionQuery.getSingleResult()).thenReturn(5L);

    final List<BusinessPartner> act = cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);

    verify(em, times(1)).createNativeQuery((String) argThat(new Equals("SELECT COUNT(*) FROM TABLE (Example(?1))")));
    verify(functionQuery, times(1)).setParameter(1, "5");
    verify(functionQuery, times(0)).getResultList();
    verify(functionQuery, times(1)).getSingleResult();
    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals(5L, act.get(0));
  }

  @Test
  public void testUnboundRaisesExceptionOnMissingParameter() throws ODataJPAModelException {

    createFunctionWithOneParameter();
    when(uriFunction.getParameters()).thenReturn(new ArrayList<>());

    try {
      cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void testUnboundConvertsExceptionOnParameterProblem() throws ODataJPAModelException {

    createFunctionWithOneParameter();
    when(jpaFunction.getParameter()).thenThrow(ODataJPAModelException.class);

    try {
      cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void testAbortsOnNotImplementedChaning() throws ODataJPAModelException {

    createFunctionWithOneParameter();

    final UriResourceCount uriResourceCount = mock(UriResourceCount.class);
    uriResourceParts.add(uriResourceCount);
    when(uriResourceCount.getKind()).thenReturn(UriResourceKind.value);

    try {
      cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    } catch (ODataApplicationException e) {
      assertEquals(e.getStatusCode(), HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void testBoundFunctionWithOneParameterReturnsBuPas() throws ODataApplicationException,
      ODataJPAModelException {

    createBoundFunctionWithOneParameter();

    final List<BusinessPartner> act = cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);

    verify(em, times(1)).createNativeQuery((String) argThat(new Equals("SELECT * FROM TABLE (Example(?1))")), eq(
        BusinessPartner.class));
    verify(functionQuery, times(1)).setParameter(1, "5");
    assertNotNull(act);
    assertEquals(2, act.size());
  }

  @Test
  public void testBoundFunctionWithTwoParameterReturnsBuPas() throws ODataApplicationException,
      ODataJPAModelException {

    createBoundFunctionWithOneParameter();
    addSecondBoundParameter();

    final List<BusinessPartner> act = cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    verify(em, times(1)).createNativeQuery((String) argThat(new Equals("SELECT * FROM TABLE (Example(?1,?2))")), eq(
        BusinessPartner.class));
    verify(functionQuery, times(1)).setParameter(1, "5");
    verify(functionQuery, times(1)).setParameter(2, "3");
    assertNotNull(act);
    assertEquals(2, act.size());
  }

  @Test
  public void testBoundFunctionWithOneParameterCount() throws ODataApplicationException,
      ODataJPAModelException {

    createBoundFunctionWithOneParameter();

    final UriResourceCount uriResourceCount = mock(UriResourceCount.class);
    uriResourceParts.add(uriResourceCount);
    when(uriResourceCount.getKind()).thenReturn(UriResourceKind.count);
    when(functionQuery.getSingleResult()).thenReturn(5L);

    final List<BusinessPartner> act = cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);

    verify(em, times(1)).createNativeQuery((String) argThat(new Equals("SELECT COUNT(*) FROM TABLE (Example(?1))")));
    verify(functionQuery, times(1)).setParameter(1, "5");
    verify(functionQuery, times(0)).getResultList();
    verify(functionQuery, times(1)).getSingleResult();
    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals(5L, act.get(0));
  }

  @Test
  public void testBoundRaisesExceptionOnMissingParameter() throws ODataJPAModelException {

    createBoundFunctionWithOneParameter();
    when(uriEntitySet.getKeyPredicates()).thenReturn(new ArrayList<>());

    try {
      cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void testBoundConvertsExceptionOnParameterProblem() throws ODataJPAModelException {

    createBoundFunctionWithOneParameter();
    when(jpaFunction.getParameter()).thenThrow(ODataJPAModelException.class);

    try {
      cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void testCheckRaisesExceptionOnIsBound() throws ODataJPAModelException {

    createBoundFunctionWithOneParameter();
    when(jpaFunction.isBound()).thenThrow(ODataJPAModelException.class);

    try {
      cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void testCheckRaiseExceptionOnProblemValueToString() throws ODataJPAModelException, EdmPrimitiveTypeException {

    createBoundFunctionWithOneParameter();

    final EdmPrimitiveType edmType = mock(EdmPrimitiveType.class);
    when(edmElement.getType()).thenReturn(edmType);
    when(edmType.valueOfString(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(EdmPrimitiveTypeException.class);
    try {
      cut.executeFunctionQuery(uriResourceParts, jpaFunction, em);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Missing exception");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAbortsOnSearchRequest() {
    final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    final CriteriaQuery<String> cq = mock(CriteriaQuery.class);
    final Root<String> root = mock(Root.class);
    final JPAEntityType entityType = mock(JPAEntityType.class);
    final SearchOption searchOption = mock(SearchOption.class);
    try {
      cut.createSearchWhereClause(cb, cq, root, entityType, searchOption);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Message not thrown");
  }

  private void addSecondParameter() {
    final JPAParameter secondParameter = mock(JPAParameter.class);
    final UriParameter secondUriParameter = mock(UriParameter.class);
    final EdmParameter edmSecondElement = mock(EdmParameter.class);

    parameterList.add(secondParameter);
    uriParameters.add(secondUriParameter);
    when(secondUriParameter.getText()).thenReturn("3");
    when(secondParameter.getName()).thenReturn("B");
    when(secondUriParameter.getName()).thenReturn("B");
    when(edmFunction.getParameter(eq("B"))).thenReturn(edmSecondElement);
    when(edmSecondElement.getType()).thenReturn(EdmString.getInstance());
    when(secondParameter.getMaxLength()).thenReturn(10);
    when(secondParameter.getType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return String.class;
      }
    });
  }

  private void createFunctionWithOneParameter() throws ODataJPAModelException {
    when(returnParameter.getType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return BusinessPartner.class;
      }
    });
    when(jpaFunction.getDBName()).thenReturn("Example");
    when(jpaFunction.getParameter()).thenReturn(parameterList);

    parameterList.add(firstParameter);
    when(firstParameter.getName()).thenReturn("A");

    uriParameters.add(firstUriParameter);
    when(firstUriParameter.getName()).thenReturn("A");
    when(edmFunction.getParameter(eq("A"))).thenReturn(edmElement);
    when(firstUriParameter.getText()).thenReturn("5");
    when(edmElement.getType()).thenReturn(EdmString.getInstance());
    when(firstParameter.getMaxLength()).thenReturn(10);
    when(firstParameter.getType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return String.class;
      }
    });

    when(functionQuery.getResultList()).thenReturn(Arrays.asList(new BusinessPartner[] { new Organization(),
        new Organization() }));
  }

  private void createBoundFunctionWithOneParameter() throws ODataJPAModelException {

    uriResourceParts.add(0, uriEntitySet);
    when(uriFunction.getParameters()).thenReturn(new ArrayList<>());
    when(jpaFunction.isBound()).thenReturn(Boolean.TRUE);
    when(returnParameter.getType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return BusinessPartner.class;
      }
    });

    when(jpaFunction.getDBName()).thenReturn("Example");
    when(jpaFunction.getParameter()).thenReturn(parameterList);

    parameterList.add(firstParameter);
    when(firstParameter.getName()).thenReturn("A");

    uriParameters.add(firstUriParameter);
    when(firstUriParameter.getName()).thenReturn("A");
    when(edmEntityType.getProperty(eq("A"))).thenReturn(edmElement);
    when(firstUriParameter.getText()).thenReturn("5");
    when(edmElement.getType()).thenReturn(EdmString.getInstance());
    when(firstParameter.getMaxLength()).thenReturn(10);
    when(firstParameter.getType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return String.class;
      }
    });

    when(functionQuery.getResultList()).thenReturn(Arrays.asList(new BusinessPartner[] { new Organization(),
        new Organization() }));
  }

  private void addSecondBoundParameter() {
    final JPAParameter secondParameter = mock(JPAParameter.class);
    final UriParameter secondUriParameter = mock(UriParameter.class);
    final EdmParameter edmSecondElement = mock(EdmParameter.class);

    parameterList.add(secondParameter);
    uriParameters.add(secondUriParameter);
    when(secondUriParameter.getText()).thenReturn("3");
    when(secondParameter.getName()).thenReturn("B");
    when(secondUriParameter.getName()).thenReturn("B");
    when(edmEntityType.getProperty(eq("B"))).thenReturn(edmSecondElement);
    when(edmSecondElement.getType()).thenReturn(EdmString.getInstance());
    when(secondParameter.getMaxLength()).thenReturn(10);
    when(secondParameter.getType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return String.class;
      }
    });
  }
}
