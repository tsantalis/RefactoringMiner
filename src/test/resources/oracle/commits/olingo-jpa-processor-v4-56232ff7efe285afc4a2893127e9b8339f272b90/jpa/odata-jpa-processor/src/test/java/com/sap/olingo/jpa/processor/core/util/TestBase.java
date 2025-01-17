package com.sap.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.BeforeClass;

import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;

public class TestBase {

  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";
  public static final String[] enumPackages = { "com.sap.olingo.jpa.processor.core.testmodel" };
  protected static EntityManagerFactory emf;
  protected TestHelper helper;
  protected Map<String, List<String>> headers;
  protected static JPAEdmNameBuilder nameBuilder;
  protected static DataSource ds;

  @BeforeClass
  public static void setupClass() throws ODataJPAModelException {
    ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_DERBY);
    emf = JPAEntityManagerFactory.getEntityManagerFactory(PUNIT_NAME, ds);
    nameBuilder = new JPAEdmNameBuilder(PUNIT_NAME);
  }

  protected void createHeaders() {
    headers = new HashMap<>();
    List<String> languageHeaders = new ArrayList<>();
    languageHeaders.add("de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");
    headers.put("accept-language", languageHeaders);
  }
}