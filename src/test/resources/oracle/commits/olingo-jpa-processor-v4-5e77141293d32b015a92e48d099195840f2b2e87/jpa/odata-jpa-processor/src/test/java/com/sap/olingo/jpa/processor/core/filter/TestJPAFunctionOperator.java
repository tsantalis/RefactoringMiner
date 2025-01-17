package com.sap.olingo.jpa.processor.core.filter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPADataBaseFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;

public class TestJPAFunctionOperator {
  private CriteriaBuilder cb;
  private JPAFunctionOperator cut;
  private UriResourceFunction uriFunction;
  private JPAVisitor jpaVisitor;
  private JPADataBaseFunction jpaFunction;
  private JPAOperationResultParameter jpaResultParam;
  private List<UriParameter> uriParams;

  @BeforeEach
  public void setUp() throws Exception {

    cb = mock(CriteriaBuilder.class);
    jpaVisitor = mock(JPAVisitor.class);
    when(jpaVisitor.getCriteriaBuilder()).thenReturn(cb);
    uriFunction = mock(UriResourceFunction.class);
    jpaFunction = mock(JPADataBaseFunction.class);
    jpaResultParam = mock(JPAOperationResultParameter.class);
    when(jpaFunction.getResultParameter()).thenReturn(jpaResultParam);
    List<UriResource> resources = new ArrayList<>();
    resources.add(uriFunction);

    uriParams = new ArrayList<>();

    cut = new JPAFunctionOperator(jpaVisitor, uriParams, jpaFunction);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testReturnsExpression() throws ODataApplicationException {

    final Expression<?>[] jpaParameter = new Expression<?>[0];

    when(jpaFunction.getDBName()).thenReturn("Test");
    doReturn(new Integer(5).getClass()).when(jpaResultParam).getType();
    when(cb.function(jpaFunction.getDBName(), jpaResultParam.getType(), jpaParameter)).thenReturn(mock(
        Expression.class));
    when(jpaFunction.getResultParameter()).thenReturn(jpaResultParam);
    Expression<?> act = cut.get();
    assertNotNull(act);
  }

  @Test
  public void testAbortOnNonFunctionReturnsCollection() {

    when(jpaFunction.getDBName()).thenReturn("org.apache.olingo.jpa::Siblings");
    when(jpaResultParam.isCollection()).thenReturn(true);

    try {
      cut.get();
    } catch (ODataApplicationException e) {
      return;
    }
    fail("Function provided not checked");
  }

  @Test
  public void testAbortOnNonScalarFunction() {

    when(jpaFunction.getDBName()).thenReturn("org.apache.olingo.jpa::Siblings");
    when(jpaResultParam.isCollection()).thenReturn(false);
    doReturn(AdministrativeDivision.class).when(jpaResultParam).getType();

    try {
      cut.get();
    } catch (ODataApplicationException e) {
      return;
    }
    fail("Function provided not checked");
  }
}
