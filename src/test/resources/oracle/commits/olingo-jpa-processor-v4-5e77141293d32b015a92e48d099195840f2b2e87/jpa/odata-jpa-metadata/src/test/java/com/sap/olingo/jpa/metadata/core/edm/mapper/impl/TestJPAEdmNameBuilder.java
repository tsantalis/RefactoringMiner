package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public class TestJPAEdmNameBuilder {
  private JPAEdmNameBuilder cut;

  @BeforeEach
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
