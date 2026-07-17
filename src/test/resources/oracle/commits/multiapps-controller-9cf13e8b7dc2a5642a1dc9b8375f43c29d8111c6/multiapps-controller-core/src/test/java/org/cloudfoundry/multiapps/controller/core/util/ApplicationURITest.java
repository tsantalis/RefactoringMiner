package org.cloudfoundry.multiapps.controller.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

class ApplicationURITest {

    private static final String CUSTOM = "custom-";

    static Stream<Arguments> testParameters() {
        return Stream.of(Arguments.of("https://valid-host.valid-domain", "valid-host", "valid-domain", ""),
                         Arguments.of("https://valid-domain", "", "valid-domain", ""), Arguments.of("valid-domain", "", "valid-domain", ""),
                         Arguments.of("https://valid-domain/really/long/path", "", "valid-domain", "/really/long/path"),
                         Arguments.of("https://valid-host.valid-domain/really/long/path", "valid-host", "valid-domain",
                                      "/really/long/path"),
                         Arguments.of("deploy-service.cfapps.industrycloud-staging.siemens.com", "deploy-service",
                                      "cfapps.industrycloud-staging.siemens.com", ""));
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    void testGetHostDomainPath(String uri, String expectedHost, String expectedDomain, String expectedPath) {
        ApplicationURI applicationURI = new ApplicationURI(uri);
        Assertions.assertEquals(expectedHost, applicationURI.getHost());
        Assertions.assertEquals(expectedDomain, applicationURI.getDomain());
        Assertions.assertEquals(expectedPath, applicationURI.getPath());
    }

    @Test
    void testGetHostDomainWithoutPathFromRoute() {
        CloudRoute route = createCloudRoute(CUSTOM + "host", createCloudDomain(CUSTOM + "domain"), null);
        ApplicationURI applicationURI = new ApplicationURI(route);
        Assertions.assertEquals(CUSTOM + "host", applicationURI.getHost());
        Assertions.assertEquals(CUSTOM + "domain", route.getDomain()
                                                        .getName());
        Assertions.assertEquals("", applicationURI.getPath());
    }

    @Test
    void testGetHostDomainWithPathFromRoute() {
        CloudRoute route = createCloudRoute(CUSTOM + "host", createCloudDomain(CUSTOM + "domain"), "/" + CUSTOM + "path");
        ApplicationURI applicationURI = new ApplicationURI(route);
        Assertions.assertEquals(CUSTOM + "host", applicationURI.getHost());
        Assertions.assertEquals(CUSTOM + "domain", route.getDomain()
                                                        .getName());
        Assertions.assertEquals("/" + CUSTOM + "path", applicationURI.getPath());
    }

    @Test
    void testGetURIParts() {
        ApplicationURI applicationURI = new ApplicationURI(createCloudRoute(CUSTOM + "host", createCloudDomain(CUSTOM + "domain"), null));
        Map<String, Object> expectedParts = new HashMap<>();
        expectedParts.put(SupportedParameters.HOST, CUSTOM + "host");
        expectedParts.put(SupportedParameters.DOMAIN, CUSTOM + "domain");
        expectedParts.put(SupportedParameters.ROUTE_PATH, "");
        Assertions.assertEquals(expectedParts, applicationURI.getURIParts());
    }

    @Test
    void testGetURIPart() {
        ApplicationURI applicationURI = new ApplicationURI(createCloudRoute(CUSTOM + "host", createCloudDomain(CUSTOM + "domain"),
                                                                            "/" + CUSTOM + "path"));
        Assertions.assertEquals(CUSTOM + "host", applicationURI.getURIPart(SupportedParameters.HOST));
        Assertions.assertEquals(CUSTOM + "domain", applicationURI.getURIPart(SupportedParameters.DOMAIN));
        Assertions.assertEquals("/" + CUSTOM + "path", applicationURI.getURIPart(SupportedParameters.ROUTE_PATH));
        Assertions.assertNull(applicationURI.getURIPart("invalid-parameter"));
    }

    @Test
    void testURIPart() {
        ApplicationURI applicationURI = new ApplicationURI(createCloudRoute(CUSTOM + "host", createCloudDomain(CUSTOM + "domain"),
                                                                            CUSTOM + "path"));
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
        ApplicationURI applicationURI = new ApplicationURI(createCloudRoute(CUSTOM + "host", createCloudDomain(CUSTOM + "domain"),
                                                                            "/" + CUSTOM + "path"));
        String expectedApplicationURI = CUSTOM + "host." + CUSTOM + "domain/" + CUSTOM + "path";
        Assertions.assertEquals(expectedApplicationURI, applicationURI.toString());
    }

    @Test
    void testToStringWithValidHostAndWithoutPath() {
        ApplicationURI applicationURI = new ApplicationURI(createCloudRoute(CUSTOM + "host", createCloudDomain(CUSTOM + "domain"), null));
        Assertions.assertEquals(CUSTOM + "host." + CUSTOM + "domain", applicationURI.toString());
    }

    @Test
    void testToStringWithoutHostAndWithoutPath() {
        ApplicationURI applicationURI = new ApplicationURI(createCloudRoute("", createCloudDomain(CUSTOM + "domain"), null));
        Assertions.assertEquals(CUSTOM + "domain", applicationURI.toString());
    }

    @Test
    void testGetDomainFromURI() {
        Assertions.assertEquals(CUSTOM + "domain",
                                ApplicationURI.getDomainFromURI("https://" + CUSTOM + "host." + CUSTOM + "domain/valid"));
    }

    private static CloudRoute createCloudRoute(String host, CloudDomain domain, String path) {
        return ImmutableCloudRoute.builder()
                                  .host(host)
                                  .domain(domain)
                                  .path(path)
                                  .build();
    }

    private static CloudDomain createCloudDomain(String name) {
        return ImmutableCloudDomain.builder()
                                   .name(name)
                                   .build();
    }

}
