package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.olingo.jpa.metadata.api.JPAEdmProvider;
import com.sap.olingo.jpa.processor.core.api.JPAODataBatchProcessor;
import com.sap.olingo.jpa.processor.core.api.JPAODataClaimsProvider;
import com.sap.olingo.jpa.processor.core.api.JPAODataContextAccessDouble;
import com.sap.olingo.jpa.processor.core.api.JPAODataPagingProvider;
import com.sap.olingo.jpa.processor.core.api.JPAODataRequestProcessor;
import com.sap.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;

public class IntegrationTestHelper {
  public final HttpServletRequestDouble req;
  public final HttpServletResponseDouble resp;
  private static final String uriPrefix = "http://localhost:8080/Test/Olingo.svc/";
  private static final String PUNIT_NAME = "com.sap.olingo.jpa";

  public IntegrationTestHelper(EntityManagerFactory localEmf, String urlPath) throws IOException,
      ODataException {
    this(localEmf, null, urlPath, null, null, null);
  }

  public IntegrationTestHelper(EntityManagerFactory localEmf, DataSource ds, String urlPath) throws IOException,
      ODataException {
    this(localEmf, ds, urlPath, null, null, null);
  }

  public IntegrationTestHelper(EntityManagerFactory localEmf, String urlPath, StringBuffer requestBody)
      throws IOException, ODataException {
    this(localEmf, null, urlPath, requestBody, null, null);
  }

  public IntegrationTestHelper(EntityManagerFactory localEmf, DataSource ds, String urlPath, String functionPackage)
      throws IOException, ODataException {
    this(localEmf, ds, urlPath, null, functionPackage, null);
  }

  public IntegrationTestHelper(EntityManagerFactory localEmf, DataSource ds, String urlPath, StringBuffer requestBody)
      throws IOException, ODataException {
    this(localEmf, ds, urlPath, requestBody, null, null);
  }

  public IntegrationTestHelper(final EntityManagerFactory localEmf, final String urlPath,
      final JPAODataPagingProvider provider) throws IOException, ODataException {
    this(localEmf, null, urlPath, null, null, provider);
  }

  public IntegrationTestHelper(EntityManagerFactory localEmf, final String urlPath, JPAODataClaimsProvider claims)
      throws IOException, ODataException {
    this(localEmf, null, urlPath, null, null, null, null, claims);
  }

  public IntegrationTestHelper(final EntityManagerFactory emf, final String urlPath,
      final JPAODataPagingProvider provider, final Map<String, List<String>> headers) throws IOException,
      ODataException {
    this(emf, null, urlPath, null, null, provider, headers, null);
  }

  public IntegrationTestHelper(EntityManagerFactory localEmf, DataSource ds, String urlPath, StringBuffer requestBody,
      String functionPackage, JPAODataPagingProvider provider) throws IOException, ODataException {
    this(localEmf, ds, urlPath, requestBody, functionPackage, provider, null, null);
  }

  public IntegrationTestHelper(final EntityManagerFactory localEmf, final DataSource ds, final String urlPath,
      final StringBuffer requestBody, final String functionPackage, final JPAODataPagingProvider provider,
      final Map<String, List<String>> headers, final JPAODataClaimsProvider claims)
      throws IOException, ODataException {

    super();
    EntityManager em = localEmf.createEntityManager();
    this.req = new HttpServletRequestDouble(uriPrefix + urlPath, requestBody, headers);
    this.resp = new HttpServletResponseDouble();
    OData odata = OData.newInstance();
    String[] packages = TestBase.enumPackages;
    if (functionPackage != null)
      packages = ArrayUtils.add(packages, functionPackage);

    JPAODataSessionContextAccess context = new JPAODataContextAccessDouble(new JPAEdmProvider(PUNIT_NAME, localEmf,
        null, packages), ds, provider, functionPackage);

    ODataHttpHandler handler = odata.createHandler(odata.createServiceMetadata(context.getEdmProvider(),
        new ArrayList<EdmxReference>()));

    handler.register(new JPAODataRequestProcessor(context, claims, em));
    handler.register(new JPAODataBatchProcessor(context, em));
    handler.process(req, resp);

  }

  public HttpServletResponseDouble getResponce() {
    return resp;
  }

  public int getStatus() {
    return resp.getStatus();
  }

  public String getRawResult() throws IOException {
    InputStream in = resp.getInputStream();
    StringBuilder sb = new StringBuilder();
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String read;

    while ((read = br.readLine()) != null) {
      sb.append(read);
    }
    br.close();
    return sb.toString();
  }

  public List<String> getRawBatchResult() throws IOException {
    List<String> result = new ArrayList<>();

    InputStream in = resp.getInputStream();
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String read;

    while ((read = br.readLine()) != null) {
      result.add(read);
    }
    br.close();
    return result;
  }

  public ArrayNode getValues() throws JsonProcessingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(getRawResult());
    if (!(node.get("value") instanceof ArrayNode))
      fail("Wrong result type; ArrayNode expected");
    ArrayNode values = (ArrayNode) node.get("value");
    return values;
  }

  public ObjectNode getValue() throws JsonProcessingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode value = mapper.readTree(getRawResult());
    if (!(value instanceof ObjectNode))
      fail("Wrong result type; ObjectNode expected");
    return (ObjectNode) value;
  }

  public void assertStatus(int exp) throws IOException {
    assertEquals(exp, getStatus(), getRawResult());

  }

  public int getBatchResultStatus(int i) throws IOException {
    List<String> result = getRawBatchResult();
    int count = 0;
    for (String resultLine : result) {
      if (resultLine.contains("HTTP/1.1")) {
        count += 1;
        if (count == i) {
          String[] statusElements = resultLine.split(" ");
          return Integer.parseInt(statusElements[1]);
        }
      }
    }
    return 0;
  }

  public JsonNode getBatchResult(int i) throws IOException {
    List<String> result = getRawBatchResult();
    int count = 0;
    boolean found = false;

    for (String resultLine : result) {
      if (resultLine.contains("HTTP/1.1")) {
        count += 1;
        if (count == i) {
          found = true;
        }
      }
      if (found && resultLine.startsWith("{")) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(resultLine);
      }
    }
    return null;
  }

  public byte[] getBinaryResult() throws IOException {
    byte[] result = new byte[resp.getBufferSize()];
    InputStream in = resp.getInputStream();
    in.read(result);
    return result;
  }
}
