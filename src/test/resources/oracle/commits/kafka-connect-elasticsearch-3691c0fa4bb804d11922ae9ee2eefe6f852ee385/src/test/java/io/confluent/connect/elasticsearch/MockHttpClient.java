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

package io.confluent.connect.elasticsearch;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import io.confluent.connect.elasticsearch.internals.Callback;
import io.confluent.connect.elasticsearch.internals.Client;
import io.confluent.connect.elasticsearch.internals.RecordBatch;
import io.confluent.connect.elasticsearch.internals.Response;
import io.searchbox.core.BulkResult;

public class MockHttpClient implements Client<Response> {

  private static final Logger log = LoggerFactory.getLogger(MockHttpClient.class);
  private int numRequestsToFail;
  private Gson gson = new Gson();
  private Random random = new Random();
  private final boolean retriable;

  public MockHttpClient(int numRequestsToFail) {
    this.numRequestsToFail = numRequestsToFail;
    this.retriable = true;
  }

  public MockHttpClient(int numRequestsToFail, boolean retriable) {
    this.numRequestsToFail = numRequestsToFail;
    this.retriable = retriable;
  }

  @Override
  public void execute(RecordBatch batch, Callback<Response> callback) {
    try {
      boolean shouldFail;
      synchronized (this) {
        shouldFail = numRequestsToFail > 0;
        numRequestsToFail--;
      }
      int idxToFail = shouldFail ? random.nextInt(batch.size() - 1) : -1;
      JsonObject obj = createResponse(idxToFail, batch.size());
      BulkResult result = new BulkResult(gson);
      result.setJsonObject(obj);
      result.setSucceeded(!shouldFail);
      callback.onResponse(new Response(result));
    } catch (Exception e) {
      log.error("Exception:", e);
    }
  }

  @Override
  public void close() {
    // do nothing
  }

  private JsonObject createResponse(int idxToFail, int numRequests) {
    JsonObject result = new JsonObject();
    result.addProperty("errors", idxToFail > 0);
    JsonArray items = new JsonArray();
    result.add("items", items);
    for (int i = 0; i < numRequests; ++i) {
      if (i == idxToFail) {
        items.add(createFailure(i, retriable));
      } else {
        items.add(createSuccess(i));
      }
    }
    return result;
  }

  private JsonObject createSuccess(int id) {
    return createResponse(id, false, false);
  }

  private JsonObject createFailure(int id, boolean retriable) {
    return createResponse(id, true, retriable);
  }

  private JsonObject createResponse(int id, boolean fail, boolean retriable) {
    String index = "test";
    String type = "kafka-connect";
    JsonObject request = new JsonObject();
    JsonObject item = new JsonObject();
    request.add("index", item);
    item.addProperty("_index", index);
    item.addProperty("_type", type);
    item.addProperty("_id", id);
    if (fail) {
      item.addProperty("status", 409);
      item.add("error", createException(retriable? "retriable_exception": "mapper_parse_exception"));
    } else {
      item.addProperty("status", 202);
    }
    return request;
  }

  private JsonObject createException(String message) {
    JsonObject obj = new JsonObject();
    obj.addProperty("type", message);
    return obj;
  }

}
