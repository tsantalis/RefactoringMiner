package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.olingo.commons.api.edm.geo.Geospatial.Dimension;
import org.junit.Before;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.ODataFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaEmConstructor;
import com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaFunctions;
import com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaOneFunction;
import com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaPrivateConstructor;
import com.sap.olingo.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaTwoParameterConstructor;

public class TestIntermediateJavaFunction extends TestMappingRoot {
  private TestHelper helper;

  @Before
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
  }

  @Test
  public void checkInternalNameEqualMethodName() throws ODataJPAModelException {
    IntermediateFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertEquals("sum", act.getInternalName());
  }

  @Test
  public void checkExternalNameEqualMethodName() throws ODataJPAModelException {
    IntermediateFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertEquals("Sum", act.getExternalName());
  }

  @Test
  public void checkReturnsConvertedPrimitiveReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertNotNull(act.getEdmItem());
    assertNotNull(act.getEdmItem().getReturnType());
    assertEquals("Edm.Int32", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsConvertedPrimitiveParameterTypes() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertNotNull(act.getEdmItem());
    assertNotNull(act.getEdmItem().getParameters());
    assertEquals(2, act.getEdmItem().getParameters().size());
    assertNotNull(act.getEdmItem().getParameter("A"));
    assertNotNull(act.getEdmItem().getParameter("B"));
    assertEquals("Edm.Int16", act.getEdmItem().getParameter("A").getType());
    assertEquals("Edm.Int32", act.getEdmItem().getParameter("B").getType());
  }

  @Test(expected = ODataJPAModelException.class)
  public void checkThrowsExcpetionForNonPrimitiveParameter() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "errorNonPrimitiveParameter");
    act.getEdmItem();
  }

  @Test
  public void checkReturnsFalseForIsBound() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertNotNull(act.getEdmItem());
    assertEquals(false, act.getEdmItem().isBound());
  }

  @Test
  public void checkReturnsTrueForHasFunctionImport() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertTrue(act.hasImport());
  }

  @Test
  public void checkReturnsAnnotatedName() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");

    assertEquals("Add", act.getExternalName());
  }

  @Test
  public void checkIgnoresGivenIsBound() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");

    assertFalse(act.getEdmItem().isBound());
    assertFalse(act.isBound());
  }

  @Test
  public void checkIgnoresGivenHasFunctionImport() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");

    assertTrue(act.hasImport());
  }

  @Test
  public void checkReturnsEnumerationTypeAsParameter() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEnumerationType");

    assertEquals("com.sap.olingo.jpa.AccessRights", act.getEdmItem().getParameters().get(0).getTypeFQN()
        .getFullQualifiedNameAsString());

    assertEquals("com.sap.olingo.jpa.AccessRights", act.getParameter("arg0").getTypeFQN()
        .getFullQualifiedNameAsString());
  }

  @Test
  public void checkIgnoresParameterAsPartFromEdmFunction() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "div");

    assertNotNull(act.getEdmItem());
    assertEquals(2, act.getEdmItem().getParameters().size());
    assertNotNull(act.getEdmItem().getParameter("A"));
    assertNotNull(act.getEdmItem().getParameter("B"));
  }

  @Test(expected = ODataJPAModelException.class)
  public void checkThrowsExceptionIfAnnotatedReturnTypeNEDeclairedType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "errorReturnType");
    act.getEdmItem();
  }

  @Test
  public void checkReturnsFacetForNumbersOfReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "now");
    assertFalse(act.getEdmItem().getReturnType().isNullable());
    assertEquals(Integer.valueOf(9), act.getEdmItem().getReturnType().getPrecision());
    assertEquals(Integer.valueOf(3), act.getEdmItem().getReturnType().getScale());
  }

  @Test
  public void checkReturnsFacetForStringsAndGeoOfReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "determineLocation");
    assertEquals(Integer.valueOf(60), act.getEdmItem().getReturnType().getMaxLength());
    assertEquals(Dimension.GEOGRAPHY, act.getEdmItem().getReturnType().getSrid().getDimension());
    assertEquals("4326", act.getEdmItem().getReturnType().getSrid().toString());
  }

  @Test
  public void checkReturnsIsCollectionIfDefinedReturnTypeIsSubclassOfCollection() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnCollection");

    assertTrue(act.getEdmItem().getReturnType().isCollection());
    assertEquals("Edm.String", act.getEdmItem().getReturnType().getType());
  }

  @Test(expected = ODataJPAModelException.class)
  public void checkThrowsExceptionIfCollectionAndReturnTypeEmpty() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnCollectionWithoutReturnType");
    act.getEdmItem();
  }

  @Test
  public void checkReturnsEmbeddableTypeAsReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEmbeddable");

    assertEquals("com.sap.olingo.jpa.ChangeInformation", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsEmbeddableCollectionTypeAsReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEmbeddableCollection");

    assertEquals("com.sap.olingo.jpa.ChangeInformation", act.getEdmItem().getReturnType().getType());
    assertTrue(act.getEdmItem().getReturnType().isCollection());
  }

  @Test
  public void checkReturnsEntityTypeAsReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEntity");
    assertEquals("com.sap.olingo.jpa.Person", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsEnumerationTypeAsReturnType() throws ODataJPAModelException {

    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEnumerationType");
    assertEquals("com.sap.olingo.jpa.ABCClassifiaction", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsEnumerationCollectionTypeAsReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEnumerationCollection");

    assertEquals("com.sap.olingo.jpa.ABCClassifiaction", act.getEdmItem().getReturnType().getType());
    assertTrue(act.getEdmItem().getReturnType().isCollection());
  }

  @Test(expected = ODataJPAModelException.class)
  public void checkThrowsExcpetionOnNotSupportedReturnType() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "wrongReturnType");
    act.getEdmItem();
  }

  @Test
  public void checkExceptConstructorWithoutParameter() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");
    act.getEdmItem();
  }

  @Test
  public void checkExceptConstructorWithEntityManagerParameter() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaEmConstructor.class, "sum");
    act.getEdmItem();
  }

  @Test(expected = ODataJPAModelException.class)
  public void checkThrowsExcpetionOnPrivateConstructor() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaPrivateConstructor.class, "sum");
    act.getEdmItem();
  }

  @Test(expected = ODataJPAModelException.class)
  public void checkThrowsExcpetionOnNoConstructorAsSpecified() throws ODataJPAModelException {
    IntermediateJavaFunction act = createFunction(ExampleJavaTwoParameterConstructor.class, "sum");
    act.getEdmItem();
  }

  private IntermediateJavaFunction createFunction(Class<? extends ODataFunction> clazz, String method)
      throws ODataJPAModelException {
    for (Method m : Arrays.asList(clazz.getMethods())) {
      EdmFunction functionDescribtion = m.getAnnotation(EdmFunction.class);
      if (functionDescribtion != null && method.equals(m.getName())) {
        return new IntermediateJavaFunction(new JPAEdmNameBuilder(PUNIT_NAME), functionDescribtion, m, helper.schema);
      }
    }
    return null;
  }
}
