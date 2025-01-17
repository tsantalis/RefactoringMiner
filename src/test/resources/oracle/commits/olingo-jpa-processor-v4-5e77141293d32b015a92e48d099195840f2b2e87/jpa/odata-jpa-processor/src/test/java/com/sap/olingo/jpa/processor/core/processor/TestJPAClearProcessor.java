package com.sap.olingo.jpa.processor.core.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
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

import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.UriResourceValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import com.sap.olingo.jpa.processor.core.api.JPAAbstractCUDRequestHandler;
import com.sap.olingo.jpa.processor.core.api.JPACUDRequestHandler;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException.MessageKeys;
import com.sap.olingo.jpa.processor.core.modify.JPAConversionHelper;
import com.sap.olingo.jpa.processor.core.modify.JPAUpdateResult;

public class TestJPAClearProcessor extends TestJPAModifyProcessor {
  private ODataRequest request;

  @BeforeEach
  public void setup() throws ODataException {
    request = mock(ODataRequest.class);
    processor = new JPACUDRequestProcessor(odata, serviceMetadata, sessionContext, requestContext,
        new JPAConversionHelper());
  }

  @Test
  public void testSuccessReturnCode() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    ODataResponse response = new ODataResponse();

    prepareDeleteName2();
    processor.clearFields(request, response);
    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testHockIsCalled() throws ODataApplicationException {
    // .../Organizations('35')/Name2

    RequestHandleSpy spy = prepareDeleteName2();

    processor.clearFields(request, new ODataResponse());
    assertTrue(spy.called);
  }

  @Test
  public void testHeadersProvided() throws ODataJPAProcessorException, SerializerException, ODataException {
    final Map<String, List<String>> headers = new HashMap<>();

    when(request.getAllHeaders()).thenReturn(headers);
    headers.put("If-Match", Arrays.asList("2"));

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    processor.clearFields(request, new ODataResponse());

    assertNotNull(spy.headers);
    assertEquals(1, spy.headers.size());
    assertNotNull(spy.headers.get("If-Match"));
    assertEquals("2", spy.headers.get("If-Match").get(0));
  }

  @Test
  public void testSimplePropertyEntityTypeProvided() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    RequestHandleSpy spy = prepareDeleteName2();

