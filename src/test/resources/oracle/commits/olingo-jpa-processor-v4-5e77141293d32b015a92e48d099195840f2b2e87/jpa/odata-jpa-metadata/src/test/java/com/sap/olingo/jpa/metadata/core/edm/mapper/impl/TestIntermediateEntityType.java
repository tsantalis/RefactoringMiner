package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlCollection;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmEnumeration;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAProtectionInfo;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateEntityTypeAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateReferenceList;
import com.sap.olingo.jpa.processor.core.testmodel.ABCClassifiaction;
import com.sap.olingo.jpa.processor.core.testmodel.TestDataConstants;

public class TestIntermediateEntityType extends TestMappingRoot {
  private Set<EntityType<?>> etList;
  private IntermediateSchema schema;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    IntermediateModelElement.setPostProcessor(new DefaultEdmPostProcessor());
    final Reflections r = mock(Reflections.class);
    when(r.getTypesAnnotatedWith(EdmEnumeration.class)).thenReturn(new HashSet<>(Arrays.asList(new Class<?>[] {
        ABCClassifiaction.class })));

    etList = emf.getMetamodel().getEntities();
    schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
  }

  @Test
  public void checkEntityTypeCanBeCreated() throws ODataJPAModelException {

    new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType("BusinessPartner"), schema);
  }

  @Test
  public void checkEntityTypeIgnoreSet() throws ODataJPAModelException {

    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "DummyToBeIgnored"), schema);
    et.getEdmItem();
    assertTrue(et.ignore());
  }

  @Test
  public void checkGetAllProperties() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals(TestDataConstants.NO_DEC_ATTRIBUTES_BUISNESS_PARTNER, et.getEdmItem()
        .getProperties()
        .size(), "Wrong number of entities");
  }

  @Test
  public void checkGetPropertyByNameNotNull() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertNotNull(et.getEdmItem().getProperty("Type"));
  }

  @Test
  public void checkGetPropertyByNameCorrectEntity() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals("Type", et.getEdmItem().getProperty("Type").getName());
  }

  @Test
  public void checkGetPropertyByNameCorrectEntityID() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals("ID", et.getEdmItem().getProperty("ID").getName());
  }

  @Test
  public void checkGetPathByNameCorrectEntityID() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals("ID", et.getPath("ID").getLeaf().getExternalName());
  }

  @Test
  public void checkGetPathByNameIgnore() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertNull(et.getPath("CustomString2"));
  }

  @Test
  public void checkGetPathByNameIgnoreCompexType() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertNull(et.getPath("Address/RegionCodePublisher"));
  }

  @Test
  public void checkGetInheritedAttributeByNameCorrectEntityID() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Person"), schema);
    assertEquals("ID", et.getPath("ID").getLeaf().getExternalName());
  }

  @Test
  public void checkGetAllNaviProperties() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals(1, et.getEdmItem().getNavigationProperties().size(), "Wrong number of entities");
  }

  @Test
  public void checkGetNaviPropertyByNameNotNull() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertNotNull(et.getEdmItem().getNavigationProperty("Roles"));
  }

  @Test
  public void checkGetNaviPropertyByNameCorrectEntity() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals("Roles", et.getEdmItem().getNavigationProperty("Roles").getName());
  }

  @Test
  public void checkGetAssoziationOfComplexTypeByNameCorrectEntity() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals("Address/AdministrativeDivision", et.getAssociationPath("Address/AdministrativeDivision").getAlias());
  }

  @Test
  public void checkGetAssoziationOfComplexTypeByNameJoinColumns() throws ODataJPAModelException {
    int actCount = 0;
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    for (JPAOnConditionItem item : et.getAssociationPath("Address/AdministrativeDivision").getJoinColumnsList()) {
      if (item.getLeftPath().getAlias().equals("Address/Region")) {
        assertTrue(item.getRightPath().getAlias().equals("DivisionCode"));
        actCount++;
      }
      if (item.getLeftPath().getAlias().equals("Address/RegionCodeID")) {
        assertTrue(item.getRightPath().getAlias().equals("CodeID"));
        actCount++;
      }
      if (item.getLeftPath().getAlias().equals("Address/RegionCodePublisher")) {
        assertTrue(item.getRightPath().getAlias().equals("CodePublisher"));
        actCount++;
      }
    }
    assertEquals(3, actCount, "Not all join columns found");
  }

  @Test
  public void checkGetPropertiesSkipIgnored() throws ODataJPAModelException {
    PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
    IntermediateModelElement.setPostProcessor(pPDouble);

    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals(TestDataConstants.NO_DEC_ATTRIBUTES_BUISNESS_PARTNER - 1, et.getEdmItem()
        .getProperties().size(), "Wrong number of entities");
  }

  @Test
  public void checkGetIsAbstract() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertTrue(et.getEdmItem().isAbstract());
  }

  @Test
  public void checkGetIsNotAbstract() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);
    assertFalse(et.getEdmItem().isAbstract());
  }

  @Test
  public void checkGetHasBaseType() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);
    assertEquals(PUNIT_NAME + ".BusinessPartner", et.getEdmItem().getBaseType());
  }

  @Test
  public void checkGetKeyProperties() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartnerRole"), schema);
    assertEquals(2, et.getEdmItem().getKey().size(), "Wrong number of key propeties");
  }

  @Test
  public void checkGetAllAttributes() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartnerRole"), schema);
    assertEquals(2, et.getPathList().size(), "Wrong number of entities");
  }

  @Test
  public void checkGetAllAttributesWithBaseType() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);
    int exp = TestDataConstants.NO_ATTRIBUTES_BUISNESS_PARTNER
        + TestDataConstants.NO_ATTRIBUTES_POSTAL_ADDRESS
        + TestDataConstants.NO_ATTRIBUTES_COMMUNICATION_DATA
        + 2 * TestDataConstants.NO_ATTRIBUTES_CHANGE_INFO
        + TestDataConstants.NO_ATTRIBUTES_ORGANIZATION;
    assertEquals(exp, et.getPathList().size(), "Wrong number of entities");
  }

  @Test
  public void checkGetAllAttributesWithBaseTypeFields() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);

    assertNotNull(et.getPath("Type"));
    assertNotNull(et.getPath("Name1"));
    assertNotNull(et.getPath("Address" + JPAPath.PATH_SEPERATOR + "Region"));
    assertNotNull(et.getPath("AdministrativeInformation" + JPAPath.PATH_SEPERATOR
        + "Created" + JPAPath.PATH_SEPERATOR + "By"));
  }

  @Test
  public void checkGetAllAttributeIDWithBaseType() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);
    assertEquals("ID", et.getPath("ID").getAlias());
  }

  @Test
  public void checkGetKeyAttributeFromEmbeddedId() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivisionDescription"), schema);

    assertNotNull(et.getAttribute("codePublisher"));
    assertEquals("CodePublisher", et.getAttribute("codePublisher").getExternalName());
  }

  @Test
  public void checkGetKeyWithBaseType() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);
    assertEquals(1, et.getKey().size());
  }

  @Test
  public void checkEmbeddedIdResovedProperties() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivisionDescription"), schema);
    assertEquals(5, et.getEdmItem().getProperties().size());
  }

  @Test
  public void checkEmbeddedIdResovedKey() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivisionDescription"), schema);
    assertEquals(4, et.getEdmItem().getKey().size());
  }

  @Test
  public void checkEmbeddedIdResovedKeyInternal() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivisionDescription"), schema);
    assertEquals(4, et.getKey().size());
  }

  @Test
  public void checkEmbeddedIdResovedPath() throws ODataJPAModelException {
    JPAStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivisionDescription"), schema);
    assertEquals(5, et.getPathList().size());
  }

  @Test
  public void checkEmbeddedIdResovedPathCodeId() throws ODataJPAModelException {
    JPAStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivisionDescription"), schema);
    assertEquals(2, et.getPath("CodeID").getPath().size());
  }

  @Test
  public void checkHasStreamNoProperties() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "PersonImage"), schema);
    assertEquals(2, et.getEdmItem().getProperties().size());
  }

  @Test
  public void checkHasStreamTrue() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "PersonImage"), schema);
    assertTrue(et.getEdmItem().hasStream());
  }

  @Test
  public void checkHasStreamFalse() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertFalse(et.getEdmItem().hasStream());
  }

  @Test
  public void checkHasETagTrue() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertTrue(et.hasEtag());
  }

  @Test
  public void checkHasETagTrueIfInherited() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);
    assertTrue(et.hasEtag());
  }

  @Test
  public void checkHasETagFalse() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivision"), schema);
    assertFalse(et.hasEtag());
  }

  @Test
  public void checkIgnoreIfAsEntitySet() throws ODataJPAModelException {
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BestOrganization"), schema);
    assertTrue(et.ignore());
  }

  @Test
  public void checkAnnotationSet() throws ODataJPAModelException {
    IntermediateModelElement.setPostProcessor(new PostProcessorSetIgnore());
    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "PersonImage"), schema);
    List<CsdlAnnotation> act = et.getEdmItem().getAnnotations();
    assertEquals(1, act.size());
    assertEquals("Core.AcceptableMediaTypes", act.get(0).getTerm());
  }

  @Test
  public void checkGetProptertyByDBFieldName() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartner"), schema);
    assertEquals("Type", et.getPropertyByDBField("\"Type\"").getExternalName());
  }

  @Test
  public void checkGetProptertyByDBFieldNameFromSuperType() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Organization"), schema);
    assertEquals("Type", et.getPropertyByDBField("\"Type\"").getExternalName());
  }

  @Test
  public void checkGetProptertyByDBFieldNameFromEmbedded() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "AdministrativeDivisionDescription"), schema);
    assertEquals("CodeID", et.getPropertyByDBField("\"CodeID\"").getExternalName());
  }

  @Test
  public void checkAllPathContainsComplexCollcetion() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Collection"), schema);
    final List<JPAPath> act = et.getPathList();

    assertEquals(11, act.size());
    assertNotNull(et.getPath("Complex/Address"));
    assertTrue(et.getPath("Complex/Address").getLeaf().isCollection());
    final IntermediateCollectionProperty actIntermediate = (IntermediateCollectionProperty) et.getPath(
        "Complex/Address").getLeaf();
    assertTrue(actIntermediate.asAssociation().getSourceType() instanceof JPAEntityType);
    assertEquals(2, actIntermediate.asAssociation().getPath().size());

    for (JPAPath p : act) {
      if (p.getPath().size() > 1
          && p.getPath().get(0).getExternalName().equals("Complex")
          && p.getPath().get(1).getExternalName().equals("Address")) {
        assertTrue(p.getPath().get(1) instanceof IntermediateCollectionProperty);
        final IntermediateCollectionProperty actProperty = (IntermediateCollectionProperty) p.getPath().get(1);
        assertNotNull(actProperty.asAssociation());
        assertEquals(et, actProperty.asAssociation().getSourceType());
        break;
      }
    }
  }

  @Test
  public void checkAllPathContainsPrimitiveCollcetion() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "Collection"), schema);
    final List<JPAPath> act = et.getPathList();

    assertEquals(11, act.size());
    assertNotNull(et.getPath("Complex/Comment"));
    assertTrue(et.getPath("Complex/Comment").getLeaf().isCollection());
    final IntermediateCollectionProperty actIntermediate = (IntermediateCollectionProperty) et.getPath(
        "Complex/Comment").getLeaf();
    assertTrue(actIntermediate.asAssociation().getSourceType() instanceof JPAEntityType);
    assertEquals("Complex/Comment", actIntermediate.asAssociation().getAlias());

    for (JPAPath p : act) {
      if (p.getPath().size() > 1
          && p.getPath().get(0).getExternalName().equals("Complex")
          && p.getPath().get(1).getExternalName().equals("Comment")) {
        assertTrue(p.getPath().get(1) instanceof IntermediateCollectionProperty);
        final IntermediateCollectionProperty actProperty = (IntermediateCollectionProperty) p.getPath().get(1);
        assertNotNull(actProperty.asAssociation());
        assertEquals(et, actProperty.asAssociation().getSourceType());
        break;
      }
    }
  }

  @Test
  public void checkAllPathContainsDeepComplexWithPrimitiveCollcetion() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "CollectionDeep"), schema);
    final List<JPAPath> act = et.getPathList();

    assertEquals(8, act.size());
    assertNotNull(et.getPath("FirstLevel/SecondLevel/Comment"));
    assertTrue(et.getPath("FirstLevel/SecondLevel/Comment").getLeaf().isCollection());
    final IntermediateCollectionProperty actIntermediate = (IntermediateCollectionProperty) et.getPath(
        "FirstLevel/SecondLevel/Comment").getLeaf();
    assertTrue(actIntermediate.asAssociation().getSourceType() instanceof JPAEntityType);
    assertEquals(3, actIntermediate.asAssociation().getPath().size());
    assertEquals("FirstLevel/SecondLevel/Comment", actIntermediate.asAssociation().getAlias());
  }

  @Test
  public void checkAllPathContainsDeepComplexWithComplexCollcetion() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "CollectionDeep"), schema);

    assertNotNull(et.getPath("FirstLevel/SecondLevel/Address"));
    assertTrue(et.getPath("FirstLevel/SecondLevel/Address").getLeaf().isCollection());
    final IntermediateCollectionProperty actIntermediate = (IntermediateCollectionProperty) et.getPath(
        "FirstLevel/SecondLevel/Address").getLeaf();
    assertTrue(actIntermediate.asAssociation().getSourceType() instanceof JPAEntityType);
    assertEquals(3, actIntermediate.asAssociation().getPath().size());
    assertEquals("FirstLevel/SecondLevel/Address", actIntermediate.asAssociation().getAlias());
    for (JPAPath path : et.getPathList()) {
      String[] pathElements = path.getAlias().split("/");
      assertEquals(pathElements.length, path.getPath().size());
    }
  }

  @Test
  public void checkOneSimpleProtectedProperty() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BusinessPartnerProtected"), schema);
    List<JPAProtectionInfo> act = et.getProtections();
    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals("Username", act.get(0).getAttribute().getExternalName());
    assertEquals("UserId", act.get(0).getClaimName());
  }

  @Test
  public void checkComplexAndInheritedProtectedProperty() throws ODataJPAModelException {
    IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "PersonDeepProtected"), schema);

    List<JPAProtectionInfo> act = et.getProtections();
    assertNotNull(act);
    assertInherited(act);
    assertComplexAnnotated(act, "Creator", "Created");
    assertComplexAnnotated(act, "Updator", "Updated");
    assertComplexDeep(act);
    assertEquals(4, act.size());
  }

  private void assertComplexDeep(List<JPAProtectionInfo> act) {
    for (final JPAProtectionInfo info : act) {
      if (info.getClaimName().equals("BuildingNumber")) {
        assertEquals("Building", info.getAttribute().getExternalName());
        assertEquals(3, info.getPath().getPath().size());
        assertEquals("InhouseAddress/InhouseAddress/Building", info.getPath().getAlias());
        return;
      }
    }
    fail("Deep protected complex attribute not found");

  }

  private void assertComplexAnnotated(List<JPAProtectionInfo> act, final String expClaimName,
      final String pathElement) {
    for (final JPAProtectionInfo info : act) {
      if (info.getClaimName().equals(expClaimName)) {
        assertEquals("By", info.getAttribute().getExternalName());
        assertEquals(3, info.getPath().getPath().size());
        assertEquals("ProtectedAdminInfo/" + pathElement + "/By", info.getPath().getAlias());
        return;
      }
    }
    fail("Complex attribute not found for: " + expClaimName);
  }

  private void assertInherited(List<JPAProtectionInfo> act) {
    for (final JPAProtectionInfo info : act) {
      if (info.getAttribute().getExternalName().equals("Username")) {
        assertEquals("UserId", info.getClaimName());
        assertEquals(1, info.getPath().getPath().size());
        assertEquals("Username", info.getPath().getAlias());
        return;
      }
    }
    fail("Inherited not found");
  }

  private class PostProcessorSetIgnore extends JPAEdmMetadataPostProcessor {

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(
          "com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner")) {
        if (property.getInternalName().equals("communicationData")) {
          property.setIgnore(true);
        }
      }
    }

    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
        String jpaManagedTypeClassName) {}

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {
      if (entity.getExternalName().equals("PersonImage")) {
        List<CsdlExpression> items = new ArrayList<>();
        CsdlCollection exp = new CsdlCollection();
        exp.setItems(items);
        CsdlConstantExpression mimeType = new CsdlConstantExpression(ConstantExpressionType.String, "ogg");
        items.add(mimeType);
        CsdlAnnotation annotation = new CsdlAnnotation();
        annotation.setExpression(exp);
        annotation.setTerm("Core.AcceptableMediaTypes");
        List<CsdlAnnotation> annotations = new ArrayList<>();
        annotations.add(annotation);
        entity.addAnnotations(annotations);
      }
    }

    @Override
    public void provideReferences(IntermediateReferenceList references) throws ODataJPAModelException {}
  }

  private EntityType<?> getEntityType(String typeName) {
    for (EntityType<?> entityType : etList) {
      if (entityType.getJavaType().getSimpleName().equals(typeName)) {
        return entityType;
      }
    }
    return null;
  }
}
