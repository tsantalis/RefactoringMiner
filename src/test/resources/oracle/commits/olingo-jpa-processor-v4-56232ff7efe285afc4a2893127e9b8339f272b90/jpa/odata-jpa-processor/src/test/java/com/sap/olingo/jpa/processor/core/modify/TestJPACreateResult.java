package com.sap.olingo.jpa.processor.core.modify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.converter.JPACollectionResult;
import com.sap.olingo.jpa.processor.core.converter.JPAExpandResult;
import com.sap.olingo.jpa.processor.core.converter.JPATupleChildConverter;
import com.sap.olingo.jpa.processor.core.util.ServiceMetadataDouble;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public abstract class TestJPACreateResult extends TestBase {

  protected JPAExpandResult cut;
  protected JPAEntityType et;
  protected Map<String, List<String>> headers;
  protected Object jpaEntity;
  protected JPATupleChildConverter converter;

  public TestJPACreateResult() {
    super();
  }

  @Test
  public void testGetChildrenProvidesEmptyMap() throws ODataJPAModelException, ODataApplicationException {
    converter = new JPATupleChildConverter(helper.sd, OData.newInstance()
        .createUriHelper(), new ServiceMetadataDouble(nameBuilder, "Organizations"));

    createCutProvidesEmptyMap();

    Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();

    assertNotNull(act);
    assertEquals(1, act.size());
  }

  @Test
  public void testGetResultSimpleEntity() throws ODataJPAModelException, ODataApplicationException {
    et = helper.getJPAEntityType("BusinessPartnerRoles");

    createCutGetResultSimpleEntity();

    List<Tuple> act = cut.getResult("root");

    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals("34", act.get(0).get("BusinessPartnerID"));
  }

  @Test
  public void testGetResultWithOneLevelEmbedded() throws ODataJPAModelException, ODataApplicationException {
    et = helper.getJPAEntityType("AdministrativeDivisionDescriptions");

    createCutGetResultWithOneLevelEmbedded();

    List<Tuple> act = cut.getResult("root");

    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals("A", act.get(0).get("CodeID"));
    assertEquals("Hugo", act.get(0).get("Name"));
  }

  @Test
  public void testGetResultWithTwoLevelEmbedded() throws ODataJPAModelException, ODataApplicationException {

    createCutGetResultWithTwoLevelEmbedded();

    List<Tuple> act = cut.getResult("root");
    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals("01", act.get(0).get("ID"));
    assertEquals("99", act.get(0).get("AdministrativeInformation/Created/By"));
  }

  @Test
  public void testGetResultWithOneLinked() throws ODataJPAModelException, ODataApplicationException {
    createCutGetResultWithWithOneLinked();
    Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();
    assertNotNull(act);
    assertEquals(1, act.size());
    for (JPAAssociationPath actPath : act.keySet()) {
      assertEquals("Children", actPath.getAlias());
      List<Tuple> subResult = act.get(actPath).getResult("Eurostat/NUTS1/BE2");
      assertEquals(1, subResult.size());
    }
  }

  @Test
  public void testGetResultWithTwoLinked() throws ODataJPAModelException, ODataApplicationException {
    createCutGetResultWithWithTwoLinked();
    Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();
    assertNotNull(act);
    assertEquals(1, act.size());
    for (JPAAssociationPath actPath : act.keySet()) {
      assertEquals("Children", actPath.getAlias());
      List<Tuple> subResult = act.get(actPath).getResult("Eurostat/NUTS1/BE2");
      assertEquals(2, subResult.size());
    }
  }

  @Test
  public void testGetResultWithPrimitiveCollection() throws ODataJPAModelException, ODataApplicationException {
    createCutGetResultEntityWithSimpleCollection();

    final Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();
    assertDoesNotContain(cut.getResult("root"), "Comment");
    assertNotNull(act);
    assertFalse(act.isEmpty());
    for (Entry<JPAAssociationPath, JPAExpandResult> entity : act.entrySet()) {
      assertEquals(1, entity.getValue().getResults().size());
      assertEquals("Comment", entity.getKey().getAlias());
      final Collection<Object> actConverted = ((JPACollectionResult) entity.getValue()).getPropertyCollection(
          JPAExpandResult.ROOT_RESULT_KEY);
      assertEquals(2, actConverted.size());
      for (Object o : actConverted) {
        assertNotNull(o);
        assertFalse(((String) o).isEmpty());
      }
    }
  }

  @Test
  public void testGetResultWithComplexCollection() throws ODataJPAModelException, ODataApplicationException {
    createCutGetResultEntityWithComplexCollection();

    Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();
    assertDoesNotContain(cut.getResult("root"), "InhouseAddress");
    assertNotNull(act);
    assertFalse(act.isEmpty());
    for (Entry<JPAAssociationPath, JPAExpandResult> entity : act.entrySet()) {
      assertEquals(1, entity.getValue().getResults().size());
      assertEquals("InhouseAddress", entity.getKey().getAlias());
      final Collection<Object> actConverted = ((JPACollectionResult) entity.getValue()).getPropertyCollection(
          JPAExpandResult.ROOT_RESULT_KEY);
      assertEquals(2, actConverted.size());
      for (Object o : actConverted) {
        assertNotNull(o);
        assertFalse(((ComplexValue) o).getValue().isEmpty());
      }
    }
  }

  @Test
  public void testGetResultWithComplexContainingCollection() throws ODataJPAModelException, ODataApplicationException {
    createCutGetResultEntityWithComplexWithCollection();

    final Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();
    boolean found = false;
    assertDoesNotContain(cut.getResult("root"), "Complex/Address");
    assertNotNull(act);
    assertFalse(act.isEmpty());
    for (Entry<JPAAssociationPath, JPAExpandResult> entity : act.entrySet()) {
      if (entity.getKey().getAlias().equals("Complex/Address")) {
        found = true;
        assertEquals(1, entity.getValue().getResults().size());
        assertEquals("Complex/Address", entity.getKey().getAlias());
        final Collection<Object> actConverted = ((JPACollectionResult) entity.getValue()).getPropertyCollection(
            JPAExpandResult.ROOT_RESULT_KEY);
        assertEquals(2, actConverted.size());
        for (Object o : actConverted) {
          assertNotNull(o);
          assertFalse(((ComplexValue) o).getValue().isEmpty());
        }
      }
    }
    assertTrue(found);
  }

  @Test
  public void testGetResultWithContainingNestedComplexCollection() throws ODataJPAModelException,
      ODataApplicationException {
    createCutGetResultEntityWithNestedComplexCollection();

    final Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();
    boolean found = false;
    assertDoesNotContain(cut.getResult("root"), "Nested");
    assertNotNull(act);
    assertFalse(act.isEmpty());
    for (Entry<JPAAssociationPath, JPAExpandResult> entity : act.entrySet()) {
      if (entity.getKey().getAlias().equals("Nested")) {
        found = true;
        assertEquals(1, entity.getValue().getResults().size());
        assertEquals("Nested", entity.getKey().getAlias());
        final Collection<Object> actConverted = ((JPACollectionResult) entity.getValue()).getPropertyCollection(
            JPAExpandResult.ROOT_RESULT_KEY);
        assertEquals(2, actConverted.size());
        for (Object o : actConverted) {
          assertNotNull(o);
          assertFalse(((ComplexValue) o).getValue().isEmpty());
        }
      }
    }
    assertTrue(found);
  }

  @Test
  public void testGetResultWithDeepComplexContainingCollection() throws ODataJPAModelException,
      ODataApplicationException {
    createCutGetResultEntityWithDeepComplexWithCollection();

    final Map<JPAAssociationPath, JPAExpandResult> act = cut.getChildren();
    boolean found = false;
    assertDoesNotContain(cut.getResult("root"), "FirstLevel/SecondLevel/Address");
    assertNotNull(act);
    assertFalse(act.isEmpty());
    for (Entry<JPAAssociationPath, JPAExpandResult> entity : act.entrySet()) {
      if (entity.getKey().getAlias().equals("FirstLevel/SecondLevel/Address")) {
        found = true;
        assertEquals(1, entity.getValue().getResults().size());
        assertEquals("FirstLevel/SecondLevel/Address", entity.getKey().getAlias());
        final Collection<Object> actConverted = ((JPACollectionResult) entity.getValue()).getPropertyCollection(
            JPAExpandResult.ROOT_RESULT_KEY);
        assertEquals(2, actConverted.size());
        for (Object o : actConverted) {
          assertNotNull(o);
          assertFalse(((ComplexValue) o).getValue().isEmpty());
        }
      }
    }
    assertTrue(found);
  }

  private void assertDoesNotContain(final List<Tuple> result, final String prefix) {
    for (Tuple t : result) {
      for (TupleElement<?> e : t.getElements())
        assertFalse(e.getAlias() + " violates prefix check: " + prefix, e.getAlias().startsWith(prefix));
    }

  }

  protected abstract void createCutProvidesEmptyMap() throws ODataJPAModelException, ODataApplicationException;

  protected abstract void createCutGetResultEntityWithDeepComplexWithCollection() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultEntityWithNestedComplexCollection() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultEntityWithComplexCollection() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultWithWithTwoLinked() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultWithWithOneLinked() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultSimpleEntity() throws ODataJPAModelException, ODataApplicationException;

  protected abstract void createCutGetResultWithOneLevelEmbedded() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultWithTwoLevelEmbedded() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultEntityWithSimpleCollection() throws ODataJPAModelException,
      ODataApplicationException;

  protected abstract void createCutGetResultEntityWithComplexWithCollection() throws ODataJPAModelException,
      ODataApplicationException;
}