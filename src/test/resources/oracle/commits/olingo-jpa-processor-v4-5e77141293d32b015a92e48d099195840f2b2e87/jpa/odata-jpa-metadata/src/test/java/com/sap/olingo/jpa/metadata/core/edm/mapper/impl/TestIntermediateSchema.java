package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import com.sap.olingo.jpa.metadata.core.edm.annotation.EdmEnumeration;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.testmodel.ABCClassifiaction;
import com.sap.olingo.jpa.processor.core.testmodel.AccessRights;
import com.sap.olingo.jpa.processor.core.testmodel.TestDataConstants;

public class TestIntermediateSchema extends TestMappingRoot {
  private Reflections r;

  @BeforeEach
  public void setup() {
    r = mock(Reflections.class);
    when(r.getTypesAnnotatedWith(EdmEnumeration.class)).thenReturn(new HashSet<>(Arrays.asList(new Class<?>[] {
        ABCClassifiaction.class, AccessRights.class })));
  }

  @Test
  public void checkSchemaCanBeCreated() throws ODataJPAModelException {

    new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
  }

  @Test
  public void checkSchemaGetAllEntityTypes() throws ODataJPAModelException {
    IntermediateSchema schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    assertEquals(TestDataConstants.NO_ENTITY_TYPES, schema.getEdmItem().getEntityTypes().size(),
        "Wrong number of entities");
  }

  @Test
  public void checkSchemaGetEntityTypeByNameNotNull() throws ODataJPAModelException {
    IntermediateSchema schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    assertNotNull(schema.getEdmItem().getEntityType("BusinessPartner"));
  }

  @Test
  public void checkSchemaGetEntityTypeByNameRightEntity() throws ODataJPAModelException {
    IntermediateSchema schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    assertEquals("BusinessPartner", schema.getEdmItem().getEntityType("BusinessPartner").getName());
  }

  @Test
  public void checkSchemaGetAllComplexTypes() throws ODataJPAModelException {
    IntermediateSchema schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    // ChangeInformation,CommunicationData,AdministrativeInformation,PostalAddressData
    assertEquals(15, schema.getEdmItem().getComplexTypes().size(), "Wrong number of complex types");
  }

  @Test
  public void checkSchemaGetComplexTypeByNameNotNull() throws ODataJPAModelException {
    IntermediateSchema schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    assertNotNull(schema.getEdmItem().getComplexType("CommunicationData"));
  }

  @Test
  public void checkSchemaGetComplexTypeByNameRightEntity() throws ODataJPAModelException {
    IntermediateSchema schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    assertEquals("CommunicationData", schema.getEdmItem().getComplexType("CommunicationData").getName());
  }

  @Test
  public void checkSchemaGetAllFunctions() throws ODataJPAModelException {
    IntermediateSchema schema = new IntermediateSchema(new JPAEdmNameBuilder(PUNIT_NAME), emf.getMetamodel(), r);
    assertEquals(10, schema.getEdmItem().getFunctions().size(), "Wrong number of entities");
  }
}
