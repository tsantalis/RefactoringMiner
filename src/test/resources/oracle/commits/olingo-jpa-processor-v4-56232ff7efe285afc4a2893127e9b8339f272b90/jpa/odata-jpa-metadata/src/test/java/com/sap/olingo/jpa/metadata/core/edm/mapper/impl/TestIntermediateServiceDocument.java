package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAServiceDocument;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public class TestIntermediateServiceDocument extends TestMappingRoot {

  @Test
  public void checkServiceDocumentCanBeCreated() throws ODataJPAModelException {
    new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null,
        new String[] { "com.sap.olingo.jpa.processor.core.testmodel" });
  }

  @Test
  public void checkServiceDocumentGetSchemaList() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null,
        new String[] { "com.sap.olingo.jpa.processor.core.testmodel" });
    assertEquals("Wrong number of schemas", 1, svc.getEdmSchemas().size());
  }

  @Test
  public void checkServiceDocumentGetContainer() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null,
        new String[] { "com.sap.olingo.jpa.processor.core.testmodel" });
    assertNotNull("Entity Container not found", svc.getEdmEntityContainer());
  }

  @Test
  public void checkServiceDocumentGetContainerFromSchema() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null,
        new String[] { "com.sap.olingo.jpa.processor.core.testmodel" });
    List<CsdlSchema> schemas = svc.getEdmSchemas();
    CsdlSchema schema = schemas.get(0);
    assertNotNull("Entity Container not found", schema.getEntityContainer());
  }

  @Test
  public void checkServiceDocumentGetEntitySetsFromContainer() throws ODataJPAModelException {
    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null,
        new String[] { "com.sap.olingo.jpa.processor.core.testmodel" });
    CsdlEntityContainer container = svc.getEdmEntityContainer();
    assertNotNull("Entity Container not found", container.getEntitySets());
  }

  @Test
  public void checkHasEtagReturnsTrueOnVersion() throws ODataJPAModelException {
    EdmBindingTarget target = mock(EdmBindingTarget.class);
    EdmEntityType et = mock(EdmEntityType.class);
    when(target.getEntityType()).thenReturn(et);
    when(et.getFullQualifiedName()).thenReturn(new FullQualifiedName(PUNIT_NAME, "BusinessPartner"));

    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null,
        new String[] { "com.sap.olingo.jpa.processor.core.testmodel" });
    assertTrue(svc.hasETag(target));
  }

  @Test
  public void checkHasEtagReturnsFalseWithoutVersion() throws ODataJPAModelException {
    EdmBindingTarget target = mock(EdmBindingTarget.class);
    EdmEntityType et = mock(EdmEntityType.class);
    when(target.getEntityType()).thenReturn(et);
    when(et.getFullQualifiedName()).thenReturn(new FullQualifiedName(PUNIT_NAME, "Country"));

    JPAServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME, emf.getMetamodel(), null, null);
    assertFalse(svc.hasETag(target));
  }
}
