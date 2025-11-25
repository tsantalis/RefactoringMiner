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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.viktorc.pp4j.api.Command;
import net.viktorc.pp4j.api.Submission;
import net.viktorc.pp4j.impl.TestUtils.TestProcessManagerFactory;
import org.junit.Assume;
import org.junit.Test;

/**
 * An integration test class for {@link ProcessPoolExecutor}.
 *
 * @author Viktor Csomor
 */
public class PPEPerformanceTest extends TestCase {

  /**
   * Creates a process pool of test processes.
   *
   * @param minPoolSize The minimum pool size.
   * @param maxPoolSize The maximum pool size.
   * @param reserveSize The reserve size.
   * @param keepAliveTime The number of milliseconds of idleness after which the process are to be terminated.
   * @param verifyStartup Whether the processes should only be considered started up after printing a certain output to their standard outs.
   * @param executeInitSubmission Whether an initial submission should be executed upon startup in each process.
   * @param manuallyTerminate Whether the processes should be terminated using the termination command or an OS level kill signal.
   * @return The process pool.
   * @throws InterruptedException If the thread is interrupted while waiting for the pool to be initialized.
   */
  private static ProcessPoolExecutor createPool(int minPoolSize, int maxPoolSize, int reserveSize, Long keepAliveTime,
      boolean verifyStartup, boolean executeInitSubmission, boolean manuallyTerminate) throws InterruptedException {
    TestProcessManagerFactory managerFactory = new TestProcessManagerFactory(
        keepAliveTime, verifyStartup, executeInitSubmission, manuallyTerminate, false);
    return new ProcessPoolExecutor(managerFactory, minPoolSize, maxPoolSize, reserveSize);
  }

  /**
   * Creates a submission to submit to a test process.
   *
   * @param procTimes An array of the number of processing cycles each command of the submission is to entail.
   * {@link Command#isCompleted(String, boolean)} method is invoked.
   * @param times A list of times that the submission is to update upon its completion to calculate the execution duration.
   * @param index The index of the submission. Only the element at this index is to be updated in <code>times</code>.
   * @return The submission.
   */
  private static SimpleSubmission<?> createSubmission(int[] procTimes, List<Long> times, int index) {
    List<Command> commands = null;
    if (procTimes != null) {
      commands = new ArrayList<>();
      for (int procTime : procTimes) {
        commands.add(new SimpleCommand("process " + procTime, (command, outputLine) -> "ready".equals(outputLine)));
      }
    }
    return new SimpleSubmission<Object>(commands) {

      @Override
      public void onFinishedExecution() {
        times.set(index, System.nanoTime() - times.get(index));
      }
    };
  }

  /**
   * Submits the specified number of submissions to the provided process pool.
   *
   * @param processPool The process pool to submit the submissions to.
   * @param procTimes An array of the number of processing cycles each command of each submission is to entail.
   * @param submissions The total number of submissions to submit.
   * @param timeSpan The number of milliseconds within which the uniformly distributed requests should be submitted.
   * @param reuse Whether a process can execute multiple commands or should be terminated after execution.
   * @param futures An empty list to add the futures returned by the process pool when submitting the submissions to.
   * @param times An empty list to which an entry is added for each submission then each submission updates it upon completion to record
   * their individual execution times.
   * @throws InterruptedException If the thread is interrupted while waiting between submissions.
   */
  private static void submitSubmissions(ProcessPoolExecutor processPool, int[] procTimes, int submissions, long timeSpan, boolean reuse,
      List<Future<?>> futures, List<Long> times) throws InterruptedException {
    long frequency = submissions > 0 ? timeSpan / submissions : 0;
    for (int i = 0; i < submissions; i++) {
      if (i != 0 && frequency > 0) {
        Thread.sleep(frequency);
      }
      Submission<?> submission = createSubmission(procTimes, times, i);
      times.add(System.nanoTime());
      futures.add(processPool.submit(submission, !reuse));
    }
  }
  /**
   * Evaluates the success of the performance test.
   *
   * @param times A list of the execution times of each submission or an empty list if an error occurred during submission.
   * @param submissions The total number of submissions.
   * @param upperBound The maximum accepted execution time in milliseconds.
   * @param lowerBound The minimum accepted execution time in milliseconds.
   * @return Whether the performance test can be considered successful.
   */
  private boolean evaluatePerfTestResults(List<Long> times, int submissions, long upperBound, long lowerBound) {
    if (times.size() == submissions) {
      boolean pass = true;
      for (Long time : times) {
        time = Math.round(((double) time / 1000000));
        boolean fail = time > upperBound || time < lowerBound;
        if (fail) {
          pass = false;
        }
        logTime(!fail, time);
      }
      return pass;
    } else {
      logger.info(String.format("Some requests were not processed: %d/%d", times.size(), submissions));
      return false;
    }
  }

