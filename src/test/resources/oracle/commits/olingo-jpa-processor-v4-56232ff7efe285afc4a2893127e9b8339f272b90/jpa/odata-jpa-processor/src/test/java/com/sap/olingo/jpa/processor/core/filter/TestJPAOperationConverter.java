package com.sap.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.junit.Before;
import org.junit.Test;

import com.sap.olingo.jpa.processor.core.database.JPAODataDatabaseOperations;
import com.sap.olingo.jpa.processor.core.filter.JPAArithmeticOperator;
import com.sap.olingo.jpa.processor.core.filter.JPAArithmeticOperatorImp;
import com.sap.olingo.jpa.processor.core.filter.JPALiteralOperator;
import com.sap.olingo.jpa.processor.core.filter.JPAMemberOperator;
import com.sap.olingo.jpa.processor.core.filter.JPAOperationConverter;

public class TestJPAOperationConverter {
  private CriteriaBuilder cb;

  private Expression<Number> expressionLeft;
  private Expression<Number> expressionRight;
  private JPAOperationConverter cut;
  private JPAODataDatabaseOperations extension;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    cb = mock(CriteriaBuilder.class);
    extension = mock(JPAODataDatabaseOperations.class);
    cut = new JPAOperationConverter(cb, extension);
    expressionLeft = mock(Path.class);
    expressionRight = mock(Path.class);
  }

  @Test
  public void testAddMemberMember() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPAMemberOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.ADD);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsExpression()).thenReturn(expressionRight);
    when(cb.sum(expressionLeft, expressionRight)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testAddMemberLiteral() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPALiteralOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.ADD);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsNumber(cb)).thenReturn(5);
    when(cb.sum(expressionLeft, 5)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testSubMemberMember() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPAMemberOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.SUB);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsExpression()).thenReturn(expressionRight);
    when(cb.diff(expressionLeft, expressionRight)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testSubMemberLiteral() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPALiteralOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.SUB);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsNumber(cb)).thenReturn(5);
    when(cb.diff(expressionLeft, 5)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testDivMemberMember() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPAMemberOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.DIV);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsExpression()).thenReturn(expressionRight);
    when(cb.quot(expressionLeft, expressionRight)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testDivMemberLiteral() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPALiteralOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.DIV);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsNumber(cb)).thenReturn(5);
    when(cb.quot(expressionLeft, 5)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testMulMemberMember() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPAMemberOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.MUL);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsExpression()).thenReturn(expressionRight);
    when(cb.prod(expressionLeft, expressionRight)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testMulMemberLiteral() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    @SuppressWarnings("unchecked")
    Expression<Number> result = mock(Path.class);
    when(operator.getRight()).thenReturn(mock(JPALiteralOperator.class));
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.MUL);
    when(operator.getLeft(cb)).thenReturn(expressionLeft);
    when(operator.getRightAsNumber(cb)).thenReturn(5);
    when(cb.prod(expressionLeft, 5)).thenReturn(result);

    Expression<?> act = cut.convert(operator);
    assertEquals(result, act);
  }

  @Test
  public void testUnknownOperation_CallExtension() throws ODataApplicationException {
    JPAArithmeticOperator operator = mock(JPAArithmeticOperatorImp.class);
    when(operator.getOperator()).thenReturn(BinaryOperatorKind.AND);
    when(extension.convert(operator)).thenThrow(new ODataApplicationException(null, HttpStatusCode.NOT_IMPLEMENTED
        .getStatusCode(), null));

    try {
      cut.convert(operator);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
      return;
    }
    fail("Exception expecetd");
  }
}

//case MOD: