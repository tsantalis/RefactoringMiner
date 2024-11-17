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

import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
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

import io.confluent.connect.elasticsearch.internals.BulkProcessor;
import io.confluent.connect.elasticsearch.internals.ESRequest;
import io.confluent.connect.elasticsearch.internals.HttpClient;
import io.confluent.connect.elasticsearch.internals.Listener;
import io.confluent.connect.elasticsearch.internals.RecordBatch;
import io.confluent.connect.elasticsearch.internals.Response;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;

/**
 * The ElasticsearchWriter handles connections to Elasticsearch, sending data and flush.
 * Transport client is used to send requests to Elasticsearch cluster. Requests are batched
 * when sending to Elasticsearch. To ensure delivery guarantee and order, we retry in case of
 * failures for a batch.
 *
 * Currently, we only send out requests to Elasticsearch when flush is called, which is not
 * desirable from the latency point of view.
 *
 * TODO: Use offset as external version to fence requests with lower version.
 */
public class ElasticsearchWriter {
  private static final Logger log = LoggerFactory.getLogger(ElasticsearchWriter.class);

  private final JestClient client;
  private final BulkProcessor bulkProcessor;
  private final String type;
  private final boolean ignoreKey;
  private final boolean ignoreSchema;
  private final Map<String, TopicConfig> topicConfigs;
  private final long flushTimeoutMs;
  private final long maxBufferedRecords;
  private final Set<String> mappings;

  /**
   * ElasticsearchWriter constructor
   * @param client The client to connect to Elasticsearch.
   * @param type The type to use when writing to Elasticsearch.
   * @param ignoreKey Whether to ignore key during indexing.
   * @param ignoreSchema Whether to ignore schema during indexing.
   * @param topicConfigs The map of per topic configs.
   * @param flushTimeoutMs The flush timeout.
   * @param maxBufferedRecords The max number of buffered records.
   * @param maxInFlightRequests The max number of inflight requests allowed.
   * @param batchSize Approximately the max number of records each writer will buffer.
   * @param lingerMs The time to wait before sending a batch.
   */
  ElasticsearchWriter(
      JestClient client,
      String type,
      boolean ignoreKey,
      boolean ignoreSchema,
      Map<String, TopicConfig> topicConfigs,
      long flushTimeoutMs,
      long maxBufferedRecords,
      int maxInFlightRequests,
      int batchSize,
      long lingerMs,
      int maxRetry,
      long retryBackoffMs) {

    this.client = client;
    this.type = type;
    this.ignoreKey = ignoreKey;
    this.ignoreSchema = ignoreSchema;

    this.topicConfigs = topicConfigs == null ? Collections.<String, TopicConfig>emptyMap() : topicConfigs;

    this.flushTimeoutMs = flushTimeoutMs;
    this.maxBufferedRecords  = maxBufferedRecords;

    // Start the BulkProcessor
    bulkProcessor = new BulkProcessor(new HttpClient(client), maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs, createDefaultListener());

    //Create mapping cache
    mappings = new HashSet<>();
  }

  public static class Builder {
    private final JestClient client;
    private String type;
    private boolean ignoreKey = false;
    private boolean ignoreSchema = false;
    private Map<String, TopicConfig> topicConfigs = new HashMap<>();
    private long flushTimeoutMs;
    private long maxBufferedRecords;
    private int maxInFlightRequests;
    private int batchSize;
    private long lingerMs;
    private int maxRetry;
    private long retryBackoffMs;

    /**
     * Constructor of ElasticsearchWriter Builder.
     * @param client The client to connect to Elasticsearch.
     */
    public Builder(JestClient client) {
      this.client = client;
    }

    /**
     * Set the index.
     * @param type The type to use for each index.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    /**
     * Set whether to ignore key during indexing.
     * @param ignoreKey Whether to ignore key.
     * @return an instance of ElasticsearchWriter Builder.
     */

    public Builder setIgnoreKey(boolean ignoreKey) {
      this.ignoreKey = ignoreKey;
      return this;
    }

