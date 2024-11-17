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

import org.apache.kafka.common.utils.SystemTime;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.confluent.connect.elasticsearch.bulk.BulkProcessor;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;

// TODO: Use offset as external version to fence requests with lower version.
public class ElasticsearchWriter {
  private static final Logger log = LoggerFactory.getLogger(ElasticsearchWriter.class);

  private final JestClient client;
  private final String type;
  private final boolean ignoreKey;
  private final boolean ignoreSchema;
  private final Map<String, TopicConfig> topicConfigs;
  private final long flushTimeoutMs;

  private final Set<String> mappings;
  private final BulkProcessor<IndexingRequest, ?> bulkProcessor;

  ElasticsearchWriter(
      JestClient client,
      String type,
      boolean ignoreKey,
      boolean ignoreSchema,
      Map<String, TopicConfig> topicConfigs,
      long flushTimeoutMs,
      int maxBufferedRecords,
      int maxInFlightRequests,
      int batchSize,
      long lingerMs,
      int maxRetries,
      long retryBackoffMs
  ) {
    this.client = client;
    this.type = type;
    this.ignoreKey = ignoreKey;
    this.ignoreSchema = ignoreSchema;
    this.topicConfigs = topicConfigs == null ? Collections.<String, TopicConfig>emptyMap() : topicConfigs;
    this.flushTimeoutMs = flushTimeoutMs;

    mappings = new HashSet<>();

    bulkProcessor = new BulkProcessor<>(
        new SystemTime(),
        new BulkIndexingClient(client),
        maxBufferedRecords,
        maxInFlightRequests,
        batchSize,
        lingerMs,
        maxRetries,
        retryBackoffMs
    );
  }

  public static class Builder {
    private final JestClient client;
    private String type;
    private boolean ignoreKey = false;
    private boolean ignoreSchema = false;
    private Map<String, TopicConfig> topicConfigs = new HashMap<>();
    private long flushTimeoutMs;
    private int maxBufferedRecords;
    private int maxInFlightRequests;
    private int batchSize;
    private long lingerMs;
    private int maxRetry;
    private long retryBackoffMs;

    public Builder(JestClient client) {
      this.client = client;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setIgnoreKey(boolean ignoreKey) {
      this.ignoreKey = ignoreKey;
      return this;
    }

    public Builder setIgnoreSchema(boolean ignoreSchema) {
      this.ignoreSchema = ignoreSchema;
      return this;
    }

    public Builder setTopicConfigs(Map<String, TopicConfig> topicConfigs) {
      this.topicConfigs = topicConfigs;
      return this;
    }

    public Builder setFlushTimoutMs(long flushTimeoutMs) {
      this.flushTimeoutMs = flushTimeoutMs;
      return this;
    }

    public Builder setMaxBufferedRecords(int maxBufferedRecords) {
      this.maxBufferedRecords = maxBufferedRecords;
      return this;
    }

    public Builder setMaxInFlightRequests(int maxInFlightRequests) {
      this.maxInFlightRequests = maxInFlightRequests;
      return this;
    }

    public Builder setBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder setLingerMs(long lingerMs) {
      this.lingerMs = lingerMs;
      return this;
    }

    public Builder setMaxRetry(int maxRetry) {
      this.maxRetry = maxRetry;
      return this;
    }

    public Builder setRetryBackoffMs(long retryBackoffMs) {
      this.retryBackoffMs = retryBackoffMs;
      return this;
    }

    public ElasticsearchWriter build() {
      return new ElasticsearchWriter(
          client, type, ignoreKey, ignoreSchema, topicConfigs, flushTimeoutMs, maxBufferedRecords, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs
      );
    }
  }

  public void write(Collection<SinkRecord> records) {
    for (SinkRecord record : records) {
      IndexingRequest request = DataConverter.convertRecord(record, type, client, ignoreKey, ignoreSchema, topicConfigs, mappings);
      bulkProcessor.add(request);
    }
  }

  public void flush() {
    bulkProcessor.flush(flushTimeoutMs);
  }

  public void start() {
    bulkProcessor.start();
  }

  public void stop() {
    try {
      bulkProcessor.flush(flushTimeoutMs);
    } catch (Exception e) {
      log.warn("Failed to flush during stop", e);
    }
    bulkProcessor.stop();
    bulkProcessor.awaitStop(flushTimeoutMs);
  }

  private boolean indexExists(String index) {
    Action action = new IndicesExists.Builder(index).build();
    try {
      JestResult result = client.execute(action);
      return result.isSucceeded();
    } catch (IOException e) {
      throw new ConnectException(e);
    }
  }

  public void createIndices(Set<String> assignedTopics) {
    Set<String> indices = new HashSet<>();
    for (String topic : assignedTopics) {
      final TopicConfig topicConfig = topicConfigs.get(topic);
      if (topicConfig != null) {
        indices.add(topicConfig.getIndex());
      } else {
        indices.add(topic);
      }
    }
    for (String index : indices) {
      if (!indexExists(index)) {
        CreateIndex createIndex = new CreateIndex.Builder(index).build();
        try {
          JestResult result = client.execute(createIndex);
          if (!result.isSucceeded()) {
            throw new ConnectException("Could not create index:" + index);
          }
        } catch (IOException e) {
          throw new ConnectException(e);
        }
      }
    }
  }

}
