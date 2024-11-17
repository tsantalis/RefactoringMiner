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

import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @param <R> record type
 * @param <B> bulk request type
 */
public class BulkProcessor<R, B> {

  private static final Logger log = LoggerFactory.getLogger(BulkProcessor.class);

  private final Time time;
  private final BulkClient<R, B> bulkClient;
  private final int maxBufferedRecords;
  private final int batchSize;
  private final long lingerMs;
  private final int maxRetries;
  private final long retryBackoffMs;

  private final Thread farmer;
  private final ExecutorService executor;

  // thread-safe stats
  private final AtomicLong createdBatches = new AtomicLong();
  private final AtomicLong successfulRecords = new AtomicLong();
  private final AtomicLong successfulBatches = new AtomicLong();

  // thread-safe state, can be mutated safely without synchronization,
  // but may be part of synchronized(this) wait() conditions so need to notifyAll() on changes
  private volatile boolean stopRequested = false;
  private volatile boolean flushRequested = false;
  private final AtomicReference<ConnectException> error = new AtomicReference<>();

  // shared state, synchronized on (this), may be part of wait() conditions so need notifyAll() on changes
  private final Deque<R> unsentRecords;
  private int inFlightRecords = 0;

  public BulkProcessor(
      Time time,
      BulkClient<R, B> bulkClient,
      int maxBufferedRecords,
      int maxInFlightRequests,
      int batchSize,
      long lingerMs,
      int maxRetries,
      long retryBackoffMs
  ) {
    this.time = time;
    this.bulkClient = bulkClient;
    this.maxBufferedRecords = maxBufferedRecords;
    this.batchSize = batchSize;
    this.lingerMs = lingerMs;
    this.maxRetries = maxRetries;
    this.retryBackoffMs = retryBackoffMs;

    unsentRecords = new ArrayDeque<>(maxBufferedRecords);

    final ThreadFactory threadFactory = makeThreadFactory();
    farmer = new Thread(farmerTask());
    executor = Executors.newFixedThreadPool(maxInFlightRequests, threadFactory);
  }

