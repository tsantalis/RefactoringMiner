package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.ManagedType;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmProtectedBy;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmProtections;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateEntityTypeAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateReferenceList;
import com.sap.olingo.jpa.metadata.core.edm.mapper.util.MemberDouble;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartnerProtected;
import com.sap.olingo.jpa.processor.core.testmodel.Comment;
import com.sap.olingo.jpa.processor.core.testmodel.DummyToBeIgnored;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;
import com.sap.olingo.jpa.processor.core.testmodel.Person;
import com.sap.olingo.jpa.processor.core.testmodel.PersonImage;

public class TestIntermediateSimpleProperty extends TestMappingRoot {
  private TestHelper helper;
  private JPAEdmMetadataPostProcessor processor;

  @Before
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
    processor = mock(JPAEdmMetadataPostProcessor.class);
  }

  @Test
  public void checkProptertyCanBeCreated() throws ODataJPAModelException {
    EmbeddableType<?> et = helper.getEmbeddedableType("CommunicationData");
    Attribute<?, ?> jpaAttribute = helper.getAttribute(et, "landlinePhoneNumber");
    new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute, helper.schema);
  }

  @Test
  public void checkGetProptertyName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "type");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals("Wrong name", "Type", property.getEdmItem().getName());
  }

  @Test
  public void checkGetProptertyDBFieldName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "type");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals("Wrong name", "\"Type\"", property.getDBFieldName());
  }

  @Test
  public void checkGetProptertySimpleType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "type");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals("Wrong type", EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString(),
        property.getEdmItem().getType());
  }

  @Test
  public void checkGetProptertyComplexType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class),
        "communicationData");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals("Wrong type", PUNIT_NAME + ".CommunicationData", property.getEdmItem().getType());
  }

  @Test
  public void checkGetProptertyEnumTypeWithoutConverter() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(Organization.class), "aBCClass");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals("Wrong type", "com.sap.olingo.jpa.ABCClassifiaction", property.getEdmItem().getType());
  }

  @Test
  public void checkGetProptertyEnumTypeWithoutConverterMustNotHaveMapper() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(Organization.class), "aBCClass");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertNull(property.getEdmItem().getMapping());
  }

  @Test
  public void checkGetProptertyEnumTypeWithConverter() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(Person.class), "accessRights");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals("Wrong type", "com.sap.olingo.jpa.AccessRights", property.getEdmItem().getType());
  }

  @Test
  public void checkGetProptertyIgnoreFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "type");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertFalse(property.ignore());
  }

  @Test
  public void checkGetProptertyIgnoreTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "customString1");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertTrue(property.ignore());
  }

  @Test
  public void checkGetProptertyFacetsNullableTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "customString1");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertTrue(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetProptertyFacetsNullableTrueComplex() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEmbeddedableType("PostalAddressData"), "POBox");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertTrue(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetProptertyFacetsNullableFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "eTag");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertFalse(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetProptertyIsETagTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "eTag");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertTrue(property.isEtag());
  }

  @Test
  public void checkGetProptertyIsETagFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "type");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertFalse(property.isEtag());
  }

  @Test
  public void checkGetProptertyMaxLength() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "type");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals(new Integer(1), property.getEdmItem().getMaxLength());
  }

  @Test
  public void checkGetProptertyMaxLengthNullForClob() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getComplexType("DummyEmbeddedToIgnore"), "command");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertNull(property.getEdmItem().getMaxLength());
  }

  @Test
  public void checkGetProptertyPrecisionDecimal() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "customNum1");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals(new Integer(16), property.getEdmItem().getPrecision());
  }

  @Test
  public void checkGetProptertyScaleDecimal() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "customNum1");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals(new Integer(5), property.getEdmItem().getScale());
  }

  @Test
  public void checkGetProptertyPrecisionTime() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "creationDateTime");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals(new Integer(3), property.getEdmItem().getPrecision());
  }

  @Test
  public void checkGetProptertyMapper() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "creationDateTime");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertNotNull(property.getEdmItem().getMapping());
    assertEquals(Timestamp.class, property.getEdmItem().getMapping().getMappedJavaClass());
  }

  @Test
  public void checkGetProptertyMapperWithConverter() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(Person.class), "birthDay");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertNotNull(property.getEdmItem().getMapping());
    assertEquals(Date.class, property.getEdmItem().getMapping().getMappedJavaClass());
  }

  @Test
  public void checkGetNoProptertyMapperForClob() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(Comment.class), "text");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertNull(property.getEdmItem().getMapping());
  }

  @Test
  public void checkPostProcessorCalled() throws ODataJPAModelException {
    IntermediateSimpleProperty.setPostProcessor(processor);
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "creationDateTime");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);

    property.getEdmItem();
    verify(processor, atLeastOnce()).processProperty(property, BUPA_CANONICAL_NAME);
  }

  @Test
  public void checkPostProcessorNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateSimpleProperty.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "customString1");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);

    assertEquals("Wrong name", "ContactPersonName", property.getEdmItem().getName());
  }

  @Test
  public void checkPostProcessorExternalNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartner.class), "customString1");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);

    assertEquals("Wrong name", "ContactPersonName", property.getExternalName());
  }

  @Test
  public void checkConverterGetConverterReturned() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(Person.class), "birthDay");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);

    assertNotNull(property.getConverter());
  }

  @Test
  public void checkConverterGetConverterNotReturned() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(Person.class), "customString1");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);

    assertNull(property.getConverter());
  }

  @Test
  public void checkConverterGetConverterNotReturnedDiffernt() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(DummyToBeIgnored.class), "uuid");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);

    assertNull(property.getConverter());
  }

  @Test
  public void checkGetProptertyDefaultValue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "regionCodePublisher");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals("ISO", property.getEdmItem().getDefaultValue());
  }

  @Test
  public void checkGetPropertyIsStream() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(PersonImage.class),
        "image");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertTrue(property.isStream());
  }

  @Test
  public void checkGetTypeBoxedForPrimitive() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(AdministrativeDivision.class),
        "population");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals(Long.class, property.getType());
  }

  @Test
  public void checkGetTypeBoxed() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(AdministrativeDivision.class),
        "area");
    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertEquals(Integer.class, property.getType());
  }

  @Test(expected = ODataJPAModelException.class)
  public void checkThrowsAnExceptionTimestampWithoutPrecision() throws ODataJPAModelException {
    // If Precision missing EdmDateTimeOffset.internalValueToString throws an exception => pre-check
    final Attribute<?, ?> jpaAttribute = mock(Attribute.class);
    final ManagedType<?> jpaManagedType = mock(ManagedType.class);
    when(jpaAttribute.getName()).thenReturn("start");
    when(jpaAttribute.getPersistentAttributeType()).thenReturn(PersistentAttributeType.BASIC);
    when(jpaAttribute.getDeclaringType()).thenAnswer(new Answer<ManagedType<?>>() {
      @Override
      public ManagedType<?> answer(InvocationOnMock invocation) throws Throwable {
        return jpaManagedType;
      }
    });
    when(jpaAttribute.getJavaType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return Timestamp.class;
      }
    });
    when(jpaManagedType.getJavaType()).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocation) throws Throwable {
        return DummyToBeIgnored.class;
      }
    });

    Column column = mock(Column.class);
    AnnotatedElement annotations = mock(AnnotatedElement.class, withSettings().extraInterfaces(Member.class));
    when(annotations.getAnnotation(Column.class)).thenReturn(column);
    when(jpaAttribute.getJavaMember()).thenReturn((Member) annotations);
    when(column.name()).thenReturn("Test");

    IntermediateSimpleProperty property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    property.getEdmItem();
  }

  @Test
  public void checkGetProptertyHasProtectionFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartnerProtected.class), "eTag");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute, helper.schema);
    assertFalse(property.hasProtection());
  }

  @Test
  public void checkGetProptertyHasProtectionTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartnerProtected.class),
        "username");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute, helper.schema);
    assertTrue(property.hasProtection());
  }

  @Test
  public void checkGetProptertyProtectedAttributeClaimName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartnerProtected.class),
        "username");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute, helper.schema);
    assertEquals("UserId", property.getProtectionClaimNames().toArray(new String[] {})[0]);
    assertNotNull(property.getProtectionPath("UserId"));
    List<String> actPath = property.getProtectionPath("UserId");
    assertEquals(1, actPath.size());
    assertEquals("Username", actPath.get(0));
  }

  @Test
  public void checkGetProptertyNotProtectedAttributeClaimName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartnerProtected.class), "eTag");
    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute, helper.schema);
    assertTrue(property.getProtectionClaimNames().isEmpty());
    assertTrue(property.getProtectionPath("Username").isEmpty());
  }

  @Test
  public void checkGetComplexProptertyProtectedAttributeClaimName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartnerProtected.class),
        "administrativeInformation");

    EdmProtectedBy annotation = Mockito.mock(EdmProtectedBy.class);
    when(annotation.name()).thenReturn("UserId");
    when(annotation.path()).thenReturn("created/by");

    MemberDouble memberSpy = new MemberDouble(jpaAttribute.getJavaMember());
    memberSpy.addAnnotation(EdmProtectedBy.class, annotation);
    Attribute<?, ?> attributeSpy = Mockito.spy(jpaAttribute);
    when(attributeSpy.getJavaMember()).thenReturn(memberSpy);

    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        attributeSpy, helper.schema);
    assertEquals("UserId", property.getProtectionClaimNames().toArray(new String[] {})[0]);
    assertNotNull(property.getProtectionPath("UserId"));
    List<String> actPath = property.getProtectionPath("UserId");
    assertEquals(1, actPath.size());
    assertEquals("AdministrativeInformation/Created/By", actPath.get(0));
  }

  @Test
  public void checkGetComplexProptertyTwoProtectedAttributeClaimName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartnerProtected.class),
        "administrativeInformation");

    EdmProtections protections = Mockito.mock(EdmProtections.class);
    EdmProtectedBy protectedBy1 = Mockito.mock(EdmProtectedBy.class);
    when(protectedBy1.name()).thenReturn("UserId");
    when(protectedBy1.path()).thenReturn("created/by");

    EdmProtectedBy protectedBy2 = Mockito.mock(EdmProtectedBy.class);
    when(protectedBy2.name()).thenReturn("UserId");
    when(protectedBy2.path()).thenReturn("updated/by");

    when(protections.value()).thenReturn(new EdmProtectedBy[] { protectedBy1, protectedBy2 });

    MemberDouble memberSpy = new MemberDouble(jpaAttribute.getJavaMember());
    memberSpy.addAnnotation(EdmProtections.class, protections);
    Attribute<?, ?> attributeSpy = Mockito.spy(jpaAttribute);
    when(attributeSpy.getJavaMember()).thenReturn(memberSpy);

    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        attributeSpy, helper.schema);
    assertEquals("UserId", property.getProtectionClaimNames().toArray(new String[] {})[0]);
    assertNotNull(property.getProtectionPath("UserId"));

    List<String> actPath = property.getProtectionPath("UserId");
    assertEquals(2, actPath.size());
    assertEquals("AdministrativeInformation/Created/By", actPath.get(0));
    assertEquals("AdministrativeInformation/Updated/By", actPath.get(1));
  }

  @Test
  public void checkGetComplexProptertyTwoProtectedAttributeTwoClaimName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType(BusinessPartnerProtected.class),
        "administrativeInformation");

    EdmProtections protections = Mockito.mock(EdmProtections.class);
    EdmProtectedBy protectedBy1 = Mockito.mock(EdmProtectedBy.class);
    when(protectedBy1.name()).thenReturn("UserId");
    when(protectedBy1.path()).thenReturn("created/by");

    EdmProtectedBy protectedBy2 = Mockito.mock(EdmProtectedBy.class);
    when(protectedBy2.name()).thenReturn("Date");
    when(protectedBy2.path()).thenReturn("created/at");

    when(protections.value()).thenReturn(new EdmProtectedBy[] { protectedBy1, protectedBy2 });

    MemberDouble memberSpy = new MemberDouble(jpaAttribute.getJavaMember());
    memberSpy.addAnnotation(EdmProtections.class, protections);
    Attribute<?, ?> attributeSpy = Mockito.spy(jpaAttribute);
    when(attributeSpy.getJavaMember()).thenReturn(memberSpy);

    IntermediatePropertyAccess property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        attributeSpy, helper.schema);

    assertTrue(property.getProtectionClaimNames().contains("UserId"));
    List<String> actPath = property.getProtectionPath("UserId");
    assertEquals(1, actPath.size());
    assertEquals("AdministrativeInformation/Created/By", actPath.get(0));

    assertTrue(property.getProtectionClaimNames().contains("Date"));
    actPath = property.getProtectionPath("Date");
    assertEquals(1, actPath.size());
    assertEquals("AdministrativeInformation/Created/At", actPath.get(0));
  }

  @Ignore
  @Test
  public void checkGetSRID() {
    // Test for spatial data missing
  }

  private class PostProcessorSetName extends JPAEdmMetadataPostProcessor {

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(
          "com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner")) {
        if (property.getInternalName().equals("customString1")) {
          property.setExternalName("ContactPersonName");
        }
      }
    }

    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
        String jpaManagedTypeClassName) {}

    @Override
    public void provideReferences(IntermediateReferenceList references) throws ODataJPAModelException {}

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {}
  }
}
