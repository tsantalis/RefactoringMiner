// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link LoadAndZoomHandler} class.
 */
class LoadAndZoomHandlerTest {
    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static LoadAndZoomHandler newHandler(String url) throws RequestHandlerBadRequestException {
        LoadAndZoomHandler req = new LoadAndZoomHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no param.
     * @throws Exception if any error occurs
     */
    @Test
    void testBadRequestNoParam() throws Exception {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler(null).handle());
        assertEquals("NumberFormatException (empty String)", e.getMessage());
    }

    /**
     * Unit test for bad request - invalid URL.
     * @throws Exception if any error occurs
     */
    @Test
    void testBadRequestInvalidUrl() throws Exception {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("invalid_url").handle());
        assertEquals("The following keys are mandatory, but have not been provided: bottom, top, left, right", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     * @throws Exception if any error occurs
     */
    @Test
    void testBadRequestIncompleteUrl() throws Exception {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost").handle());
        assertEquals("The following keys are mandatory, but have not been provided: bottom, top, left, right", e.getMessage());
    }

    /**
     * Unit test for nominal request - local data file.
     * @throws Exception if any error occurs
     */
    @Test
    void testNominalRequest() throws Exception {
        assertDoesNotThrow(() -> newHandler("https://localhost?bottom=0&top=0&left=1&right=1").handle());
    }
}
