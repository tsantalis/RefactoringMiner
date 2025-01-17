package com.sap.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.apache.olingo.commons.api.data.Annotatable;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmParameter;
import org.apache.olingo.commons.api.edm.EdmReturnType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt32;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.processor.core.api.JPAODataRequestContextAccess;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import com.sap.olingo.jpa.processor.core.processor.JPAFunctionRequestProcessor;
import com.sap.olingo.jpa.processor.core.serializer.JPAOperationSerializer;
import com.sap.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import com.sap.olingo.jpa.processor.core.testobjects.TestFunctionParameter;

public class TestJPAFunctionJava {
  protected static final String PUNIT_NAME = "com.sap.olingo.jpa";

  private JPAFunctionRequestProcessor cut;
  private OData odata;
  private JPAODataSessionContextAccess context;
  private JPAODataRequestContextAccess requestContext;
  private UriInfo uriInfo;
  private List<UriResource> uriResources;
  private ODataRequest request;
  private ODataResponse response;
  private UriResourceFunction uriResource;
  private EdmFunction edmFunction;
  private JPAOperationSerializer serializer;
  private SerializerResult serializerResult;

  @Before
  public void setup() throws ODataException {
    odata = mock(OData.class);
    context = mock(JPAODataSessionContextAccess.class);
    requestContext = mock(JPAODataRequestContextAccess.class);
    EntityManager em = mock(EntityManager.class);
    uriInfo = mock(UriInfo.class);
    serializer = mock(JPAOperationSerializer.class);
    serializerResult = mock(SerializerResult.class);

    DataSource ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB);
    Map<String, Object> properties = new HashMap<>();
    properties.put("javax.persistence.nonJtaDataSource", ds);
    final EntityManagerFactory emf = Persistence.createEntityManagerFactory(PUNIT_NAME, properties);
    uriResources = new ArrayList<>();
    when(uriInfo.getUriResourceParts()).thenReturn(uriResources);
    when(context.getEdmProvider()).thenReturn(new JPAEdmProvider(PUNIT_NAME, emf, null, new String[] {
        "com.sap.olingo.jpa.processor.core", "com.sap.olingo.jpa.processor.core.testmodel" }));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getEntityManager()).thenReturn(em);
    when(requestContext.getSerializer()).thenReturn(serializer);
    when(serializer.serialize(any(Annotatable.class), any(EdmType.class))).thenReturn(serializerResult);

    request = mock(ODataRequest.class);
    response = mock(ODataResponse.class);
    uriResource = mock(UriResourceFunction.class);
    edmFunction = mock(EdmFunction.class);
    uriResources.add(uriResource);
    when(uriResource.getFunction()).thenReturn(edmFunction);

    cut = new JPAFunctionRequestProcessor(odata, context, requestContext);
  }

  @After
  public void teardown() {
    TestFunctionParameter.calls = 0;
    TestFunctionParameter.param1 = 0;
    TestFunctionParameter.param2 = 0;
  }

  @Test
  public void testCallsFunction() throws ODataApplicationException, ODataLibraryException {
    EdmParameter edmParamA = mock(EdmParameter.class);
    EdmParameter edmParamB = mock(EdmParameter.class);
    EdmReturnType edmReturn = mock(EdmReturnType.class);
    EdmType edmType = mock(EdmType.class);

    when(edmFunction.getReturnType()).thenReturn(edmReturn);
    when(edmFunction.getName()).thenReturn("Sum");
    when(edmFunction.getNamespace()).thenReturn(PUNIT_NAME);
    when(edmFunction.getParameter("A")).thenReturn(edmParamA);
    when(edmParamA.getType()).thenReturn(new EdmInt32());
    when(edmFunction.getParameter("B")).thenReturn(edmParamB);
    when(edmParamB.getType()).thenReturn(new EdmInt32());
    List<UriParameter> parameterList = buildParameters();
    when(uriResource.getParameters()).thenReturn(parameterList);
    when(edmReturn.getType()).thenReturn(edmType);
    when(edmType.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);

    cut.retrieveData(request, response, ContentType.JSON);
    assertEquals(1, TestFunctionParameter.calls);
  }

  @Test
  public void testProvidesParameter() throws ODataApplicationException, ODataLibraryException {
    EdmParameter edmParamA = mock(EdmParameter.class);
    EdmParameter edmParamB = mock(EdmParameter.class);
    EdmReturnType edmReturn = mock(EdmReturnType.class);
    EdmType edmType = mock(EdmType.class);

    when(edmFunction.getReturnType()).thenReturn(edmReturn);
    when(edmFunction.getName()).thenReturn("Sum");
    when(edmFunction.getNamespace()).thenReturn(PUNIT_NAME);
    when(edmFunction.getParameter("A")).thenReturn(edmParamA);
    when(edmParamA.getType()).thenReturn(new EdmInt32());
    when(edmFunction.getParameter("B")).thenReturn(edmParamB);
    when(edmParamB.getType()).thenReturn(new EdmInt32());
    List<UriParameter> parameterList = buildParameters();
    when(uriResource.getParameters()).thenReturn(parameterList);
    when(edmReturn.getType()).thenReturn(edmType);
    when(edmType.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);

    cut.retrieveData(request, response, ContentType.JSON);
    assertEquals(5, TestFunctionParameter.param1);
    assertEquals(7, TestFunctionParameter.param2);
  }

  private List<UriParameter> buildParameters() {
    UriParameter param1 = mock(UriParameter.class);
    UriParameter param2 = mock(UriParameter.class);
    when(param1.getName()).thenReturn("A");
    when(param1.getText()).thenReturn("5");
    when(param2.getName()).thenReturn("B");
    when(param2.getText()).thenReturn("7");
    return Arrays.asList(new UriParameter[] { param1, param2 });
  }
}