package org.jenkinsci.plugins.workflow.multibranch.template.finder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationValueFinderTest {

    public static final String EXPECTED_RESULT = "expectedResult";

    /**
     * Simple helper method to call ConfigurationValueFinder.findFirstConfigurationValue
     * to improve test readability.
     *
     * @param configurationContents test config
     * @param keyToFind test key
     * @return evaluated result
     */
    private String find(String configurationContents, String keyToFind) {
        return ConfigurationValueFinder.findFirstConfigurationValue(configurationContents, keyToFind);
    }

    private static Stream<Arguments> keyNotFoundProvider() {
        return Stream.of(
                Arguments.of("null key",        null,      "test"),
                Arguments.of("null config",     "someKey", null),
                Arguments.of("key not found",   "someKey", "keynotfound")
        );
    }

    @ParameterizedTest
    @MethodSource("keyNotFoundProvider")
    void cannotFindKey(String testCase, String keyToFind, String configurationContents) {
        assertNull(find(configurationContents, keyToFind), "Should not have found value for " + testCase);
    }


    private static Stream<Arguments> keysFoundProvider() {
        return Stream.of(
                Arguments.of("noNewLine",                   "noNewLine:" + EXPECTED_RESULT),
                Arguments.of("firstKey",                    "firstKey:" + EXPECTED_RESULT + "\n test1:second\n test1:third"),
                Arguments.of("lastKey",                     "bare:hi\n test1:second\n lastKey:" + EXPECTED_RESULT),
                Arguments.of("keyWithQuotesAround",         "\"keyWithQuotesAround\":\"" + EXPECTED_RESULT + "\"\n test1:second\n test1:third"),
                Arguments.of("keyWithTicksAround",          "'keyWithTicksAround':'" + EXPECTED_RESULT + "'\n test1:second\n test1:third"),
                Arguments.of("keyWithTicksSpaces",          "'keyWithTicksSpaces' :          '" + EXPECTED_RESULT + "'        \n test1:second\n test1:third"),
                Arguments.of("keyWithEqualsDelim",          "keyWithEqualsDelim=\"" + EXPECTED_RESULT + "\"\ntest1=second\ntest2=third"),
                Arguments.of("lastKeyWithEqualsDelim",      "equals=\"hi\"\ntest1=second\nlastKeyWithEqualsDelim=" + EXPECTED_RESULT),
                Arguments.of("keyWithSpaceAroundEquals",    "keyWithSpaceAroundEquals = \"" + EXPECTED_RESULT + "\"\ntest1 = second\ntest2 = third"),
                Arguments.of("lastKeyWithSpaceyEquals",     "equals_space = \"hi\"\ntest1 = second\nlastKeyWithSpaceyEquals = " + EXPECTED_RESULT)
                );
    }

    @ParameterizedTest
    @MethodSource("keysFoundProvider")
    void findKey(String keyToFind, String configurationContents) {
        assertEquals(EXPECTED_RESULT, find(configurationContents, keyToFind), "Did not find expected value for " + keyToFind);
    }
}