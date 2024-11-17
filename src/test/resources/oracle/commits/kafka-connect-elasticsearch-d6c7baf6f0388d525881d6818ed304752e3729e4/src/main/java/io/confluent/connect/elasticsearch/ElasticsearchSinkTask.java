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

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

public class ElasticsearchSinkTask extends SinkTask {

  private static final Logger log = LoggerFactory.getLogger(ElasticsearchSinkTask.class);
  private ElasticsearchWriter writer;
  private JestClient client;

  @Override
  public String version() {
    return Version.getVersion();
  }

  @Override
  public void start(Map<String, String> props) {
    start(props, null);
  }

  // public for testing
  public void start(Map<String, String> props, JestClient client) {
    try {
      log.info("Starting ElasticsearchSinkTask.");

      ElasticsearchSinkConnectorConfig config = new ElasticsearchSinkConnectorConfig(props);
      String type = config.getString(ElasticsearchSinkConnectorConfig.TYPE_NAME_CONFIG);
      boolean ignoreKey = config.getBoolean(ElasticsearchSinkConnectorConfig.KEY_IGNORE_CONFIG);
      boolean ignoreSchema = config.getBoolean(ElasticsearchSinkConnectorConfig.SCHEMA_IGNORE_CONFIG);

      List<String> topicIndex = config.getList(ElasticsearchSinkConnectorConfig.TOPIC_INDEX_MAP_CONFIG);
      List<String> topicIgnoreKey = config.getList(ElasticsearchSinkConnectorConfig.TOPIC_KEY_IGNORE_CONFIG);
      List<String> topicIgnoreSchema = config.getList(ElasticsearchSinkConnectorConfig.TOPIC_SCHEMA_IGNORE_CONFIG);
      Map<String, TopicConfig> topicConfigs = constructTopicConfig(topicIndex, topicIgnoreKey, topicIgnoreSchema);

      long flushTimeoutMs = config.getLong(ElasticsearchSinkConnectorConfig.FLUSH_TIMEOUT_MS_CONFIG);
      int maxBufferedRecords = config.getInt(ElasticsearchSinkConnectorConfig.MAX_BUFFERED_RECORDS_CONFIG);
      int batchSize = config.getInt(ElasticsearchSinkConnectorConfig.BATCH_SIZE_CONFIG);
      long lingerMs = config.getLong(ElasticsearchSinkConnectorConfig.LINGER_MS_CONFIG);
      int maxInFlightRequests = config.getInt(ElasticsearchSinkConnectorConfig.MAX_IN_FLIGHT_REQUESTS_CONFIG);
      long retryBackoffMs = config.getLong(ElasticsearchSinkConnectorConfig.RETRY_BACKOFF_MS_CONFIG);
      int maxRetry = config.getInt(ElasticsearchSinkConnectorConfig.MAX_RETRIES_CONFIG);

      if (client != null) {
        this.client = client;
      } else {
        String address = config.getString(ElasticsearchSinkConnectorConfig.CONNECTION_URL_CONFIG);
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(address).multiThreaded(true).build());
        this.client = factory.getObject();
      }

      ElasticsearchWriter.Builder builder = new ElasticsearchWriter.Builder(this.client)
          .setType(type)
          .setIgnoreKey(ignoreKey)
          .setIgnoreSchema(ignoreSchema)
          .setTopicConfigs(topicConfigs)
          .setFlushTimoutMs(flushTimeoutMs)
          .setMaxBufferedRecords(maxBufferedRecords)
          .setMaxInFlightRequests(maxInFlightRequests)
          .setBatchSize(batchSize)
          .setLingerMs(lingerMs)
          .setRetryBackoffMs(retryBackoffMs)
          .setMaxRetry(maxRetry);

      writer = builder.build();
      writer.start();
    } catch (ConfigException e) {
      throw new ConnectException("Couldn't start ElasticsearchSinkTask due to configuration error:", e);
    }
  }

  @Override
  public void open(Collection<TopicPartition> partitions) {
    log.debug("Opening the task for topic partitions: {}", partitions);
    Set<String> topics = new HashSet<>();
    for (TopicPartition tp : partitions) {
      topics.add(tp.topic());
    }
    writer.createIndices(topics);
  }

  @Override
  public void put(Collection<SinkRecord> records) throws ConnectException {
    log.trace("Putting {} to Elasticsearch.", records);
    writer.write(records);
  }

  @Override
  public void flush(Map<TopicPartition, OffsetAndMetadata> offsets) {
    log.trace("Flushing data to Elasticsearch with the following offsets: {}", offsets);
    writer.flush();
  }

  @Override
  public void close(Collection<TopicPartition> partitions) {
    log.debug("Closing the task for topic partitions: {}", partitions);
  }

  @Override
  public void stop() throws ConnectException {
    log.info("Stopping ElasticsearchSinkTask.");
    if (writer != null) {
      writer.stop();
    }
    if (client != null) {
      client.shutdownClient();
    }
  }

  private Map<String, String> parseMapConfig(List<String> values) {
    Map<String, String> map = new HashMap<>();
    for (String value: values) {
      String[] parts = value.split(":");
      String topic = parts[0];
      String type = parts[1];
      map.put(topic, type);
    }
    return map;
  }

  private Map<String, TopicConfig> constructTopicConfig(List<String> topicType, List<String> topicIgnoreKey, List<String> topicIgnoreSchema) {
    Map<String, TopicConfig> topicConfigMap = new HashMap<>();
    Map<String, String> topicTypeMap = parseMapConfig(topicType);
    Set<String> topicIgnoreKeySet = new HashSet<>(topicIgnoreKey);
    Set<String> topicIgnoreSchemaSet = new HashSet<>(topicIgnoreSchema);

    for (String topic: topicTypeMap.keySet()) {
      String type = topicTypeMap.get(topic);
      TopicConfig topicConfig = new TopicConfig(type, topicIgnoreKeySet.contains(topic), topicIgnoreSchemaSet.contains(topic));
      topicConfigMap.put(topic, topicConfig);
    }
    return topicConfigMap;
  }

}
