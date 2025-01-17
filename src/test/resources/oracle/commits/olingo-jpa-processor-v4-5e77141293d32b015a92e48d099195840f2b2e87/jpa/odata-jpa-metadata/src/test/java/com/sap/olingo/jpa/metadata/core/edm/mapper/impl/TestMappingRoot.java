package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import javax.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeAll;

import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;

public class TestMappingRoot {
  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";
  protected static EntityManagerFactory emf;
  public static final String BUPA_CANONICAL_NAME = "com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner";
  public static final String ORG_CANONICAL_NAME = "com.sap.olingo.jpa.processor.core.testmodel.Organization";
  public static final String ADDR_CANONICAL_NAME = "com.sap.olingo.jpa.processor.core.testmodel.PostalAddressData";
  public static final String COMM_CANONICAL_NAME = "com.sap.olingo.jpa.processor.core.testmodel.CommunicationData";
  public static final String ADMIN_CANONICAL_NAME =
      "com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision";

  @BeforeAll
  public static void setupClass() {
    emf = JPAEntityManagerFactory.getEntityManagerFactory(PUNIT_NAME, DataSourceHelper.createDataSource(
        DataSourceHelper.DB_HSQLDB));
  }
}