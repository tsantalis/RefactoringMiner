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

import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.storage.Converter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.confluent.connect.elasticsearch.internals.ESRequest;
import io.searchbox.client.JestClient;

import static io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConstants.MAP_KEY;
import static io.confluent.connect.elasticsearch.ElasticsearchSinkConnectorConstants.MAP_VALUE;

public class DataConverter {

  private static final Converter JSON_CONVERTER;
  static {
    JSON_CONVERTER = new JsonConverter();
    JSON_CONVERTER.configure(Collections.singletonMap("schemas.enable", "false"), false);
  }

  /**
   * Convert the key to the string representation.
   *
   * @param key The key of a SinkRecord.
   * @param keySchema The key schema.
   * @return The string representation of the key.
   * @throws ConnectException if the key is null.
   */
  public static String convertKey(Object key, Schema keySchema) {
    if (key == null) {
      throw new ConnectException("Key is used as document id and can not be null.");
    }
    Schema.Type schemaType;
    if (keySchema == null) {
      schemaType = ConnectSchema.schemaType(key.getClass());
      if (schemaType == null)
        throw new DataException("Java class " + key.getClass() + " does not have corresponding schema type.");
    } else {
      schemaType = keySchema.type();
    }

    switch (schemaType) {
      case INT8:
      case INT16:
      case INT32:
      case INT64:
      case STRING:
        return String.valueOf(key);
      default:
        throw new DataException(schemaType.name() + " is not supported as the document id.");
    }
  }

  /**
   * Convert a SinkRecord to an IndexRequest.
   *
   * @param record The SinkRecord to be converted.
   * @param client The client to connect to Elasticsearch.
   * @param ignoreKey Whether to ignore the key during indexing.
   * @param ignoreSchema Whether to ignore the schema during indexing.
   * @param topicConfigs The map of per topic configs.
   * @param mappings The mapping cache.
   * @return The converted IndexRequest.
   */

  public static ESRequest convertRecord(
      SinkRecord record,
      String type,
      JestClient client,
      boolean ignoreKey,
      boolean ignoreSchema,
      Map<String, TopicConfig> topicConfigs,
      Set<String> mappings) {

    String topic = record.topic();
    int partition = record.kafkaPartition();
    long offset = record.kafkaOffset();

    Object key = record.key();
    Schema keySchema = record.keySchema();
    Object value = record.value();
    Schema valueSchema = record.valueSchema();

    String index;
    String id;
    boolean topicIgnoreKey;
    boolean topicIgnoreSchema;

    if (topicConfigs.containsKey(topic)) {
      TopicConfig topicConfig = topicConfigs.get(topic);
      index = topicConfig.getIndex();
      topicIgnoreKey = topicConfig.ignoreKey();
      topicIgnoreSchema = topicConfig.ignoreSchema();
    } else {
      index = topic;
      topicIgnoreKey = ignoreKey;
      topicIgnoreSchema = ignoreSchema;
    }

    if (topicIgnoreKey) {
      id = topic + "+" + String.valueOf(partition) + "+" + String.valueOf(offset);
    } else {
      id = DataConverter.convertKey(key, keySchema);
    }

    try {
      if (!topicIgnoreSchema && !mappings.contains(index) && !Mapping.doesMappingExist(client, index, type, mappings)) {
        Mapping.createMapping(client, index, type, valueSchema);
        mappings.add(index);
      }
    } catch (IOException e) {
      // TODO: It is possible that two clients are creating the mapping at the same time and
      // one request to create mapping may fail. In this case, we should allow the task to
      // proceed instead of throw the exception.
      throw new ConnectException("Cannot create mapping:", e);
    }

    Schema newSchema;
    Object newValue;
    if (!topicIgnoreSchema) {
      newSchema = preProcessSchema(valueSchema);
      newValue = preProcessValue(value, valueSchema, newSchema);
    } else {
      newSchema = valueSchema;
      newValue = value;
    }

    byte[] json = JSON_CONVERTER.fromConnectData(topic, newSchema, newValue);
    return new ESRequest(index, type, id, json);
  }

