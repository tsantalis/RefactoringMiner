// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link BugReportExceptionHandler} class.
 */
class BugReportExceptionHandlerTest {
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
