/**
 * Copyright 2016 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 **/

package io.confluent.connect.elasticsearch.internals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult;
import io.searchbox.core.BulkResult.BulkResultItem;

public class Response {

  private static final Logger log = LoggerFactory.getLogger(Response.class);
  private Throwable throwable;
  private JestResult result;
  private final String nonRetriableError = "mapper_parse_exception";

  public Response(BulkResult result) {
    this.result = result;
    Throwable firstException = null;
    for (BulkResultItem bulkResultItem: result.getFailedItems()) {
      ObjectNode obj = parseError(bulkResultItem.error);
      String exceptionType = obj.get("type").asText();
      if (exceptionType.equals(nonRetriableError)) {
        throwable = new Throwable(exceptionType);
        break;
      } else {
        if (firstException == null) {
          firstException = new Throwable(bulkResultItem.error);
        }
      }
    }
    if (throwable == null) {
      throwable = firstException;
    }
  }

  public boolean canRetry() {
    return throwable == null || !throwable.getMessage().equals(nonRetriableError);
  }

  public JestResult getResult() {
    return result;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public boolean hasFailures() {
    return !result.isSucceeded();
  }

  private ObjectNode parseError(String error) {
    if (error != null && !error.trim().isEmpty()) {
      try {
        return (ObjectNode) new ObjectMapper().readTree(error);
      } catch (IOException e) {
        log.error("Exception when parsing to JSON:", e);
      }
    }
    return JsonNodeFactory.instance.objectNode();
  }
}
