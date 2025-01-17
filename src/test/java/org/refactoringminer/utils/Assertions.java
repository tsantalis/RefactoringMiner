package org.refactoringminer.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Assertions {
    public static void assertHasSameElementsAs(List<String> expected, List<String> actual, Supplier<String> lazyErrorMessage) {
        var expectedSet = new LinkedHashSet<>(expected);
        var actualSet = new LinkedHashSet<>(actual);
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals(expected.size(), actual.size(), () ->
                        "Expected size (" + expected.size() + ") != actual size (" + actual.size() + "):"
                                + System.lineSeparator() + lazyErrorMessage.get() + System.lineSeparator()),
                () -> org.junit.jupiter.api.Assertions.assertTrue(actualSet.containsAll(expectedSet), () -> System.lineSeparator() + expectedSet.stream()
                        .filter((String s) -> !actualSet.contains(s))
                        .map((String s) -> "+" + s)
                        .map((String s) -> s + System.lineSeparator())
                        .collect(Collectors.joining())),
                () -> org.junit.jupiter.api.Assertions.assertTrue(expectedSet.containsAll(actualSet), () -> System.lineSeparator() + actualSet.stream()
                        .filter((String s) -> !expectedSet.contains(s))
                        .map((String s) -> "-" + s)
                        .map((String s) -> s + System.lineSeparator())
                        .collect(Collectors.joining())));
    }
    public static void assertHasSameElementsAs(List<String> expected, List<String> actual) {
        assertHasSameElementsAs(expected, actual, () -> "");
    }
}
