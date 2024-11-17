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
package io.confluent.connect.elasticsearch.bulk;

import org.apache.kafka.common.utils.SystemTime;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BulkProcessorTest {

  private static class Expectation {
    final List<Integer> request;
    final BulkResponse response;

    private Expectation(List<Integer> request, BulkResponse response) {
      this.request = request;
      this.response = response;
    }
  }

  private static final class Client implements BulkClient<Integer, List<Integer>> {
    private final Queue<Expectation> expectQ = new LinkedList<>();
    private volatile boolean executeMetExpectations = true;

    @Override
    public List<Integer> bulkRequest(List<Integer> batch) {
      List<Integer> ids = new ArrayList<>(batch.size());
      for (Integer id : batch) {
        ids.add(id);
      }
      return ids;
    }

    public void expect(List<Integer> ids, BulkResponse response) {
      expectQ.add(new Expectation(ids, response));
    }

    public boolean expectationsMet() {
      return expectQ.isEmpty() && executeMetExpectations;
    }

    @Override
    public BulkResponse execute(List<Integer> request) throws IOException {
      final Expectation expectation;
      try {
        expectation = expectQ.remove();
        assertEquals(expectation.request, request);
      } catch (Throwable t) {
        executeMetExpectations = false;
        throw t;
      }
      executeMetExpectations &= true;
      return expectation.response;
    }
  }

  @Test
  public void batchingAndLingering() throws InterruptedException, ExecutionException {
    final Client client = new Client();

    final int maxBufferedRecords = 100;
    final int maxInFlightBatches = 5;
    final int batchSize = 5;
    final int lingerMs = 5;
    final int maxRetries = 0;
    final int retryBackoffMs = 0;

    final BulkProcessor<Integer, ?> bulkProcessor = new BulkProcessor<>(
        new SystemTime(),
        client,
        maxBufferedRecords,
        maxInFlightBatches,
        batchSize,
        lingerMs,
        maxRetries,
        retryBackoffMs
    );

    bulkProcessor.add(1);
    bulkProcessor.add(2);
    bulkProcessor.add(3);
    bulkProcessor.add(4);
    bulkProcessor.add(5);
    bulkProcessor.add(6);
    bulkProcessor.add(7);
    bulkProcessor.add(8);
    bulkProcessor.add(9);
    bulkProcessor.add(10);
    bulkProcessor.add(11);
    bulkProcessor.add(12);

    client.expect(Arrays.asList(1, 2, 3, 4, 5), BulkResponse.success());
    client.expect(Arrays.asList(6, 7, 8, 9, 10), BulkResponse.success());
    client.expect(Arrays.asList(11, 12), BulkResponse.success()); // batch not full, but upon linger timeout
    assertTrue(bulkProcessor.tick().get().succeeded);
    assertTrue(bulkProcessor.tick().get().succeeded);
    assertTrue(bulkProcessor.tick().get().succeeded);
    assertTrue(client.expectationsMet());
  }

  @Test
  public void flushing() {
    final Client client = new Client();

    final int maxBufferedRecords = 100;
    final int maxInFlightBatches = 5;
    final int batchSize = 5;
    final int lingerMs = 100000; // super high on purpose to make sure flush is what's causing the request
    final int maxRetries = 0;
    final int retryBackoffMs = 0;

    final BulkProcessor<Integer, ?> bulkProcessor = new BulkProcessor<>(
        new SystemTime(),
        client,
        maxBufferedRecords,
        maxInFlightBatches,
        batchSize,
        lingerMs,
        maxRetries,
        retryBackoffMs
    );

    client.expect(Arrays.asList(1, 2, 3), BulkResponse.success());

    bulkProcessor.start();
    bulkProcessor.add(1);
    bulkProcessor.add(2);
    bulkProcessor.add(3);

    assertFalse(client.expectationsMet());

    final int flushTimeoutMs = 10;
    bulkProcessor.flush(flushTimeoutMs);

    assertTrue(client.expectationsMet());
  }

  @Test
  public void retriableErrors() {
    // TODO
  }

  @Test
  public void unretriableErrors() {
    // TODO
  }

}
