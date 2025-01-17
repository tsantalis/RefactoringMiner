package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.edm.provider.CsdlOnDelete;
import org.apache.olingo.commons.api.edm.provider.CsdlOnDeleteAction;
import org.apache.olingo.commons.api.edm.provider.CsdlReferentialConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmEnumeration;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateEntityTypeAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateReferenceList;
import com.sap.olingo.jpa.processor.core.testmodel.ABCClassifiaction;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import com.sap.olingo.jpa.processor.core.testmodel.DummyToBeIgnored;
import com.sap.olingo.jpa.processor.core.testmodel.JoinSource;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;
import com.sap.olingo.jpa.processor.core.testmodel.Person;

public class TestIntermediateNavigationProperty extends TestMappingRoot {
  private IntermediateSchema schema;
  private TestHelper helper;
  private JPAEdmMetadataPostProcessor processor;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    final Reflections r = mock(Reflections.class);
    when(r.getTypesAnnotatedWith(EdmEnumeration.class)).thenReturn(new HashSet<>(Arrays.asList(new Class<?>[] {
        ABCClassifiaction.class })));

    schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
    processor = mock(JPAEdmMetadataPostProcessor.class);
  }

  @Test
  public void checkNaviProptertyCanBeCreated() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME), schema.getStructuredType(jpaAttribute),
        jpaAttribute, schema);
  }

  @Test
  public void checkGetName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals("Roles", property.getEdmItem().getName(), "Wrong name");
  }

  @Test
  public void checkGetType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals(PUNIT_NAME + ".BusinessPartnerRole", property.getEdmItem().getType(), "Wrong name");
  }

  @Test
  public void checkGetIgnoreFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getStructuredType(jpaAttribute), jpaAttribute, schema);
    assertFalse(property.ignore());
  }

  @Test
  public void checkGetIgnoreTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(DummyToBeIgnored.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getStructuredType(jpaAttribute), jpaAttribute, schema);
    assertTrue(property.ignore());
  }

  @Test
  public void checkGetProptertyFacetsNullableTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertTrue(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetPropertyOnDelete() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals(CsdlOnDeleteAction.Cascade, property.getEdmItem().getOnDelete().getAction());
  }

  @Test
  public void checkGetProptertyFacetsNullableFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartnerRole.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, schema);

    assertFalse(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetProptertyFacetsCollectionTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertTrue(property.getEdmItem().isNullable());
  }

  @Test
  public void checkGetProptertyFacetsColletionFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartnerRole.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, schema);

    assertFalse(property.getEdmItem().isCollection());
  }

  @Test
  public void checkGetJoinColumnsSize1BP() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals(1, property.getJoinColumns().size());
  }

  @Test
  public void checkGetPartnerAdmin_Parent() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(AdministrativeDivision.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "parent");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("Children", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerAdmin_Children() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(AdministrativeDivision.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "children");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("Parent", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerBP_Roles() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("BusinessPartner", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerRole_BP() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartnerRole.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals("Roles", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetJoinColumnFilledCompletely() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartner.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);

    IntermediateJoinColumn act = property.getJoinColumns().get(0);
    assertEquals("\"BusinessPartnerID\"", act.getName());
    assertEquals("\"ID\"", act.getReferencedColumnName());
  }

  @Test
  public void checkGetJoinColumnFilledCompletelyInvert() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartnerRole.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);

    IntermediateJoinColumn act = property.getJoinColumns().get(0);
    assertEquals("\"BusinessPartnerID\"", act.getName());
    assertEquals("\"ID\"", act.getReferencedColumnName());
  }

  @Test
  public void checkGetJoinColumnsSize1Roles() throws ODataJPAModelException {
    EntityType<?> et = helper.getEntityType(BusinessPartnerRole.class);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(et.getJavaType()), jpaAttribute, schema);
    assertEquals(1, property.getJoinColumns().size());
  }

  @Test
  public void checkGetJoinColumnsSize2() throws ODataJPAModelException {
    EmbeddableType<?> et = helper.getEmbeddedableType("PostalAddressData");
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "administrativeDivision");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getComplexType(et.getJavaType()), jpaAttribute, schema);
    List<IntermediateJoinColumn> columns = property.getJoinColumns();
    assertEquals(3, columns.size());
  }

  @Test
  public void checkGetReferentialConstraintSize() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);
    assertEquals(1, property.getProperty().getReferentialConstraints().size());
  }

  @Test
  public void checkGetReferentialConstraintBuPaRole() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);
    List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

    for (CsdlReferentialConstraint c : constraints) {
      assertEquals("ID", c.getProperty());
      assertEquals("BusinessPartnerID", c.getReferencedProperty());
    }
  }

  @Test
  public void checkGetReferentialConstraintRoleBuPa() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartnerRole.class),
        "businessPartner");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, schema);
    List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

    for (CsdlReferentialConstraint c : constraints) {
      assertEquals("BusinessPartnerID", c.getProperty());
      assertEquals("ID", c.getReferencedProperty());
    }
  }

  @Test
  public void checkGetReferentialConstraintViaEmbeddedId() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(AdministrativeDivision.class),
        "allDescriptions");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(AdministrativeDivision.class), jpaAttribute, schema);
    List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

    assertEquals(3, constraints.size());
    for (CsdlReferentialConstraint c : constraints) {
      assertEquals(c.getReferencedProperty(), c.getProperty());
    }
  }

  @Test
  public void checkPostProcessorCalled() throws ODataJPAModelException {
    IntermediateModelElement.setPostProcessor(processor);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(
        BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    property.getEdmItem();
    verify(processor, atLeastOnce()).processNavigationProperty(property, BUPA_CANONICAL_NAME);
  }

  @Test
  public void checkPostProcessorNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals("RoleAssignment", property.getEdmItem().getName(), "Wrong name");
  }

  @Test
  public void checkPostProcessorExternalNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(BusinessPartner.class), "roles");
    JPAAssociationAttribute property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getStructuredType(jpaAttribute), jpaAttribute, schema);

    assertEquals("RoleAssignment", property.getExternalName(), "Wrong name");
  }

  @Test
  public void checkPostProcessorSetOnDelete() throws ODataJPAModelException {
    PostProcessorOneDelete pPDouble = new PostProcessorOneDelete();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(AdministrativeDivision.class),
        "children");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(AdministrativeDivision.class), jpaAttribute, schema);

    assertEquals(CsdlOnDeleteAction.None, property.getProperty().getOnDelete().getAction());
  }

  @Test
  public void checkGetJoinTable() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertNotNull(property.getJoinTable());
  }

  @Test
  public void checkGetJoinTableName() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertEquals("\"SupportRelationship\"", property.getJoinTable().getTableName());
  }

  @Test
  public void checkGetNullIfNoJoinTableGiven() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(AdministrativeDivision.class),
        "parent");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertNull(property.getJoinTable());
  }

  @Test
  public void checkGetJoinTableJoinColumns() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertFalse(property.getJoinColumns().isEmpty());
  }

  @Test
  public void checkGetJoinTableEntityType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Person.class),
        "supportedOrganizations");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertNotNull(property.getJoinTable().getEntityType());
  }

  @Test
  public void checkGetJoinTableJoinColumnsNotMapped() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(JoinSource.class),
        "oneToMany");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(JoinSource.class), jpaAttribute, schema);

    assertFalse(property.getJoinColumns().isEmpty());
    assertNotNull(property.getJoinTable());
    IntermediateJoinTable act = (IntermediateJoinTable) property.getJoinTable();
    for (JPAOnConditionItem item : act.getJoinColumns()) {
      assertNotNull(item.getLeftPath());
      assertNotNull(item.getRightPath());
    }
  }

  @Test
  public void checkGetJoinTableJoinColumnsMapped() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(Organization.class),
        "supportEngineers");
    IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        schema.getEntityType(BusinessPartner.class), jpaAttribute, schema);

    assertFalse(property.getJoinColumns().isEmpty());
  }

  private class PostProcessorSetName extends JPAEdmMetadataPostProcessor {
    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
        String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(
          BUPA_CANONICAL_NAME)) {
        if (property.getInternalName().equals("roles")) {
          property.setExternalName("RoleAssignment");
        }
      }
    }

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {

    }

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {}

    @Override
    public void provideReferences(IntermediateReferenceList references) throws ODataJPAModelException {}
  }

  private class PostProcessorOneDelete extends JPAEdmMetadataPostProcessor {
    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
        String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(ADMIN_CANONICAL_NAME)) {
        if (property.getInternalName().equals("children")) {
          CsdlOnDelete oD = new CsdlOnDelete();
          oD.setAction(CsdlOnDeleteAction.None);
          property.setOnDelete(oD);
        }
      }
    }

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {}

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {}

    @Override
    public void provideReferences(IntermediateReferenceList references) throws ODataJPAModelException {}
  }
}
