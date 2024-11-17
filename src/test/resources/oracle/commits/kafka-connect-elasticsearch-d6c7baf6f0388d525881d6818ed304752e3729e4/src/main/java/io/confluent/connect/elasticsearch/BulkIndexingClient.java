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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.confluent.connect.elasticsearch.bulk.BulkClient;
import io.confluent.connect.elasticsearch.bulk.BulkResponse;
import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;

public class BulkIndexingClient implements BulkClient<IndexingRequest, Bulk> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Set<String> NON_RETRIABLE_ERROR_TYPES = Collections.singleton("mapper_parse_exception");

  private final JestClient client;

  public BulkIndexingClient(JestClient client) {
    this.client = client;
  }

  @Override
  public Bulk bulkRequest(List<IndexingRequest> batch) {
    final Bulk.Builder builder = new Bulk.Builder();
    for (IndexingRequest request : batch) {
      builder.addAction(
          new Index.Builder(request.getPayload())
              .index(request.getIndex())
              .type(request.getType())
              .id(request.getId())
              .build()
      );
    }
    return builder.build();
  }

  @Override
  public BulkResponse execute(Bulk bulk) throws IOException {
    return toBulkResponse(client.execute(bulk));
  }

  private static BulkResponse toBulkResponse(BulkResult result) {
    if (result.isSucceeded()) {
      return BulkResponse.success();
    }

    final List<BulkResult.BulkResultItem> failedItems = result.getFailedItems();
    if (failedItems.isEmpty()) {
      return BulkResponse.failure(true, result.getErrorMessage());
    }

    boolean retriable = true;
    final List<String> errors = new ArrayList<>(failedItems.size());
    for (BulkResult.BulkResultItem failedItem : failedItems) {
      errors.add(failedItem.error);
      retriable &= isRetriableError(failedItem.error);
    }
    return BulkResponse.failure(retriable, errors.toString());
  }

  private static boolean isRetriableError(String error) {
    if (error != null && !error.trim().isEmpty()) {
      try {
        final ObjectNode parsedError = (ObjectNode) OBJECT_MAPPER.readTree(error);
        return !NON_RETRIABLE_ERROR_TYPES.contains(parsedError.get("type").asText());
      } catch (IOException e) {
        return true;
      }
    }
    return true;
  }

}