    /**
     * Set whether to ignore schema during indexing.
     * @param ignoreSchema Whether to ignore key.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setIgnoreSchema(boolean ignoreSchema) {
      this.ignoreSchema = ignoreSchema;
      return this;
    }

    /**
     * Set per topic configurations.
     * @param topicConfigs The map of per topic configuration.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setTopicConfigs(Map<String, TopicConfig> topicConfigs) {
      this.topicConfigs = topicConfigs;
      return this;
    }

    /**
     * Set the flush timeout.
     * @param flushTimeoutMs The flush timeout in milliseconds.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setFlushTimoutMs(long flushTimeoutMs) {
      this.flushTimeoutMs = flushTimeoutMs;
      return this;
    }

    /**
     * Set the max number of records to buffer for each writer.
     * @param maxBufferedRecords The max number of buffered records.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setMaxBufferedRecords(long maxBufferedRecords) {
      this.maxBufferedRecords = maxBufferedRecords;
      return this;
    }

    /**
     * Set the max number of inflight requests.
     * @param maxInFlightRequests The max allowed number of inflight requests.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setMaxInFlightRequests(int maxInFlightRequests) {
      this.maxInFlightRequests = maxInFlightRequests;
      return this;
    }

    /**
     * Set the number of requests to process as a batch when writing.
     * to Elasticsearch.
     * @param batchSize the size of each batch.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    /**
     * Set the linger time.
     * @param lingerMs The linger time to use in milliseconds.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setLingerMs(long lingerMs) {
      this.lingerMs = lingerMs;
      return this;
    }

    /**
     * Set the max retry for a batch
     * @param maxRetry The number of max retry.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setMaxRetry(int maxRetry) {
      this.maxRetry = maxRetry;
      return this;
    }

    /**
     * Set the retry backoff.
     * @param retryBackoffMs The retry backoff in milliseconds.
     * @return an instance of ElasticsearchWriter Builder.
     */
    public Builder setRetryBackoffMs(long retryBackoffMs) {
      this.retryBackoffMs = retryBackoffMs;
      return this;
    }

    /**
     * Build the ElasticsearchWriter.
     * @return an instance of ElasticsearchWriter.
     */
    public ElasticsearchWriter build() {
      return new ElasticsearchWriter(
          client, type, ignoreKey, ignoreSchema, topicConfigs, flushTimeoutMs, maxBufferedRecords, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs);
    }
  }

  public void write(Collection<SinkRecord> records) {
    if (bulkProcessor.getException() != null) {
      throw new ConnectException("BulkProcessor failed with non-retriable exception", bulkProcessor.getException());
    }
    if (bulkProcessor.getTotalBufferedRecords() + records.size() > maxBufferedRecords) {
      throw new RetriableException("Exceeded max number of buffered records: " + maxBufferedRecords);
    }
    for (SinkRecord record: records) {
      ESRequest request = DataConverter.convertRecord(record, type, client, ignoreKey, ignoreSchema, topicConfigs, mappings);
      bulkProcessor.add(request);
    }
  }

  public void flush() {
    try {
      if (!bulkProcessor.flush(flushTimeoutMs)) {
        throw new ConnectException("Cannot finish flush messages within " + flushTimeoutMs);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Throwable t) {
      throw new ConnectException("Flush failed with non retriable exception.", t);
    }
  }

  public void start() {
    bulkProcessor.start();
  }

  public void stop() {
    bulkProcessor.stop();
    try {
      bulkProcessor.awaitStop(flushTimeoutMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Throwable t) {
      throw new ConnectException("Close failed with non retriable exception", t);
    }
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
    for (String topic: assignedTopics) {
      final TopicConfig topicConfig = topicConfigs.get(topic);
      if (topicConfig != null) {
        indices.add(topicConfig.getIndex());
      } else {
        indices.add(topic);
      }
    }
    for (String index: indices) {
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

  private Listener createDefaultListener() {
    return new Listener() {
      @Override
      public void beforeBulk(long executionId, RecordBatch batch) {

      }

      @Override
      public void afterBulk(long executionId, RecordBatch batch, Response response) {

      }

      @Override
      public void afterBulk(long executionId, RecordBatch batch, Throwable failure) {

      }
    };
  }
}
