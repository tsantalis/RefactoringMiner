// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Tests the bug report class.
 * @author Michael Zangl
 */
// Preferences for the report text
@BasicPreferences
class BugReportTest {
    /**
     * Test {@link BugReport#getReportText}
     */
    @Test
    void testReportText() {
        ReportedException e = interceptInChildMethod(new IOException("test-exception-message"));
        e.put("test-key", "test-value");
        String text = new BugReport(e).getReportText(ShowStatusReportAction.getReportHeader());

        assertTrue(text.contains("test-exception-message"));
        assertTrue(text.contains("interceptInChildMethod"));
        assertTrue(text.contains("testReportText")); // stack trace
        assertTrue(text.contains("test-key: test-value"));
    }

    /**
     * Test {@link BugReport#intercept(Throwable)}
     */
    @Test
    void testIntercept() {
        IOException base = new IOException("test");
        ReportedException intercepted = interceptInChildMethod(base);
        assertEquals(intercepted.getCause(), base);

        StringWriter out = new StringWriter();
        intercepted.printReportDataTo(new PrintWriter(out));

        assertTrue(out.toString().contains("interceptInChildMethod")); // calling method.

        assertSame(intercepted, BugReport.intercept(intercepted));
    }

    private ReportedException interceptInChildMethod(IOException base) {
        return BugReport.intercept(base);
    }

    /**
     * Test {@link BugReport#getCallingMethod(int)}
     */
    @Test
    void testGetCallingMethod() {
        assertEquals("BugReportTest#testGetCallingMethod", BugReport.getCallingMethod(1));
        assertEquals("BugReportTest#testGetCallingMethod", testGetCallingMethod2());
        assertEquals("?", BugReport.getCallingMethod(100));
    }

    private String testGetCallingMethod2() {
        return BugReport.getCallingMethod(2);
    }
}