  // We need to pre process the Kafka Connect schema before converting to JSON as Elasticsearch
  // expects a different JSON format from the current JSON converter provides. Rather than completely
  // rewrite a converter for Elasticsearch, we will refactor the JSON converter to support customized
  // translation. The pre process is no longer needed once we have the JSON converter refactored.
  static Schema preProcessSchema(Schema schema) {
    if (schema == null) {
      return null;
    }
    // Handle logical types
    SchemaBuilder builder;
    String schemaName = schema.name();
    if (schemaName != null) {
      switch (schemaName) {
        case Decimal.LOGICAL_NAME:
          builder = SchemaBuilder.float64();
          return builder.build();
        case Date.LOGICAL_NAME:
        case Time.LOGICAL_NAME:
        case Timestamp.LOGICAL_NAME:
          return schema;
      }
    }

    Schema.Type schemaType = schema.type();
    Schema keySchema;
    Schema valueSchema;
    switch (schemaType) {
      case ARRAY:
        valueSchema = schema.valueSchema();
        builder = SchemaBuilder.array(preProcessSchema(valueSchema));
        return builder.build();
      case MAP:
        keySchema = schema.keySchema();
        valueSchema = schema.valueSchema();
        String keyName = keySchema.name() == null ? keySchema.type().name() : keySchema.name();
        String valueName = valueSchema.name() == null ? valueSchema.type().name() : valueSchema.name();
        builder = SchemaBuilder.array(SchemaBuilder.struct().name(keyName + "-" + valueName)
            .field(MAP_KEY, preProcessSchema(keySchema))
            .field(MAP_VALUE, preProcessSchema(valueSchema))
            .build());
        return builder.build();
      case STRUCT:
        builder = SchemaBuilder.struct().name(schema.name());
        for (Field field: schema.fields()) {
          builder.field(field.name(), preProcessSchema(field.schema()));
        }
        return builder.build();
      default:
        return schema;
    }
  }

  // visible for testing
  static Object preProcessValue(Object value, Schema schema, Schema newSchema) {
    if (schema == null) {
      return value;
    }

    // Handle logical types
    String schemaName = schema.name();
    if (schemaName != null) {
      switch (schemaName) {
        case Decimal.LOGICAL_NAME:
          return ((BigDecimal) value).doubleValue();
        case Date.LOGICAL_NAME:
        case Time.LOGICAL_NAME:
        case Timestamp.LOGICAL_NAME:
          return value;
      }
    }

    Schema.Type schemaType = schema.type();
    Schema keySchema;
    Schema valueSchema;
    switch (schemaType) {
      case ARRAY:
        Collection collection = (Collection) value;
        ArrayList<Object> result = new ArrayList<>();
        for (Object element: collection) {
          result.add(preProcessValue(element, schema.valueSchema(), newSchema.valueSchema()));
        }
        return result;
      case MAP:
        keySchema = schema.keySchema();
        valueSchema = schema.valueSchema();
        ArrayList<Struct> mapStructs = new ArrayList<>();
        Map<?, ?> map = (Map<?, ?>) value;
        Schema newValueSchema = newSchema.valueSchema();
        for (Map.Entry<?, ?> entry: map.entrySet()) {
          Struct mapStruct = new Struct(newValueSchema);
          mapStruct.put(MAP_KEY, preProcessValue(entry.getKey(), keySchema, newValueSchema.field(MAP_KEY).schema()));
          mapStruct.put(MAP_VALUE, preProcessValue(entry.getValue(), valueSchema, newValueSchema.field(MAP_VALUE).schema()));
          mapStructs.add(mapStruct);
        }
        return mapStructs;
      case STRUCT:
        Struct struct = (Struct) value;
        Struct newStruct = new Struct(newSchema);
        for (Field field : schema.fields()) {
          Object converted =  preProcessValue(struct.get(field), field.schema(), newSchema.field(field.name()).schema());
          newStruct.put(field.name(), converted);
        }
        return newStruct;
      default:
        return value;
    }
  }
}
