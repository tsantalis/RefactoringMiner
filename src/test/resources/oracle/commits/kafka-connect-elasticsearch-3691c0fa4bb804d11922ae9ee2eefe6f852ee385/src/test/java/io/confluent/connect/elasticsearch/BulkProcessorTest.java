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

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import io.confluent.connect.elasticsearch.internals.BulkProcessor;
import io.confluent.connect.elasticsearch.internals.Client;
import io.confluent.connect.elasticsearch.internals.ESRequest;
import io.confluent.connect.elasticsearch.internals.Listener;
import io.confluent.connect.elasticsearch.internals.RecordBatch;
import io.confluent.connect.elasticsearch.internals.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BulkProcessorTest {

  private volatile int numFailure = 0;
  private volatile int numSuccess = 0;
  private volatile int numExecute = 0;
  private final byte[] dummyPayload = ByteBuffer.allocate(0).array();
  private final String index = "test";
  private final String type = "connect";
  private final String topic = "topic";
  private final int partition = 0;
  private final long flushTimeoutMs = 30000;
  private final long lingerMs = 2000;
  private final int maxRetry = 5;
  private final long retryBackoffMs = 3000;

  @Before
  public void setUp() {
    synchronized (this) {
      numExecute = 0;
      numSuccess = 0;
      numFailure = 0;
    }
  }

  @Test
  public void testWrite() throws Throwable {
    int maxInFlightRequests = 2;
    int batchSize = 5;
    int numRecords = 10;
    Client<Response> client = new MockHttpClient(0);

    BulkProcessor bulkProcessor = new BulkProcessor(client, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs, createTestListener());
    bulkProcessor.start();

    addRecords(bulkProcessor, numRecords);
    bulkProcessor.flush(flushTimeoutMs);

    int numIncompletes = bulkProcessor.getNumIncompletes();
    assertEquals(0, numIncompletes);
    assertEquals(0, numFailure);
    assertEquals(2, numSuccess);
    assertEquals(2, numExecute);
  }

  @Test
  public void testLinger() throws Throwable {
    int maxInFlightRequests = 2;
    int batchSize = 5;
    int numRecords = 4;

    Client<Response> client = new MockHttpClient(0);
    BulkProcessor bulkProcessor = new BulkProcessor(client, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs, createTestListener());
    bulkProcessor.start();

    addRecords(bulkProcessor, numRecords);
    bulkProcessor.flush(flushTimeoutMs);

    int numIncompletes = bulkProcessor.getNumIncompletes();
    assertEquals(0, numIncompletes);
    assertEquals(0, numFailure);
    assertEquals(1, numSuccess);
    assertEquals(1, numExecute);
  }

  @Test
  public void testBatchRetry() throws Throwable {
    int maxInFlightRequests = 2;
    int batchSize = 5;
    int numRecords = 10;

    Client<Response> client = new MockHttpClient(1);
    BulkProcessor bulkProcessor = new BulkProcessor(client, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs, createTestListener());

    bulkProcessor.start();

    addRecords(bulkProcessor, numRecords);
    bulkProcessor.flush(flushTimeoutMs);

    int numIncompletes = bulkProcessor.getNumIncompletes();
    assertEquals(0, numIncompletes);
    assertEquals(1, numFailure);
    assertEquals(2, numSuccess);
    assertEquals(3, numExecute);
  }

  @Test
  public void testBatchFailure() throws Throwable {
    int maxInFlightRequests = 1;
    int batchSize = 5;
    int numRecords = 5;

    Client<Response> client = new MockHttpClient(1, false);
    BulkProcessor bulkProcessor = new BulkProcessor(client, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs, createTestListener());
    bulkProcessor.start();

    addRecords(bulkProcessor, numRecords);
    try {
      bulkProcessor.flush(flushTimeoutMs);
      fail("Flush should throw non retriable exception.");
    } catch (Throwable t) {
      // expected
    }

    int numIncompletes = bulkProcessor.getNumIncompletes();
    assertEquals(1, numIncompletes);
    assertEquals(1, numFailure);
    assertEquals(0, numSuccess);
    assertEquals(1, numExecute);
  }

  @Test
  public void testClose() throws Throwable {
    int maxInFlightRequests = 2;
    int batchSize = 5;
    int numRecords = 4;

    Client<Response> client = new MockHttpClient(0);
    BulkProcessor bulkProcessor = new BulkProcessor(client, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs, createTestListener());
    bulkProcessor.start();

    addRecords(bulkProcessor, numRecords);

    bulkProcessor.stop();
    bulkProcessor.awaitStop(flushTimeoutMs);

    int numIncompletes = bulkProcessor.getNumIncompletes();
    assertEquals(0, numIncompletes);
    assertEquals(0, numFailure);
    assertEquals(1, numSuccess);
    assertEquals(1, numExecute);
  }

  @Test
  public void testFlush() throws Throwable {
    int maxInFlightRequests = 2;
    int batchSize = 5;
    int numRecords = 5;
    long shortTimeoutMs = 500;
    long mediumTimeoutMs = 3000;

    Client<Response> client = new MockHttpClient(1);
    BulkProcessor bulkProcessor = new BulkProcessor(client, maxInFlightRequests, batchSize, lingerMs, maxRetry, retryBackoffMs, createTestListener());
    bulkProcessor.start();

    addRecords(bulkProcessor, numRecords);
    assertFalse(bulkProcessor.flush(shortTimeoutMs));
    int numIncompletes = bulkProcessor.getNumIncompletes();
    assertEquals(1, numIncompletes);
    assertEquals(1, numFailure);
    assertEquals(0, numSuccess);
    assertEquals(1, numExecute);

    assertTrue(bulkProcessor.flush(mediumTimeoutMs));

    numIncompletes = bulkProcessor.getNumIncompletes();
    assertEquals(0, numIncompletes);
    assertEquals(1, numFailure);
    assertEquals(1, numSuccess);
    assertEquals(2, numExecute);
  }

  private void addRecords(BulkProcessor bulkProcessor, int numRecords) {
    for (int offset = 0; offset < numRecords; ++offset) {
      String id = topic + "+" + partition + "+" + offset;
      ESRequest esRequest = new ESRequest(index, type, id, dummyPayload);
      bulkProcessor.add(esRequest);
    }
  }

  private Listener createTestListener() {
    return new Listener() {
      @Override
      public void beforeBulk(long executionId, RecordBatch batch) {
        synchronized (this) {
          numExecute++;
        }
      }

      @Override
      public void afterBulk(long executionId, RecordBatch batch, Response response) {
        if (response.hasFailures()) {
          synchronized (this) {
            numFailure++;
          }
        } else {
          synchronized (this) {
            numSuccess++;
          }
        }
      }

      @Override
      public void afterBulk(long executionId, RecordBatch batch, Throwable failure) {
        synchronized (this) {
          numFailure++;
        }
      }
    };
  }
}
