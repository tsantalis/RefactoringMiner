package com.sap.olingo.jpa.processor.core.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.ArgumentMatchers;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.processor.core.api.JPAAbstractCUDRequestHandler;
import com.sap.olingo.jpa.processor.core.api.JPAODataRequestContextAccess;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import com.sap.olingo.jpa.processor.core.api.JPAServiceDebugger;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import com.sap.olingo.jpa.processor.core.modify.JPAConversionHelper;
import com.sap.olingo.jpa.processor.core.query.EdmEntitySetInfo;
import com.sap.olingo.jpa.processor.core.serializer.JPASerializer;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivisionKey;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public abstract class TestJPAModifyProcessor {
  protected static final String LOCATION_HEADER = "Organization('35')";
  protected static final String PREFERENCE_APPLIED = "return=minimal";
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

  protected JPACUDRequestProcessor processor;
  protected OData odata;
  protected ServiceMetadata serviceMetadata;
  protected JPAODataSessionContextAccess sessionContext;
  protected JPAODataRequestContextAccess requestContext;
  protected UriInfo uriInfo;
  protected UriResourceEntitySet uriEts;
  protected EntityManager em;
  protected EntityTransaction transaction;
  protected JPASerializer serializer;
  protected EdmEntitySet ets;
  protected EdmEntitySetInfo etsInfo;
  protected List<UriParameter> keyPredicates;
  protected JPAConversionHelper convHelper;
  protected List<UriResource> pathParts = new ArrayList<>();
  protected SerializerResult serializerResult;
  protected List<String> header = new ArrayList<>();
  protected JPAServiceDebugger debugger;

  @Before
  public void setUp() throws Exception {
    odata = OData.newInstance();
    sessionContext = mock(JPAODataSessionContextAccess.class);
    requestContext = mock(JPAODataRequestContextAccess.class);
    serviceMetadata = mock(ServiceMetadata.class);
    uriInfo = mock(UriInfo.class);
    keyPredicates = new ArrayList<>();
    ets = mock(EdmEntitySet.class);
    etsInfo = mock(EdmEntitySetInfo.class);
    serializer = mock(JPASerializer.class);
    uriEts = mock(UriResourceEntitySet.class);
    pathParts.add(uriEts);
    convHelper = mock(JPAConversionHelper.class);
    em = mock(EntityManager.class);
    transaction = mock(EntityTransaction.class);
    serializerResult = mock(SerializerResult.class);
    debugger = mock(JPAServiceDebugger.class);

    when(sessionContext.getEdmProvider()).thenReturn(jpaEdm);
    when(sessionContext.getDebugger()).thenReturn(debugger);
    when(requestContext.getEntityManager()).thenReturn(em);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getSerializer()).thenReturn(serializer);
    when(uriInfo.getUriResourceParts()).thenReturn(pathParts);
    when(uriEts.getKeyPredicates()).thenReturn(keyPredicates);
    when(uriEts.getEntitySet()).thenReturn(ets);
    when(uriEts.getKind()).thenReturn(UriResourceKind.entitySet);
    when(ets.getName()).thenReturn("Organizations");
    when(em.getTransaction()).thenReturn(transaction);
    when(etsInfo.getEdmEntitySet()).thenReturn(ets);
    processor = new JPACUDRequestProcessor(odata, serviceMetadata, sessionContext, requestContext, convHelper);

  }

  protected ODataRequest prepareRepresentationRequest(JPAAbstractCUDRequestHandler spy)
      throws ODataJPAProcessorException, SerializerException, ODataException {

    ODataRequest request = prepareSimpleRequest("return=representation");

    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);
    Organization org = new Organization();
    when(em.find(Organization.class, "35")).thenReturn(org);
    org.setID("35");
    Edm edm = mock(Edm.class);
    when(serviceMetadata.getEdm()).thenReturn(edm);
    EdmEntityType edmET = mock(EdmEntityType.class);
    FullQualifiedName fqn = new FullQualifiedName("com.sap.olingo.jpa.Organization");
    when(edm.getEntityType(fqn)).thenReturn(edmET);
    List<String> keyNames = new ArrayList<>();
    keyNames.add("ID");
    when(edmET.getKeyPredicateNames()).thenReturn(keyNames);
    EdmKeyPropertyRef refType = mock(EdmKeyPropertyRef.class);
    when(edmET.getKeyPropertyRef("ID")).thenReturn(refType);
    when(edmET.getFullQualifiedName()).thenReturn(fqn);
    EdmProperty edmProperty = mock(EdmProperty.class);
    when(refType.getProperty()).thenReturn(edmProperty);
    when(refType.getName()).thenReturn("ID");
    EdmPrimitiveType type = mock(EdmPrimitiveType.class);
    when(edmProperty.getType()).thenReturn(type);
    when(type.toUriLiteral(ArgumentMatchers.any())).thenReturn("35");

    when(serializer.serialize(ArgumentMatchers.eq(request), ArgumentMatchers.any(EntityCollection.class))).thenReturn(
        serializerResult);
    when(serializerResult.getContent()).thenReturn(new ByteArrayInputStream("{\"ID\":\"35\"}".getBytes()));

    return request;
  }

  protected ODataRequest prepareLinkRequest(JPAAbstractCUDRequestHandler spy)
      throws ODataJPAProcessorException, SerializerException, ODataException {

    // .../AdministrativeDivisions(DivisionCode='DE60',CodeID='NUTS2',CodePublisher='Eurostat')
    final ODataRequest request = prepareSimpleRequest("return=representation");
    final Edm edm = mock(Edm.class);
    final EdmEntityType edmET = mock(EdmEntityType.class);

    final FullQualifiedName fqn = new FullQualifiedName("com.sap.olingo.jpa.AdministrativeDivision");
    final List<String> keyNames = new ArrayList<>();

    final AdministrativeDivisionKey key = new AdministrativeDivisionKey("Eurostat", "NUTS2", "DE60");
    final AdministrativeDivision div = new AdministrativeDivision(key);

    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);
    when(em.find(AdministrativeDivision.class, key)).thenReturn(div);
    when(serviceMetadata.getEdm()).thenReturn(edm);
    when(edm.getEntityType(fqn)).thenReturn(edmET);
    when(ets.getName()).thenReturn("AdministrativeDivisions");
    when(uriEts.getKeyPredicates()).thenReturn(keyPredicates);
    keyNames.add("DivisionCode");
    keyNames.add("CodeID");
    keyNames.add("CodePublisher");
    when(edmET.getKeyPredicateNames()).thenReturn(keyNames);
    when(edmET.getFullQualifiedName()).thenReturn(fqn);

    EdmPrimitiveType type = EdmString.getInstance();
    EdmKeyPropertyRef refType = mock(EdmKeyPropertyRef.class);
    EdmProperty edmProperty = mock(EdmProperty.class);
    when(edmET.getKeyPropertyRef("DivisionCode")).thenReturn(refType);
    when(refType.getProperty()).thenReturn(edmProperty);
    when(refType.getName()).thenReturn("DivisionCode");
    when(edmProperty.getType()).thenReturn(type);
    when(edmProperty.getMaxLength()).thenReturn(50);

    refType = mock(EdmKeyPropertyRef.class);
    edmProperty = mock(EdmProperty.class);
    when(edmET.getKeyPropertyRef("CodeID")).thenReturn(refType);
    when(refType.getProperty()).thenReturn(edmProperty);
    when(refType.getName()).thenReturn("CodeID");
    when(edmProperty.getType()).thenReturn(type);
    when(edmProperty.getMaxLength()).thenReturn(50);

    refType = mock(EdmKeyPropertyRef.class);
    edmProperty = mock(EdmProperty.class);
    when(edmET.getKeyPropertyRef("CodePublisher")).thenReturn(refType);
    when(refType.getProperty()).thenReturn(edmProperty);
    when(refType.getName()).thenReturn("CodePublisher");
    when(edmProperty.getType()).thenReturn(type);
    when(edmProperty.getMaxLength()).thenReturn(50);

    when(serializer.serialize(ArgumentMatchers.eq(request), ArgumentMatchers.any(EntityCollection.class))).thenReturn(
        serializerResult);
    when(serializerResult.getContent()).thenReturn(new ByteArrayInputStream("{\"ParentCodeID\":\"NUTS1\"}".getBytes()));

    return request;

  }

  protected ODataRequest prepareSimpleRequest() throws ODataException, ODataJPAProcessorException, SerializerException {

    return prepareSimpleRequest("return=minimal");
  }

  protected ODataRequest prepareSimpleRequest(String content) throws ODataException, ODataJPAProcessorException,
      SerializerException {

    final EntityTransaction transaction = mock(EntityTransaction.class);
    when(em.getTransaction()).thenReturn(transaction);

    final ODataRequest request = mock(ODataRequest.class);
    when(request.getHeaders(HttpHeader.PREFER)).thenReturn(header);
    when(sessionContext.getEdmProvider()).thenReturn(jpaEdm);
    when(etsInfo.getEdmEntitySet()).thenReturn(ets);
    header.add(content);

    Entity odataEntity = mock(Entity.class);
    when(convHelper.convertInputStream(same(odata), same(request), same(ContentType.JSON), any())).thenReturn(
        odataEntity);
    when(convHelper.convertKeyToLocal(ArgumentMatchers.eq(odata), ArgumentMatchers.eq(request), ArgumentMatchers.eq(
        ets), ArgumentMatchers.any(JPAEntityType.class), ArgumentMatchers.any())).thenReturn(LOCATION_HEADER);
    return request;
  }
}
