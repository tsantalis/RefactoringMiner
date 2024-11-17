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

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

import java.util.Map;

public class ElasticsearchSinkConnectorConfig extends AbstractConfig {

  private static final String ELASTICSEARCH_GROUP = "Elasticsearch";
  private static final String CONNECTOR_GROUP = "Connector";

  public static final String CONNECTION_URL_CONFIG = "connection.url";
  private static final String CONNECTION_URL_DOC = "The URL to connect to Elasticsearch.";
  private static final String CONNECTION_URL_DISPLAY = "Connection URL";

  public static final String TYPE_NAME_CONFIG = "type.name";
  private static final String TYPE_NAME_DOC = "The type to use for each index.";
  private static final String TYPE_NAME_DISPLAY = "Type Name";

  public static final String KEY_IGNORE_CONFIG = "key.ignore";
  private static final String KEY_IGNORE_DOC =
      "Whether to ignore the key during indexing. When this is set to true, only the value from the message will be written to Elasticsearch."
      + "Note that this is a global config that applies to all topics. If this is set to true, "
      + "Use ``topic.key.ignore`` to config for different topics. This value will be overridden by the per topic configuration.";
  private static final boolean KEY_IGNORE_DEFAULT = false;
  private static final String KEY_IGNORE_DISPLAY = "Ignore Key";

  // TODO: remove this config when single message transform is in
  public static final String TOPIC_INDEX_MAP_CONFIG = "topic.index.map";
  private static final String TOPIC_INDEX_MAP_DOC = "The map between Kafka topics and Elasticsearch indices.";
  private static final String TOPIC_INDEX_MAP_DEFAULT = "";
  private static final String TOPIC_INDEX_MAP_DISPLAY = "Topic to Type";

  public static final String TOPIC_KEY_IGNORE_CONFIG = "topic.key.ignore";
  private static final String TOPIC_KEY_IGNORE_DOC =
      "A list of topics to ignore key when indexing. In case that the key for a topic can be null, you should include the topic in this config "
      + "in order to generate a valid document id.";
  private static final String TOPIC_KEY_IGNORE_DEFAULT = "";
  private static final String TOPIC_KEY_IGNORE_DISPLAY = "Topics to Ignore Key";

  public static final String FLUSH_TIMEOUT_MS_CONFIG = "flush.timeout.ms";
  private static final String FLUSH_TIMEOUT_MS_DOC = "The timeout when flushing data to Elasticsearch.";
  private static final long FLUSH_TIMEOUT_MS_DEFAULT = 10000;
  private static final String FLUSH_TIMEOUT_MS_DISPLAY = "Flush Timeout (ms)";

  public static final String MAX_BUFFERED_RECORDS_CONFIG = "max.buffered.records";
  private static final String MAX_BUFFERED_RECORDS_DOC =
      "Approximately the max number of records each task will buffer. This config controls the memory usage for each task.";
  private static final int MAX_BUFFERED_RECORDS_DEFAULT = 20000;
  private static final String MAX_BUFFERED_RECORDS_DISPLAY = "Max Number of Records to Buffer";

  public static final String BATCH_SIZE_CONFIG = "batch.size";
  private static final String BATCH_SIZE_DOC = "The number of requests to process as a batch when writing to Elasticsearch.";
  private static final int BATCH_SIZE_DEFAULT = 2000;
  private static final String BATCH_SIZE_DISPLAY = "Batch Size";

  public static final String LINGER_MS_CONFIG = "linger.ms";
  private static final String LINGER_MS_DOC =
      "The task groups together any records that arrive in between request transmissions into a single batched request. "
      + "Normally this occurs only under load when records arrive faster than they can be sent out. However in some circumstances the "
      + "tasks may want to reduce the number of requests even under moderate load. This setting accomplishes this by adding a small amount "
      + "of artificial delay. Rather than immediately sending out a record the task will wait for up to the given delay to allow other "
      + "records to be sent so that the sends can be batched together.";
  private static final long LINGER_MS_DEFAULT = 1;
  private static final String LINGER_MS_DISPLAY = "Linger (ms)";

  public static final String MAX_IN_FLIGHT_REQUESTS_CONFIG = "max.in.flight.requests";
  private static final String MAX_IN_FLIGHT_REQUESTS_DOC =
      "The maximum number of incomplete batches each task will send before blocking. Note that if this is set to be greater "
      + "than 1 and there are failed sends, there is a risk of message re-ordering due to retries";
  private static final int MAX_IN_FLIGHT_REQUESTS_DEFAULT = 5;
  private static final String MAX_IN_FLIGHT_REQUESTS_DISPLAY = "Max in Flight Requests";

  public static final String RETRY_BACKOFF_MS_CONFIG = "retry.backoff.ms";
  private static final String RETRY_BACKOFF_MS_DOC =
      "The amount of time to wait before attempting to retry a failed batch. "
      + "This avoids repeatedly sending requests in a tight loop under some failure scenarios.";
  private static final long RETRY_BACKOFF_MS_DEFAULT = 100L;
  private static final String RETRY_BACKOFF_MS_DISPLAY = "Retry Backoff (ms)";

  public static final String MAX_RETRIES_CONFIG = "max.retries";
  private static final String MAX_RETRIES_DOC = "The max allowed number of retries. Allowing retries will potentially change the ordering of records.";
  private static final int MAX_RETRIES_DEFAULT = 5;
  private static final String MAX_RETRIES_DISPLAY = "Max Retries";

