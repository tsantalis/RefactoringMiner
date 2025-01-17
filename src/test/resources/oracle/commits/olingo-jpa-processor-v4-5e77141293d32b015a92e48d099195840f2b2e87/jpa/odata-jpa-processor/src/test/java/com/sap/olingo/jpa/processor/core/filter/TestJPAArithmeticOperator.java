package com.sap.olingo.jpa.processor.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;

public class TestJPAArithmeticOperator {
  private CriteriaBuilder cb;

  private JPAOperationConverter converter;
  private Path<Integer> expression;

  @SuppressWarnings("unchecked")
  @BeforeEach
  public void setUp() throws Exception {
    converter = mock(JPAOperationConverter.class);
    cb = mock(CriteriaBuilder.class);
    expression = mock(Path.class);
  }

  @Test
  public void testMemberLiteralGetLeft_Member() throws ODataApplicationException {
    JPAMemberOperator left = mock(JPAMemberOperator.class);
    JPALiteralOperator right = mock(JPALiteralOperator.class);

    when(right.get()).thenReturn(5);
    when(left.get()).thenAnswer(new Answer<Path<Integer>>() {
      @Override
      public Path<Integer> answer(InvocationOnMock invocation) throws Throwable {
        return expression;
      }
    });

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    assertEquals(expression, cut.getLeft(cb));
  }

  @Test
  public void testLiteralMemberGetLeft_Member() throws ODataApplicationException {
    JPAMemberOperator right = mock(JPAMemberOperator.class);
    JPALiteralOperator left = mock(JPALiteralOperator.class);

    when(left.get()).thenReturn(5);
    when(right.get()).thenAnswer(new Answer<Path<Integer>>() {
      @Override
      public Path<Integer> answer(InvocationOnMock invocation) throws Throwable {
        return expression;
      }
    });

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    assertEquals(expression, cut.getLeft(cb));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetLeftLiteralLiteral_Left() throws ODataApplicationException {
    JPALiteralOperator right = mock(JPALiteralOperator.class);
    JPALiteralOperator left = mock(JPALiteralOperator.class);
    Integer leftValue = new Integer(5);

    final Expression<Number> result = mock(Expression.class);

    when(left.get()).thenReturn(leftValue);
    when(right.get()).thenReturn(10);

    when(cb.literal(leftValue)).thenAnswer(new Answer<Expression<Number>>() {
      @Override
      public Expression<Number> answer(InvocationOnMock invocation) throws Throwable {
        invocation.getArguments();
        return result;
      }
    });

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    Expression<Number> act = cut.getLeft(cb);
    assertEquals(result, act);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetLeftMemberMember_Left() throws ODataApplicationException {
    JPAMemberOperator right = mock(JPAMemberOperator.class);
    JPAMemberOperator left = mock(JPAMemberOperator.class);

    final Path<Integer> expressionRight = mock(Path.class);

    when(right.get()).thenAnswer(new Answer<Path<Integer>>() {
      @Override
      public Path<Integer> answer(InvocationOnMock invocation) throws Throwable {
        return expressionRight;
      }
    });
    when(left.get()).thenAnswer(new Answer<Path<Integer>>() {
      @Override
      public Path<Integer> answer(InvocationOnMock invocation) throws Throwable {
        return expression;
      }
    });

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    assertEquals(expression, cut.getLeft(cb));

  }

  @Test
  public void testMemberLiteralGetRightAsNumber_Right() throws ODataApplicationException {
    JPAMemberOperator left = mock(JPAMemberOperator.class);
    JPALiteralOperator right = mock(JPALiteralOperator.class);
    JPAAttribute attribute = mock(JPAAttribute.class);

    when(right.get(attribute)).thenReturn(new BigDecimal("5.1"));
    when(left.determineAttribute()).thenReturn(attribute);

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    assertEquals(new BigDecimal("5.1"), cut.getRightAsNumber(cb));
  }

  @Test
  public void testLiteralMemberGetRightAsNumber_Left() throws ODataApplicationException {
    JPAMemberOperator right = mock(JPAMemberOperator.class);
    JPALiteralOperator left = mock(JPALiteralOperator.class);
    JPAAttribute attribute = mock(JPAAttribute.class);

    when(left.get(attribute)).thenReturn(new BigDecimal("5.1"));
    when(right.determineAttribute()).thenReturn(attribute);

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    assertEquals(new BigDecimal("5.1"), cut.getRightAsNumber(cb));
  }

  @Test
  public void testLiteralLiteralGetRightAsNumber_Right() throws ODataApplicationException {
    JPALiteralOperator right = mock(JPALiteralOperator.class);
    JPALiteralOperator left = mock(JPALiteralOperator.class);

    when(left.get()).thenReturn(new BigDecimal("5.1"));
    when(right.get()).thenReturn(new BigDecimal("10.1"));

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    assertEquals(new BigDecimal("10.1"), cut.getRightAsNumber(cb));
  }

  @Test
  public void testGetMemberMemberGetRightAsNumber_Exeption() throws ODataApplicationException {
    JPAMemberOperator right = mock(JPAMemberOperator.class);
    JPAMemberOperator left = mock(JPAMemberOperator.class);
    JPAAttribute attribute = mock(JPAAttribute.class);

    when(left.determineAttribute()).thenReturn(attribute);
    when(right.determineAttribute()).thenReturn(attribute);

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    try {
      cut.getRightAsNumber(cb);
    } catch (ODataApplicationException e) {
      return;
    }
    fail("Exception expecetd");
  }

  @Test
  public void testGetBooleanMemberGetRightAsNumber_Exeption() throws ODataApplicationException {
    JPAMemberOperator right = mock(JPAMemberOperator.class);
    JPABooleanOperatorImp left = mock(JPABooleanOperatorImp.class);

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    try {
      cut.getRightAsNumber(cb);
    } catch (ODataApplicationException e) {
      return;
    }
    fail("Exception expecetd");
  }

  @Test
  public void testGetMemberBooleanGetRightAsNumber_Exeption() throws ODataApplicationException {
    JPAMemberOperator left = mock(JPAMemberOperator.class);
    JPABooleanOperatorImp right = mock(JPABooleanOperatorImp.class);

    JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
    try {
      cut.getRightAsNumber(cb);
    } catch (ODataApplicationException e) {
      return;
    }
    fail("Exception expecetd");
  }
}