  /**
   * Submits the specified number of commands with the specified frequency to a the test process pool corresponding to the specified
   * parameters and determines whether it performs well enough based on the number of processed requests and the times it took to process
   * them.
   *
   * @param processPool The process pool executor to test.
   * @param reuse Whether a process can execute multiple commands.
   * @param procTimes The times for which the test processes should "execute" commands. Each element stands for a command. If there are
   * multiple elements, the commands will be chained.
   * @param submissions The number of commands to submit.
   * @param timeSpan The number of milliseconds within which the uniformly distributed requests should be submitted.
   * @param lowerBound The minimum acceptable submission execution time.
   * @param upperBound The maximum acceptable submission execution time.
   * @return Whether the test passes.
   * @throws Exception If the process pool cannot be created.
   */
  private boolean perfTest(ProcessPoolExecutor processPool, boolean reuse, int[] procTimes, int submissions, long timeSpan,
      long lowerBound, long upperBound) throws Exception {
    try {
      List<Long> times = new ArrayList<>(submissions);
      List<Future<?>> futures = new ArrayList<>(submissions);
      submitSubmissions(processPool, procTimes, submissions, timeSpan, reuse, futures, times);
      for (Future<?> future : futures) {
        future.get();
      }
      return evaluatePerfTestResults(times, submissions, upperBound, lowerBound);
    } finally {
      processPool.shutdown();
      processPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test01() throws Exception {
    ProcessPoolExecutor pool = createPool(0, 100, 0, null, true, true, false);
    Assume.assumeTrue(perfTest(pool, true, new int[]{5}, 100, 10000, 4995, 6250));
  }

  @Test
  public void test02() throws Exception {
    ProcessPoolExecutor pool = createPool(50, 150, 20, null, false, true, false);
    Assume.assumeTrue(perfTest(pool, true, new int[]{5}, 100, 5000, 4995, 5100));
  }

  @Test
  public void test03() throws Exception {
    ProcessPoolExecutor pool = createPool(10, 25, 5, 15000L, true, true, false);
    Assume.assumeTrue(perfTest(pool, true, new int[]{5}, 20, 10000, 4995, 5100));
  }

  @Test
  public void test04() throws Exception {
    ProcessPoolExecutor pool = createPool(50, 150, 20, null, false, true, true);
    Assume.assumeTrue(perfTest(pool, true, new int[]{5}, 100, 5000, 4995, 5100));
  }

  @Test
  public void test05() throws Exception {
    ProcessPoolExecutor pool = createPool(10, 50, 5, 15000L, true, true, false);
    Assume.assumeTrue(perfTest(pool, true, new int[]{5, 3, 2}, 50, 10000, 9995, 10340));
  }

  @Test
  public void test06() throws Exception {
    ProcessPoolExecutor pool = createPool(100, 250, 20, null, true, true, true);
    Assume.assumeTrue(perfTest(pool, true, new int[]{5}, 800, 20000, 4995, 6000));
  }

  @Test
  public void test07() throws Exception {
    ProcessPoolExecutor pool = createPool(0, 100, 0, null, false, true, false);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 100, 10000, 4995, 6850));
  }

  @Test
  public void test08() throws Exception {
    ProcessPoolExecutor pool = createPool(50, 150, 10, null, true, true, false);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 100, 5000, 4995, 5620));
  }

  @Test
  public void test09() throws Exception {
    ProcessPoolExecutor pool = createPool(10, 25, 5, 15000L, false, true, true);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 20, 10000, 4995, 5100));
  }

  @Test
  public void test10() throws Exception {
    ProcessPoolExecutor pool = createPool(50, 150, 10, null, true, true, true);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 100, 5000, 4995, 5600));
  }

  @Test
  public void test11() throws Exception {
    ProcessPoolExecutor pool = createPool(10, 50, 5, 15000L, false, true, false);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5, 3, 2}, 50, 10000, 9995, 10350));
  }

  @Test
  public void test12() throws Exception {
    ProcessPoolExecutor pool = createPool(50, 250, 20, null, true, true, true);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 800, 20000, 4995, 6000));
  }

  @Test
  public void test13() throws Exception {
    ProcessPoolExecutor pool = createPool(20, 40, 4, 250L, true, true, true);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 50, 5000, 4995, 8200));
  }

  @Test
  public void test14() throws Exception {
    ProcessPoolExecutor pool = createPool(1, 1, 0, 20000L, true, true, true);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 5, 30000, 4995, 5250));
  }

  @Test
  public void test15() throws Exception {
    ProcessPoolExecutor pool = createPool(1, 1, 0, null, true, true, false);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 5, 20000, 4995, 13250));
  }

  @Test
  public void test16() throws Exception {
    ProcessPoolExecutor pool = createPool(20, 20, 0, null, true, true, false);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 20, 5000, 4995, 5200));
  }

  @Test
  public void test17() throws Exception {
    ProcessPoolExecutor pool = createPool(20, 20, 0, null, true, true, false);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 40, 10000, 4995, 6200));
  }

  @Test
  public void test18() throws Exception {
    ProcessPoolExecutor pool = createPool(50, 250, 20, null, true, false, true);
    Assume.assumeTrue(perfTest(pool, false, new int[]{5}, 800, 20000, 4995, 5250));
  }

}