package com.sap.olingo.jpa.processor.core.modify;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import com.sap.olingo.jpa.processor.core.query.EdmEntitySetInfo;
import com.sap.olingo.jpa.processor.core.testmodel.ABCClassifiaction;
import com.sap.olingo.jpa.processor.core.testmodel.AccessRights;
import com.sap.olingo.jpa.processor.core.testmodel.AccessRightsConverter;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import com.sap.olingo.jpa.processor.core.testmodel.DateConverter;

public class TestJPACUDRequestHelper {
  private JPAConversionHelper cut;

  @BeforeEach
  public void setUp() throws Exception {
    cut = new JPAConversionHelper();
  }

  @Test
  public void testInstanceNull() {

    try {
      cut.buildGetterMap(null);
    } catch (ODataJPAProcessorException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testInstanceWithoutGetter() throws ODataJPAProcessorException {

    Map<String, Object> act = cut.buildGetterMap(new DateConverter());
    assertNotNull(act);
    assertEquals(1, act.size());
    assertNotNull(act.get("class"));
  }

  @Test
  public void testInstanceWithGetter() throws ODataJPAProcessorException {
    BusinessPartnerRole role = new BusinessPartnerRole();
    role.setBusinessPartnerID("ID");

    Map<String, Object> act = cut.buildGetterMap(role);
    assertNotNull(act);
    assertEquals(5, act.size());
    assertEquals("ID", act.get("businessPartnerID"));
  }

  @Test
  public void testSameInstanceWhenReadingTwice() throws ODataJPAProcessorException {
    BusinessPartnerRole role = new BusinessPartnerRole();

    Map<String, Object> exp = cut.buildGetterMap(role);
    Map<String, Object> act = cut.buildGetterMap(role);

    assertTrue(exp == act);
  }

  @Disabled
  @Test
  public void testDifferentInstanceWhenReadingDifferentInstance() throws ODataJPAProcessorException {

    Map<String, Object> exp = cut.buildGetterMap(new BusinessPartnerRole("100", "A"));
    Map<String, Object> act = cut.buildGetterMap(new BusinessPartnerRole("100", "A"));

    assertFalse(exp == act);
  }

  @Test
  public void testConvertEmptyInputStream() throws UnsupportedEncodingException {
    final ODataRequest request = mock(ODataRequest.class);
    final EdmEntitySetInfo etsInfo = mock(EdmEntitySetInfo.class);
    final EdmEntitySet ets = mock(EdmEntitySet.class);

    final InputStream is = new ByteArrayInputStream("".getBytes("UTF-8"));
    when(request.getBody()).thenReturn(is);
    when(etsInfo.getEdmEntitySet()).thenReturn(ets);
    when(etsInfo.getTargetEdmEntitySet()).thenReturn(ets);

    try {
      cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, etsInfo);
    } catch (ODataJPAProcessorException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertInputStream() throws UnsupportedEncodingException, ODataJPAProcessorException,
      EdmPrimitiveTypeException {

    final ODataRequest request = mock(ODataRequest.class);
    final EdmEntitySetInfo edmEntitySetInfo = mock(EdmEntitySetInfo.class);
    final EdmEntitySet edmEntitySet = mock(EdmEntitySet.class);
    final EdmEntityType edmEntityType = mock(EdmEntityType.class);
    final EdmProperty edmPropertyId = mock(EdmProperty.class);
    final EdmPrimitiveType edmTypeId = mock(EdmPrimitiveType.class);

    FullQualifiedName fqn = new FullQualifiedName("test", "Organisation");
    FullQualifiedName fqnString = new FullQualifiedName("test", "Organisation");

    List<String> propertyNames = new ArrayList<>();
    propertyNames.add("ID");

    when(edmTypeId.getFullQualifiedName()).thenReturn(fqnString);
    when(edmTypeId.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);
    when(edmTypeId.getName()).thenReturn("String");
    when(edmTypeId.valueOfString(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean(),
        (Class<String>) ArgumentMatchers.any())).thenReturn("35");

    when(edmEntitySet.getEntityType()).thenReturn(edmEntityType);
    when(edmEntityType.getFullQualifiedName()).thenReturn(fqn);
    when(edmEntityType.getPropertyNames()).thenReturn(propertyNames);
    when(edmEntityType.getProperty("ID")).thenReturn(edmPropertyId);
    when(edmPropertyId.getName()).thenReturn("ID");
    when(edmPropertyId.getType()).thenReturn(edmTypeId);
    when(edmEntitySetInfo.getEdmEntitySet()).thenReturn(edmEntitySet);
    when(edmEntitySetInfo.getTargetEdmEntitySet()).thenReturn(edmEntitySet);
    InputStream is = new ByteArrayInputStream("{\"ID\" : \"35\"}".getBytes("UTF-8"));
    when(request.getBody()).thenReturn(is);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, edmEntitySetInfo);
    assertEquals("35", act.getProperty("ID").getValue());
  }

  @Test
  public void testConvertPropertiesEmptyList() throws ODataJPAProcessException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(0, act.size());
  }

  @Test
  public void testConvertPropertiesUnknownValueType() {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);

    when(propertyID.getValueType()).thenReturn(ValueType.COLLECTION_ENTITY);
    when(propertyID.getName()).thenReturn("ID");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);

