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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.viktorc.pp4j.api.JavaProcessConfig.JVMArch;
import net.viktorc.pp4j.api.JavaProcessConfig.JVMType;
import org.junit.Assume;
import org.junit.Test;

/**
 * An integration test class for {@link JavaProcessPoolExecutor}.
 *
 * @author Viktor Csomor
 */
public class JPPEPerformanceTest extends TestCase {

  /**
   * Tests the startup time of the process pool.
   *
   * @param processManagerFactory The process manager factory to use for the process pool.
   * @param minSize The minimum pool size.
   * @param maxSize The maximum pool size.
   * @param reserveSize The pool's reserve size.
   * @param upperBound The maximum acceptable startup time in milliseconds.
   * @return Whether the performance test was successful.
   * @throws InterruptedException If the thread is interrupted while the pool is starting up.
   */
  private boolean startupPerfTest(JavaProcessManagerFactory<?> processManagerFactory, int minSize, int maxSize, int reserveSize,
      long upperBound) throws InterruptedException {
    long start = System.currentTimeMillis();
    JavaProcessPoolExecutor pool = new JavaProcessPoolExecutor(processManagerFactory, minSize, maxSize, reserveSize);
    long time = System.currentTimeMillis() - start;
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    boolean success = time <= upperBound;
    logTime(success, time);
    return success;
  }

  /**
   * Submits <code>numOfSubmissions</code> tasks provided by the task supplier to the Java process pool and measures the total execution
   * time.
   *
   * @param pool The Java process pool to test.
   * @param taskSupplier The supplier of the tasks to submit.
   * @param reuse Whether the process executors are to be terminated after each submission.
   * @param numOfSubmissions The number of tasks to submit to the pool.
   * @param delay The number of milliseconds to wait between submissions.
   * @param lowerBound The minimum acceptable total execution time, including the delay periods between submissions, in milliseconds.
   * @param upperBound The maximum acceptable total execution time, including the delay periods between submissions, in milliseconds.
   * @return Whether the performance test was successful.
   * @throws InterruptedException If the thread is interrupted before the pool is terminated.
   * @throws ExecutionException If the execution of a task fails.
   */
  private boolean submissionPerfTest(JavaProcessPoolExecutor pool, Supplier<Callable<Serializable>> taskSupplier, boolean reuse,
      int numOfSubmissions, long delay, long lowerBound, long upperBound) throws InterruptedException, ExecutionException {
    List<Future<?>> futures = new ArrayList<>();
    long start = System.currentTimeMillis();
    for (int i = 0; i < numOfSubmissions; i++) {
      Thread.sleep(delay);
      futures.add(pool.submit(taskSupplier.get(), !reuse));
    }
    for (Future<?> future : futures) {
      future.get();
    }
    long time = System.currentTimeMillis() - start;
    pool.shutdown();
    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    boolean success = time >= lowerBound && time <= upperBound;
    logTime(success, time);
    return success;
  }

  @Test
  public void testStartup01() throws InterruptedException {
    Assume.assumeTrue(startupPerfTest(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()), 1, 1, 0, 1500));
  }

  @Test
  public void testStartup02() throws InterruptedException {
    Assume.assumeTrue(startupPerfTest(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.CLIENT, 2, 4, 256)), 1, 1, 0, 750));
  }

  @Test
  public void testStartup03() throws InterruptedException {
    Assume.assumeTrue(startupPerfTest(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.SERVER, 256, 4096, 4096), null, null, 5000L), 1, 1, 0, 750));
  }

  @Test
  public void testStartup04() throws InterruptedException {
    Assume.assumeTrue(startupPerfTest(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()), 10, 15, 5, 2500));
  }

  @Test
  public void testStartup05() throws InterruptedException {
    Assume.assumeTrue(startupPerfTest(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.CLIENT, 2, 4, 256)), 10, 15, 5, 2500));
  }

  @Test
  public void testStartup06() throws InterruptedException {
    Assume.assumeTrue(startupPerfTest(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.SERVER, 256, 4096, 4096), null, null, 5000L), 1, 1, 0, 2500));
  }

}
