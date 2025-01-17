package com.sap.olingo.jpa.processor.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.apache.olingo.commons.api.ex.ODataException;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.olingo.jpa.processor.core.util.IntegrationTestHelper;
import com.sap.olingo.jpa.processor.core.util.TestBase;

public class TestBatchRequests extends TestBase {

  @Test
  public void testOneGetRequestGetResponce() throws IOException, ODataException {
    StringBuffer requestBody = createBodyOneGet();

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "$batch", requestBody);
    List<String> act = helper.getRawBatchResult();
    assertNotNull(act);
  }

  @Test
  public void testOneGetRequestCheckStatus() throws IOException, ODataException {
    StringBuffer requestBody = createBodyOneGet();

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "$batch", requestBody);
    assertEquals(200, helper.getBatchResultStatus(1));
  }

  @Test
  public void testOneGetRequestCheckValue() throws IOException, ODataException {
    StringBuffer requestBody = createBodyOneGet();

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "$batch", requestBody);
    JsonNode value = helper.getBatchResult(1);
    assertEquals("3", value.get("ID").asText());
  }

  @Test
  public void testTwoGetRequestSecondFailCheckStatus() throws IOException, ODataException {
    StringBuffer requestBody = createBodyTwoGetOneFail();

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "$batch", requestBody);
    assertEquals(404, helper.getBatchResultStatus(2));
  }

  @Test
  public void testTwoGetRequestCheckValue() throws IOException, ODataException {
    StringBuffer requestBody = createBodyTwoGet();

    IntegrationTestHelper helper = new IntegrationTestHelper(emf, "$batch", requestBody);
    JsonNode value = helper.getBatchResult(2);
    assertEquals("5", value.get("ID").asText());
  }

  private StringBuffer createBodyTwoGetOneFail() {
    StringBuffer requestBody = new StringBuffer("--abc123\r\n");
    requestBody.append("Content-Type: application/http\r\n");
    requestBody.append("Content-Transfer-Encoding: binary\r\n");
    requestBody.append("\r\n");
    requestBody.append("GET Organizations('3') HTTP/1.1\r\n");
    requestBody.append("Content-Type: application/json\r\n");
    requestBody.append("\r\n");
    requestBody.append("\r\n");
    requestBody.append("--abc123\r\n");
    requestBody.append("Content-Type: application/http\r\n");
    requestBody.append("Content-Transfer-Encoding: binary\r\n");
    requestBody.append("\r\n");
    requestBody.append("GET AdministrativeDivision HTTP/1.1\r\n");
    requestBody.append("Content-Type: application/json\r\n");
    requestBody.append("\r\n");
    requestBody.append("\r\n");
    requestBody.append("--abc123--");
    return requestBody;
  }

  private StringBuffer createBodyTwoGet() {
    StringBuffer requestBody = new StringBuffer("--abc123\r\n");
    requestBody.append("Content-Type: application/http\r\n");
    requestBody.append("Content-Transfer-Encoding: binary\r\n");
    requestBody.append("\r\n");
    requestBody.append("GET Organizations('3') HTTP/1.1\r\n");
    requestBody.append("Content-Type: application/json\r\n");
    requestBody.append("\r\n");
    requestBody.append("\r\n");
    requestBody.append("--abc123\r\n");
    requestBody.append("Content-Type: application/http\r\n");
    requestBody.append("Content-Transfer-Encoding: binary\r\n");
    requestBody.append("\r\n");
    requestBody.append("GET Organizations('5') HTTP/1.1\r\n");
    requestBody.append("Content-Type: application/json\r\n");
    requestBody.append("\r\n");
    requestBody.append("\r\n");
    requestBody.append("--abc123--");
    return requestBody;
  }

  private StringBuffer createBodyOneGet() {
    StringBuffer requestBody = new StringBuffer("--abc123\r\n");
    requestBody.append("Content-Type: application/http\r\n");
    requestBody.append("Content-Transfer-Encoding: binary\r\n");
    requestBody.append("\r\n");
    requestBody.append("GET Organizations('3') HTTP/1.1\r\n");
    requestBody.append("Content-Type: application/json\r\n");
    requestBody.append("\r\n");
    requestBody.append("\r\n");
    requestBody.append("--abc123--");
    return requestBody;
  }
}