    try {
      cut.convertProperties(OData.newInstance(), st, odataProperties);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testConvertPropertiesConvertException() throws ODataJPAModelException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);

    when(propertyID.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyID.getName()).thenReturn("iD");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);
    when(st.getPath(ArgumentMatchers.anyString())).thenThrow(new ODataJPAModelException(new NullPointerException()));
    try {
      cut.convertProperties(OData.newInstance(), st, odataProperties);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testConvertPropertiesOnePrimitiveProperty() throws ODataJPAProcessException, ODataJPAModelException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath path = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("ID")).thenReturn(path);
    when(path.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("iD");

    Answer<?> a = (new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return String.class;
      }
    });
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(edmProperty.getMaxLength()).thenReturn(100);
    when(propertyID.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyID.getName()).thenReturn("ID");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals("35", act.get("iD"));
  }

  @Test
  public void testConvertPropertiesOneEnumPropertyWithoutConverter() throws ODataJPAProcessException,
      ODataJPAModelException {

    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath path = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("ABCClass")).thenReturn(path);
    when(path.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("aBCClass");

    Answer<?> a = (new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return ABCClassifiaction.class;
      }
    });
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(attribute.isEnum()).thenReturn(true);
    when(propertyID.getValueType()).thenReturn(ValueType.ENUM);
    when(propertyID.getName()).thenReturn("ABCClass");
    when(propertyID.getValue()).thenReturn(1);
    odataProperties.add(propertyID);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals(ABCClassifiaction.B, act.get("aBCClass"));
  }

  @Test
  public void testConvertPropertiesOneEnumPropertyWithConverter() throws ODataJPAProcessException,
      ODataJPAModelException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath path = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("AccessRights")).thenReturn(path);
    when(path.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("accessRights");

    Answer<?> a = (new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return AccessRights.class;
      }
    });
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(attribute.getConverter()).thenAnswer(new Answer<AttributeConverter<?, ?>>() {
      @Override
      public AttributeConverter<?, ?> answer(InvocationOnMock invocation) throws Throwable {
        return new AccessRightsConverter();
      }
    });
    when(edmProperty.getMaxLength()).thenReturn(100);
    when(propertyID.getValueType()).thenReturn(ValueType.ENUM);
    when(propertyID.getName()).thenReturn("AccessRights");
    when(propertyID.getValue()).thenReturn((short) 8);
    odataProperties.add(propertyID);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(1, act.size());
    AccessRights[] actProperty = (AccessRights[]) act.get("accessRights");
    assertArrayEquals(new Object[] { AccessRights.Delete }, actProperty);
  }

  @Test
  public void testConvertPropertiesOneComplexProperty() throws ODataJPAProcessException, ODataJPAModelException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath pathID = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("ID")).thenReturn(pathID);
    when(pathID.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("iD");

    Answer<?> a = (new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        return String.class;
      }
    });
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(edmProperty.getMaxLength()).thenReturn(100);
    when(propertyID.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyID.getName()).thenReturn("ID");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);

    ComplexValue cv = new ComplexValue();
    List<JPAElement> addressPathElements = new ArrayList<>();
    JPAElement addressElement = mock(JPAElement.class);
    addressPathElements.add(addressElement);
    when(addressElement.getInternalName()).thenReturn("address");

    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(cv);
    odataProperties.add(propertyAddress);
    JPAPath pathAddress = mock(JPAPath.class);
    when(st.getPath("Address")).thenReturn(pathAddress);
    when(pathAddress.getPath()).thenReturn(addressPathElements);
    JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(st.getAttribute("address")).thenReturn(attributeAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(2, act.size());
    assertTrue(act.get("address") instanceof Map<?, ?>);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertPropertiesOneComplexCollcetionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {
    final List<Property> odataProperties = new ArrayList<>();
    final List<ComplexValue> odataComment = new ArrayList<>();
    final List<Property> addressProperties = new ArrayList<>();
    final JPAStructuredType st = createMetadataForSimpleProperty("Address", "address");
    final JPAStructuredType nb = createMetadataForSimpleProperty("Number", "number");
    final JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(attributeAddress.getStructuredType()).thenReturn(nb);
    when(st.getAttribute("address")).thenReturn(attributeAddress);
    final ComplexValue cv1 = mock(ComplexValue.class);

    final Property propertyNumber = mock(Property.class);
    when(propertyNumber.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyNumber.getName()).thenReturn("Number");
    when(propertyNumber.getValue()).thenReturn(32);
    addressProperties.add(propertyNumber);
    when(cv1.getValue()).thenReturn(addressProperties);

    odataComment.add(cv1);
    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COLLECTION_COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("address"));
    assertEquals(1, ((List<Map<String, Object>>) act.get("address")).size());
    Map<String, Object> actAddr = (Map<String, Object>) ((List<?>) act.get("address")).get(0);
    assertEquals(32, actAddr.get("number"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertPropertiesTwoComplexCollcetionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {
    final List<Property> odataProperties = new ArrayList<>();
    final List<ComplexValue> odataComment = new ArrayList<>();
    final JPAStructuredType st = createMetadataForSimpleProperty("Address", "address");
    final JPAStructuredType nb = createMetadataForSimpleProperty("Number", "number");
    final JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(attributeAddress.getStructuredType()).thenReturn(nb);
    when(st.getAttribute("address")).thenReturn(attributeAddress);

    List<Property> addressProperties = new ArrayList<>();
    final ComplexValue cv1 = mock(ComplexValue.class);
    Property propertyNumber = mock(Property.class);
    when(propertyNumber.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyNumber.getName()).thenReturn("Number");
    when(propertyNumber.getValue()).thenReturn(32);
    addressProperties.add(propertyNumber);
    when(cv1.getValue()).thenReturn(addressProperties);

    addressProperties = new ArrayList<>();
    final ComplexValue cv2 = mock(ComplexValue.class);
    propertyNumber = mock(Property.class);
    when(propertyNumber.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyNumber.getName()).thenReturn("Number");
    when(propertyNumber.getValue()).thenReturn(16);
    addressProperties.add(propertyNumber);
    when(cv2.getValue()).thenReturn(addressProperties);

    odataComment.add(cv1);
    odataComment.add(cv2);
    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COLLECTION_COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("address"));
    assertEquals(2, ((List<Map<String, Object>>) act.get("address")).size());
    Map<String, Object> actAddr1 = (Map<String, Object>) ((List<?>) act.get("address")).get(0);
    assertEquals(32, actAddr1.get("number"));

    Map<String, Object> actAddr2 = (Map<String, Object>) ((List<?>) act.get("address")).get(1);
    assertEquals(16, actAddr2.get("number"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertPropertiesEmptyComplexCollcetionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {
    final List<Property> odataProperties = new ArrayList<>();
    final List<ComplexValue> odataComment = new ArrayList<>();
    final JPAStructuredType st = createMetadataForSimpleProperty("Address", "address");
    final JPAStructuredType nb = createMetadataForSimpleProperty("Number", "number");
    final JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(attributeAddress.getStructuredType()).thenReturn(nb);
    when(st.getAttribute("address")).thenReturn(attributeAddress);

    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COLLECTION_COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("address"));
    assertEquals(0, ((List<Map<String, Object>>) act.get("address")).size());
  }

  @Test
  public void testConvertPropertiesOneSimpleCollcetionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {
    final List<Property> odataProperties = new ArrayList<>();
    final List<String> odataComment = new ArrayList<>();

    final JPAStructuredType st = createMetadataForSimpleProperty("Comment", "comment");

    odataComment.add("First Test");
    Property propertyComment = mock(Property.class);
    when(propertyComment.getValueType()).thenReturn(ValueType.COLLECTION_PRIMITIVE);
    when(propertyComment.getName()).thenReturn("Comment");
    when(propertyComment.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyComment);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("comment"));
    assertEquals(1, ((List<?>) act.get("comment")).size());
    assertEquals("First Test", ((List<?>) act.get("comment")).get(0));
  }

  @Test
  public void testConvertPropertiesTwoSimpleCollcetionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {
    final List<Property> odataProperties = new ArrayList<>();
    final List<String> odataComment = new ArrayList<>();

    final JPAStructuredType st = createMetadataForSimpleProperty("Comment", "comment");

    odataComment.add("First Test");
    odataComment.add("Second Test");
    Property propertyComment = mock(Property.class);
    when(propertyComment.getValueType()).thenReturn(ValueType.COLLECTION_PRIMITIVE);
    when(propertyComment.getName()).thenReturn("Comment");
    when(propertyComment.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyComment);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("comment"));
    assertEquals(2, ((List<?>) act.get("comment")).size());
    assertEquals("First Test", ((List<?>) act.get("comment")).get(0));
    assertEquals("Second Test", ((List<?>) act.get("comment")).get(1));
  }

  @Test
  public void testConvertPropertiesEmptySimpleCollcetionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {
    final List<Property> odataProperties = new ArrayList<>();
    final List<String> odataComment = new ArrayList<>();

    final JPAStructuredType st = createMetadataForSimpleProperty("Comment", "comment");

    Property propertyComment = mock(Property.class);
    when(propertyComment.getValueType()).thenReturn(ValueType.COLLECTION_PRIMITIVE);
    when(propertyComment.getName()).thenReturn("Comment");
    when(propertyComment.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyComment);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("comment"));
    assertTrue(((List<?>) act.get("comment")).isEmpty());
  }

  private JPAStructuredType createMetadataForSimpleProperty(final String externalName, final String internalName)
      throws ODataJPAModelException {
    final JPAStructuredType st = mock(JPAStructuredType.class);
    final JPAAttribute attribute = mock(JPAAttribute.class);
    final JPAPath pathID = mock(JPAPath.class);
    final List<JPAElement> pathElements = new ArrayList<>();
    pathElements.add(attribute);
    when(st.getPath(externalName)).thenReturn(pathID);
    when(pathID.getLeaf()).thenReturn(attribute);
    when(pathID.getPath()).thenReturn(pathElements);
    when(attribute.getInternalName()).thenReturn(internalName);
    return st;
  }
}
