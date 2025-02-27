package com.sap.olingo.jpa.processor.core.modify;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Before;

import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.converter.JPATupleChildConverter;
import com.sap.olingo.jpa.processor.core.processor.JPARequestEntity;
import com.sap.olingo.jpa.processor.core.util.ServiceMetadataDouble;
import com.sap.olingo.jpa.processor.core.util.TestHelper;

public class TestJPAMapResult extends TestJPACreateResult {
  List<JPARequestEntity> children;

  @Before
  public void setUp() throws Exception {
    headers = new HashMap<>();
    jpaEntity = new HashMap<String, Object>();
    helper = new TestHelper(emf, PUNIT_NAME);
    et = helper.getJPAEntityType("Organizations");
    children = new ArrayList<>();
    converter = new JPATupleChildConverter(helper.sd, OData.newInstance()
        .createUriHelper(), new ServiceMetadataDouble(nameBuilder, "Organizations"));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutProvidesEmptyMap() throws ODataJPAModelException, ODataApplicationException {
    // Make Map equal to empty Organization instance
    ((Map<String, Object>) jpaEntity).put("type", "2");
    ((Map<String, Object>) jpaEntity).put("comment", new ArrayList<String>(1));
    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultSimpleEntity() throws ODataJPAModelException, ODataApplicationException {

    ((Map<String, Object>) jpaEntity).put("businessPartnerID", "34");
    ((Map<String, Object>) jpaEntity).put("roleCategory", "A");
    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);

  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultWithOneLevelEmbedded() throws ODataJPAModelException, ODataApplicationException {

    Map<String, Object> key = new HashMap<>();
    key.put("codeID", "A");
    key.put("language", "en");

    ((Map<String, Object>) jpaEntity).put("name", "Hugo");
    ((Map<String, Object>) jpaEntity).put("key", key);

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);

  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultWithTwoLevelEmbedded() throws ODataJPAModelException,
      ODataApplicationException {

    long time = new Date().getTime();
    Map<String, Object> created = new HashMap<>();
    created.put("by", "99");
    created.put("at", new Timestamp(time));

    Map<String, Object> admin = new HashMap<>();
    admin.put("created", created);
    admin.put("updated", created);

    ((Map<String, Object>) jpaEntity).put("iD", "01");
    ((Map<String, Object>) jpaEntity).put("customString1", "Dummy");
    ((Map<String, Object>) jpaEntity).put("administrativeInformation", admin);

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultWithWithOneLinked() throws ODataJPAModelException, ODataApplicationException {
    prepareAdminWithChildren();

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultWithWithTwoLinked() throws ODataJPAModelException, ODataApplicationException {
    prepareAdminWithChildren();

    Map<String, Object> childProperties = new HashMap<>();
    JPARequestEntity child = mock(JPARequestEntity.class);
    when(child.getEntityType()).thenReturn(et);
    when(child.getData()).thenReturn(childProperties);
    childProperties.put("codeID", "NUTS2");
    childProperties.put("divisionCode", "BE22");
    childProperties.put("codePublisher", "Eurostat");
    children.add(child);

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  private void prepareAdminWithChildren() throws ODataJPAModelException, ODataApplicationException {
    et = helper.getJPAEntityType("AdministrativeDivisions");

    ((Map<String, Object>) jpaEntity).put("codeID", "NUTS1");
    ((Map<String, Object>) jpaEntity).put("divisionCode", "BE2");
    ((Map<String, Object>) jpaEntity).put("codePublisher", "Eurostat");

    Map<String, Object> childProperties = new HashMap<>();

    JPARequestEntity child = mock(JPARequestEntity.class);
    when(child.getEntityType()).thenReturn(et);
    when(child.getData()).thenReturn(childProperties);
    childProperties.put("codeID", "NUTS2");
    childProperties.put("divisionCode", "BE21");
    childProperties.put("codePublisher", "Eurostat");
    children.add(child);
    ((Map<String, Object>) jpaEntity).put("children", children);

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultEntityWithSimpleCollection() throws ODataJPAModelException,
      ODataApplicationException {

    ((Map<String, Object>) jpaEntity).put("iD", "1");
    ((Map<String, Object>) jpaEntity).put("comment", Arrays.asList("First", "Second"));

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultEntityWithComplexCollection() throws ODataJPAModelException,
      ODataApplicationException {
    et = helper.getJPAEntityType("Persons");

    ((Map<String, Object>) jpaEntity).put("iD", "1");
    final Map<String, Object> addr1 = new HashMap<>();
    final Map<String, Object> addr2 = new HashMap<>();

    addr1.put("building", "A");
    addr1.put("taskID", "DEV");
    addr2.put("building", "C");
    addr2.put("taskID", "MAIN");
    ((Map<String, Object>) jpaEntity).put("inhouseAddress", Arrays.asList(addr1, addr2));

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultEntityWithComplexWithCollection() throws ODataJPAModelException,
      ODataApplicationException {

    et = helper.getJPAEntityType("Collections");
    final Map<String, Object> complex = new HashMap<>();

    ((Map<String, Object>) jpaEntity).put("iD", "1");
    ((Map<String, Object>) jpaEntity).put("complex", complex);
    complex.put("number", 2L);

    final Map<String, Object> addr1 = new HashMap<>();
    final Map<String, Object> addr2 = new HashMap<>();

    addr1.put("building", "A");
    addr1.put("taskID", "DEV");
    addr2.put("building", "C");
    addr2.put("taskID", "MAIN");
    complex.put("address", Arrays.asList(addr1, addr2));

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultEntityWithNestedComplexCollection() throws ODataJPAModelException,
      ODataApplicationException {
    et = helper.getJPAEntityType("Collections");

    ((Map<String, Object>) jpaEntity).put("iD", "1");

    final Map<String, Object> nested1 = new HashMap<>();
    final Map<String, Object> nested2 = new HashMap<>();
    final Map<String, Object> inner1 = new HashMap<>();
    final Map<String, Object> inner2 = new HashMap<>();

    nested1.put("inner", inner1);
    nested2.put("inner", inner2);
    nested1.put("number", 100L);
    nested1.put("number", 200L);

    inner1.put("figure1", 1L);
    inner1.put("figure3", 3L);
    inner2.put("figure1", 11L);
    inner2.put("figure3", 13L);
    ((Map<String, Object>) jpaEntity).put("nested", Arrays.asList(nested1, nested2));

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void createCutGetResultEntityWithDeepComplexWithCollection() throws ODataJPAModelException,
      ODataApplicationException {

    et = helper.getJPAEntityType("CollectionDeeps");
    final Map<String, Object> firstLevel = new HashMap<>();
    final Map<String, Object> secondLevel = new HashMap<>();

    ((Map<String, Object>) jpaEntity).put("iD", "1");
    ((Map<String, Object>) jpaEntity).put("firstLevel", firstLevel);
    firstLevel.put("levelID", 2);
    firstLevel.put("secondLevel", secondLevel);
    secondLevel.put("number", 2L);
    final Map<String, Object> addr1 = new HashMap<>();
    final Map<String, Object> addr2 = new HashMap<>();

    addr1.put("building", "A");
    addr1.put("taskID", "DEV");
    addr2.put("building", "C");
    addr2.put("taskID", "MAIN");
    secondLevel.put("address", Arrays.asList(addr1, addr2));

    cut = new JPAMapResult(et, (Map<String, Object>) jpaEntity, headers, converter);
  }
}
