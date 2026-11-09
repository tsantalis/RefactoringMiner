package org.refactoringminer.astDiff.graph;

import java.util.*;
import javax.annotation.Nullable;

/**
 * Knowledge of the platforms of the languages RM parses, which RM's model does not give: the library types and methods
 * whose calls change a value in place, hand a value to the function they take, write to a log or draw at random, and
 * the operators RM keeps as text only. The lists name Java's platform (the JDK, its logging and randomized testing
 * libraries); each is extended for a language once that language is supported.
 */
final class PlatformKnowledge {
  private PlatformKnowledge() {
  }

  // The types of java.util.concurrent.atomic, whose value a call of one of the methods below changes in place
  static final Set<String> ATOMIC_TYPES = Set.of("AtomicInteger", "AtomicLong", "AtomicBoolean", "AtomicReference",
          "AtomicIntegerArray", "AtomicLongArray", "AtomicReferenceArray", "LongAdder", "DoubleAdder", "LongAccumulator",
          "DoubleAccumulator");

  static final Set<String> ATOMIC_UPDATES = Set.of("set", "lazySet", "setPlain", "setOpaque", "setRelease",
          "getAndSet", "compareAndSet", "weakCompareAndSet", "weakCompareAndSetPlain", "weakCompareAndSetVolatile",
          "weakCompareAndSetAcquire", "weakCompareAndSetRelease", "compareAndExchange", "compareAndExchangeAcquire",
          "compareAndExchangeRelease", "getAndIncrement", "getAndDecrement", "getAndAdd", "incrementAndGet",
          "decrementAndGet", "addAndGet", "getAndUpdate", "updateAndGet", "getAndAccumulate", "accumulateAndGet", "add",
          "increment", "decrement", "reset", "accumulate");

  // The bitwise and of the languages RM parses, as RM records the operator of an infix expression
  static final String BITWISE_AND = "&";

  // The packages of the logging libraries (slf4j, log4j, logback, flogger, commons/java.util/jboss logging), and a
  // logging package of a repository, as netty's io.netty.util.internal.logging, by a segment of the qualified name
  static final Set<String> LOGGING_PACKAGES = Set.of("logging", "slf4j", "log4j", "logback", "flogger");

  // The methods of a CompletionStage that hand the value the stage completes with to the first parameter of the function
  // they take (stage 5 of the TODO on data and control flow, futures): the function runs once the value is there, as a
  // continuation of the code keeping the stage. exceptionally hands it the exception instead, thenRun nothing
  static final Set<String> STAGE_VALUE_METHODS = Set.of("thenApply", "thenApplyAsync", "thenAccept", "thenAcceptAsync",
          "thenCompose", "thenComposeAsync", "whenComplete", "whenCompleteAsync", "handle", "handleAsync", "thenCombine",
          "thenCombineAsync");

  // The types of the locals holding such a stage
  static final Set<String> STAGE_TYPES = Set.of("CompletableFuture", "CompletionStage");

  // The classes of the randomized testing frameworks whose random() gives the random source of a test, and the packages
  // whose static methods draw from it (RandomNumbers, RandomPicks, RandomStrings), with the random sources of the JDK
  static final Set<String> RANDOM_TEST_BASES = Set.of("com.carrotsearch.randomizedtesting.RandomizedTest",
          "org.apache.lucene.tests.util.LuceneTestCase", "org.apache.lucene.util.LuceneTestCase");

  private static final Set<String> RANDOM_DRAWING_TYPES = Set.of("java.util.concurrent.ThreadLocalRandom",
          "com.carrotsearch.randomizedtesting.RandomizedTest");

  private static final String RANDOM_GENERATORS = "com.carrotsearch.randomizedtesting.generators.";

  static boolean isRandomDrawingType(@Nullable String qualified) {
    return qualified != null && (qualified.startsWith(RANDOM_GENERATORS) || RANDOM_DRAWING_TYPES.contains(qualified));
  }
}
