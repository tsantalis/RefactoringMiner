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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BulkProcessor implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(BulkProcessor.class);
  private final ArrayDeque<RecordBatch> requests;
  private final Client<Response> client;
  private final int batchSize;
  private final ExecutorService executorService;
  private final AtomicLong executionIdGen = new AtomicLong();
  private final AtomicBoolean running = new AtomicBoolean();
  private final IncompleteRecordBatches incomplete;
  private final Listener listener;
  private final long lingerMs;
  private final long retryBackOffMs;
  private volatile Throwable exception;
  private final Thread workThread;
  private final int maxRetry;
  private final boolean guaranteeOrdering;
  private volatile boolean muted;

  public BulkProcessor(
      Client<Response> client,
      int maxInFlightRequests,
      int batchSize,
      long lingerMs,
      int maxRetry,
      long retryBackOffMs,
      Listener listener) {
    this.client = client;
    this.requests = new ArrayDeque<>();
    this.batchSize = batchSize;
    this.lingerMs = lingerMs;
    this.maxRetry = maxRetry;
    this.retryBackOffMs = retryBackOffMs;
    this.incomplete = new IncompleteRecordBatches();
    this.listener = listener;
    this.executorService = Executors.newFixedThreadPool(maxInFlightRequests);
    this.guaranteeOrdering = maxInFlightRequests == 1;
    this.muted = false;
    // TODO: Add more information to thread name
    this.workThread = new Thread(this, "BulkProcessor");
  }

  public void start() {
    running.set(true);
    workThread.start();
  }

  @Override
  public void run() {
    try {
      while (running.get()) {
        RecordBatch batch;
        synchronized (requests) {
          while (requests.isEmpty() && running.get()) {
            requests.wait();
          }
          if (!running.get()) {
            return;
          }
          batch = requests.peekFirst();
        }
        long now = System.currentTimeMillis();
        if (canSubmit(batch, now)) {
          if (guaranteeOrdering) {
            muted = true;
          }
          executorService.submit(new BulkTask(batch));
          batch.setLastAttemptMs(now);
          synchronized (requests) {
            requests.pollFirst();
          }
        }
      }
    } catch (InterruptedException e) {
      log.info("Work thread is interrupted, shutting down.");
    }
  }

  private boolean canSubmit(RecordBatch batch, long now) {
    if (guaranteeOrdering && muted) {
      return false;
    } else {
      return (!batch.inRetry() && (batch.size() == batchSize || now - batch.getLastAttemptMs() > lingerMs))
             || (batch.inRetry() && now - batch.getLastAttemptMs() > retryBackOffMs);
    }
  }

  private void execute(final RecordBatch batch, final long executionId) {
    try {
      try {
        listener.beforeBulk(executionId, batch);
      } catch (Throwable t) {
        log.error("Error executing the beforeBulk callback", t);
      }
      Callback<Response> callback = new Callback<Response>() {
        @Override
        public void onResponse(Response response) {
          if (!response.hasFailures()) {
            incomplete.remove(batch);
            batch.result().done(null);
            muted = false;
          } else {
            if (response.canRetry()) {
              retryOrFail(batch, response.getThrowable());
            } else {
              fail(batch, response.getThrowable());
            }
          }
          try {
            listener.afterBulk(executionId, batch, response);
          } catch (Throwable t) {
            log.error("Error executing the afterBulk callback", t);
          }
        }

        @Override
        public void onFailure(Throwable t) {
          log.error("Failed to execute the batch", t);
          retryOrFail(batch, t);
          try {
            listener.afterBulk(executionId, batch, t);
          } catch (Throwable tt) {
            log.error("Error executing the afterBulk callback", tt);
          }
        }
      };

      client.execute(batch, callback);
    } catch (Throwable t) {
      log.warn("Failed to execute bulk request {}.", executionId, t);
      try {
        listener.afterBulk(executionId, batch, t);
      } catch (Throwable tt) {
        log.error("Error executing the afterBulk callback", tt);
      }
    }
  }

  public void add(ESRequest request) {
    synchronized (requests) {
      boolean wasEmpty = false;
      if (requests.isEmpty() || requests.peekLast().size() == batchSize) {
        wasEmpty = requests.isEmpty();
        RecordBatch batch = new RecordBatch(System.currentTimeMillis());
        requests.add(batch);
        incomplete.add(batch);
      }
      RecordBatch batch = requests.peekLast();
      batch.add(request);
      if (wasEmpty) {
        requests.notify();
      }
    }
  }

  private void retryOrFail(RecordBatch batch, Throwable t) {
    log.error("Failed to execute the batch, retry or fail", t);
    synchronized (requests) {
      batch.setRetry();
      batch.incrementAttempts();
      if (batch.getAttempts() > maxRetry) {
        fail(batch, t);
      } else {
        requests.addFirst(batch);
        requests.notify();
      }
    }
  }

  private void fail(RecordBatch batch, Throwable t) {
    log.error("Batch failed with non retriable exception or exceed the max retry", t);
    batch.result().done(t);
    exception = t;
    stop();
  }

  public boolean flush(long timeout) throws Throwable {
    long remaining = timeout;
    long start = System.currentTimeMillis();
    for (RecordBatch batch: incomplete.all()) {
      if (batch.result().await(remaining, TimeUnit.MILLISECONDS)) {
        long now = System.currentTimeMillis();
        remaining -= now - start;
        start = now;
        if (remaining < 0) {
          return false;
        }
      } else {
        return false;
      }
      if (batch.result().getError() != null) {
        throw batch.result().getError();
      }
    }
    return true;
  }

  public boolean awaitStop(long timeout) throws Throwable {
    if (running.get()) {
      return false;
    }

    // Send remaining batches
    synchronized (requests) {
      for (RecordBatch batch: requests) {
        executorService.submit(new BulkTask(batch));
      }
    }
    // Note that we will not retry during shutdown. The batch that needs to be retried will cause
    // the flush to timeout. Offset will not be committed during unclean shutdown.
    boolean cleanShutdown = true;
    Throwable firstException = null;
    try {
      cleanShutdown = flush(timeout);
    } catch (Throwable t) {
      firstException = t;
    }
    executorService.shutdown();
    cleanShutdown = cleanShutdown && executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);

    try {
      workThread.join();
    } catch (Throwable t) {
      if (firstException != null) {
        firstException = t;
      }
    }

    client.close();

    if (firstException != null) {
      throw firstException;
    }
    return cleanShutdown;
  }

  public void stop() {
    synchronized (requests) {
      running.set(false);
      requests.notify();
    }
  }

  public int getTotalBufferedRecords() {
    int total = 0;
    synchronized (requests) {
      for (RecordBatch batch: requests) {
        total += batch.size();
      }
    }
    return total;
  }

  // visible for testing
  public int getNumIncompletes() {
    return incomplete.numIncompletes();
  }

  public Throwable getException() {
    return exception;
  }

  private class BulkTask implements Callable<Void> {

    private final RecordBatch batch;

    BulkTask(RecordBatch batch) {
      log.trace("Batch size: {}", batch.size());
      this.batch = batch;
    }

    @Override
    public Void call() throws Exception {
      final long executionId = executionIdGen.incrementAndGet();
      execute(batch, executionId);
      return null;
    }
  }
}
