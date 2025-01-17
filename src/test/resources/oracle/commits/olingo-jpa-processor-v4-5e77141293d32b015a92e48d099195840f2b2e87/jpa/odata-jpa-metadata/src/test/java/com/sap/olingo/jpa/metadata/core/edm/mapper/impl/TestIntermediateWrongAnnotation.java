package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys.COMPLEX_PROPERTY_MISSING_PROTECTION_PATH;
import static com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys.COMPLEX_PROPERTY_WRONG_PROTECTION_PATH;
import static com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys.NOT_SUPPORTED_PROTECTED_COLLECTION;
import static com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys.NOT_SUPPORTED_PROTECTED_NAVIGATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.errormodel.CollectionAttributeProtected;
import com.sap.olingo.jpa.processor.core.errormodel.ComplextProtectedNoPath;
import com.sap.olingo.jpa.processor.core.errormodel.ComplextProtectedWrongPath;
import com.sap.olingo.jpa.processor.core.errormodel.NavigationAttributeProtected;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;

public class TestIntermediateWrongAnnotation {
  private TestHelper helper;
  protected static final String PUNIT_NAME = "error";
  protected static EntityManagerFactory emf;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    emf = JPAEntityManagerFactory.getEntityManagerFactory(PUNIT_NAME, DataSourceHelper.createDataSource(
        DataSourceHelper.DB_HSQLDB));
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
  }

  @Test
  public void checkErrorOnProtectedCollectionAttribute() {
    PluralAttribute<?, ?, ?> jpaAttribute = helper.getCollectionAttribute(helper.getEntityType(
        CollectionAttributeProtected.class), "inhouseAddress");

    try {
      IntermediateCollectionProperty property = new IntermediateCollectionProperty(new JPAEdmNameBuilder(PUNIT_NAME),
          jpaAttribute, helper.schema, helper.schema.getEntityType(CollectionAttributeProtected.class));

      property.getEdmItem();
    } catch (ODataJPAModelException e) {
      assertEquals(NOT_SUPPORTED_PROTECTED_COLLECTION.name(), e.getId());
      assertFalse(e.getMessage().isEmpty());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void checkErrorOnProtectedNavigationAttribute() {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(NavigationAttributeProtected.class),
        "teams");

    try {
      IntermediateModelElement property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
          helper.schema.getEntityType(NavigationAttributeProtected.class), jpaAttribute, helper.schema);

      property.getEdmItem();
    } catch (ODataJPAModelException e) {
      assertEquals(NOT_SUPPORTED_PROTECTED_NAVIGATION.name(), e.getId());
      assertFalse(e.getMessage().isEmpty());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void checkErrorOnProtectedComplexAttributeMissingPath() {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(ComplextProtectedNoPath.class),
        "administrativeInformation");

    try {
      IntermediateModelElement property = new IntermediateSimpleProperty(new JPAEdmNameBuilder(PUNIT_NAME),
          jpaAttribute, helper.schema);

      property.getEdmItem();
    } catch (ODataJPAModelException e) {
      assertEquals(COMPLEX_PROPERTY_MISSING_PROTECTION_PATH.name(), e.getId());
      assertFalse(e.getMessage().isEmpty());
      return;
    }
    fail("Missing exception");
  }

  @Test
  public void checkErrorOnProtectedComplexAttributeWrongPath() {
    // ComplextProtectedWrongPath
    final EntityType<?> jpaEt = helper.getEntityType(ComplextProtectedWrongPath.class);
    try {
      IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), jpaEt, helper.schema);
      et.getEdmItem();
      et.getProtections();
    } catch (ODataJPAModelException e) {
      assertEquals(COMPLEX_PROPERTY_WRONG_PROTECTION_PATH.name(), e.getId());
      assertFalse(e.getMessage().isEmpty());
      return;
    }
    fail("Missing exception");
  }
}
