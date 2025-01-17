package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.ManagedType;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmDescriptionAssoziation;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateEntityTypeAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateReferenceList;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner;

public class TestIntermediateDescriptionProperty extends TestMappingRoot {
  private TestHelper helper;
  private IntermediateDescriptionProperty cut;
  private JPAEdmMetadataPostProcessor processor;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
    processor = mock(JPAEdmMetadataPostProcessor.class);
    IntermediateModelElement.setPostProcessor(new DefaultEdmPostProcessor());
  }

  @Test
  public void checkProptertyCanBeCreated() throws ODataJPAModelException {
    EmbeddableType<?> et = helper.getEmbeddedableType("PostalAddressData");
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "countryName");
    new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute, helper.schema);
  }

  @Test
  public void checkGetProptertyNameOneToMany() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals("CountryName", cut.getEdmItem().getName(), "Wrong name");
  }

  @Test
  public void checkGetProptertyNameManyToMany() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "regionName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals("RegionName", cut.getEdmItem().getName(), "Wrong name");
  }

  @Test
  public void checkGetProptertyType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals(EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString(),
        cut.getEdmItem().getType(), "Wrong type");
  }

  @Test
  public void checkGetProptertyIgnoreFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    IntermediatePropertyAccess property = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertFalse(property.ignore());
  }

  @Test
  public void checkGetProptertyFacetsNullableTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertTrue(cut.getEdmItem().isNullable());
  }

  @Test
  public void checkGetProptertyMaxLength() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals(new Integer(100), cut.getEdmItem().getMaxLength());
  }

  @Test
  public void checkWrongPathElementThrowsEcxeption() {

    Attribute<?, ?> jpaAttribute = mock(Attribute.class);
    EdmDescriptionAssoziation assoziation = prepareCheckPath(jpaAttribute);

    EdmDescriptionAssoziation.valueAssignment[] valueAssignments = new EdmDescriptionAssoziation.valueAssignment[1];
    EdmDescriptionAssoziation.valueAssignment valueAssignment = mock(EdmDescriptionAssoziation.valueAssignment.class);
    valueAssignments[0] = valueAssignment;
    when(valueAssignment.attribute()).thenReturn("communicationData/dummy");
    when(assoziation.valueAssignments()).thenReturn(valueAssignments);

    try {
      cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
          helper.schema);
      cut.getEdmItem();
    } catch (ODataJPAModelException e) {
      return;
    }
    fail();
  }

  @Test
  public void checkWrongPathStartThrowsEcxeption() {

    Attribute<?, ?> jpaAttribute = mock(Attribute.class);
    EdmDescriptionAssoziation assoziation = prepareCheckPath(jpaAttribute);

    EdmDescriptionAssoziation.valueAssignment[] valueAssignments = new EdmDescriptionAssoziation.valueAssignment[1];
    EdmDescriptionAssoziation.valueAssignment valueAssignment = mock(EdmDescriptionAssoziation.valueAssignment.class);
    valueAssignments[0] = valueAssignment;
    when(valueAssignment.attribute()).thenReturn("communicationDummy/dummy");
    when(assoziation.valueAssignments()).thenReturn(valueAssignments);

    try {
      cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
          helper.schema);
      cut.getEdmItem();
    } catch (ODataJPAModelException e) {
      return;
    }
    fail();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private EdmDescriptionAssoziation prepareCheckPath(Attribute<?, ?> jpaAttribute) {
    AnnotatedMember jpaField = mock(AnnotatedMember.class);
    ManagedType jpaManagedType = mock(ManagedType.class);
    EdmDescriptionAssoziation assoziation = mock(EdmDescriptionAssoziation.class);

    when(jpaAttribute.getJavaType()).thenAnswer(new Answer<Class<BusinessPartner>>() {
      @Override
      public Class<BusinessPartner> answer(InvocationOnMock invocation) throws Throwable {
        return BusinessPartner.class;
      }
    });
    when(jpaAttribute.getJavaMember()).thenReturn(jpaField);
    when(jpaAttribute.getName()).thenReturn("dummy");
    when(jpaAttribute.getDeclaringType()).thenReturn(jpaManagedType);
    when(jpaManagedType.getJavaType()).thenReturn(BusinessPartner.class);

    when(jpaField.getAnnotation(EdmDescriptionAssoziation.class)).thenReturn(assoziation);

    when(assoziation.descriptionAttribute()).thenReturn("country");
    when(assoziation.languageAttribute()).thenReturn("language");
    when(assoziation.localeAttribute()).thenReturn("");
    return assoziation;
  }

  @Test
  public void checkAnnotations() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class),
        "locationName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    List<CsdlAnnotation> annotations = cut.getEdmItem().getAnnotations();
    assertEquals(1, annotations.size());
    assertEquals("Core.IsLanguageDependent", annotations.get(0).getTerm());
    assertEquals(ConstantExpressionType.Bool, annotations.get(0).getExpression().asConstant().getType());
    assertEquals("true", annotations.get(0).getExpression().asConstant().getValue());
  }

  @Test
  public void checkPostProcessorCalled() throws ODataJPAModelException {
    // PostProcessorSpy spy = new PostProcessorSpy();
    IntermediateModelElement.setPostProcessor(processor);
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);

    cut.getEdmItem();
    verify(processor, atLeastOnce()).processProperty(cut, ADDR_CANONICAL_NAME);
  }

  @Test
  public void checkPostProcessorNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);

    assertEquals("CountryDescription", cut.getEdmItem().getName(), "Wrong name");
  }

  @Test
  public void checkPostProcessorExternalNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);

    assertEquals("CountryDescription", property.getExternalName(), "Wrong name");
  }

  private class PostProcessorSetName extends JPAEdmMetadataPostProcessor {

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(ADDR_CANONICAL_NAME)) {
        if (property.getInternalName().equals("countryName")) {
          property.setExternalName("CountryDescription");
        }
      }
    }

    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
        String jpaManagedTypeClassName) {}

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {}

    @Override
    public void provideReferences(IntermediateReferenceList references) throws ODataJPAModelException {}
  }

  private interface AnnotatedMember extends Member, AnnotatedElement {

  }
}
