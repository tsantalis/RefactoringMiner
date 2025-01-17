package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;

public class TestJPAEdmNameBuilder {
  private JPAEdmNameBuilder cut;

  @Before
  public void setup() throws ODataJPAModelException {

  }

  @Test
  public void CheckBuildContainerNameSimple() {
    cut = new JPAEdmNameBuilder("cdw");
    assertEquals("CdwContainer", cut.buildContainerName());
  }

  @Test
  public void CheckBuildContainerNameComplex() {
    cut = new JPAEdmNameBuilder("org.apache.olingo.jpa");
    assertEquals("OrgApacheOlingoJpaContainer", cut.buildContainerName());
  }
}
