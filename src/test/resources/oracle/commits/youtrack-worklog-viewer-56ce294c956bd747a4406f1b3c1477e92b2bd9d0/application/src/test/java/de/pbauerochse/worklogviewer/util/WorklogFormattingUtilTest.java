package de.pbauerochse.worklogviewer.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Patrick Bauerochse
 * @since 14.04.15
 */
public class WorklogFormattingUtilTest {

    private Map<Long, String> TEST_DATA_MAP = new HashMap<>();

    @Before
    public void initialize() {
        TEST_DATA_MAP.put(1501L, "3d 1h 1m");
        TEST_DATA_MAP.put(15L, "15m");
        TEST_DATA_MAP.put(61L, "1h 1m");
        TEST_DATA_MAP.put(90L, "1h 30m");
        TEST_DATA_MAP.put(60L, "1h");
        TEST_DATA_MAP.put(961L, "2d 1m");
    }

    @Test
    public void testFormattingUtil() {
        TEST_DATA_MAP.forEach((timeInMinutes, expectedResult) -> {
            String formattedMinutes = FormattingUtil.formatMinutes(timeInMinutes);
            Assert.assertEquals(expectedResult, formattedMinutes);
        });
    }


}
