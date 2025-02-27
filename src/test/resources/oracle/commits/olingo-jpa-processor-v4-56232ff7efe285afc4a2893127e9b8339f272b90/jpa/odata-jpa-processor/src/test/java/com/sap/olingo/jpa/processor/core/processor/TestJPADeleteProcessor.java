package com.sap.olingo.jpa.processor.core.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.metadata.api.JPAEntityManagerFactory;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.processor.core.api.JPAAbstractCUDRequestHandler;
import com.sap.olingo.jpa.processor.core.api.JPACUDRequestHandler;
import com.sap.olingo.jpa.processor.core.api.JPAODataRequestContextAccess;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import com.sap.olingo.jpa.processor.core.api.JPAServiceDebugger;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import com.sap.olingo.jpa.processor.core.modify.JPAConversionHelper;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestJPADeleteProcessor {
  private JPACUDRequestProcessor processor;
  private OData odata;
  private ServiceMetadata serviceMetadata;
  private JPAODataSessionContextAccess sessionContext;
  private JPAODataRequestContextAccess requestContext;
  private UriInfo uriInfo;
  private UriResourceEntitySet uriEts;
  private EdmEntitySet ets;
  private List<UriParameter> keyPredicates;
  private JPAServiceDebugger debugger;
  private EntityManager em;
  private EntityTransaction transaction;

  private static final String PUNIT_NAME = "com.sap.olingo.jpa";
  private static EntityManagerFactory emf;
  private static JPAEdmProvider jpaEdm;
  private static DataSource ds;

  @BeforeClass
  public static void setupClass() throws ODataException {
    JPAEdmMetadataPostProcessor pP = mock(JPAEdmMetadataPostProcessor.class);
    ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB);
    emf = JPAEntityManagerFactory.getEntityManagerFactory(PUNIT_NAME, ds);
    jpaEdm = new JPAEdmProvider(PUNIT_NAME, emf.getMetamodel(), pP, TestBase.enumPackages);

  }

  @Before
  public void setUp() throws Exception {
    odata = OData.newInstance();
    sessionContext = mock(JPAODataSessionContextAccess.class);
    requestContext = mock(JPAODataRequestContextAccess.class);
    serviceMetadata = mock(ServiceMetadata.class);
    uriInfo = mock(UriInfo.class);
    debugger = mock(JPAServiceDebugger.class);
    keyPredicates = new ArrayList<>();
    ets = mock(EdmEntitySet.class);
    final List<UriResource> pathParts = new ArrayList<>();
    uriEts = mock(UriResourceEntitySet.class);
    em = mock(EntityManager.class);
    transaction = mock(EntityTransaction.class);
    pathParts.add(uriEts);

    when(sessionContext.getEdmProvider()).thenReturn(jpaEdm);
    when(sessionContext.getDebugger()).thenReturn(debugger);
    when(requestContext.getEntityManager()).thenReturn(emf.createEntityManager());
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getUriResourceParts()).thenReturn(pathParts);
    when(uriEts.getKeyPredicates()).thenReturn(keyPredicates);
    when(uriEts.getEntitySet()).thenReturn(ets);
    when(ets.getName()).thenReturn("Organizations");
    processor = new JPACUDRequestProcessor(odata, serviceMetadata, sessionContext, requestContext,
        new JPAConversionHelper());
  }

  @Test
  public void testSuccessReturnCode() throws ODataApplicationException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);
    when(sessionContext.getCUDRequestHandler()).thenReturn(new RequestHandleSpy());

    processor.deleteEntity(request, response);
    assertEquals(204, response.getStatusCode());
  }

  @Test
  public void testThrowUnexpectedExceptionInCaseOfError() throws ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);
    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    doThrow(NullPointerException.class).when(handler).deleteEntity(any(JPARequestEntity.class), any(
        EntityManager.class));

    when(sessionContext.getCUDRequestHandler()).thenReturn(handler);

    try {
      processor.deleteEntity(request, response);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testThrowExpectedExceptionInCaseOfError() throws ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);
    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).deleteEntity(any(JPARequestEntity.class), any(EntityManager.class));

    when(sessionContext.getCUDRequestHandler()).thenReturn(handler);

    try {
      processor.deleteEntity(request, response);
    } catch (ODataApplicationException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testConvertEntityType() throws ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);
    RequestHandleSpy spy = new RequestHandleSpy();
    UriParameter param = mock(UriParameter.class);

    keyPredicates.add(param);

    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    when(param.getName()).thenReturn("ID");
    when(param.getText()).thenReturn("'1'");

    processor.deleteEntity(request, response);

    assertEquals("com.sap.olingo.jpa.processor.core.testmodel.Organization", spy.et.getInternalName());
  }

  @Test
  public void testHeadersProvided() throws ODataJPAProcessorException, SerializerException, ODataException {
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = mock(ODataRequest.class);
    final Map<String, List<String>> headers = new HashMap<>();

    when(request.getAllHeaders()).thenReturn(headers);
    headers.put("If-Match", Arrays.asList("2"));

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.deleteEntity(request, response);

    assertNotNull(spy.headers);
    assertEquals(1, spy.headers.size());
    assertNotNull(spy.headers.get("If-Match"));
    assertEquals("2", spy.headers.get("If-Match").get(0));
  }

  @Test
  public void testConvertKeySingleAttribute() throws ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);
    RequestHandleSpy spy = new RequestHandleSpy();
    UriParameter param = mock(UriParameter.class);

    keyPredicates.add(param);

    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    when(param.getName()).thenReturn("ID");
    when(param.getText()).thenReturn("'1'");
    processor.deleteEntity(request, response);

    assertEquals(1, spy.keyPredicates.size());
    assertTrue(spy.keyPredicates.get("iD") instanceof String);
    assertEquals("1", spy.keyPredicates.get("iD"));
  }

  @Test
  public void testConvertKeyTwoAttributes() throws ODataJPAProcessException {
    // BusinessPartnerRole
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);
    RequestHandleSpy spy = new RequestHandleSpy();
    UriParameter param1 = mock(UriParameter.class);
    UriParameter param2 = mock(UriParameter.class);

    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);
    when(ets.getName()).thenReturn("BusinessPartnerRoles");
    when(param1.getName()).thenReturn("BusinessPartnerID");
    when(param1.getText()).thenReturn("'1'");
    when(param2.getName()).thenReturn("RoleCategory");
    when(param2.getText()).thenReturn("'A'");
    keyPredicates.add(param1);
    keyPredicates.add(param2);
    processor.deleteEntity(request, response);

    assertEquals(2, spy.keyPredicates.size());
    assertTrue(spy.keyPredicates.get("businessPartnerID") instanceof String);
    assertEquals("1", spy.keyPredicates.get("businessPartnerID"));
    assertTrue(spy.keyPredicates.get("roleCategory") instanceof String);
    assertEquals("A", spy.keyPredicates.get("roleCategory"));

  }

  @Test
  public void testCallsValidateChangesOnSuccessfullProcessing() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.deleteEntity(request, response);
    assertEquals(1, spy.noValidateCalls);
  }

  @Test
  public void testDoesNotCallsValidateChangesOnForginTransaction() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);
    when(em.getTransaction()).thenReturn(transaction);
    when(transaction.isActive()).thenReturn(Boolean.TRUE);
    when(requestContext.getEntityManager()).thenReturn(em);

    processor = new JPACUDRequestProcessor(odata, serviceMetadata, sessionContext, requestContext,
        new JPAConversionHelper());

    processor.deleteEntity(request, response);
    assertEquals(0, spy.noValidateCalls);
  }

  @Test
  public void testDoesNotCallsValidateChangesOnError() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(sessionContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).deleteEntity(any(JPARequestEntity.class), any(EntityManager.class));

    try {
      processor.deleteEntity(request, response);
    } catch (ODataApplicationException e) {
      verify(handler, never()).validateChanges(em);
      return;
    }
    fail();
  }

  @Test
  public void testDoesRollbackIfValidateRaisesError() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = mock(ODataRequest.class);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);
    when(em.getTransaction()).thenReturn(transaction);
    when(transaction.isActive()).thenReturn(Boolean.FALSE);
    when(requestContext.getEntityManager()).thenReturn(em);

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(sessionContext.getCUDRequestHandler()).thenReturn(handler);

    processor = new JPACUDRequestProcessor(odata, serviceMetadata, sessionContext, requestContext,
        new JPAConversionHelper());

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).validateChanges(em);

    try {
      processor.deleteEntity(request, response);
    } catch (ODataApplicationException e) {
      verify(transaction, never()).commit();
      verify(transaction, times(1)).rollback();
      return;
    }
    fail();
  }

  class RequestHandleSpy extends JPAAbstractCUDRequestHandler {
    public int noValidateCalls;
    public Map<String, Object> keyPredicates;
    public JPAEntityType et;
    public Map<String, List<String>> headers;

    @Override
    public void deleteEntity(final JPARequestEntity entity, final EntityManager em) {

      this.keyPredicates = entity.getKeys();
      this.et = entity.getEntityType();
      this.headers = entity.getAllHeader();
    }

    @Override
    public void validateChanges(final EntityManager em) throws ODataJPAProcessException {
      noValidateCalls++;
    }
  }
}
