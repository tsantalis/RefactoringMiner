package org.cloudfoundry.multiapps.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Objects;

import org.apache.commons.lang3.Functions.FailableCallable;
import org.apache.commons.lang3.Functions.FailableRunnable;

public class Tester {

    private final Class<?> testedClass;

    protected Tester(Class<?> testedClass) {
        this.testedClass = testedClass;
    }

    public <E extends Exception> void test(FailableRunnable<E> runnable, Expectation expectation) {
        test(toCallable(runnable), expectation);
    }

    public <E extends Exception> void test(FailableCallable<?, E> callable, Expectation expectation) {
        if (expectation.shouldSkipTest()) {
            return;
        }
        try {
            Object result = callable.call();
            validateSuccess(expectation, result);
        } catch (Exception e) {
            validateFailure(expectation, e);
        }
    }

    private void validateSuccess(Expectation expectation, Object result) {
        assertTrue(expectation.expectsSuccess(), "Expected an exception, but the test finished successfully!");

        if (expectation.type == Expectation.Type.STRING) {
            String expected = expectation.expectation;
            String actual = Objects.toString(result, null);
            assertEquals(expected, actual);
            return;
        }
        Object expected = loadResourceAsJsonObject(expectation.expectation);
        Object actual = toJsonObject(result);
        // If they're not equal, we want to compare them as JSON strings, because then the differences would be easier to see. That's
        // why we're not using assertEquals here.
        if (actual.equals(expected)) {
            return;
        }
        expected = loadResourceAsString(expectation.expectation);
        actual = toJson(result);
        assertEquals(expected, actual);
    }

    private void validateFailure(Expectation expectation, Exception e) {
        if (!expectation.expectsFailure()) {
            e.printStackTrace();
            fail("Test failed: " + e.toString());
        }
        String exceptionMessage = e.getMessage();
        assertTrue(exceptionMessage.contains(expectation.expectation), "Exception's message doesn't match up!");
    }

    private Object loadResourceAsJsonObject(String resource) {
        String json = loadResourceAsString(resource);
        return JsonUtil.fromJson(json, Object.class);
    }

    private String loadResourceAsString(String resource) {
        return TestUtil.getResourceAsString(resource, testedClass);
    }

    private Object toJsonObject(Object object) {
        String json = toJson(object);
        return JsonUtil.fromJson(json, Object.class);
    }

    private String toJson(Object object) {
        String json = JsonUtil.toJson(object, true);
        return TestUtil.removeCarriageReturns(json);
    }

    private static <E extends Exception> FailableCallable<Void, E> toCallable(FailableRunnable<E> runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    public static Tester forClass(Class<?> testedClass) {
        return new Tester(testedClass);
    }

    public static class Expectation {

        public enum Type {
            STRING, JSON, EXCEPTION, SKIP
        }

        private final Type type;
        private final String expectation;

        public Expectation(String expectation) {
            this(Type.STRING, expectation);
        }

        public Expectation(Type type, String expectation) {
            this.type = type;
            this.expectation = expectation;
        }

        private boolean expectsSuccess() {
            return type == Type.STRING || type == Type.JSON;
        }

        private boolean expectsFailure() {
            return type == Type.EXCEPTION;
        }

        private boolean shouldSkipTest() {
            return type == Type.SKIP;
        }

        public String toString() {
            return "Expecting test to " + (expectsSuccess() ? "SUCCEED and result in " : "FAIL with ") + expectation;
        }

    }

}
