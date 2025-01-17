package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;

public class TestJPAPath extends TestMappingRoot {
  private JPAEntityType organization;
  // private JPAStructuredType postalAddress;
  private TestHelper helper;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
    organization = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), helper.getEntityType(
        Organization.class), helper.schema);
  }

  @Test
  public void checkOnePathElementAlias() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("Name1");
    assertEquals("Name1", cut.getAlias());
  }

  @Test
  public void checkOnePathElementPathSize() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("Name1");
    assertEquals(1, cut.getPath().size());
  }

  @Test
  public void checkOnePathElementElement() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("Name1");
    assertEquals("name1", cut.getPath().get(0).getInternalName());
  }

  @Test
  public void checkOnePathElementFromSuperTypeAlias() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("Type");
    assertEquals("Type", cut.getAlias());
  }

  @Test
  public void checkTwoPathElementAlias() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("Address/Country");
    assertEquals("Address/Country", cut.getAlias());
  }

  @Test
  public void checkTwoPathElementPathSize() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("Address/Country");
    assertEquals(2, cut.getPath().size());
  }

  @Test
  public void checkTwoPathElementPathElements() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("Address/Country");
    assertEquals("address", cut.getPath().get(0).getInternalName());
    assertEquals("country", cut.getPath().get(1).getInternalName());
  }

  @Test
  public void checkThreePathElementAlias() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("AdministrativeInformation/Created/By");
    assertEquals("AdministrativeInformation/Created/By", cut.getAlias());
  }

  @Test
  public void checkThreePathElementPathSize() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("AdministrativeInformation/Created/By");
    assertEquals(3, cut.getPath().size());
  }

  @Test
  public void checkThreePathElementPathElements() throws ODataApplicationException, ODataJPAModelException {
    JPAPath cut = organization.getPath("AdministrativeInformation/Created/By");
    assertEquals("administrativeInformation", cut.getPath().get(0).getInternalName());
    assertEquals("created", cut.getPath().get(1).getInternalName());
    assertEquals("by", cut.getPath().get(2).getInternalName());
  }

}
