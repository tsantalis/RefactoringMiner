package com.sap.olingo.jpa.processor.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
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
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivisionDescription;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivisionDescriptionKey;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartner;
import com.sap.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;
import com.sap.olingo.jpa.processor.core.testmodel.Person;

public class TestCriteriaBuilder {
  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";
  private static final String ENTITY_MANAGER_DATA_SOURCE = "javax.persistence.nonJtaDataSource";
  private static EntityManagerFactory emf;
  private EntityManager em;
  private CriteriaBuilder cb;

  @BeforeClass
  public static void setupClass() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ENTITY_MANAGER_DATA_SOURCE, DataSourceHelper.createDataSource(
        DataSourceHelper.DB_HSQLDB));
    emf = Persistence.createEntityManagerFactory(PUNIT_NAME, properties);
  }

  @Before
  public void setup() {
    em = emf.createEntityManager();
    cb = em.getCriteriaBuilder();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSubstringWithExperession() {
    CriteriaQuery<Tuple> adminQ = cb.createTupleQuery();
    Root<AdministrativeDivisionDescription> adminRoot1 = adminQ.from(AdministrativeDivisionDescription.class);
//    (Expression<T>) cb.sum(jpaOperator.getLeft(), jpaOperator.getRightAsNumber());
//    cb.substring((Expression<String>) (jpaFunction.getParameter(0).get()), start, length);
    Path<?> p = adminRoot1.get("name");

    Expression<Integer> sum = cb.sum(cb.literal(1), cb.literal(4));

    adminQ.where(cb.equal(cb.substring((Expression<String>) (p), cb.literal(1), sum), "North"));
    adminQ.multiselect(adminRoot1.get("name"));
    TypedQuery<Tuple> tq = em.createQuery(adminQ);
    tq.getResultList();
  }

  @Ignore // To time consuming
  @Test
  public void testSubSelect() {
    // https://stackoverflow.com/questions/29719321/combining-conditional-expressions-with-and-and-or-predicates-using-the-jpa-c
    CriteriaQuery<Tuple> adminQ1 = cb.createTupleQuery();
    Subquery<Long> adminQ2 = adminQ1.subquery(Long.class);
    Subquery<Long> adminQ3 = adminQ2.subquery(Long.class);
    Subquery<Long> org = adminQ3.subquery(Long.class);

    Root<AdministrativeDivision> adminRoot1 = adminQ1.from(AdministrativeDivision.class);
    Root<AdministrativeDivision> adminRoot2 = adminQ2.from(AdministrativeDivision.class);
    Root<AdministrativeDivision> adminRoot3 = adminQ3.from(AdministrativeDivision.class);
    Root<Organization> org1 = org.from(Organization.class);

    org.where(cb.and(cb.equal(org1.get("iD"), "3")), createParentOrg(org1, adminRoot3));
    org.select(cb.literal(1L));

    adminQ3.where(cb.and(createParentAdmin(adminRoot3, adminRoot2), cb.exists(org)));
    adminQ3.select(cb.literal(1L));

    adminQ2.where(cb.and(createParentAdmin(adminRoot2, adminRoot1), cb.exists(adminQ3)));
    adminQ2.select(cb.literal(1L));

    adminQ1.where(cb.exists(adminQ2));
    adminQ1.multiselect(adminRoot1.get("divisionCode"));

    TypedQuery<Tuple> tq = em.createQuery(adminQ1);
    tq.getResultList();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSubSelectTopOrderBy() {
    // https://stackoverflow.com/questions/9321916/jpa-criteriabuilder-how-to-use-in-comparison-operator
    // https://stackoverflow.com/questions/24109412/in-clause-with-a-composite-primary-key-in-jpa-criteria#24265131
    CriteriaQuery<Tuple> roleQ = cb.createTupleQuery();
    Root<BusinessPartnerRole> roleRoot = roleQ.from(BusinessPartnerRole.class);

    Subquery<BusinessPartner> bupaQ = roleQ.subquery(BusinessPartner.class);
    @SuppressWarnings("rawtypes")
    Root bupaRoot = roleQ.from(BusinessPartner.class);

    bupaQ.select(bupaRoot.get("iD"));
//    Expression<String> exp = scheduleRequest.get("createdBy");
//    Predicate predicate = exp.in(myList);
//    criteria.where(predicate);

    List<String> ids = new ArrayList<>();
    ids.add("1");
    ids.add("2");
    bupaQ.where(bupaRoot.get("iD").in(ids));
//    bupaQ.select(
//        (Expression<BusinessPartner>) cb.construct(
//            BusinessPartner.class,
//            bupaRoot.get("ID")));

    // roleQ.where(cb.in(roleRoot.get("businessPartnerID")).value(bupaQ));
    roleQ.where(cb.in(roleRoot.get("businessPartnerID")).value(bupaQ));
    roleQ.multiselect(roleRoot.get("businessPartnerID"));
    TypedQuery<Tuple> tq = em.createQuery(roleQ);
    tq.getResultList();
  }

  @Test
  public void testFilterOnPrimitiveCollectionAttribute() {
    CriteriaQuery<Tuple> orgQ = cb.createTupleQuery();
    Root<Organization> orgRoot = orgQ.from(Organization.class);
    orgQ.select(orgRoot.get("iD"));
    orgQ.where(cb.like(orgRoot.get("comment"), "%just%"));
    TypedQuery<Tuple> tq = em.createQuery(orgQ);
    List<Tuple> act = tq.getResultList();
    assertEquals(1, act.size());
  }

  @Test
  public void testFilterOnEmbeddedCollectionAttribute() {
    CriteriaQuery<Tuple> pQ = cb.createTupleQuery();
    Root<Person> pRoot = pQ.from(Person.class);
    pQ.select(pRoot.get("iD"));
    pQ.where(cb.equal(pRoot.get("inhouseAddress").get("taskID"), "MAIN"));
    TypedQuery<Tuple> tq = em.createQuery(pQ);
    List<Tuple> act = tq.getResultList();
    assertEquals(1, act.size());
  }

  @Test
  public void TestExpandCount() {
    CriteriaQuery<Tuple> count = cb.createTupleQuery();
    Root<?> roles = count.from(BusinessPartnerRole.class);

    count.multiselect(roles.get("businessPartnerID"), cb.count(roles).alias("$count"));
    count.groupBy(roles.get("businessPartnerID"));
    count.orderBy(cb.desc(cb.count(roles)));
    TypedQuery<Tuple> tq = em.createQuery(count);
    List<Tuple> act = tq.getResultList();
    tq.getFirstResult();
  }

  @Test
  public void TestAnd() {
    CriteriaQuery<Tuple> count = cb.createTupleQuery();
    Root<?> adminDiv = count.from(AdministrativeDivision.class);

    count.multiselect(adminDiv);
    Predicate[] restrictions = new Predicate[3];
    restrictions[0] = cb.equal(adminDiv.get("codeID"), "NUTS2");
    restrictions[1] = cb.equal(adminDiv.get("divisionCode"), "BE34");
    restrictions[2] = cb.equal(adminDiv.get("codePublisher"), "Eurostat");
    count.where(cb.and(restrictions));
    TypedQuery<Tuple> tq = em.createQuery(count);
    List<Tuple> act = tq.getResultList();
    tq.getFirstResult();
  }

  @Ignore
  @Test
  public void TestSearchEmbeddedId() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<?> adminDiv = cq.from(AdministrativeDivisionDescription.class);
    cq.multiselect(adminDiv);

    Subquery<AdministrativeDivisionDescriptionKey> sq = cq.subquery(AdministrativeDivisionDescriptionKey.class);
    Root<AdministrativeDivisionDescription> text = sq.from(AdministrativeDivisionDescription.class);
    sq.where(cb.function("CONTAINS", Boolean.class, text.get("name"), cb.literal("luettich")));
    Expression<AdministrativeDivisionDescriptionKey> exp = text.get("key");
    sq.select(exp);

    cq.where(cb.and(cb.equal(adminDiv.get("key").get("codeID"), "NUTS2"),
        cb.in(sq).value(sq)));
    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> act = tq.getResultList();
    System.out.println(act.size());
  }

  @Ignore
  @Test
  public void TestSearchNoSubquery() {
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<?> adminDiv = cq.from(AdministrativeDivisionDescription.class);
    cq.multiselect(adminDiv);

    // Predicate[] restrictions = new Predicate[2];
    cq.where(
        cb.and(cb.equal(cb.conjunction(),
            cb.function("CONTAINS", Boolean.class, adminDiv.get("name"), cb.literal("luettich"))),
            cb.equal(adminDiv.get("key").get("codeID"), "NUTS2")));

    TypedQuery<Tuple> tq = em.createQuery(cq);
    List<Tuple> act = tq.getResultList();
    System.out.println(act.size());
  }

  private Expression<Boolean> createParentAdmin(Root<AdministrativeDivision> subQuery,
      Root<AdministrativeDivision> query) {
    return cb.and(cb.equal(query.get("codePublisher"), subQuery.get("codePublisher")),
        cb.and(cb.equal(query.get("codeID"), subQuery.get("parentCodeID")),
            cb.equal(query.get("divisionCode"), subQuery.get("parentDivisionCode"))));
  }

  private Predicate createParentOrg(Root<Organization> org1, Root<AdministrativeDivision> adminRoot3) {
    return cb.and(cb.equal(adminRoot3.get("codePublisher"), org1.get("address").get("regionCodePublisher")),
        cb.and(cb.equal(adminRoot3.get("codeID"), org1.get("address").get("regionCodeID")),
            cb.equal(adminRoot3.get("divisionCode"), org1.get("address").get("region"))));
  }
}