package de.pbauerochse.worklogviewer.logging;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LimitedLogMessageBufferTest {

    @Test
    public void test() {
        LimitedLogMessageBuffer messageBuilder = new LimitedLogMessageBuffer(10);
        for (int i = 0; i < 20; i++) {
            messageBuilder.onLogMessage(Collections.singletonList(String.valueOf(i)));
        }

        assertEquals("Expected the log messages to be limited", List.of("10", "11", "12", "13", "14", "15", "16", "17", "18", "19"), messageBuilder.getAllMessages());
    }

}
