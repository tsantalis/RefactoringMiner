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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import net.viktorc.pp4j.impl.JavaProcessPoolExecutor.UncheckedExecutionException;
import net.viktorc.pp4j.impl.JavaSubmission.SerializableTask;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * A unit test class for {@link JavaProcessPoolExecutor}.
 *
 * @author Viktor Csomor
 */
public class JavaProcessPoolExecutorTest extends TestCase {

  /**
   * Constructs and returns a Java process pool using the default Java process configuration, no startup or wrap-up tasks, and no idle
   * process termination.
   *
   * @param minSize The minimum pool size.
   * @param maxSize The maximum pool size.
   * @param reserveSize The reserve size.
   * @return The Java process pool.
   * @throws InterruptedException If the thread is interrupted while the pool is starting up.
   */
  private static JavaProcessPoolExecutor newDefaultJavaProcessPool(int minSize, int maxSize, int reserveSize)
      throws InterruptedException {
    return new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()), minSize, maxSize, reserveSize);
  }

  /**
   * Creates a temporary file that is deleted when the JVM exits.
   *
   * @return A reference to the temporary file.
   * @throws IOException If the file cannot be created.
   */
  private static File createTempFile() throws IOException {
    File tempFile = File.createTempFile("temp", null);
    tempFile.deleteOnExit();
    return tempFile;
  }

  /**
   * Writes the specified message to the provided file.
   *
   * @param file The file to write to.
   * @param message The message to write.
   * @throws IOException If the message cannot be written to the file.
   */
  private static void writeToFile(File file, String message) throws IOException {
    Files.write(file.toPath(), message.getBytes(JavaObjectCodec.CHARSET), StandardOpenOption.APPEND);
  }

  /**
   * Reads all contents of the specified file into a string.
   *
   * @param file The file whose contents are to be read.
   * @return The contents of the file.
   * @throws IOException If the contents of the file cannot be read.
   */
  private static String readFileContents(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), JavaObjectCodec.CHARSET);
  }

  /**
   * Creates and returns a serializable runnable that writes the provided message to the specified file.
   *
   * @param file The file to write to.
   * @param message The message to write.
   * @return The serializable runnable lambda.
   */
  private static Runnable newRunnableForWritingMessageToFile(File file, String message) {
    return (Runnable & Serializable) () -> {
      try {
        writeToFile(file, message);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @Test
  public void testStartupTasksExecuted() throws IOException, InterruptedException {
    File tempFile = createTempFile();
    String message = "oofta";
    JavaProcessPoolExecutor pool = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig(),
        (Runnable & Serializable) newRunnableForWritingMessageToFile(tempFile, message), null, null), 3, 3, 0);
    Assert.assertEquals(message + message + message, readFileContents(tempFile));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testWrapUpTasksExecuted() throws IOException, InterruptedException {
    File tempFile = createTempFile();
    String message = "dingus";
    JavaProcessPoolExecutor pool = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig(), null,
        (Runnable & Serializable) newRunnableForWritingMessageToFile(tempFile, message), null), 2, 4, 0);
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertEquals(message + message, readFileContents(tempFile));
  }

  @Test
  public void testExecuteSuccessful() throws InterruptedException, IOException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(2, 2, 1);
    File tempFile = createTempFile();
    String message = "whoop-dee-doo";
    pool.execute(newRunnableForWritingMessageToFile(tempFile, message));
    Assert.assertEquals(message, readFileContents(tempFile));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testExecuteThrowsUncheckedExecutionExceptionIfTaskFails() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(UncheckedExecutionException.class);
    exceptionRule.expectCause(CoreMatchers.isA(ExecutionException.class));
    try {
      pool.execute((Runnable & Serializable) () -> {
        throw new RuntimeException();
      });
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testExecuteThrowsUncheckedExecutionExceptionIfThreadInterrupted() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    Thread executorThread = Thread.currentThread();
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
      } catch (InterruptedException e) {
        logger.error(e.getMessage(), e);
      }
      executorThread.interrupt();
    });
    exceptionRule.expect(UncheckedExecutionException.class);
    exceptionRule.expectCause(CoreMatchers.isA(InterruptedException.class));
    thread.start();
    try {
      pool.execute((Runnable & Serializable) () -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
    } finally {
      try {
        Thread.sleep(0);
      } catch (InterruptedException e) {
        // Reset the interrupted flag of the main thread.
      }
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitCallableThrowsIllegalArgumentExceptionIfTaskNull() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 2, 1);
    exceptionRule.expect(IllegalArgumentException.class);
    try {
      pool.submit((Callable<Integer> & Serializable) null);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitCallableThrowsRejectedExecutionExceptionIfTasNotSerializable() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 2, 1);
    exceptionRule.expect(RejectedExecutionException.class);
    try {
      pool.submit(() -> 1);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitCallableFutureReturnsExpectedValue() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 2, 1);
    int multiplicand1 = 3;
    int multiplicand2 = 4;
    Future<Integer> future = pool.submit((Callable<Integer> & Serializable) () -> multiplicand1 * multiplicand2);
    Assert.assertEquals(new Integer(12), future.get());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitRunnableWithResultFutureReturnsExpectedValue() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 2, 1);
    AtomicInteger result = new AtomicInteger();
    Future<AtomicInteger> future = pool.submit((Runnable & Serializable) () -> result.set(1992), result);
    Assert.assertEquals(1992, future.get().get());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitRunnableSuccessful() throws InterruptedException, ExecutionException, IOException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(2, 2, 0);
    File tempFile = createTempFile();
    String message = "dingleberry";
    Future<?> future = pool.submit(newRunnableForWritingMessageToFile(tempFile, message));
    future.get();
    Assert.assertEquals(message, readFileContents(tempFile));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitCallableTerminatesProcessAfterwards() throws IOException, InterruptedException, ExecutionException {
    File tempFile = createTempFile();
    String message = "jiffy";
    JavaProcessPoolExecutor pool = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig(), null,
        (Runnable & Serializable) newRunnableForWritingMessageToFile(tempFile, message), null), 1, 1, 0);
    Future<Integer> future = pool.submit((Callable<Integer> & Serializable) () -> 1, true);
    Assert.assertEquals(new Integer(1), future.get());
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Assert.assertEquals(message, readFileContents(tempFile));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitRunnableWithResultTerminatesProcessAfterwards() throws IOException, InterruptedException, ExecutionException {
    File tempFile = createTempFile();
    String message = "dinghy";
    JavaProcessPoolExecutor pool = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig(), null,
        (Runnable & Serializable) newRunnableForWritingMessageToFile(tempFile, message), null), 1, 2, 0);
    AtomicInteger result = new AtomicInteger();
    Future<AtomicInteger> future = pool.submit((Runnable & Serializable) () -> result.set(-1), result, true);
    Assert.assertEquals(-1, future.get().get());
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Assert.assertEquals(message, readFileContents(tempFile));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitRunnableTerminatesProcessAfterwards() throws IOException, InterruptedException, ExecutionException {
    File tempFile = createTempFile();
    String message = "fair dinkum";
    JavaProcessPoolExecutor pool = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig(), null,
        (Runnable & Serializable) newRunnableForWritingMessageToFile(tempFile, message), null), 2, 2, 0);
    Future<?> future = pool.submit((Runnable & Serializable) () -> {}, true);
    future.get();
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    Assert.assertEquals(message, readFileContents(tempFile));
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testSubmitNonSerializableCallableThrowsRejectedExecutionException() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(RejectedExecutionException.class);
    try {
      pool.submit(() -> 2019);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitNonSerializableRunnableWithResultThrowsRejectedExecutionException() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(RejectedExecutionException.class);
    try {
      pool.submit(() -> {}, 0);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitRunnableWithNonSerializableResultThrowsRejectedExecutionException() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(RejectedExecutionException.class);
    try {
      pool.submit((Runnable & Serializable) () -> {}, Optional.empty());
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitNonSerializableRunnableThrowsRejectedExecutionException() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(RejectedExecutionException.class);
    try {
      pool.submit(() -> {});
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testSubmitCallableWithNonSerializableReturnTypeFutureThrowsExecutionException()
      throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(ExecutionException.class);
    try {
      Future<Optional<?>> future = pool.submit((Callable<Optional<?>> & Serializable) Optional::empty);
      future.get();
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAllThrowsNullPointerExceptionIfTasksNull() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(NullPointerException.class);
    try {
      pool.invokeAll(null);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAllThrowsIllegalArgumentExceptionIfTasksEmpty() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(IllegalArgumentException.class);
    try {
      pool.invokeAll(Collections.emptyList());
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAllThrowsIllegalArgumentExceptionIfTasksContainsNull() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(IllegalArgumentException.class);
    try {
      pool.invokeAll(Arrays.asList((Callable<Integer> & Serializable) () -> 1, null));
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAllReturnedFuturesDone() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(4, 4, 0);
    List<Callable<Integer>> tasks = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(500);
        return 3;
      });
    }
    List<Future<Integer>> futures = pool.invokeAll(tasks);
    Assert.assertEquals(6, futures.size());
    for (Future<Integer> future : futures) {
      Assert.assertTrue(future.isDone());
      Assert.assertEquals(new Integer(3), future.get());
    }
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testInvokeAllReturnsFutureForFailedTask() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(4, 4, 0);
    List<Future<Serializable>> futures = pool.invokeAll(Collections.singletonList((Callable<Serializable> & Serializable) () -> {
      throw new Exception();
    }));
    Assert.assertEquals(1, futures.size());
    Future<Serializable> future = futures.get(0);
    Assert.assertTrue(future.isDone());
    exceptionRule.expect(ExecutionException.class);
    exceptionRule.expectCause(CoreMatchers.isA(Exception.class));
    try {
      future.get();
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAllThrowsInterruptedException() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(4, 4, 0);
    Thread executorThread = Thread.currentThread();
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      executorThread.interrupt();
    });
    exceptionRule.expect(InterruptedException.class);
    thread.start();
    try {
      pool.invokeAll(Collections.singletonList((Callable<Serializable> & Serializable) () -> {
        Thread.sleep(500);
        return null;
      }));
    } finally {
      try {
        Thread.sleep(0);
      } catch (InterruptedException e) {
        // Reset interrupted flag.
      }
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAllUncompletedTasksCancelledAfterTimeout() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(2, 2, 0);
    List<Callable<Integer>> tasks = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(1000);
        return 1;
      });
    }
    List<Future<Integer>> futures = pool.invokeAll(tasks, WAIT_TIME_FOR_CONCURRENT_EVENTS, TimeUnit.MILLISECONDS);
    Assert.assertEquals(4, futures.size());
    for (Future<Integer> future : futures) {
      Assert.assertTrue(future.isDone());
      Assert.assertTrue(future.isCancelled());
      try {
        future.get();
        Assert.assertTrue(false);
      } catch (Exception e) {
        Assert.assertTrue(e instanceof CancellationException);
      }
    }
    pool.shutdownNow();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testInvokeAllCompletedTasksNotCancelledAfterTimeout() throws InterruptedException, ExecutionException {
    String result = "hell yeah";
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(2, 2, 0);
    List<Future<String>> futures = pool.invokeAll(Arrays.asList((Callable<String> & Serializable) () -> {
          Thread.sleep(2000);
          return "no way";
        }, (Callable<String> & Serializable) () -> result), 1000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(2, futures.size());
    Future<String> future1 = futures.get(0);
    Future<String> future2 = futures.get(1);
    Assert.assertTrue(future1.isDone());
    Assert.assertTrue(future1.isCancelled());
    Assert.assertTrue(future2.isDone());
    Assert.assertFalse(future2.isCancelled());
    Assert.assertEquals(result, future2.get());
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testInvokeAnyThrowsNullPointerExceptionIfTasksNull() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(NullPointerException.class);
    try {
      pool.invokeAny(null);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAnyThrowsIllegalArgumentExceptionIfTasksEmpty() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(IllegalArgumentException.class);
    try {
      pool.invokeAny(Collections.emptyList());
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAnyThrowsIllegalArgumentExceptionIfTasksContainsNull() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(1, 1, 0);
    exceptionRule.expect(IllegalArgumentException.class);
    try {
      pool.invokeAny(Arrays.asList((Callable<Integer> & Serializable) () -> 1, null));
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAnyReturnsExpectedValue() throws InterruptedException, ExecutionException {
    int expectedResult = 27;
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(2, 2, 0);
    int actualResult = pool.invokeAny(Arrays.asList((Callable<Integer> & Serializable) () -> 27,
        (Callable<Integer> & Serializable) () -> 27));
    Assert.assertEquals(expectedResult, actualResult);
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testInvokeAnyReturnsValueIfAtLeastOneTaskSuccessful() throws InterruptedException, ExecutionException {
    int expectedResult = 13;
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(3, 3, 0);
    int actualResult = pool.invokeAny(Arrays.asList((Callable<Integer> & Serializable) () -> {
          throw new Exception();
        },
        (Callable<Integer> & Serializable) () -> expectedResult));
    Assert.assertEquals(expectedResult, actualResult);
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
  }

  @Test
  public void testInvokeAnyThrowsExecutionExceptionIfNoSuccessfulTask() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(2, 2, 0);
    exceptionRule.expect(ExecutionException.class);
    try {
      pool.invokeAny(Collections.singletonList((Callable<Integer> & Serializable) () -> {
        throw new Exception();
      }));
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testInvokeAnyThrowsTimeoutExceptionIfNoSuccessfulTaskUntilTimeout()
      throws InterruptedException, ExecutionException, TimeoutException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(2, 2, 0);
    exceptionRule.expect(TimeoutException.class);
    try {
      pool.invokeAny(Arrays.asList((Callable<Integer> & Serializable) () -> {
        Thread.sleep(1000);
        return 1;
      }, (Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return 2;
      }), 500, TimeUnit.MILLISECONDS);
    } finally {
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void testShutdownNow() throws InterruptedException {
    JavaProcessPoolExecutor pool = newDefaultJavaProcessPool(4, 4, 0);
    List<SerializableTask<?>> submissionTasks = new ArrayList<>();
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      JavaSubmission<?> submission = new JavaSubmission<>((Callable<Integer> & Serializable) () -> {
        Thread.sleep(1000);
        return (new Random()).nextInt();
      });
      submissionTasks.add(submission.getTask());
      futures.add(pool.submit(submission));
    }
    Thread.sleep(WAIT_TIME_FOR_CONCURRENT_EVENTS);
    List<Runnable> tasksAwaitingExecution = pool.shutdownNow();
    Assert.assertTrue(pool.isShutdown());
    for (Future<?> future : futures) {
      Assert.assertFalse(future.isDone());
    }
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    Assert.assertTrue(pool.isTerminated());
    int numOfDoneSubmissions = 0;
    for (Future<?> future : futures) {
      if (future.isDone()) {
        numOfDoneSubmissions++;
      }
    }
    Assert.assertEquals(4, numOfDoneSubmissions);
    Assert.assertEquals(4, tasksAwaitingExecution.size());
    for (Runnable task : tasksAwaitingExecution) {
      Assert.assertTrue(submissionTasks.contains(task));
      submissionTasks.remove(task);
    }
  }

}