  private ThreadFactory makeThreadFactory() {
    final AtomicInteger threadCounter = new AtomicInteger();
    final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception in BulkProcessor thread {}", t, e);
        failAndStop(e);
      }
    };
    return new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        final Thread t = new Thread(r, "BulkProcessor-t" + threadCounter.getAndIncrement());
        t.setDaemon(true);
        t.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return t;
      }
    };
  }

  private Runnable farmerTask() {
    return new Runnable() {
      @Override
      public void run() {
        log.debug("Starting farmer task");
        try {
          while (!stopRequested) {
            tick();
          }
        } catch (InterruptedException e) {
          throw new ConnectException(e);
        }
        log.debug("Finished farmer task");
      }
    };
  }

  // Visible for testing
  synchronized Future<BulkResponse> tick() throws InterruptedException {
    for (long waitStartTimeMs = time.milliseconds(), elapsedMs = 0;
         !stopRequested && !canSubmit(elapsedMs);
         elapsedMs = time.milliseconds() - waitStartTimeMs) {
      // when linger time has already elapsed, we still have to ensure the other submission conditions hence the wait(0) in that case
      wait(Math.max(0, lingerMs - elapsedMs));
    }
    return stopRequested ? null : submitBatch();
  }

  /**
   * Submission is possible when there are unsent records and:
   * <ul>
   * <li>flush is called, or</li>
   * <li>the linger timeout passes, or</li>
   * <li>there are sufficient records to fill a batch</li>
   * </ul>
   */
  private synchronized boolean canSubmit(long elapsedMs) {
    return !unsentRecords.isEmpty()
           && (flushRequested || elapsedMs >= lingerMs || unsentRecords.size() >= batchSize);
  }

  /**
   * Start concurrently creating and sending batched requests using the client.
   */
  public void start() {
    farmer.start();
  }

  /**
   * Initiate shutdown.
   *
   * Pending buffered records are not automatically flushed, so call {@link #flush(long)} before this method if this is desirable.
   */
  public void stop() {
    log.trace("stop");
    stopRequested = true;
    synchronized (this) {
      // shutdown the pool under synchronization to avoid rejected submissions
      executor.shutdown();
      notifyAll();
    }
  }

  /**
   * Block upto {@code timeoutMs} till shutdown is complete.
   *
   * This should only be called after a previous {@link #stop()} invocation.
   */
  public void awaitStop(long timeoutMs) {
    log.trace("awaitStop {}", timeoutMs);
    assert stopRequested;
    try {
      if (!executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
        throw new ConnectException("Timed-out waiting for executor termination");
      }
    } catch (InterruptedException e) {
      throw new ConnectException(e);
    } finally {
      executor.shutdownNow();
    }
  }

  private synchronized Future<BulkResponse> submitBatch() {
    final int batchableSize = Math.min(batchSize, unsentRecords.size());
    final List<R> batch = new ArrayList<>(batchableSize);
    for (int i = 0; i < batchableSize; i++) {
      batch.add(unsentRecords.removeFirst());
    }
    inFlightRecords += batchableSize;
    return executor.submit(new BulkTask(batch));
  }

  /**
   * @return whether {@link #stop()} has been requested
   */
  public boolean isStopping() {
    return stopRequested;
  }

  /**
   * @return whether any task failed with an error
   */
  public boolean isFailed() {
    return error.get() != null;
  }

  /**
   * @return {@link #isTerminal()} or {@link #isFailed()}
   */
  public boolean isTerminal() {
    return isStopping() || isFailed();
  }

  /**
   * Throw a {@link ConnectException} if {@link #isStopping()}.
   */
  public void throwIfStopping() {
    if (stopRequested) {
      throw new ConnectException("Stopping");
    }
  }

  /**
   * Throw the relevant {@link ConnectException} if {@link #isFailed()}.
   */
  public void throwIfFailed() {
    if (isFailed()) {
      throw error.get();
    }
  }

  /**
   * {@link #throwIfFailed()} and {@link #throwIfStopping()}
   */
  public void throwIfTerminal() {
    throwIfFailed();
    throwIfStopping();
  }

  public synchronized void add(R record) {
    add(record, Long.MAX_VALUE);
  }

  /**
   * Add a record, may block upto {@code timeoutMs} if at capacity with respect to {@code maxBufferedRecords}.
   *
   * If any task has failed prior to or while blocked in the add, {@link ConnectException} will be thrown with that error.
   */
  public synchronized void add(R record, long timeoutMs) {
    final long addStartTimeMs = time.milliseconds();
    for (long elapsedMs = time.milliseconds() - addStartTimeMs;
         !isTerminal() && elapsedMs < timeoutMs && bufferedRecords() >= maxBufferedRecords;
         elapsedMs = time.milliseconds() - addStartTimeMs) {
      try {
        wait(timeoutMs - elapsedMs);
      } catch (InterruptedException e) {
        throw new ConnectException(e);
      }
    }
    throwIfTerminal();
    if (bufferedRecords() >= maxBufferedRecords) {
      throw new ConnectException("Add timeout expired before buffer availability");
    }
    unsentRecords.addLast(record);
    notifyAll();
  }

  /**
   * Request a flush and block upto {@code timeoutMs} until all pending records have been flushed.
   *
   * If any task has failed prior to or during the flush, {@link ConnectException} will be thrown with that error.
   */
  public void flush(long timeoutMs) {
    log.trace("flush {}", timeoutMs);
    final long flushStartTimeMs = time.milliseconds();
    try {
      flushRequested = true;
      synchronized (this) {
        notifyAll();
        for (long elapsedMs = time.milliseconds() - flushStartTimeMs;
             !isTerminal() && elapsedMs < timeoutMs && bufferedRecords() > 0;
             elapsedMs = time.milliseconds() - flushStartTimeMs) {
          wait(timeoutMs - elapsedMs);
        }
        throwIfTerminal();
        if (bufferedRecords() > 0) {
          throw new ConnectException("Flush timeout expired with unflushed records: " + bufferedRecords());
        }
      }
    } catch (InterruptedException e) {
      throw new ConnectException(e);
    } finally {
      flushRequested = false;
    }
  }

  private final class BulkTask implements Callable<BulkResponse> {

    final long batchId = createdBatches.incrementAndGet();

    final List<R> batch;

    BulkTask(List<R> batch) {
      this.batch = batch;
    }

    @Override
    public BulkResponse call() throws Exception {
      final BulkResponse rsp;
      try {
        rsp = execute();
      } catch (Exception e) {
        failAndStop(e);
        throw e;
      }
      log.debug("Successfully executed batch {} of {} records", batchId, batch.size());
      onBatchCompletion(batch.size());
      return rsp;
    }

    private BulkResponse execute() throws Exception {
      final B bulkReq;
      try {
        bulkReq = bulkClient.bulkRequest(batch);
      } catch (Exception e) {
        log.error("Failed to create bulk request from batch {} of {} records", batchId, batch.size(), e);
        throw e;
      }
      for (int remainingRetries = maxRetries; true; remainingRetries--) {
        boolean retriable = true;
        try {
          log.trace("Executing batch {} of {} records", batchId, batch.size());
          final BulkResponse bulkRsp = bulkClient.execute(bulkReq);
          if (bulkRsp.isSucceeded()) {
            return bulkRsp;
          }
          retriable = bulkRsp.isRetriable();
          throw new ConnectException("Bulk request failed: " + bulkRsp.getErrorInfo());
        } catch (Exception e) {
          if (retriable && remainingRetries > 0) {
            log.warn("Failed to execute batch {} of {} records, retrying after {} ms", batchId, batch.size(), retryBackoffMs, e);
            time.sleep(retryBackoffMs);
          } else {
            log.error("Failed to execute batch {} of {} records", batchId, batch.size(), e);
            throw e;
          }
        }
      }
    }

  }

  private synchronized void onBatchCompletion(int batchSize) {
    successfulBatches.incrementAndGet();
    successfulRecords.addAndGet(batchSize);
    inFlightRecords -= batchSize;
    assert inFlightRecords >= 0;
    notifyAll();
  }

  private void failAndStop(Throwable t) {
    error.compareAndSet(null, toConnectException(t));
    stop();
  }

  /**
   * @return count of currently buffered records
   */
  public synchronized int unsentRecords() {
    return unsentRecords.size();
  }

  /**
   * @return count of records currently in flight
   */
  public synchronized int inFlightRecords() {
    return inFlightRecords;
  }

  /**
   * @return sum of unsent and in-flight record counts
   */
  public synchronized int bufferedRecords() {
    return unsentRecords.size() + inFlightRecords;
  }

  /**
   * @return count of batches that have been created
   */
  public long createdBatches() {
    return createdBatches.get();
  }

  /**
   * @return count of batches successfully executed
   */
  public long successfulBatches() {
    return successfulBatches.get();
  }

  /**
   * @return count of records successfully sent
   */
  public long successfulRecords() {
    return successfulRecords.get();
  }

  private static ConnectException toConnectException(Throwable t) {
    if (t instanceof ConnectException) {
      return (ConnectException) t;
    } else {
      return new ConnectException(t);
    }
  }

}
