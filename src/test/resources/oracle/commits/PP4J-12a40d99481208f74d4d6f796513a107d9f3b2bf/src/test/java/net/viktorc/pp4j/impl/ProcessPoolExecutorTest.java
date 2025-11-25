/*
 * Copyright 2017 Viktor Csomor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.viktorc.pp4j.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.viktorc.pp4j.api.Command;
import net.viktorc.pp4j.api.DisruptedExecutionException;
import net.viktorc.pp4j.api.FailedCommandException;
import net.viktorc.pp4j.api.Submission;
import net.viktorc.pp4j.impl.TestUtils.TestProcessManagerFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * A unit test class for {@link ProcessPoolExecutor}.
 *
 * @author Viktor Csomor
 */
public class ProcessPoolExecutorTest extends TestCase {

  /**
   * Creates and returns a new submission that has the test process stall for 1 second when executed.
   *
   * @return A new simple submission instance.
   */
  private static Submission<?> newSimpleSubmission() {
    return new SimpleSubmission<>(new SimpleCommand("process 1", (c, o) -> "ready".equals(o)));
  }

  /**
   * Tests how the pool scales in terms of managed processes in response to submissions.
   *
   * @param pool The process pool instance to test.
   * @param numOfSubmissions The number of submissions to submit to the pool.
   * @param terminateAfterSubmission Whether processes are to be terminated after executing the submissions delegated to them.
   * @param keepAliveTime The duration of idleness after which processes are to be terminated, in milliseconds.
   * @throws InterruptedException If the thread is interrupted.
   * @throws ExecutionException If the execution of any submission fails.
   */
  private static void testPoolSizeScaling(ProcessPoolExecutor pool, int numOfSubmissions, boolean terminateAfterSubmission,
      Long keepAliveTime) throws InterruptedException, ExecutionException {
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < numOfSubmissions; i++) {
      futures.add(pool.submit(newSimpleSubmission(), terminateAfterSubmission));
    }
    Assert.assertEquals(numOfSubmissions, pool.getNumOfSubmissions());
    int expectedSizeOfScaledPool = Math.max(Math.min(numOfSubmissions + pool.getReserveSize(), pool.getMaxSize()), pool.getMinSize());
    Assert.assertEquals(expectedSizeOfScaledPool, pool.getNumOfProcesses());
    for (Future<?> future : futures) {
      future.get();
    }
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Assert.assertEquals(0, pool.getNumOfSubmissions());
    if (terminateAfterSubmission || keepAliveTime != null) {
      if (!terminateAfterSubmission) {
        Thread.sleep(keepAliveTime);
      }
      int expectedSizeOfRestingPool = Math.max(pool.getMinSize(), pool.getReserveSize());
      Assert.assertEquals(expectedSizeOfRestingPool, pool.getNumOfProcesses());
    } else {
      Assert.assertEquals(expectedSizeOfScaledPool, pool.getNumOfProcesses());
    }
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertEquals(0, pool.getNumOfProcesses());
  }

  /**
   * Tests the behaviour of the process pool with respect to already submitted submissions when shut down.
   *
   * @param poolSize The pool size to use.
   * @param numOfSubmissions The number of submissions to submit to the pool.
   * @throws InterruptedException If the thread is interrupted.
   * @throws ExecutionException If the execution of any submission fails.
   */
  private static void testSubmissionFuturesAfterShutdown(int poolSize, int numOfSubmissions)
      throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), poolSize, poolSize, 0);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < numOfSubmissions; i++) {
      futures.add(pool.submit(newSimpleSubmission()));
    }
    pool.shutdown();
    for (Future<?> future : futures) {
      future.get();
    }
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  /**
   * Tests the behaviour of the process pool with respect to already submitted submissions when forcibly shut down.
   *
   * @param poolSize The pool size to use.
   * @param numOfSubmissions The number of submissions to submit to the pool.
   * @throws InterruptedException If the thread is interrupted.
   */
  private static void testSubmissionFuturesAfterForceShutdown(int poolSize, int numOfSubmissions) throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), poolSize, poolSize, 0);
    List<Submission<?>> submissions = new ArrayList<>();
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < numOfSubmissions; i++) {
      Submission<?> submission = newSimpleSubmission();
      submissions.add(submission);
      futures.add(pool.submit(submission));
    }
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    List<Submission<?>> submissionsAwaitingExecution = pool.forceShutdown();
    Assert.assertEquals(Math.max(0, numOfSubmissions - poolSize), submissionsAwaitingExecution.size());
    for (Submission<?> submission : submissionsAwaitingExecution) {
      Assert.assertTrue(submissions.contains(submission));
      submissions.remove(submission);
    }
    for (Future<?> future : futures) {
      Assert.assertFalse(future.isDone());
    }
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  /**
   * Tests if the process pool can shutdown with cancelled submissions stuck in its queue.
   *
   * @param mayInterruptIfRunning If the submissions executor threads may be interrupted when cancelling the submissions.
   * @throws InterruptedException If the main thread is interrupted.
   */
  private static void testShutdownWithCancelledQueuedSubmissions(boolean mayInterruptIfRunning) throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 2, 2, 0);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      futures.add(pool.submit(newSimpleSubmission()));
    }
    for (Future<?> future : futures) {
      future.cancel(mayInterruptIfRunning);
    }
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertEquals(0, pool.getNumOfSubmissions());
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIfProcessManagerFactoryNull() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(null, 1, 5, 1);
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIfMinPoolSizeNegative() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), -1, 5, 1);
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIfMaxPoolSizeZero() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), 0, 0, 0);
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIfMaxPoolSizeLessThanMinPoolSize() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), 3, 2, 1);
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIfReserveSizeNegative() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 5, -1);
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIReserveSizeGreaterThanMaxPoolSize() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 3, 4);
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIfThreadKeepAliveTimeZero() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 5, 3, 0);
  }

  @Test
  public void testThrowsIllegalArgumentExceptionIfThreadKeepAliveTimeNegative() throws InterruptedException {
    exceptionRule.expect(IllegalArgumentException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 5, 3, -1);
  }

  @Test
  public void testThrowsInterruptedExceptionIfConstructorInterrupted() throws InterruptedException {
    Thread.currentThread().interrupt();
    exceptionRule.expect(InterruptedException.class);
    new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
  }

  @Test
  public void testPoolShutsDownIfStartupFails() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(null, true, false, false, true), 3, 6, 0);
    Assert.assertTrue(pool.isShutdown());
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertTrue(pool.isTerminated());
  }

  @Test
  public void testIsShutdownAndIsTerminated() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 2, 3, 0);
    Assert.assertFalse(pool.isShutdown());
    Assert.assertFalse(pool.isTerminated());
    pool.shutdown();
    Assert.assertTrue(pool.isShutdown());
    Assert.assertFalse(pool.isTerminated());
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertTrue(pool.isTerminated());
  }

  @Test
  public void testPoolInitializedWithEmptyPool() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 0, 2, 0);
    Assert.assertEquals(0, pool.getNumOfProcesses());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testPoolInitializedWithMinPoolSize() throws InterruptedException {
    int minPoolSize = 5;
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), minPoolSize, 7, 0);
    Assert.assertEquals(minPoolSize, pool.getNumOfProcesses());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testPoolInitializedWithAtLeastReserveSize() throws InterruptedException {
    int reserveSize = 5;
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 2, 7, reserveSize);
    Assert.assertEquals(reserveSize, pool.getNumOfProcesses());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testPoolScalesToMaintainReserveSize() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 5, 10, 3);
    testPoolSizeScaling(pool, 5, false, null);
  }

  @Test
  public void testPoolDoesNotScaleBeyondMinPoolSize() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 5, 8, 2);
    testPoolSizeScaling(pool, 2, false, null);
  }

  @Test
  public void testPoolDoesNotScaleBeyondMaxPoolSizeWithReserve() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 2, 8, 3);
    testPoolSizeScaling(pool, 7, false, null);
  }

  @Test
  public void testPoolDoesNotScaleBeyondMaxPoolSizeWithoutReserve() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 3, 8, 0);
    testPoolSizeScaling(pool, 12, false, null);
  }

  @Test
  public void testPoolWithKeepAliveTimeScalesDownToMinPoolSize() throws InterruptedException, ExecutionException {
    long keepAliveTime = 1000L;
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(keepAliveTime, true, false, false, false),
        5, 10, 3);
    testPoolSizeScaling(pool, 10, false, keepAliveTime);
  }

  @Test
  public void testPoolWithKeepAliveTimeScalesDownToReserveSize() throws InterruptedException, ExecutionException {
    long keepAliveTime = 1000L;
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(keepAliveTime, true, false, false, false),
        2, 10, 4);
    testPoolSizeScaling(pool, 8, false, keepAliveTime);
  }

  @Test
  public void testPoolWithoutProcessReuseScalesDownToMinPoolSize() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 5, 10, 3);
    testPoolSizeScaling(pool, 10, true, null);
  }

  @Test
  public void testPoolWithoutProcessReuseScalesDownToReserveSize() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 2, 10, 4);
    testPoolSizeScaling(pool, 8, true, null);
  }

  @Test
  public void testPoolWithKeepAliveTimeAndWithoutProcessReuseScalesDownToMinPoolSize() throws InterruptedException, ExecutionException {
    long keepAliveTime = 1000L;
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(keepAliveTime, true, false, false, false),
        5, 10, 3);
    testPoolSizeScaling(pool, 10, true, keepAliveTime);
  }

  @Test
  public void testPoolWithKeepAliveTimeAndWithoutProcessReuseScalesDownToReserveSize() throws InterruptedException, ExecutionException {
    long keepAliveTime = 1000L;
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(keepAliveTime, true, false, false, false),
        2, 10, 4);
    testPoolSizeScaling(pool, 8, true, keepAliveTime);
  }

  @Test
  public void testSubmitThrowsIllegalArgumentExceptionIfSubmissionNull() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 3, 5, 0);
    exceptionRule.expect(IllegalArgumentException.class);
    try {
      pool.submit(null);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitThrowsRejectedExecutionExceptionIfPoolShutdown() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 5, 5, 0);
    pool.shutdown();
    exceptionRule.expect(RejectedExecutionException.class);
    try {
      pool.submit(newSimpleSubmission());
    } finally {
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitSubmissionsExecutedIfPoolShutdownAfterSubmission() throws InterruptedException, ExecutionException {
    testSubmissionFuturesAfterShutdown(5, 5);
  }

  @Test
  public void testSubmitQueuedSubmissionsExecutedIfPoolShutdownAfterSubmission() throws InterruptedException, ExecutionException {
    testSubmissionFuturesAfterShutdown(3, 6);
  }

  @Test
  public void testSubmitSubmissionFutureThrowsExecutionExceptionIfPoolForceShutdownAfterSubmission() throws InterruptedException {
    testSubmissionFuturesAfterForceShutdown(5, 5);
  }

  @Test
  public void testForceShutdownReturnsQueuedSubmissionsSubmissionFuturesThrowExecutionException() throws InterruptedException {
    testSubmissionFuturesAfterForceShutdown(4, 8);
  }

  @Test
  public void testSubmitSubmissionWithReturnValue() throws InterruptedException, ExecutionException {
    AtomicInteger result = new AtomicInteger(0);
    Command command = new SimpleCommand("process 1", (c, o) -> {
      if ("ready".equals(o)) {
        result.set(13);
        return true;
      }
      return false;
    });
    Submission<AtomicInteger> submission = new SimpleSubmission<>(command, result);
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 3, 3, 0);
    Future<AtomicInteger> future = pool.submit(submission);
    Assert.assertFalse(future.isDone());
    Assert.assertEquals(13, future.get().get());
    Assert.assertTrue(future.isDone());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitSubmissionFutureThrowsExecutionExceptionIfCommandFails() throws InterruptedException, ExecutionException {
    Command command = new SimpleCommand("process 1", (c, o) -> {
      throw new FailedCommandException(c, o);
    });
    Submission<?> submission = new SimpleSubmission<>(command);
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 3, 5, 0);
    Future<?> future = pool.submit(submission);
    Assert.assertFalse(future.isDone());
    exceptionRule.expect(ExecutionException.class);
    exceptionRule.expectCause(CoreMatchers.isA(FailedCommandException.class));
    try {
      future.get();
    } finally {
      Assert.assertTrue(future.isDone());
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitSubmissionFutureThrowsTimeoutExceptionIfSubmissionNotCompleted()
      throws InterruptedException, ExecutionException, TimeoutException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Future<?> future = pool.submit(newSimpleSubmission());
    Assert.assertFalse(future.isDone());
    exceptionRule.expect(TimeoutException.class);
    try {
      future.get(WAIT_TIME_FOR_CONCURRENT_EVENTS, TimeUnit.MILLISECONDS);
    } finally {
      Assert.assertFalse(future.isDone());
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      Assert.assertTrue(future.isDone());
    }
  }

  @Test
  public void testSubmitSubmissionFutureDoesNotThrowTimeoutExceptionIfSubmissionCompleted()
      throws InterruptedException, ExecutionException, TimeoutException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Future<?> future = pool.submit(newSimpleSubmission());
    Assert.assertFalse(future.isDone());
    future.get(1000L + WAIT_TIME_FOR_CONCURRENT_EVENTS, TimeUnit.MILLISECONDS);
    Assert.assertTrue(future.isDone());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitSubmissionFutureThrowsExecutionExceptionIfCancelled() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Future<?> future = pool.submit(newSimpleSubmission());
    Assert.assertFalse(future.isCancelled());
    Assert.assertFalse(future.isDone());
    Assert.assertTrue(future.cancel(false));
    Assert.assertTrue(future.isCancelled());
    Assert.assertTrue(future.isDone());
    exceptionRule.expect(CancellationException.class);
    try {
      future.get();
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitSubmissionFutureThrowsCancellationExceptionButTaskExecutes() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    long start = System.currentTimeMillis();
    Future<?> future = pool.submit(newSimpleSubmission());
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Assert.assertTrue(future.cancel(false));
    exceptionRule.expect(CancellationException.class);
    try {
      future.get();
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      Assert.assertTrue(System.currentTimeMillis() - start > 1000);
    }
  }

  @Test
  public void testSubmitSubmissionFutureThrowsCancellationExceptionAndTaskInterruptedIfMayInterrupt()
      throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    long start = System.currentTimeMillis();
    Future<?> future = pool.submit(newSimpleSubmission());
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Assert.assertTrue(future.cancel(true));
    exceptionRule.expect(CancellationException.class);
    try {
      future.get();
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      Assert.assertTrue(System.currentTimeMillis() - start < 1000);
    }
  }

  @Test
  public void testSubmitSubmissionFutureCannotCancelAlreadyCancelledSubmission() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Future<?> future = pool.submit(newSimpleSubmission());
    Assert.assertTrue(future.cancel(false));
    Assert.assertTrue(future.isCancelled());
    Assert.assertTrue(future.isDone());
    Assert.assertFalse(future.cancel(false));
    Assert.assertFalse(future.cancel(true));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitSubmissionFutureCannotCancelAlreadyForceCancelledSubmission() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Future<?> future = pool.submit(newSimpleSubmission());
    Assert.assertTrue(future.cancel(true));
    Assert.assertTrue(future.isCancelled());
    Assert.assertTrue(future.isDone());
    Assert.assertFalse(future.cancel(false));
    Assert.assertFalse(future.cancel(true));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitSubmissionFutureCannotCancelAlreadyCompletedSubmission() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Future<?> future = pool.submit(newSimpleSubmission());
    future.get();
    Assert.assertTrue(future.isDone());
    Assert.assertFalse(future.cancel(false));
    Assert.assertFalse(future.cancel(true));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitSubmissionFutureCanCancelTimedOutSubmission() throws InterruptedException, ExecutionException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Future<?> future = pool.submit(newSimpleSubmission());
    try {
      future.get(WAIT_TIME_FOR_CONCURRENT_EVENTS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      // Ignore.
    }
    Assert.assertFalse(future.isDone());
    Assert.assertTrue(future.cancel(false));
    Assert.assertTrue(future.isCancelled());
    Assert.assertTrue(future.isDone());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testExecuteSubmissionWithReturnValue() throws InterruptedException, DisruptedExecutionException, FailedCommandException {
    AtomicReference<String> stringReference = new AtomicReference<>();
    Command command = new SimpleCommand("process 1", (c, o) -> {
      if ("ready".equals(o)) {
        stringReference.set("ready");
        return true;
      }
      return false;
    });
    Submission<AtomicReference<String>> submission = new SimpleSubmission<>(command, stringReference);
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    pool.execute(submission);
    Assert.assertTrue(submission.getResult().isPresent());
    Assert.assertEquals("ready", submission.getResult().get().get());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testExecuteThrowsFailedCommandExceptionIfCommandFails()
      throws InterruptedException, DisruptedExecutionException, FailedCommandException {
    Command command = new SimpleCommand("process 1", (c, o) -> {
      throw new FailedCommandException(c, o);
    });
    Submission<?> submission = new SimpleSubmission<>(command);
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    exceptionRule.expect(FailedCommandException.class);
    try {
      pool.execute(submission);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testExecuteThrowsDisruptedExecutionExceptionIfInterrupted()
      throws InterruptedException, DisruptedExecutionException, FailedCommandException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 1, 1, 0);
    Thread executorThread = Thread.currentThread();
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
      } catch (InterruptedException e) {
        logger.error(e.getMessage(), e);
      }
      executorThread.interrupt();
    });
    thread.start();
    exceptionRule.expect(DisruptedExecutionException.class);
    try {
      pool.execute(newSimpleSubmission());
    } finally {
      Assert.assertTrue(Thread.interrupted());
      try {
        Thread.sleep(0);
      } catch (InterruptedException e) {
        // Do this to reset the interrupted flag of the thread.
      }
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

    }
  }

  @Test
  public void testShutdownDoesNotWaitOnCancelledSubmissionsInTheQueue() throws InterruptedException {
    testShutdownWithCancelledQueuedSubmissions(false);
  }

  @Test
  public void testShutdownDoesNotWaitOnForceCancelledSubmissionsInTheQueue() throws InterruptedException {
    testShutdownWithCancelledQueuedSubmissions(true);
  }

  @Test
  public void testForceShutdownOnlyReturnsSubmissionsInTheQueue() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 2, 2, 0);
    Submission<?> submission1 = newSimpleSubmission();
    Submission<?> submission2 = newSimpleSubmission();
    Submission<?> submission3 = newSimpleSubmission();
    Future<?> future1 = pool.submit(submission1);
    Future<?> future2 = pool.submit(submission2);
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Future<?> future3 = pool.submit(submission3);
    List<Submission<?>> submissionsFromTheQueue = pool.forceShutdown();
    Assert.assertEquals(1, submissionsFromTheQueue.size());
    Assert.assertEquals(submission3, submissionsFromTheQueue.get(0));
    Assert.assertFalse(future1.isDone());
    Assert.assertFalse(future2.isDone());
    Assert.assertFalse(future3.isDone());
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertTrue(future1.isDone());
    Assert.assertTrue(future2.isDone());
    Assert.assertFalse(future3.isDone());
  }

  @Test
  public void testForceShutdownReturnsSubmissionsInTheQueueEvenIfCalledAfterShutdown() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 2, 2, 0);
    for (int i = 0; i < 4; i++) {
      pool.submit(newSimpleSubmission());
    }
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    pool.shutdown();
    Assert.assertTrue(pool.isShutdown());
    Assert.assertEquals(2, pool.forceShutdown().size());
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testForceShutdownReturnsEmptyListOnSecondCall() throws InterruptedException {
    ProcessPoolExecutor pool = new ProcessPoolExecutor(new TestProcessManagerFactory(), 3, 3, 0);
    for (int i = 0; i < 6; i++) {
      pool.submit(newSimpleSubmission());
    }
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Assert.assertEquals(3, pool.forceShutdown().size());
    Assert.assertTrue(pool.isShutdown());
    Assert.assertTrue(pool.forceShutdown().isEmpty());
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertTrue(pool.forceShutdown().isEmpty());
  }

}
