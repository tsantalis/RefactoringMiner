package com.sap.olingo.jpa.processor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivisionDescription;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import com.sap.olingo.jpa.processor.core.testmodel.Country;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;

public class TestAssociations {
  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";
  private static final String ENTITY_MANAGER_DATA_SOURCE = "javax.persistence.nonJtaDataSource";
  private static EntityManagerFactory emf;
  private EntityManager em;
  private CriteriaBuilder cb;

  @BeforeAll
  public static void setupClass() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ENTITY_MANAGER_DATA_SOURCE, DataSourceHelper.createDataSource(
        DataSourceHelper.DB_HSQLDB));
    emf = Persistence.createEntityManagerFactory(PUNIT_NAME, properties);
  }

  @BeforeEach
  public void setup() {
    em = emf.createEntityManager();
    cb = em.getCriteriaBuilder();
  }

  @Test
  public void getBuPaRoles() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<BusinessPartner> root = cq.from(BusinessPartner.class);

    cq.multiselect(root.get("roles").alias("roles"));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    BusinessPartnerRole role = (BusinessPartnerRole) result.get(0).get("roles");
    assertNotNull(role);
  }

  @Test
  public void getBuPaLocation() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<BusinessPartner> root = cq.from(BusinessPartner.class);

    cq.multiselect(root.get("locationName").alias("L"));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    AdministrativeDivisionDescription act = (AdministrativeDivisionDescription) result.get(0).get("L");
    assertNotNull(act);
  }

  @Test
  public void getRoleBuPa() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<BusinessPartnerRole> root = cq.from(BusinessPartnerRole.class);

    cq.multiselect(root.get("businessPartner").alias("BuPa"));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    BusinessPartner bp = (BusinessPartner) result.get(0).get("BuPa");
    assertNotNull(bp);
  }

  @Test
  public void getBuPaCountryName() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<BusinessPartner> root = cq.from(BusinessPartner.class);

    cq.multiselect(root.get("address").get("countryName").alias("CN"));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    Country region = (Country) result.get(0).get("CN");
    assertNotNull(region);
  }

  @Test
  public void getBuPaRegionName() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<BusinessPartner> root = cq.from(BusinessPartner.class);

    cq.multiselect(root.get("address").get("regionName").alias("RN"));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    AdministrativeDivisionDescription region = (AdministrativeDivisionDescription) result.get(0).get("RN");
    assertNotNull(region);
  }

  @Test
  public void getAdministrativeDivisionParent() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<AdministrativeDivision> root = cq.from(AdministrativeDivision.class);

    cq.multiselect(root.get("parent").alias("P"));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    AdministrativeDivision act = (AdministrativeDivision) result.get(0).get("P");
    assertNotNull(act);
  }

  @Test
  public void getAdministrativeDivisionOneParent() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<AdministrativeDivision> root = cq.from(AdministrativeDivision.class);
    root.alias("Source");
    cq.multiselect(root.get("parent").alias("P"));
    // cq.select((Selection<? extends Tuple>) root);
    cq.where(cb.and(
        cb.equal(root.get("codePublisher"), "Eurostat"),
        cb.and(
            cb.equal(root.get("codeID"), "NUTS3"),
            cb.equal(root.get("divisionCode"), "BE251"))));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    AdministrativeDivision act = (AdministrativeDivision) result.get(0).get("P");
    assertNotNull(act);
    assertEquals("NUTS2", act.getCodeID());
    assertEquals("BE25", act.getDivisionCode());
  }

  @Test
  public void getAdministrativeDivisionChildrenOfOneParent() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<AdministrativeDivision> root = cq.from(AdministrativeDivision.class);
    root.alias("Source");
    cq.multiselect(root.get("children").alias("C"));
    cq.where(cb.and(
        cb.equal(root.get("codePublisher"), "Eurostat"),
        cb.and(
            cb.equal(root.get("codeID"), "NUTS2"),
            cb.equal(root.get("divisionCode"), "BE25"))));
    cq.orderBy(cb.desc(root.get("divisionCode")));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> result = tq.getResultList();
    AdministrativeDivision act = (AdministrativeDivision) result.get(0).get("C");
    assertNotNull(act);
    assertEquals(8, result.size());
    assertEquals("NUTS3", act.getCodeID());
    assertEquals("BE251", act.getDivisionCode());
  }
}
