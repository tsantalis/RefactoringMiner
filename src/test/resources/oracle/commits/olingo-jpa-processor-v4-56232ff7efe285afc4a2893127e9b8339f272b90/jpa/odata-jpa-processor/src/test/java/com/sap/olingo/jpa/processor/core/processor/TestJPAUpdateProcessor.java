package com.sap.olingo.jpa.processor.core.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.processor.core.api.JPAAbstractCUDRequestHandler;
import com.sap.olingo.jpa.processor.core.api.JPACUDRequestHandler;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import com.sap.olingo.jpa.processor.core.modify.JPAUpdateResult;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import com.sap.olingo.jpa.processor.core.testmodel.AdministrativeDivisionKey;
import com.sap.olingo.jpa.processor.core.testmodel.Organization;

public class TestJPAUpdateProcessor extends TestJPAModifyProcessor {

  @Test
  public void testHockIsCalled() throws ODataJPAModelException, ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertTrue(spy.called);
  }

  @Test
  public void testHttpMethodProvided() throws ODataJPAModelException, ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpMethod.PATCH, spy.method);
  }

  @Test
  public void testEntityTypeProvided() throws ODataJPAModelException, ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertTrue(spy.et instanceof JPAEntityType);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testJPAAttributes() throws ODataJPAModelException, ODataException, UnsupportedEncodingException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    InputStream is = new ByteArrayInputStream("{\"ID\" : \"35\", \"Country\" : \"USA\"}".getBytes("UTF-8"));
    when(request.getBody()).thenReturn(is);
    Map<String, Object> jpaAttributes = new HashMap<>();
    jpaAttributes.put("id", "35");
    jpaAttributes.put("country", "USA");
    when(convHelper.convertProperties(any(OData.class), any(JPAStructuredType.class), any(List.class)))
        .thenReturn(jpaAttributes);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(2, spy.jpaAttributes.size());
  }

  @Test
  public void testHeadersProvided() throws ODataJPAProcessorException, SerializerException, ODataException {
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareSimpleRequest();
    final Map<String, List<String>> headers = new HashMap<>();

    when(request.getAllHeaders()).thenReturn(headers);
    headers.put("If-Match", Arrays.asList("2"));

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertNotNull(spy.headers);
    assertEquals(1, spy.headers.size());
    assertNotNull(spy.headers.get("If-Match"));
    assertEquals("2", spy.headers.get("If-Match").get(0));
  }

  @Test
  public void testMinimalResponseUpdateStatusCode() throws ODataJPAProcessorException, SerializerException,
      ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(false, new Organization()));
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testMinimalResponseCreatedStatusCode() throws ODataJPAProcessorException, SerializerException,
      ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(true, new Organization()));
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testMinimalResponseUpdatePreferHeader() throws ODataJPAProcessorException, SerializerException,
      ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(false, new Organization()));
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(PREFERENCE_APPLIED, response.getHeader(HttpHeader.PREFERENCE_APPLIED));
  }

  @Test
  public void testMinimalResponseCreatedPreferHeader() throws ODataJPAProcessorException, SerializerException,
      ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(true, new Organization()));
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(PREFERENCE_APPLIED, response.getHeader(HttpHeader.PREFERENCE_APPLIED));
  }

  @Test
  public void testRepresentationResponseUpdatedStatusCode() throws ODataJPAProcessorException, SerializerException,
      ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(false,
        new Organization())));

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testRepresentationResponseCreatedStatusCode() throws ODataJPAProcessorException, SerializerException,
      ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(true,
        new Organization())));

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testRepresentationResponseUpdatedErrorMissingEntity() throws ODataJPAProcessorException, ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(false, null)));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testRepresentationResponseCreatedErrorMissingEntity() throws ODataJPAProcessorException, ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(true, null)));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testCallsValidateChangesOnSuccessfullProcessing() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    assertEquals(1, spy.noValidateCalls);
  }

  @Test
  public void testDoesNotCallsValidateChangesOnForginTransaction() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);
    when(em.getTransaction()).thenReturn(transaction);
    when(transaction.isActive()).thenReturn(Boolean.TRUE);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    assertEquals(0, spy.noValidateCalls);
  }

  @Test
  public void testDoesNotCallsValidateChangesOnError() throws ODataException {
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareSimpleRequest();
    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    final JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(sessionContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).updateEntity(any(JPARequestEntity.class), any(EntityManager.class),
            any(HttpMethod.class));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataApplicationException e) {
      verify(handler, never()).validateChanges(em);
      return;
    }
    fail();
  }

  @Test
  public void testDoesRollbackIfValidateRaisesError() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(em.getTransaction()).thenReturn(transaction);

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(sessionContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).validateChanges(em);

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataApplicationException e) {
      verify(transaction, never()).commit();
      verify(transaction, times(1)).rollback();
      return;
    }
    fail();
  }

  @Test
  public void testResponseErrorIfNull() throws ODataJPAProcessorException, ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(null));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testResponseUpdateLink() throws ODataJPAProcessorException, ODataException {

    final AdministrativeDivisionKey key = new AdministrativeDivisionKey("Eurostat", "NUTS2", "DE60");
    final AdministrativeDivision resultEntity = new AdministrativeDivision(key);

    final AdministrativeDivisionKey childKey = new AdministrativeDivisionKey("Eurostat", "NUTS3", "DE600");
    final AdministrativeDivision childEntity = new AdministrativeDivision(childKey);

    final JPAUpdateResult result = new JPAUpdateResult(false, resultEntity);
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareLinkRequest(new RequestHandleSpy(result));

    resultEntity.setChildren(new ArrayList<>(Arrays.asList(childEntity)));

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    assertNotNull(response);

  }

  class RequestHandleSpy extends JPAAbstractCUDRequestHandler {
    public int noValidateCalls;
    public JPAEntityType et;
    public Map<String, Object> jpaAttributes;
    public EntityManager em;
    public boolean called = false;
    public HttpMethod method;
    public Map<String, List<String>> headers;
    private final JPAUpdateResult change;
    // private Map<String, Object> keys;

    RequestHandleSpy() {
      this(new JPAUpdateResult(true, new Organization()));
    }

    RequestHandleSpy(final JPAUpdateResult typeOfChange) {
      this.change = typeOfChange;
    }

    @Override
    public JPAUpdateResult updateEntity(final JPARequestEntity requestEntity, final EntityManager em,
        final HttpMethod verb) throws ODataJPAProcessException {
      this.et = requestEntity.getEntityType();
      this.jpaAttributes = requestEntity.getData();
      // this.keys = requestEntity.getKeys();
      this.em = em;
      this.called = true;
      this.method = verb;
      this.headers = requestEntity.getAllHeader();
      return change;
    }

    @Override
    public void validateChanges(final EntityManager em) throws ODataJPAProcessException {
      this.noValidateCalls++;
    }
  }
}