    processor.clearFields(request, new ODataResponse());
    assertEquals("Organization", spy.et.getExternalName());
  }

  @Test
  public void testSimplePropertyKeyProvided() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    RequestHandleSpy spy = prepareDeleteName2();

    List<UriParameter> keys = new ArrayList<>();
    UriParameter uriParam = mock(UriParameter.class);
    when(uriParam.getText()).thenReturn("'35'");
    when(uriParam.getName()).thenReturn("ID");
    keys.add(uriParam);

    when(uriEts.getKeyPredicates()).thenReturn(keys);

    processor.clearFields(request, new ODataResponse());
    assertEquals(1, spy.keyPredicates.size());
    assertEquals("35", spy.keyPredicates.get("iD"));
  }

  @Test
  public void testSimplePropertyAttributeProvided() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    RequestHandleSpy spy = prepareDeleteName2();

    processor.clearFields(request, new ODataResponse());
    assertEquals(1, spy.jpaAttributes.size());
    Object[] keys = spy.jpaAttributes.keySet().toArray();
    assertEquals("name2", keys[0].toString());
  }

  @Test
  public void testComplexPropertyHoleProvided() throws ODataApplicationException {
    // .../Organizations('35')/Address
    RequestHandleSpy spy = prepareDeleteAddress();

    processor.clearFields(request, new ODataResponse());
    assertEquals(1, spy.jpaAttributes.size());
    Object[] keys = spy.jpaAttributes.keySet().toArray();
    assertEquals("address", keys[0].toString());
  }

  @Test
  public void testSimplePropertyValueAttributeProvided() throws ODataApplicationException {
    // .../Organizations('35')/Name2/$value
    RequestHandleSpy spy = prepareDeleteName2();

    UriResourceValue uriProperty;
    uriProperty = mock(UriResourceValue.class);
    pathParts.add(uriProperty);

    processor.clearFields(request, new ODataResponse());
    assertEquals(1, spy.jpaAttributes.size());
    Object[] keys = spy.jpaAttributes.keySet().toArray();
    assertEquals("name2", keys[0].toString());
  }

  @Test
  public void testComplexPropertyOnePropertyProvided() throws ODataApplicationException {
    // .../Organizations('35')/Address/Country
    RequestHandleSpy spy = prepareDeleteAddressCountry();

    processor.clearFields(request, new ODataResponse());
    assertEquals(1, spy.jpaAttributes.size());

    @SuppressWarnings("unchecked")
    Map<String, Object> address = (Map<String, Object>) spy.jpaAttributes.get("address");
    assertEquals(1, address.size());
    Object[] keys = address.keySet().toArray();
    assertEquals("country", keys[0].toString());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTwoComplexPropertiesOnePropertyProvided() throws ODataApplicationException {
    // .../Organizations('4')/AdministrativeInformation/Updated/By
    RequestHandleSpy spy = prepareDeleteAdminInfo();

    processor.clearFields(request, new ODataResponse());
    assertEquals(1, spy.jpaAttributes.size());

    Map<String, Object> adminInfo = (Map<String, Object>) spy.jpaAttributes.get("administrativeInformation");
    assertEquals(1, adminInfo.size());
    Map<String, Object> update = (Map<String, Object>) adminInfo.get("updated");
    assertEquals(1, update.size());
    Object[] keys = update.keySet().toArray();
    assertEquals("by", keys[0].toString());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTwoComplexPropertiesOnePropertyValueProvided() throws ODataApplicationException {
    // .../Organizations('4')/AdministrativeInformation/Updated/By/$value
    RequestHandleSpy spy = prepareDeleteAdminInfo();

    UriResourceValue uriProperty;
    uriProperty = mock(UriResourceValue.class);
    pathParts.add(uriProperty);

    processor.clearFields(request, new ODataResponse());
    assertEquals(1, spy.jpaAttributes.size());

    Map<String, Object> adminInfo = (Map<String, Object>) spy.jpaAttributes.get("administrativeInformation");
    assertEquals(1, adminInfo.size());
    Map<String, Object> update = (Map<String, Object>) adminInfo.get("updated");
    assertEquals(1, update.size());
    Object[] keys = update.keySet().toArray();
    assertEquals("by", keys[0].toString());
  }

  @Test
  public void testBeginIsCalledOnNoTransaction() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    prepareDeleteName2();

    processor.clearFields(request, new ODataResponse());

    verify(transaction, times(1)).begin();
  }

  @Test
  public void testBeginIsNotCalledOnTransaction() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    prepareDeleteName2();
    when(transaction.isActive()).thenReturn(true);

    processor.clearFields(request, new ODataResponse());

    verify(transaction, times(0)).begin();
  }

  @Test
  public void testCommitIsCalledOnNoTransaction() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    prepareDeleteName2();

    processor.clearFields(request, new ODataResponse());

    verify(transaction, times(1)).commit();
  }

  @Test
  public void testCommitIsNotCalledOnTransaction() throws ODataApplicationException {
    // .../Organizations('35')/Name2
    prepareDeleteName2();
    when(transaction.isActive()).thenReturn(true);

    processor.clearFields(request, new ODataResponse());

    verify(transaction, times(0)).commit();
  }

  @Test
  public void testErrorReturnCodeWithRollback() {
    // .../Organizations('35')/Name2
    ODataResponse response = new ODataResponse();

    RequestHandleSpy spy = prepareDeleteName2();
    spy.raiseException(1);
    try {
      processor.clearFields(request, response);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      verify(transaction, times(1)).rollback();
      return;
    }
    fail();
  }

  @Test
  public void testErrorReturnCodeWithOutRollback() {
    // .../Organizations('35')/Name2
    ODataResponse response = new ODataResponse();

    RequestHandleSpy spy = prepareDeleteName2();
    spy.raiseException(1);
    when(transaction.isActive()).thenReturn(true);
    try {
      processor.clearFields(request, response);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      verify(transaction, times(0)).rollback();
      return;
    }
    fail();
  }

  @Test
  public void testReraiseWithRollback() {
    // .../Organizations('35')/Name2
    ODataResponse response = new ODataResponse();

    RequestHandleSpy spy = prepareDeleteName2();
    spy.raiseException(2);
    try {
      processor.clearFields(request, response);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
      verify(transaction, times(1)).rollback();
      return;
    }
    fail();
  }

  @Test
  public void testReraiseReturnCodeWithOutRollback() throws ODataJPAProcessException {
    // .../Organizations('35')/Name2
    ODataResponse response = new ODataResponse();

    RequestHandleSpy spy = prepareDeleteName2();
    spy.raiseException(2);
    when(transaction.isActive()).thenReturn(true);
    try {
      processor.clearFields(request, response);
    } catch (ODataJPAProcessorException e) {
      assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
      verify(transaction, times(0)).rollback();
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

    processor.clearFields(request, response);
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

    processor.clearFields(request, response);
    assertEquals(0, spy.noValidateCalls);
  }

  @Test
  public void testDoesNotCallsValidateChangesOnError() throws ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();
    when(request.getMethod()).thenReturn(HttpMethod.POST);

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(sessionContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).updateEntity(any(JPARequestEntity.class), any(EntityManager.class),
            any(HttpMethod.class));

    try {
      processor.clearFields(request, response);
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
      processor.clearFields(request, response);
    } catch (ODataApplicationException e) {
      verify(transaction, never()).commit();
      verify(transaction, times(1)).rollback();
      return;
    }
    fail();
  }

  private RequestHandleSpy prepareDeleteName2() {

    UriResourcePrimitiveProperty uriProperty;
    EdmProperty property;
    uriProperty = mock(UriResourcePrimitiveProperty.class);
    property = mock(EdmProperty.class);

    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(property);
    when(property.getName()).thenReturn("Name2");

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    return spy;
  }

  private RequestHandleSpy prepareDeleteAddress() {

    UriResourceComplexProperty uriProperty;
    EdmProperty property;
    uriProperty = mock(UriResourceComplexProperty.class);
    property = mock(EdmProperty.class);

    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(property);
    when(property.getName()).thenReturn("Address");

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    return spy;
  }

  private RequestHandleSpy prepareDeleteAddressCountry() {
    RequestHandleSpy spy = prepareDeleteAddress();

    UriResourcePrimitiveProperty uriProperty;
    EdmProperty property;
    uriProperty = mock(UriResourcePrimitiveProperty.class);
    property = mock(EdmProperty.class);

    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(property);
    when(property.getName()).thenReturn("Country");

    return spy;
  }

  private RequestHandleSpy prepareDeleteAdminInfo() {

    UriResourceComplexProperty uriProperty;
    EdmProperty property;
    uriProperty = mock(UriResourceComplexProperty.class);
    property = mock(EdmProperty.class);

    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(property);
    when(property.getName()).thenReturn("AdministrativeInformation");

    uriProperty = mock(UriResourceComplexProperty.class);
    property = mock(EdmProperty.class);

    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(property);
    when(property.getName()).thenReturn("Updated");

    UriResourcePrimitiveProperty uriPrimProperty;
    uriPrimProperty = mock(UriResourcePrimitiveProperty.class);
    property = mock(EdmProperty.class);

    pathParts.add(uriPrimProperty);
    when(uriPrimProperty.getProperty()).thenReturn(property);
    when(property.getName()).thenReturn("By");

    RequestHandleSpy spy = new RequestHandleSpy();
    when(sessionContext.getCUDRequestHandler()).thenReturn(spy);

    return spy;
  }

  class RequestHandleSpy extends JPAAbstractCUDRequestHandler {
    public int noValidateCalls;
    public Map<String, Object> keyPredicates;
    public Map<String, Object> jpaAttributes;
    public JPAEntityType et;
    public boolean called;
    public Map<String, List<String>> headers;
    private int raiseEx;

    @Override
    public JPAUpdateResult updateEntity(final JPARequestEntity requestEntity, final EntityManager em,
        final HttpMethod verb) throws ODataJPAProcessException {

      this.et = requestEntity.getEntityType();
      this.keyPredicates = requestEntity.getKeys();
      this.jpaAttributes = requestEntity.getData();
      this.headers = requestEntity.getAllHeader();
      called = true;

      if (raiseEx == 1)
        throw new NullPointerException();
      if (raiseEx == 2)
        throw new ODataJPAProcessorException(MessageKeys.NOT_SUPPORTED_DELETE, HttpStatusCode.NOT_IMPLEMENTED);
      return null;
    }

    public void raiseException(int type) {
      this.raiseEx = type;

    }

    @Override
    public void validateChanges(final EntityManager em) throws ODataJPAProcessException {
      noValidateCalls++;
    }
  }
}
