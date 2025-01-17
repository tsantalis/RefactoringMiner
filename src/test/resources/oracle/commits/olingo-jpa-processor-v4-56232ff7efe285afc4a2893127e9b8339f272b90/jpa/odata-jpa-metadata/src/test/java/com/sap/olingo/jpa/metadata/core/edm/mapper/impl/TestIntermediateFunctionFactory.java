package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;

import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.ODataFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaOneFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaTwoFunctions;

public class TestIntermediateFunctionFactory extends TestMappingRoot {
  private TestHelper helper;

  private Reflections reflections;
  private IntermediateFunctionFactory cut;
  private Set<Class<? extends ODataFunction>> javaFunctions;

  @Before
  public void setUp() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);

    reflections = mock(Reflections.class);
    cut = new IntermediateFunctionFactory();
    javaFunctions = new HashSet<Class<? extends ODataFunction>>();
    when(reflections.getSubTypesOf(ODataFunction.class)).thenReturn(javaFunctions);
  }

  @Test
  public void checkReturnEmptyMapIfReflectionsNull() throws ODataJPAModelException {
    Reflections r = null;
    assertNotNull(cut.create(new JPAEdmNameBuilder(PUNIT_NAME), r, helper.schema));
  }

  @Test
  public void checkReturnEmptyMapIfNoJavaFunctionsFound() throws ODataJPAModelException {
    assertNotNull(cut.create(new JPAEdmNameBuilder(PUNIT_NAME), reflections, helper.schema));
  }

  @Test
  public void checkReturnMapWithOneIfOneJavaFunctionsFound() throws ODataJPAModelException {
    javaFunctions.add(ExampleJavaOneFunction.class);
    Map<? extends String, ? extends IntermediateFunction> act = cut.create(new JPAEdmNameBuilder(PUNIT_NAME),
        reflections, helper.schema);
    assertEquals(1, act.size());
  }

  @Test
  public void checkReturnMapWithTwoIfTwoJavaFunctionsFound() throws ODataJPAModelException {
    javaFunctions.add(ExampleJavaTwoFunctions.class);
    Map<? extends String, ? extends IntermediateFunction> act = cut.create(new JPAEdmNameBuilder(PUNIT_NAME),
        reflections, helper.schema);
    assertEquals(2, act.size());
  }

  @Test
  public void checkReturnMapWithWithJavaFunctionsFromTwoClassesFound() throws ODataJPAModelException {
    javaFunctions.add(ExampleJavaOneFunction.class);
    javaFunctions.add(ExampleJavaTwoFunctions.class);
    Map<? extends String, ? extends IntermediateFunction> act = cut.create(new JPAEdmNameBuilder(PUNIT_NAME),
        reflections, helper.schema);
    assertEquals(3, act.size());
  }

}
