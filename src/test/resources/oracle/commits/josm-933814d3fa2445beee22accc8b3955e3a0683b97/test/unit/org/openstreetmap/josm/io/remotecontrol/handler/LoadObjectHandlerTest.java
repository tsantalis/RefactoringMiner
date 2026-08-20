// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.github.tomakehurst.wiremock.WireMockServer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link LoadObjectHandler} class.
 */
class LoadObjectHandlerTest {
    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static LoadObjectHandler newHandler(String url) throws RequestHandlerBadRequestException {
        LoadObjectHandler req = new LoadObjectHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no param.
     */
    @Test
    void testBadRequestNoParam() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler(null).handle());
        assertEquals("No valid object identifier has been provided", e.getMessage());
    }

    /**
     * Unit test for bad request - invalid URL.
     */
    @Test
    void testBadRequestInvalidUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("invalid_url").handle());
        assertEquals("The following keys are mandatory, but have not been provided: objects", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     */
    @Test
    void testBadRequestIncompleteUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost").handle());
        assertEquals("The following keys are mandatory, but have not been provided: objects", e.getMessage());
    }

    /**
     * Unit test for nominal request - local data file.
     */
    @Test
    void testNominalRequest() {
        WireMockServer wiremock = TestUtils.getWireMockServer();
        wiremock.addStubMapping(get(urlEqualTo("/capabilities")).willReturn(aResponse().withStatusMessage("OK")
                .withBodyFile("api/capabilities")).build());
        String w1 = "<way id=\"1\" version=\"1\"><nd ref=\"1\"/><nd ref=\"2\"/></way>";
        String n1 = "<node id=\"1\" version=\"1\"/>";
        String n2 = "<node id=\"2\" version=\"1\"/>";
        wiremock.addStubMapping(get(urlEqualTo("/0.6/ways?ways=1")).willReturn(aResponse().withStatusMessage("OK")
                .withBody(osm(w1))).build());
        wiremock.addStubMapping(get(urlEqualTo("/0.6/nodes?nodes=1,2")).willReturn(aResponse().withStatusMessage("OK")
                .withBody(osm(n1 + n2))).build());
        wiremock.addStubMapping(get(urlEqualTo("/0.6/way/1/full")).willReturn(aResponse().withStatusMessage("OK")
                .withBody(osm(n1 + n2 + w1))).build());
        wiremock.start();
        Config.getPref().put("osm-server.url", wiremock.baseUrl());
        try {
            assertDoesNotThrow(() -> newHandler("https://localhost?objects=n1,w1").handle());
        } finally {
            wiremock.stop();
        }
    }

    private static String osm(String xml) {
        return "<osm version=\"0.6\">" + xml + "</osm>";
    }
}
