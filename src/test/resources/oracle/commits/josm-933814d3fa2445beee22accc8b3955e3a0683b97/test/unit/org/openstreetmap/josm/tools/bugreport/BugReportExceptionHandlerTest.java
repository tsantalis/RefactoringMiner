// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link BugReportExceptionHandler} class.
 */
class BugReportExceptionHandlerTest {
    /**
     * No dependencies
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test for {@link BugReportExceptionHandler#handleException} method.
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    @Test
    void testHandleException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        BugReportQueue.getInstance().addBugReportHandler(e -> {
            latch.countDown(); return false;
        });
        BugReportExceptionHandler.handleException(new Exception("testHandleException"));
        latch.await();
    }
}
