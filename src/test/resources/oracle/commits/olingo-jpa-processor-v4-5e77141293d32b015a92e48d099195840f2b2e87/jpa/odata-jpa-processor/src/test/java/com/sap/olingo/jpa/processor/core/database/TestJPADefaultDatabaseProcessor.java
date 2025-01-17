package com.sap.olingo.jpa.processor.core.database;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.olingo.jpa.processor.core.exception.ODataJPADBAdaptorException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import com.sap.olingo.jpa.processor.core.filter.JPAAggregationOperation;
import com.sap.olingo.jpa.processor.core.filter.JPAArithmeticOperator;
import com.sap.olingo.jpa.processor.core.filter.JPABooleanOperator;
import com.sap.olingo.jpa.processor.core.filter.JPAComparisonOperator;
import com.sap.olingo.jpa.processor.core.filter.JPAEnumerationBasedOperator;
import com.sap.olingo.jpa.processor.core.filter.JPAMethodCall;
import com.sap.olingo.jpa.processor.core.filter.JPAUnaryBooleanOperator;

public class TestJPADefaultDatabaseProcessor {
  private JPADefaultDatabaseProcessor cut;

  @BeforeEach
  public void setup() {
    cut = new JPADefaultDatabaseProcessor();
  }

  @Test
  public void testNotSupportedConvertBooleanOperator() throws ODataApplicationException {
    final JPABooleanOperator operator = mock(JPABooleanOperator.class);
    when(operator.getName()).thenReturn("Hugo");
    assertThrows(ODataJPAFilterException.class, () -> {
      cut.convert(operator);
    });
  }

  @Test
  public void testNotSupportedConvertAggregationOperator() throws ODataApplicationException {
    final JPAAggregationOperation operator = mock(JPAAggregationOperation.class);
    when(operator.getName()).thenReturn("Hugo");
    assertThrows(ODataJPAFilterException.class, () -> {
      cut.convert(operator);
    });
  }

  @Test
  public void testNotSupportedConvertArithmeticOperator() throws ODataApplicationException {
    final JPAArithmeticOperator operator = mock(JPAArithmeticOperator.class);
    when(operator.getName()).thenReturn("Hugo");
    assertThrows(ODataJPAFilterException.class, () -> {
      cut.convert(operator);
    });
  }

  @Test
  public void testNotSupportedConvertMethodCall() throws ODataApplicationException {
    final JPAMethodCall operator = mock(JPAMethodCall.class);
    when(operator.getName()).thenReturn("Hugo");
    assertThrows(ODataJPAFilterException.class, () -> {
      cut.convert(operator);
    });
  }

  @Test
  public void testNotSupportedConvertUnaryBooleanOperator() throws ODataApplicationException {
    final JPAUnaryBooleanOperator operator = mock(JPAUnaryBooleanOperator.class);
    when(operator.getName()).thenReturn("Hugo");
    assertThrows(ODataJPAFilterException.class, () -> {
      cut.convert(operator);
    });
  }

  @Test
  public void testNotSupportedConvertComparisonOperatorOthersThenHAS() throws ODataApplicationException {
    @SuppressWarnings("unchecked")
    final JPAComparisonOperator<String> operator = mock(JPAComparisonOperator.class);
    when(operator.getName()).thenReturn("Hugo");
    when(operator.getOperator()).then(new Answer<BinaryOperatorKind>() {
      @Override
      public BinaryOperatorKind answer(InvocationOnMock invocation) throws Throwable {
        return BinaryOperatorKind.SUB;
      }
    });
    assertThrows(ODataJPAFilterException.class, () -> {
      cut.convert(operator);
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSupportedConvertComparisonOperatorOperatorHAS() throws ODataApplicationException {
    final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    Expression<Integer> cbResult = mock(Expression.class);
    Predicate cbPredicate = mock(Predicate.class);
    final JPAComparisonOperator<Long> operator = mock(JPAComparisonOperator.class);
    final Expression<Long> left = mock(Expression.class);
    final JPAEnumerationBasedOperator right = mock(JPAEnumerationBasedOperator.class);

    when(operator.getName()).thenReturn("Hugo");
    when(operator.getOperator()).then(new Answer<BinaryOperatorKind>() {
      @Override
      public BinaryOperatorKind answer(InvocationOnMock invocation) throws Throwable {
        return BinaryOperatorKind.HAS;
      }
    });
    when(operator.getRight()).thenReturn(right);
    when(right.getValue()).thenReturn(5L);
    when(operator.getLeft()).thenReturn(left);

    when(cb.quot(left, 5L)).thenAnswer(new Answer<Expression<Integer>>() {
      @Override
      public Expression<Integer> answer(InvocationOnMock invocation) throws Throwable {
        return cbResult;
      }
    });
    when(cb.mod(cbResult, 2)).thenReturn(cbResult);
    when(cb.equal(cbResult, 1)).thenReturn(cbPredicate);
    cut.setCriterialBuilder(cb);
    final Expression<Boolean> act = cut.convert(operator);
    assertNotNull(act);
  }

  @Test
  public void testNotSupportedSearch() throws ODataApplicationException {
    assertThrows(ODataJPADBAdaptorException.class, () -> {
      cut.createSearchWhereClause(null, null, null, null, null);
    });
  }
}
