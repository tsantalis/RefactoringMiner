package me.atrox.haikunator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Random;

/**
 * Unit test for simple App.
 */
public class HaikunatorTest extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HaikunatorTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(HaikunatorTest.class);
    }

    public void testDefaultUse() {
        Haikunator haikunator = new HaikunatorBuilder().build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))(-)((?:[a-z][a-z]+))(-)(\\d{4})$"));
    }

    public void testHexUse() {
        Haikunator haikunator = new HaikunatorBuilder().setTokenHex(true).build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))(-)((?:[a-z][a-z]+))(-)(.{4})$"));
    }

    public void test9DigitsUse() {
        Haikunator haikunator = new HaikunatorBuilder().setTokenLength(9).build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))(-)((?:[a-z][a-z]+))(-)(\\d{9})$"));
    }

    public void test9DigitsAsHexUse() {
        Haikunator haikunator = new HaikunatorBuilder().setTokenHex(true).setTokenLength(9).build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))(-)((?:[a-z][a-z]+))(-)(.{9})$"));
    }

    public void testWontReturnSameForSubsequentCalls() {
        Haikunator haikunator = new HaikunatorBuilder().build();
        String haiku = haikunator.haikunate();
        String haiku2 = haikunator.haikunate();

        assertNotSame(haiku, haiku2);
    }

    public void testDropsToken() {
        Haikunator haikunator = new HaikunatorBuilder().setTokenLength(0).build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))(-)((?:[a-z][a-z]+))$"));
    }

    public void testPermitsOptionalDelimiter() {
        Haikunator haikunator = new HaikunatorBuilder().setDelimiter(".").build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))(\\.)((?:[a-z][a-z]+))(\\.)(\\d+)$"));
    }

    public void testSpaceDelimiterAndNoToken() {
        Haikunator haikunator = new HaikunatorBuilder().setDelimiter(" ").setTokenLength(0).build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))( )((?:[a-z][a-z]+))$"));
    }

    public void testOneSingleWord() {
        Haikunator haikunator = new HaikunatorBuilder().setDelimiter("").setTokenLength(0).build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))$"));
    }

    public void testCustomChars() {
        Haikunator haikunator = new HaikunatorBuilder().setTokenChars("A").build();
        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("((?:[a-z][a-z]+))(-)((?:[a-z][a-z]+))(-)(AAAA)$"));
    }

    public void testCustomAdjectivesAndNouns() {
        Haikunator haikunator = new HaikunatorBuilder().build();
        haikunator.setAdjectives(new String[]{"red"});
        haikunator.setNouns(new String[]{"reindeer"});

        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("(red)(-)(reindeer)(-)(\\d{4})$"));
    }

    public void testRemoveAdjectivesAndNouns() {
        Haikunator haikunator = new HaikunatorBuilder().build();
        haikunator.setAdjectives(new String[]{});
        haikunator.setNouns(new String[]{});

        String haiku = haikunator.haikunate();

        assertTrue(haiku.matches("(\\d{4})$"));
    }

    public void testCustomRandomWithSeed() {
        Haikunator haikunator = new HaikunatorBuilder().build();
        haikunator.setRandom(new Random(1234));
        String haiku1 = haikunator.haikunate();

        Haikunator haikunator2 = new HaikunatorBuilder().build();
        haikunator2.setRandom(new Random(1234));
        String haiku2 = haikunator2.haikunate();

        assertEquals(haiku1, haiku2);
    }
}