  public static final String SCHEMA_IGNORE_CONFIG = "schema.ignore";
  private static final String SCHEMA_IGNORE_DOC =
      "Whether to ignore schemas during indexing. When this is set to true, the schema in ``SinkRecord`` will be ignored and Elasticsearch will infer the mapping from data. "
      + "Note that this is a global config that applies to all topics."
      + "Use ``topic.schema.ignore`` to config for different topics. This value will be overridden by the per topic configuration.";
  private static final boolean SCHEMA_IGNORE_DEFAULT = false;
  private static final String SCHEMA_IGNORE_DISPLAY = "Ignore Schema";

  public static final String TOPIC_SCHEMA_IGNORE_CONFIG = "topic.schema.ignore";
  private static final String TOPIC_SCHEMA_IGNORE_DOC = "A list of topics to ignore schema.";
  private static final String TOPIC_SCHEMA_IGNORE_DEFAULT = "";
  private static final String TOPIC_SCHEMA_IGNORE_DISPLAY = "Topics to Ignore Schema";

  public static ConfigDef baseConfigDef() {
    return new ConfigDef()
        .define(CONNECTION_URL_CONFIG, Type.STRING, Importance.HIGH, CONNECTION_URL_DOC, ELASTICSEARCH_GROUP, 1, Width.LONG,
                CONNECTION_URL_DISPLAY)
        .define(TYPE_NAME_CONFIG, Type.STRING, Importance.HIGH, TYPE_NAME_DOC, ELASTICSEARCH_GROUP, 2, Width.SHORT, TYPE_NAME_DISPLAY)
        .define(KEY_IGNORE_CONFIG, Type.BOOLEAN, KEY_IGNORE_DEFAULT, Importance.HIGH, KEY_IGNORE_DOC, CONNECTOR_GROUP, 3, Width.SHORT, KEY_IGNORE_DISPLAY)
        .define(BATCH_SIZE_CONFIG, Type.INT, BATCH_SIZE_DEFAULT, Importance.MEDIUM, BATCH_SIZE_DOC, CONNECTOR_GROUP, 4, Width.SHORT, BATCH_SIZE_DISPLAY)
        .define(MAX_IN_FLIGHT_REQUESTS_CONFIG, Type.INT, MAX_IN_FLIGHT_REQUESTS_DEFAULT, Importance.MEDIUM,
                MAX_IN_FLIGHT_REQUESTS_DOC, CONNECTOR_GROUP, 5, Width.SHORT,
                MAX_IN_FLIGHT_REQUESTS_DISPLAY)
        .define(TOPIC_INDEX_MAP_CONFIG, Type.LIST, TOPIC_INDEX_MAP_DEFAULT, Importance.LOW, TOPIC_INDEX_MAP_DOC, CONNECTOR_GROUP, 6, Width.LONG, TOPIC_INDEX_MAP_DISPLAY)
        .define(TOPIC_KEY_IGNORE_CONFIG, Type.LIST, TOPIC_KEY_IGNORE_DEFAULT, Importance.LOW, TOPIC_KEY_IGNORE_DOC, CONNECTOR_GROUP, 7, Width.LONG, TOPIC_KEY_IGNORE_DISPLAY)
        .define(SCHEMA_IGNORE_CONFIG, Type.BOOLEAN, SCHEMA_IGNORE_DEFAULT, Importance.LOW, SCHEMA_IGNORE_DOC, CONNECTOR_GROUP, 8, Width.SHORT, SCHEMA_IGNORE_DISPLAY)
        .define(TOPIC_SCHEMA_IGNORE_CONFIG, Type.LIST, TOPIC_SCHEMA_IGNORE_DEFAULT, Importance.LOW, TOPIC_SCHEMA_IGNORE_DOC, CONNECTOR_GROUP, 9, Width.LONG, TOPIC_SCHEMA_IGNORE_DISPLAY)
        .define(LINGER_MS_CONFIG, Type.LONG, LINGER_MS_DEFAULT, Importance.LOW, LINGER_MS_DOC, CONNECTOR_GROUP, 10, Width.SHORT, LINGER_MS_DISPLAY)
        .define(RETRY_BACKOFF_MS_CONFIG, Type.LONG, RETRY_BACKOFF_MS_DEFAULT, Importance.LOW, RETRY_BACKOFF_MS_DOC, CONNECTOR_GROUP, 11, Width.SHORT, RETRY_BACKOFF_MS_DISPLAY)
        .define(MAX_RETRIES_CONFIG, Type.INT, MAX_RETRIES_DEFAULT, Importance.LOW, MAX_RETRIES_DOC, CONNECTOR_GROUP, 12, Width.SHORT, MAX_RETRIES_DISPLAY)
        .define(FLUSH_TIMEOUT_MS_CONFIG, Type.LONG, FLUSH_TIMEOUT_MS_DEFAULT, Importance.LOW, FLUSH_TIMEOUT_MS_DOC, CONNECTOR_GROUP, 13, Width.SHORT, FLUSH_TIMEOUT_MS_DISPLAY)
        .define(MAX_BUFFERED_RECORDS_CONFIG, Type.INT, MAX_BUFFERED_RECORDS_DEFAULT, Importance.LOW, MAX_BUFFERED_RECORDS_DOC, CONNECTOR_GROUP, 14, Width.SHORT, MAX_BUFFERED_RECORDS_DISPLAY);
  }

  static ConfigDef config = baseConfigDef();

  public ElasticsearchSinkConnectorConfig(Map<String, String> props) {
    super(config, props);
  }

  public static void main(String[] args) {
    System.out.println(config.toRst());
  }
}
