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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import net.viktorc.pp4j.api.JavaProcessConfig.JVMArch;
import net.viktorc.pp4j.api.JavaProcessConfig.JVMType;
import org.junit.Assert;
import org.junit.Test;

/**
 * An integration test class for {@link JavaProcessPoolExecutor}.
 *
 * @author Viktor Csomor
 */
public class JPPEPerformanceTest extends TestCase {

  // Startup testing
  @Test
  public void test01() throws InterruptedException {
    long start = System.currentTimeMillis();
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        1, 1, 0);
    try {
      long time = System.currentTimeMillis() - start;
      boolean success = time < 1500;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test02() throws InterruptedException {
    long start = System.currentTimeMillis();
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.CLIENT, 2, 4, 256)),
        1, 1, 0);
    try {
      long time = System.currentTimeMillis() - start;
      boolean success = time < 750;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test03() throws InterruptedException {
    long start = System.currentTimeMillis();
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.SERVER, 256, 4096, 4096), null, null, 5000L),
        1, 1, 0);
    try {
      long time = System.currentTimeMillis() - start;
      boolean success = time < 750;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test04() throws InterruptedException {
    long start = System.currentTimeMillis();
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        10, 15, 5);
    try {
      long time = System.currentTimeMillis() - start;
      boolean success = time < 2500;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test05() throws InterruptedException {
    long start = System.currentTimeMillis();
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.CLIENT, 2, 4, 256)),
        10, 15, 5);
    try {
      long time = System.currentTimeMillis() - start;
      boolean success = time < 2500;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test06() throws InterruptedException {
    long start = System.currentTimeMillis();
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.SERVER, 256, 4096, 4096), null, null, 5000L),
        10, 15, 5);
    try {
      long time = System.currentTimeMillis() - start;
      boolean success = time < 2500;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Submission testing.
  @Test
  public void test07() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        5, 5, 0);
    try {
      List<Future<?>> futures = new ArrayList<>();
      AtomicInteger j = new AtomicInteger(2);
      for (int i = 0; i < 5; i++) {
        futures.add(exec.submit((Runnable & Serializable) () -> {
          j.incrementAndGet();
          Thread t = new Thread(() -> {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(e);
            }
            j.incrementAndGet();
          });
          t.start();
          try {
            t.join();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }));
      }
      for (Future<?> f : futures) {
        f.get();
      }
      Assert.assertEquals(2, j.get());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test08() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.CLIENT, 2, 4, 256)),
        5, 5, 0);
    try {
      List<Future<?>> futures = new ArrayList<>();
      AtomicInteger j = new AtomicInteger(2);
      for (int i = 0; i < 5; i++) {
        futures.add(exec.submit((Runnable & Serializable) () -> {
          j.incrementAndGet();
          Thread t = new Thread(() -> {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(e);
            }
            j.incrementAndGet();
          });
          t.start();
          try {
            t.join();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }, j));
      }
      for (Future<?> f : futures) {
        Assert.assertEquals(4, ((AtomicInteger) f.get()).get());
      }
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test09() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        1, 1, 0);
    int base = 13;
    try {
      Assert.assertEquals(52, (int) exec.submit((Callable<Integer> & Serializable) () -> 4 * base).get());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Synchronous execution testing.
  @Test
  public void test10() throws InterruptedException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        2, 2, 0);
    try {
      long start = System.currentTimeMillis();
      exec.execute((Runnable & Serializable) () -> {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      });
      long time = System.currentTimeMillis() - start;
      boolean success = time < 5650 && time > 4995;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Invocation testing.
  @Test
  public void test11() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        2, 2, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(4000);
        return (int) Math.pow(base, 3);
      });
      long start = System.currentTimeMillis();
      List<Future<Integer>> results = exec.invokeAll(tasks);
      long time = System.currentTimeMillis() - start;
      boolean success = time < 4650 && time > 3995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(169, (int) results.get(0).get());
      Assert.assertEquals(2197, (int) results.get(1).get());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test12() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        1, 1, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(4000);
        return (int) Math.pow(base, 3);
      });
      long start = System.currentTimeMillis();
      List<Future<Integer>> results = exec.invokeAll(tasks);
      long time = System.currentTimeMillis() - start;
      boolean success = time < 6350 && time > 5995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(169, (int) results.get(0).get());
      Assert.assertEquals(2197, (int) results.get(1).get());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test13() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        2, 2, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(4000);
        return (int) Math.pow(base, 3);
      });
      long start = System.currentTimeMillis();
      List<Future<Integer>> results = exec.invokeAll(tasks, 3000, TimeUnit.MILLISECONDS);
      long time = System.currentTimeMillis() - start;
      boolean success = time < 3350 && time > 2995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(169, (int) results.get(0).get());
      exceptionRule.expect(CancellationException.class);
      results.get(1).get();
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test14() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        1, 1, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(4000);
        return (int) Math.pow(base, 3);
      });
      long start = System.currentTimeMillis();
      List<Future<Integer>> results = exec.invokeAll(tasks, 3000, TimeUnit.MILLISECONDS);
      long time = System.currentTimeMillis() - start;
      boolean success = time < 3350 && time > 2995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(169, (int) results.get(0).get());
      exceptionRule.expect(CancellationException.class);
      results.get(1).get();
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test15() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        2, 2, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        throw new RuntimeException();
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(4000);
        return (int) Math.pow(base, 3);
      });
      long start = System.currentTimeMillis();
      int result = exec.invokeAny(tasks);
      long time = System.currentTimeMillis() - start;
      boolean success = time < 4350 && time > 3995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertTrue(result == 169 || result == 2197);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test16() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        2, 2, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        throw new Exception();
      });
      long start = System.currentTimeMillis();
      int result = exec.invokeAny(tasks);
      long time = System.currentTimeMillis() - start;
      boolean success = time < 2350 && time > 1995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(169, result);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test17() throws InterruptedException, ExecutionException, TimeoutException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        2, 2, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(4000);
        return (int) Math.pow(base, 3);
      });
      long start = System.currentTimeMillis();
      int result = exec.invokeAny(tasks, 3000, TimeUnit.MILLISECONDS);
      long time = System.currentTimeMillis() - start;
      boolean success = time < 3350 && time > 2995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(169, result);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test18() throws InterruptedException, ExecutionException, TimeoutException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        2, 2, 0);
    try {
      int base = 13;
      List<Callable<Integer>> tasks = new ArrayList<>();
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(2000);
        return (int) Math.pow(base, 2);
      });
      tasks.add((Callable<Integer> & Serializable) () -> {
        Thread.sleep(4000);
        return (int) Math.pow(base, 3);
      });
      exceptionRule.expect(TimeoutException.class);
      exec.invokeAny(tasks, 1000, TimeUnit.MILLISECONDS);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Test of shutdownNow.
  @Test
  public void test19() throws InterruptedException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        1, 1, 0);
    try {
      Runnable task1 = (Runnable & Serializable) () -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      };
      Callable<Integer> task2 = (Callable<Integer> & Serializable) () -> {
        try {
          Thread.sleep(2000);
          return 0;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      };
      exec.submit(task1);
      exec.submit(task1);
      exec.submit(task2);
      long start = System.currentTimeMillis();
      List<Runnable> queuedTasks = exec.shutdownNow();
      long time = System.currentTimeMillis() - start;
      boolean success = time < 20;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(2, queuedTasks.size());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test20() throws InterruptedException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        1, 1, 0);
    try {
      JavaSubmission<?> submission1 = new JavaSubmission<>((Runnable & Serializable) () -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      });
      JavaSubmission<Integer> submission2 = new JavaSubmission<>((Callable<Integer> & Serializable) () -> {
        try {
          Thread.sleep(2000);
          return 0;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      });
      exec.submit(submission1);
      Thread.sleep(100);
      exec.submit(submission2);
      exec.submit(submission2);
      long start = System.currentTimeMillis();
      List<Runnable> queuedTasks = exec.shutdownNow();
      long time = System.currentTimeMillis() - start;
      boolean success = time < 20;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(2, queuedTasks.size());
      Assert.assertFalse(queuedTasks.contains(submission1.getTask()));
      Assert.assertTrue(queuedTasks.contains(submission2.getTask()));
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Task and result exchange performance testing.
  @Test
  public void test21() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        1, 1, 0);
    try {
      long start = System.currentTimeMillis();
      AtomicInteger res = exec.submit((Callable<AtomicInteger> & Serializable) () -> {
        Thread.sleep(2000);
        return new AtomicInteger(13);
      }).get();
      long time = System.currentTimeMillis() - start;
      boolean success = time < 2350 && time > 1995;
      logTime(success, time);
      Assert.assertTrue(success);
      Assert.assertEquals(13, res.get());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test22() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(JVMArch.BIT_64, JVMType.CLIENT, 2, 4, 256)),
        30, 80, 10);
    try {
      List<Future<AtomicInteger>> results = new ArrayList<>();
      long start = System.currentTimeMillis();
      for (int i = 0; i < 50; i++) {
        Thread.sleep(50);
        results.add(exec.submit((Callable<AtomicInteger> & Serializable) () -> {
          Thread.sleep(5000);
          return new AtomicInteger();
        }));
      }
      for (Future<AtomicInteger> res : results) {
        res.get();
      }
      long time = System.currentTimeMillis() - start;
      boolean success = time < 15550 && time > 7495;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test23() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(
        new SimpleJavaProcessConfig(2, 4, 256), null, null, 500L),
        30, 80, 10);
    try {
      List<Future<AtomicInteger>> results = new ArrayList<>();
      long start = System.currentTimeMillis();
      for (int i = 0; i < 50; i++) {
        Thread.sleep(50);
        results.add(exec.submit((Callable<AtomicInteger> & Serializable) () -> {
          Thread.sleep(5000);
          return new AtomicInteger();
        }));
      }
      for (Future<AtomicInteger> res : results) {
        res.get();
      }
      long time = System.currentTimeMillis() - start;
      boolean success = time < 20550 && time > 7500;
      logTime(success, time);
      Assert.assertTrue(success);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Java process manager factory testing.
  @Test
  public void test24() throws InterruptedException {
    JavaProcessManagerFactory<?> processManagerFactory = new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig());
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(processManagerFactory, 5, 5, 0);
    try {
      Assert.assertEquals(processManagerFactory, exec.getProcessManagerFactory());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Not serializable task testing.
  @Test
  public void test25() throws InterruptedException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        0, 1, 0);
    try {
      exceptionRule.expect(RejectedExecutionException.class);
      exec.submit(() -> 1);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test26() throws InterruptedException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        0, 1, 0);
    try {
      exceptionRule.expect(RejectedExecutionException.class);
      exec.submit(System::gc);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  @Test
  public void test27() throws InterruptedException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig()),
        0, 1, 0);
    try {
      exceptionRule.expect(RejectedExecutionException.class);
      AtomicInteger n = new AtomicInteger(0);
      exec.submit(() -> n.set(1), n);
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Startup task testing.
  @Test
  public void test28() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig(),
        (Runnable & Serializable) () -> {
          for (int i = 0; i < 10; i++) {
            System.out.println("Starting up");
          }
        }, null, null), 0, 1, 0);
    try {
      AtomicInteger n = new AtomicInteger(0);
      Assert.assertEquals(1, exec.submit((Runnable & Serializable) () -> n.set(1), n).get().get());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

  // Wrap-up task testing.
  @Test
  public void test29() throws InterruptedException, ExecutionException {
    JavaProcessPoolExecutor exec = new JavaProcessPoolExecutor(new JavaProcessManagerFactory<>(new SimpleJavaProcessConfig(),
        null, (Runnable & Serializable) () -> System.out.println("Wrapping up"), null),
        0, 1, 0);
    try {
      AtomicInteger n = new AtomicInteger(-1);
      Assert.assertEquals(4, exec.submit((Runnable & Serializable) () -> n.addAndGet(5), n).get().get());
    } finally {
      exec.shutdown();
      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
  }

}
