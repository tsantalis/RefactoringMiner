package com.sap.olingo.jpa.processor.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.batch.BatchFacade;
import org.eclipse.persistence.jpa.jpql.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.olingo.jpa.processor.core.exception.ODataJPAProcessorException;

public class JPAODataBatchProcessorTest {
  private JPAODataBatchProcessor cut;

  @Mock
  private EntityManager em;
  @Mock
  private EntityTransaction transaction;
  @Mock
  private OData odata;
  @Mock
  private ServiceMetadata serviceMetadata;
  @Mock
  private BatchFacade facade;
  @Mock
  private ODataRequest request;
  @Mock
  private ODataResponse response;
  @Mock
  private RollbackException e;
  @Mock
  private JPAODataSessionContextAccess context;
  @Mock
  private JPACUDRequestHandler cudHandler;

  private List<ODataRequest> requests;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    cut = new JPAODataBatchProcessor(context, em);
    cut.init(odata, serviceMetadata);
    requests = new ArrayList<>();
    requests.add(request);
    when(context.getDebugger()).thenReturn(new JPAEmptyDebugger());
    when(context.getCUDRequestHandler()).thenReturn(cudHandler);
  }

  @Test
  public void whenNotOptimisticLockRollBackExceptionThenThrowODataJPAProcessorExceptionWithHttpCode500()
      throws ODataApplicationException, ODataLibraryException {
    when(em.getTransaction()).thenReturn(transaction);
    when(response.getStatusCode()).thenReturn(HttpStatusCode.OK.getStatusCode());
    when(facade.handleODataRequest(request)).thenReturn(response);
    doThrow(e).when(transaction).commit();

    try {
      cut.processChangeSet(facade, requests);
      Assert.fail("Should have thrown ODataJPAProcessorException!");
    } catch (ODataJPAProcessorException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
    }

  }

  @Test
  public void whenOptimisticLockRollBackExceptionThenThrowODataJPAProcessorExceptionWithHttpCode412()
      throws ODataApplicationException, ODataLibraryException {
    when(em.getTransaction()).thenReturn(transaction);
    when(response.getStatusCode()).thenReturn(HttpStatusCode.OK.getStatusCode());
    when(facade.handleODataRequest(request)).thenReturn(response);
    doThrow(e).when(transaction).commit();
    when(e.getCause()).thenReturn(new OptimisticLockException());

    try {
      cut.processChangeSet(facade, requests);
      Assert.fail("Should have thrown ODataJPAProcessorException!");
    } catch (ODataJPAProcessorException e) {
      assertEquals(HttpStatusCode.PRECONDITION_FAILED.getStatusCode(), e.getStatusCode());
    }
  }

  @Test
  public void whenProcessChangeSetCallValidateChangesOnSccess() throws ODataApplicationException,
      ODataLibraryException {
    cut = new JPAODataBatchProcessor(context, em);

    when(em.getTransaction()).thenReturn(transaction);
    when(response.getStatusCode()).thenReturn(HttpStatusCode.OK.getStatusCode());
    when(facade.handleODataRequest(request)).thenReturn(response);

    cut.processChangeSet(facade, requests);
    verify(cudHandler, times(1)).validateChanges(em);
  }
}
