package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateEntityContainerAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateEntityTypeAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateReferenceList;
import com.sap.olingo.jpa.processor.core.testmodel.TestDataConstants;

public class TestIntermediateContainer extends TestMappingRoot {
  private static final String PACKAGE1 = "com.sap.olingo.jpa.metadata.core.edm.mapper.impl";
  private static final String PACKAGE2 = "com.sap.olingo.jpa.processor.core.testmodel";
  private HashMap<String, IntermediateSchema> schemas = new HashMap<>();
  private Set<EntityType<?>> etList;
  private IntermediateSchema schema;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    IntermediateModelElement.setPostProcessor(new DefaultEdmPostProcessor());
    Reflections r =
        new Reflections(
            new ConfigurationBuilder()
                .forPackages(PACKAGE1, PACKAGE2)
                .filterInputsBy(new FilterBuilder().includePackage(PACKAGE1, PACKAGE2))
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner()));

    schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    etList = emf.getMetamodel().getEntities();
    schemas.put(PUNIT_NAME, schema);
  }

  @Test
  public void checkContainerCanBeCreated() throws ODataJPAModelException {

    new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);
  }

  @Test
  public void checkGetName() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);
    assertEquals("ComSapOlingoJpaContainer", container.getExternalName());
  }

  @Test
  public void checkGetNoEntitySets() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);
    assertEquals(TestDataConstants.NO_ENTITY_SETS, container.getEdmItem().getEntitySets().size());
  }

  @Test
  public void checkGetEntitySetName() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);
    List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
    for (CsdlEntitySet entitySet : entitySets) {
      if (entitySet.getName().equals("BusinessPartners")) return;
    }
    fail();
  }

  @Test
  public void checkGetEntitySetType() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);
    List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
    for (CsdlEntitySet entitySet : entitySets) {
      if (entitySet.getName().equals("BusinessPartners")) {
        assertEquals(new JPAEdmNameBuilder(PUNIT_NAME).buildFQN("BusinessPartner"), entitySet.getTypeFQN());
        return;
      }
    }
    fail();
  }

  @Test
  public void checkGetNoNavigationPropertyBindings() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
    for (CsdlEntitySet entitySet : entitySets) {
      if (entitySet.getName().equals("BusinessPartners")) {
        assertEquals(4, entitySet.getNavigationPropertyBindings().size());
        return;
      }
    }
    fail();
  }

  @Test
  public void checkGetNavigationPropertyBindingsPath() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
    for (CsdlEntitySet entitySet : entitySets) {
      if (entitySet.getName().equals("BusinessPartners")) {
        for (CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
          if ("Roles".equals(binding.getPath()))
            return;
        }
      }
    }
    fail();
  }

  @Test
  public void checkGetNavigationPropertyBindingsTarget() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
    for (CsdlEntitySet entitySet : entitySets) {
      if (entitySet.getName().equals("BusinessPartners")) {
        for (CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
          if ("Roles".equals(binding.getPath())) {
            assertEquals("BusinessPartnerRoles", binding.getTarget());
            return;
          }
        }
      }
    }
    fail();
  }

  @Test
  public void checkGetNavigationPropertyBindingsPathComplexType() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
    for (CsdlEntitySet entitySet : entitySets) {
      if (entitySet.getName().equals("BusinessPartners")) {
        for (CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
          if ("Address/AdministrativeDivision".equals(binding.getPath()))
            return;
        }
      }
    }
    fail();
  }

  @Test
  public void checkGetNavigationPropertyBindingsPathComplexTypeNested() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
    for (CsdlEntitySet entitySet : entitySets) {
      if (entitySet.getName().equals("BusinessPartners")) {
        for (CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
          if ("AdministrativeInformation/Created/User".equals(binding.getPath()))
            return;
        }
      }
    }
    fail();
  }

  @Test
  public void checkGetNoFunctionImportIfBound() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlFunctionImport> funcImports = container.getEdmItem().getFunctionImports();
    for (CsdlFunctionImport funcImport : funcImports) {
      if (funcImport.getName().equals("CountRoles")) {
        fail("Bound function must not generate a function import");
      }
    }
  }

  @Test
  public void checkGetNoFunctionImportIfUnBoundHasImportFalse() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlFunctionImport> funcImports = container.getEdmItem().getFunctionImports();
    for (CsdlFunctionImport funcImport : funcImports) {
      if (funcImport.getName().equals("max")) {
        fail("UnBound function must not generate a function import is not annotated");
      }
    }
  }

  @Test
  public void checkGetNoFunctionImportForJavaBasedFunction() throws ODataJPAModelException {
    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlFunctionImport> funcImports = container.getEdmItem().getFunctionImports();
    for (CsdlFunctionImport funcImport : funcImports) {
      if ("Sum".equals(funcImport.getName()))
        return;
      System.out.println(funcImport.getName());
    }
    fail("Import not found");
  }

  @Test
  public void checkGetFunctionImportIfUnBoundHasImportTrue() throws ODataJPAModelException {

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    List<CsdlFunctionImport> funcImports = container.getEdmItem().getFunctionImports();
    for (CsdlFunctionImport funcImport : funcImports) {
      if (funcImport.getName().equals("Olingo V4 ")) {
        fail("UnBound function must be generate a function import is annotated");
      }
    }
  }

  @Test
  public void checkAnnotationSet() throws ODataJPAModelException {
    IntermediateModelElement.setPostProcessor(new PostProcessorSetIgnore());
    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);
    List<CsdlAnnotation> act = container.getEdmItem().getAnnotations();
    assertEquals(1, act.size());
    assertEquals("Capabilities.AsynchronousRequestsSupported", act.get(0).getTerm());
  }

  @Test
  public void checkReturnEntitySetBasedOnInternalEntityType() throws ODataJPAModelException {

    IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
        "BestOrganization"), schema);

    IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), schemas);

    JPAElement act = container.getEntitySet(et);
    assertNotNull(act);
    assertEquals("BestOrganizations", act.getExternalName());

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
    public void processEntityType(IntermediateEntityTypeAccess entity) {}

    @Override
    public void provideReferences(IntermediateReferenceList references) throws ODataJPAModelException {}

    @Override
    public void processEntityContainer(IntermediateEntityContainerAccess container) {

      CsdlConstantExpression mimeType = new CsdlConstantExpression(ConstantExpressionType.Bool, "false");
      CsdlAnnotation annotation = new CsdlAnnotation();
      annotation.setExpression(mimeType);
      annotation.setTerm("Capabilities.AsynchronousRequestsSupported");
      List<CsdlAnnotation> annotations = new ArrayList<>();
      annotations.add(annotation);
      container.addAnnotations(annotations);
    }
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