package com.sap.olingo.jpa.processor.core.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.api.JPAODataRequestContextAccess;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import com.sap.olingo.jpa.processor.core.modify.JPAConversionHelper;
import com.sap.olingo.jpa.processor.core.serializer.JPASerializer;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestCreateRequestEntity {
  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";
  protected static EntityManagerFactory emf;
  protected static JPAEdmProvider jpaEdm;
  protected static DataSource ds;

  @BeforeClass
  public static void setupClass() throws ODataException {
    JPAEdmMetadataPostProcessor pP = mock(JPAEdmMetadataPostProcessor.class);

    ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB);
    emf = JPAEntityManagerFactory.getEntityManagerFactory(PUNIT_NAME, ds);
    jpaEdm = new JPAEdmProvider(PUNIT_NAME, emf.getMetamodel(), pP, TestBase.enumPackages);

  }

  private OData odata;
  private JPACUDRequestProcessor cut;
  private Entity oDataEntity;
  private ServiceMetadata serviceMetadata;
  private JPAODataSessionContextAccess sessionContext;
  private JPAODataRequestContextAccess requestContext;
  private UriInfo uriInfo;
  private UriResourceEntitySet uriEts;
  private EntityManager em;
  private EntityTransaction transaction;
  private JPASerializer serializer;
  private EdmEntitySet ets;
  private List<UriParameter> keyPredicates;
  private JPAConversionHelper convHelper;
  private List<UriResource> pathParts = new ArrayList<>();
  private Map<String, List<String>> headers;

  @Before
  public void setUp() throws Exception {
    odata = OData.newInstance();
    sessionContext = mock(JPAODataSessionContextAccess.class);
    requestContext = mock(JPAODataRequestContextAccess.class);
    serviceMetadata = mock(ServiceMetadata.class);
    uriInfo = mock(UriInfo.class);
    keyPredicates = new ArrayList<>();
    ets = mock(EdmEntitySet.class);
    serializer = mock(JPASerializer.class);
    uriEts = mock(UriResourceEntitySet.class);
    pathParts.add(uriEts);
    convHelper = new JPAConversionHelper();// mock(JPAConversionHelper.class);
    em = mock(EntityManager.class);
    transaction = mock(EntityTransaction.class);
    headers = new HashMap<>(0);

    when(sessionContext.getEdmProvider()).thenReturn(jpaEdm);
    when(requestContext.getEntityManager()).thenReturn(em);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getSerializer()).thenReturn(serializer);
    when(uriInfo.getUriResourceParts()).thenReturn(pathParts);
    when(uriEts.getKeyPredicates()).thenReturn(keyPredicates);
    when(uriEts.getEntitySet()).thenReturn(ets);
    when(uriEts.getKind()).thenReturn(UriResourceKind.entitySet);
    when(ets.getName()).thenReturn("AdministrativeDivisions");
    when(em.getTransaction()).thenReturn(transaction);
    cut = new JPACUDRequestProcessor(odata, serviceMetadata, sessionContext, requestContext, convHelper);

  }

  @Test
  public void testCreateDataAndEtCreated() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    assertNotNull(act.getData());
    assertNotNull(act.getEntityType());
  }

  @Test
  public void testCreateEtName() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    assertEquals("AdministrativeDivision", act.getEntityType().getExternalName());
  }

  @Test
  public void testCreateDataHasProperty() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);
    String actValue = (String) act.getData().get("codeID");
    assertNotNull(actValue);
    assertEquals("DE50", actValue);
  }

  @Test
  public void testCreateEmptyRelatedEntities() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    assertNotNull(act.getRelatedEntities());
    assertTrue(act.getRelatedEntities().isEmpty());
  }

  @Test
  public void testCreateEmptyRelationLinks() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    assertNotNull(act.getRelationLinks());
    assertTrue(act.getRelationLinks().isEmpty());
  }

  @Test
  public void testCreateDeepOneChildResultContainsEntityLink() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    List<Link> navigationLinks = new ArrayList<>();
    addChildrenNavigationLinkDE501(navigationLinks);
    when(oDataEntity.getNavigationLinks()).thenReturn(navigationLinks);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findEntitryList(act.getRelatedEntities(), ("children"));
    assertNotNull("Is null", actValue);
    assertTrue("Wrong type", actValue instanceof List);
  }

  @Test
  public void testCreateDeepOneChildResultContainsEntityLinkSize() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    List<Link> navigationLinks = new ArrayList<>();
    addChildrenNavigationLinkDE501(navigationLinks);
    when(oDataEntity.getNavigationLinks()).thenReturn(navigationLinks);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findEntitryList(act.getRelatedEntities(), ("children"));
    assertEquals("Wrong size", 1, ((List<?>) actValue).size());
  }

  @Test
  public void testCreateDeepOneChildResultContainsEntityLinkEntityType() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    List<Link> navigationLinks = new ArrayList<>();
    addChildrenNavigationLinkDE501(navigationLinks);
    when(oDataEntity.getNavigationLinks()).thenReturn(navigationLinks);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findEntitryList(act.getRelatedEntities(), ("children"));
    assertNotNull(((List<?>) actValue).get(0));
    assertNotNull("Entity type not found", ((JPARequestEntity) ((List<?>) actValue).get(0)).getEntityType());
    assertEquals("Wrong Type", "AdministrativeDivision", ((JPARequestEntity) ((List<?>) actValue).get(0))
        .getEntityType().getExternalName());
  }

  @Test
  public void testCreateDeepOneChildResultContainsEntityLinkData() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    List<Link> navigationLinks = new ArrayList<>();
    addChildrenNavigationLinkDE501(navigationLinks);
    when(oDataEntity.getNavigationLinks()).thenReturn(navigationLinks);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findEntitryList(act.getRelatedEntities(), ("children"));
    assertNotNull(((List<?>) actValue).get(0));
    assertNotNull("Entity type not found", ((JPARequestEntity) ((List<?>) actValue).get(0)).getEntityType());
    Map<String, Object> actData = ((JPARequestEntity) ((List<?>) actValue).get(0)).getData();
    assertNotNull("Data not found", actData);
    assertNotNull("CodeID not found", actData.get("codeID"));
    assertEquals("Value not found", "DE501", actData.get("codeID"));
  }

  @Test
  public void testCreateDeepTwoChildResultContainsEntityLinkSize() throws ODataJPAModelException, ODataException {
    List<Property> properties = createProperties();
    createODataEntity(properties);

    List<Link> navigationLinks = new ArrayList<>();
    addNavigationLinkDE502(navigationLinks);
    when(oDataEntity.getNavigationLinks()).thenReturn(navigationLinks);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findEntitryList(act.getRelatedEntities(), ("children"));
    assertEquals("Wrong size", 2, ((List<?>) actValue).size());
  }

  @Test
  public void testCreateWithLinkToOne() throws ODataJPAProcessorException {
    List<Property> properties = createProperties();
    createODataEntity(properties);
    List<Link> bindingLinks = new ArrayList<>();
    addParentBindingLink(bindingLinks);
    when(oDataEntity.getNavigationBindings()).thenReturn(bindingLinks);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findLinkList(act.getRelationLinks(), ("parent"));
    assertNotNull(actValue);
    assertTrue(actValue instanceof List<?>);
  }

  @Test
  public void testCreateWithLinkToMany() throws ODataJPAProcessorException {
    List<Property> properties = createProperties();
    createODataEntity(properties);
    List<Link> bindingLinks = new ArrayList<>();
    addChildrenBindingLink(bindingLinks);
    when(oDataEntity.getNavigationBindings()).thenReturn(bindingLinks);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findLinkList(act.getRelationLinks(), ("children"));
    assertNotNull(actValue);
    assertTrue(actValue instanceof List<?>);
    assertEquals(2, ((List<?>) actValue).size());
  }

  @Test
  public void testCreateDeepToOne() throws ODataJPAProcessorException {
    final List<Property> properties = createProperties();
    createODataEntity(properties);

    final List<Link> navigationLinks = new ArrayList<>();
    final Link navigationLink = addParentNavigationLink(navigationLinks);
    when(oDataEntity.getNavigationLinks()).thenReturn(navigationLinks);
    when(oDataEntity.getNavigationLink("Parent")).thenReturn(navigationLink);

    JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    Object actValue = findEntitryList(act.getRelatedEntities(), ("parent"));
    assertNotNull(actValue);
    assertTrue(actValue instanceof List<?>);
  }

  @Test
  public void testCreateOrgWithRoles() throws ODataJPAProcessorException {

    final List<Property> properties = new ArrayList<>();
    createPropertyBuPaID(properties, "20");
    createODataEntity(properties);
//-----------------------------
    final List<Link> navigationLinks = new ArrayList<>();

    final Link navigationLink = mock(Link.class);
    when(navigationLink.getTitle()).thenReturn("Roles");
    navigationLinks.add(navigationLink);
    final EntityCollection navigationEntitySet = mock(EntityCollection.class);
    final List<Entity> entityCollection = new ArrayList<>();

    final Entity navigationEntity1 = mock(Entity.class);
    final List<Property> navigationEntityProperties1 = createPropertiesRoles("20", "A");
    when(navigationEntity1.getProperties()).thenReturn(navigationEntityProperties1);//
    entityCollection.add(navigationEntity1);

    final Entity navigationEntity2 = mock(Entity.class);
    final List<Property> navigationEntityProperties2 = createPropertiesRoles("20", "C");
    when(navigationEntity2.getProperties()).thenReturn(navigationEntityProperties2);//
    entityCollection.add(navigationEntity2);

    when(navigationEntitySet.getEntities()).thenReturn(entityCollection);
    when(navigationLink.getInlineEntitySet()).thenReturn(navigationEntitySet);

    when(oDataEntity.getNavigationLinks()).thenReturn(navigationLinks);
    when(oDataEntity.getNavigationLink("Roles")).thenReturn(navigationLink);
//------------------------------------
    when(ets.getName()).thenReturn("Organizations");
    final JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);

    assertNotNull(act);
    assertNotNull(act.getData());
    assertNotNull(findEntitryList(act.getRelatedEntities(), ("roles")));
  }

  @Test
  public void testCreateDeepOneChildViaComplex() throws ODataJPAModelException, ODataException {
    final List<Property> properties = new ArrayList<>();
    final List<Property> inlineProperties = new ArrayList<>();
    final Entity inlineEntity = mock(Entity.class);

    createODataEntity(properties);

    when(ets.getName()).thenReturn("Persons");
    createPropertyBuPaID(properties, "20");
    when(inlineEntity.getProperties()).thenReturn(inlineProperties);
    createPropertyBuPaID(inlineProperties, "200");

    final List<Property> adminProperties = createComplexProperty(properties, "AdministrativeInformation", null, null,
        oDataEntity);
    final List<Property> createdProperties = createComplexProperty(adminProperties, "Created", "User", inlineEntity,
        oDataEntity);
    createPrimitiveProperty(createdProperties, "99", "By");
    createPrimitiveProperty(createdProperties, Timestamp.valueOf("2016-01-20 09:21:23.0"), "At");

    final JPARequestEntity act = cut.createRequestEntity(ets, oDataEntity, headers);
    final Object actValue = findEntitryList(act.getRelatedEntities(), ("administrativeInformation"));

    assertNotNull(actValue);
    assertNotNull(((List<?>) actValue).get(0));
    @SuppressWarnings("unchecked")
    JPARequestEntity actDeepEntity = ((List<JPARequestEntity>) actValue).get(0);
    assertEquals("200", actDeepEntity.getData().get("iD"));
  }

  private List<Property> createComplexProperty(final List<Property> properties, final String name, final String target,
      final Entity inlineEntity, final Entity oDataEntity) {

    final Property property = mock(Property.class);
    final ComplexValue cv = mock(ComplexValue.class);
    final List<Property> cProperties = new ArrayList<>();
    final Link navigationLink = mock(Link.class);

    when(property.getName()).thenReturn(name);
    when(property.getValue()).thenReturn(cv);
    when(property.getValueType()).thenReturn(ValueType.COMPLEX);
    when(property.isComplex()).thenReturn(true);
    when(property.asComplex()).thenReturn(cv);

    when(cv.getValue()).thenReturn(cProperties);
    when(cv.getNavigationLink(target)).thenReturn(navigationLink);
    when(navigationLink.getInlineEntity()).thenReturn(inlineEntity);

    when(oDataEntity.getProperty(name)).thenReturn(property);

    properties.add(property);
    return cProperties;
  }

  private Link addParentNavigationLink(List<Link> navigationLinks) {

    final Link navigationLink = mock(Link.class);
    when(navigationLink.getTitle()).thenReturn("Parent");
    navigationLinks.add(navigationLink);
    final Entity navigationEntity = mock(Entity.class);
    when(navigationLink.getInlineEntity()).thenReturn(navigationEntity);
    final List<Property> navigationEntityProperties = createPropertyCodeID("DE5");
    when(navigationEntity.getProperties()).thenReturn(navigationEntityProperties);//
    return navigationLink;
  }

  private void addParentBindingLink(List<Link> bindingLinks) {
    Link bindingLink = mock(Link.class);
    when(bindingLink.getTitle()).thenReturn("Parent");
    bindingLinks.add(bindingLink);
    when(bindingLink.getBindingLink()).thenReturn(
        "AdministrativeDivisions(DivisionCode='DE1',CodeID='NUTS1',CodePublisher='Eurostat')");
  }

  private void addChildrenNavigationLinkDE501(List<Link> navigationLinks) {
    addChildrenNavigationLink(navigationLinks, "DE501", null);
  }

  private void addNavigationLinkDE502(List<Link> navigationLinks) {

    addChildrenNavigationLink(navigationLinks, "DE501", "DE502");
  }

  private void addChildrenNavigationLink(List<Link> navigationLinks, String codeValue1, String codeValue2) {
    final Link navigationLink = mock(Link.class);
    when(navigationLink.getTitle()).thenReturn("Children");
    navigationLinks.add(navigationLink);
    final EntityCollection navigationEntitySet = mock(EntityCollection.class);
    final List<Entity> entityCollection = new ArrayList<>();

    final Entity navigationEntity1 = mock(Entity.class);
    final List<Property> navigationEntityProperties1 = createPropertyCodeID(codeValue1);
    when(navigationEntity1.getProperties()).thenReturn(navigationEntityProperties1);//
    entityCollection.add(navigationEntity1);
    if (codeValue2 != null) {
      Entity navigationEntity2 = mock(Entity.class);
      List<Property> navigationEntityProperties2 = createPropertyCodeID(codeValue2);
      when(navigationEntity2.getProperties()).thenReturn(navigationEntityProperties2);//
      entityCollection.add(navigationEntity2);
    }
    when(navigationEntitySet.getEntities()).thenReturn(entityCollection);
    when(navigationLink.getInlineEntitySet()).thenReturn(navigationEntitySet);
    when(oDataEntity.getNavigationLink("Children")).thenReturn(navigationLink);

  }

  private void addChildrenBindingLink(List<Link> bindingLinks) {
    List<String> links = new ArrayList<>();

    Link bindingLink = mock(Link.class);
    when(bindingLink.getTitle()).thenReturn("Children");
    bindingLinks.add(bindingLink);
    when(bindingLink.getBindingLinks()).thenReturn(links);
    links.add("AdministrativeDivisions(DivisionCode='DE100',CodeID='NUTS3',CodePublisher='Eurostat')");
    links.add("AdministrativeDivisions(DivisionCode='DE101',CodeID='NUTS3',CodePublisher='Eurostat')");
  }

  private void createODataEntity(final List<Property> properties) {
    oDataEntity = mock(Entity.class);
    when(oDataEntity.getProperties()).thenReturn(properties);
  }

  private List<Property> createProperties() {
    return createPropertyCodeID("DE50");
  }

  private List<Property> createPropertyCodeID(final String codeID) {
    List<Property> properties = new ArrayList<>();
    createPrimitiveProperty(properties, codeID, "CodeID");
    return properties;
  }

  private void createPropertyBuPaID(final List<Property> properties, final String value) {

    createPrimitiveProperty(properties, value, "ID");
  }

  private void createPrimitiveProperty(final List<Property> properties, final Object value, final String name) {

    final Property property = mock(Property.class);
    when(property.getName()).thenReturn(name);
    when(property.getValue()).thenReturn(value);
    when(property.getValueType()).thenReturn(ValueType.PRIMITIVE);
    properties.add(property);
  }

  private List<Property> createPropertiesRoles(String BuPaId, String RoleCategory) {
    List<Property> properties = new ArrayList<>();
    createPrimitiveProperty(properties, BuPaId, "BusinessPartnerID");
    createPrimitiveProperty(properties, RoleCategory, "RoleCategory");
    return properties;
  }

  private Object findEntitryList(Map<JPAAssociationPath, List<JPARequestEntity>> relatedEntities,
      String assoziationName) {
    for (Entry<JPAAssociationPath, List<JPARequestEntity>> entity : relatedEntities.entrySet()) {
      if (entity.getKey().getPath().get(0).getInternalName().equals(assoziationName))
        return entity.getValue();
    }
    return null;
  }

  private Object findLinkList(Map<JPAAssociationPath, List<JPARequestLink>> relationLink, String assoziationName) {
    for (Entry<JPAAssociationPath, List<JPARequestLink>> entity : relationLink.entrySet()) {
      if (entity.getKey().getPath().get(0).getInternalName().equals(assoziationName))
        return entity.getValue();
    }
    return null;
  }

}
