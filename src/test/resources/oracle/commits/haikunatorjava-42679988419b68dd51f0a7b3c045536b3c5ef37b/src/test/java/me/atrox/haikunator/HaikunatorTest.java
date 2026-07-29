package me.atrox.haikunator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for haikunator
 */
class HaikunatorTest {

    /**
     * Arguments for general functionality test
     */
    static Stream<Arguments> testGeneralFunctionality() {
        return Stream.of(
                Arguments.of(new Haikunator(), "[a-z]+-[a-z]+-[0-9]{4}$"),
                Arguments.of(new Haikunator().setTokenHex(true), "[a-z]+-[a-z]+-[0-f]{4}$"),
                Arguments.of(new Haikunator().setTokenLength(9), "[a-z]+-[a-z]+-[0-9]{9}$"),
                Arguments.of(new Haikunator().setTokenLength(9).setTokenHex(true), "[a-z]+-[a-z]+-[0-f]{9}$"),
                Arguments.of(new Haikunator().setTokenLength(0), "[a-z]+-[a-z]+$"),
                Arguments.of(new Haikunator().setDelimiter("."), "[a-z]+.[a-z]+.[0-9]{4}$"),
                Arguments.of(new Haikunator().setDelimiter(" ").setTokenLength(0), "[a-z]+ [a-z]+$"),
                Arguments.of(new Haikunator().setDelimiter("").setTokenLength(0), "[a-z]+$"),
                Arguments.of(new Haikunator().setTokenChars("xyz"), "[a-z]+-[a-z]+-[x-z]{4}$"),
                Arguments.of(new Haikunator().setAdjectives(new String[]{"adjective"}).setNouns(new String[]{"noun"}), "adjective-noun-\\d{4}$")
        );
    }

    @ParameterizedTest(name = "testGeneralFunctionality #{index}")
    @MethodSource
    void testGeneralFunctionality(Haikunator haikunator, String regex) {
        String haiku = haikunator.haikunate();
        assertTrue(haiku.matches(regex));
    }

    @Test
    void testWontReturnSameForSubsequentCalls() {
        List<Haikunator> haikunators = Arrays.asList(new Haikunator(), new Haikunator());

        for (Haikunator h1 : haikunators) {
            for (Haikunator h2 : haikunators) {
                assertNotEquals(h1.haikunate(), h2.haikunate());
            }
        }
    }

    @Test
    void testReturnsSameForSameSeed() {
        Long seed = 1001L;

        Haikunator h1 = new Haikunator().setRandom(new Random(seed));
        Haikunator h2 = new Haikunator().setRandom(new Random(seed));

        assertEquals(h1.haikunate(), h2.haikunate());
        assertEquals(h1.haikunate(), h2.haikunate());
    }

    @Test
    void testZeroLengthOptionsException() {
        Haikunator haikunator = new Haikunator().setAdjectives(null).setNouns(null).setTokenChars(null);
        assertEquals(haikunator.haikunate(), "");
    }
}
