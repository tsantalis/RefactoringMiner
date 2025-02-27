package de.pbauerochse.worklogviewer.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorklogFormattingUtilTest {

    @ParameterizedTest
    @CsvSource(value = {
            "1501;3d 1h 1m", "15;15m", "61;1h 1m", "90;1h 30m", "60;1h", "961;2d 1m",
    }, delimiter = ';')
    void testFormattingShort(long timeInMinutes, String expectedResult) {
        String formattedMinutes = FormattingUtil.formatMinutes(timeInMinutes);
        assertEquals(expectedResult, formattedMinutes);
    }


    @ParameterizedTest
    @CsvSource(value = {
            "1501;3d 1h 1m", "15;15m", "61;1h 1m", "90;1h 30m", "60;1h 0m", "961;2d 0h 1m",
    }, delimiter = ';')
    void testFormattingLong(long timeInMinutes, String expectedResult) {
        String formattedMinutes = FormattingUtil.formatMinutes(timeInMinutes, true);
        assertEquals(expectedResult, formattedMinutes);
    }

}
