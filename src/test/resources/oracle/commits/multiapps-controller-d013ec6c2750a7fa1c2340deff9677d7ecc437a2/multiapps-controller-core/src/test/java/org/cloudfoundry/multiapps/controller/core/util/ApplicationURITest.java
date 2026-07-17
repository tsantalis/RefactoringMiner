package org.cloudfoundry.multiapps.controller.core.util;

import static org.cloudfoundry.multiapps.controller.core.util.TestData.routeSummary;

import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;

class ApplicationURITest {

    private static final String CUSTOM = "custom-";

    static Stream<Arguments> testGetHostDomainPath() {
        return Stream.of(Arguments.of("https://valid-host.valid-domain", false, "valid-host", "valid-domain", ""),
                         Arguments.of("https://valid-domain", false, "", "valid-domain", ""),
                         Arguments.of("valid-domain", true, "", "valid-domain", ""),
                         Arguments.of("https://valid-domain/really/long/path", false, "", "valid-domain", "/really/long/path"),
                         Arguments.of("https://valid-host.valid-domain/really/long/path", false, "valid-host", "valid-domain",
                                      "/really/long/path"),
                         Arguments.of("deploy-service.cfapps.industrycloud-staging.siemens.com", false, "deploy-service",
                                      "cfapps.industrycloud-staging.siemens.com", ""),
                         Arguments.of("everything.is.in.domain/and/path", true, "", "everything.is.in.domain", "/and/path"));
    }

    @ParameterizedTest
    @MethodSource
    void testGetHostDomainPath(String uri, boolean noHostname, String expectedHost, String expectedDomain, String expectedPath) {
        ApplicationURI applicationURI = new ApplicationURI(uri, noHostname);
        Assertions.assertEquals(expectedHost, applicationURI.getHost());
        Assertions.assertEquals(expectedDomain, applicationURI.getDomain());
        Assertions.assertEquals(expectedPath, applicationURI.getPath());
    }

    @SuppressWarnings("serial")
    static Stream<Arguments> testGetURIParts() {
        return Stream.of(
//@formatter:off
                Arguments.of("host", "domain.com", "/path",
                             Map.of(SupportedParameters.HOST, "host", 
                                    SupportedParameters.DOMAIN, "domain.com",
                                    SupportedParameters.ROUTE_PATH, "/path")),
                Arguments.of("", "domain.com", "/path",
                             Map.of(SupportedParameters.DOMAIN, "domain.com", 
                                    SupportedParameters.ROUTE_PATH, "/path")),
                Arguments.of(null, "domain.com", "/path",
                             Map.of(SupportedParameters.DOMAIN, "domain.com", 
                                    SupportedParameters.ROUTE_PATH, "/path")),
                Arguments.of("", "domain.only.this.time", "", 
                             Map.of(SupportedParameters.DOMAIN, "domain.only.this.time")));
// @formatter:on
    }

    @ParameterizedTest
    @MethodSource
    void testGetURIParts(String host, String domain, String path, Map<String, Object> expectedParts) {
        ApplicationURI applicationURIFromSummary = new ApplicationURI(routeSummary(host, domain, path));

        Assertions.assertEquals(expectedParts, applicationURIFromSummary.getURIParts());
    }

    @Test
    void testGetHostDomainWithoutPathFromRoute() {
        CloudRouteSummary route = routeSummary(CUSTOM + "host", CUSTOM + "domain", "");
        ApplicationURI applicationURI = new ApplicationURI(route);
        Assertions.assertEquals(CUSTOM + "host", applicationURI.getHost());
        Assertions.assertEquals(CUSTOM + "domain", route.getDomain());
        Assertions.assertEquals("", applicationURI.getPath());
    }

    @Test
    void testGetHostDomainWithPathFromRoute() {
        CloudRouteSummary route = routeSummary(CUSTOM + "host", CUSTOM + "domain", "/" + CUSTOM + "path");
        ApplicationURI applicationURI = new ApplicationURI(route);
        Assertions.assertEquals(CUSTOM + "host", applicationURI.getHost());
        Assertions.assertEquals(CUSTOM + "domain", applicationURI.getDomain());
        Assertions.assertEquals("/" + CUSTOM + "path", applicationURI.getPath());
    }

    @Test
    void testGetURIPart() {
        CloudRouteSummary route = routeSummary(CUSTOM + "host", CUSTOM + "domain", "/" + CUSTOM + "path");
        ApplicationURI applicationURI = new ApplicationURI(route);
        Assertions.assertEquals(CUSTOM + "host", applicationURI.getURIPart(SupportedParameters.HOST));
        Assertions.assertEquals(CUSTOM + "domain", applicationURI.getURIPart(SupportedParameters.DOMAIN));
        Assertions.assertEquals("/" + CUSTOM + "path", applicationURI.getURIPart(SupportedParameters.ROUTE_PATH));
        Assertions.assertNull(applicationURI.getURIPart("invalid-parameter"));
    }

    @Test
    void testURIPart() {
        ApplicationURI applicationURI = new ApplicationURI(routeSummary(CUSTOM + "host", CUSTOM + "domain", CUSTOM + "path"));
        applicationURI.setURIPart(SupportedParameters.HOST, CUSTOM + "host-1");
        applicationURI.setURIPart(SupportedParameters.DOMAIN, CUSTOM + "domain-1");
        applicationURI.setURIPart(SupportedParameters.ROUTE_PATH, "/" + CUSTOM + "path-1");
        applicationURI.setURIPart("invalid-parameter", "value");
        Assertions.assertEquals(CUSTOM + "host-1", applicationURI.getURIPart(SupportedParameters.HOST));
        Assertions.assertEquals(CUSTOM + "domain-1", applicationURI.getURIPart(SupportedParameters.DOMAIN));
        Assertions.assertEquals("/" + CUSTOM + "path-1", applicationURI.getURIPart(SupportedParameters.ROUTE_PATH));
    }

    @Test
    void testToStringWithValidHostAndPath() {
        ApplicationURI applicationURI = new ApplicationURI(routeSummary(CUSTOM + "host", CUSTOM + "domain", "/" + CUSTOM + "path"));
        String expectedApplicationURI = CUSTOM + "host." + CUSTOM + "domain/" + CUSTOM + "path";
        Assertions.assertEquals(expectedApplicationURI, applicationURI.toString());
    }

    @Test
    void testToStringWithValidHostAndWithoutPath() {
        ApplicationURI applicationURI = new ApplicationURI(routeSummary(CUSTOM + "host", CUSTOM + "domain", null));
        Assertions.assertEquals(CUSTOM + "host." + CUSTOM + "domain", applicationURI.toString());
    }

    @Test
    void testToStringWithoutHostAndWithoutPath() {
        ApplicationURI applicationURI = new ApplicationURI(routeSummary("", CUSTOM + "domain", null));
        Assertions.assertEquals(CUSTOM + "domain", applicationURI.toString());
    }

    @Test
    void testGetDomainFromURI() {
        Assertions.assertEquals(CUSTOM + "domain",
                                ApplicationURI.getDomainFromURI("https://" + CUSTOM + "host." + CUSTOM + "domain/valid", false));
    }

}
