package org.cloudfoundry.multiapps.controller.core.util;

import java.util.List;

import org.cloudfoundry.multiapps.common.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

class UriUtilTest {

    private static final String HOST_BASED_URI_WITH_PORT = "https://valid-host.valid-domain:4000";
    private static final String HOST_BASED_URI_WITHOUT_PORT = "https://valid-host.valid-domain";
    private static final String PORT_BASED_URI = "https://valid-domain:4000";
    private static final String PORT_BASED_URI_WITHOUT_SCHEME = "valid-domain:4000";

    private final CloudRoute route = ImmutableCloudRoute.builder()
                                                        .host("valid-host")
                                                        .domain(ImmutableCloudDomain.builder()
                                                                                    .name("valid-domain")
                                                                                    .build())
                                                        .build();

    @Test
    void testFindRouteWithHostBasedUriWithPort() {
        List<CloudRoute> routes = List.of(route);
        Assertions.assertThrows(NotFoundException.class, () -> UriUtil.findRoute(routes, HOST_BASED_URI_WITH_PORT));
    }

    @Test
    void testFindRouteWithHostBasedUriWithoutPort() {
        List<CloudRoute> routes = List.of(route);
        CloudRoute actualResult = UriUtil.findRoute(routes, HOST_BASED_URI_WITHOUT_PORT);
        Assertions.assertEquals(route, actualResult);
    }

    @Test
    void testFindRouteWithPortBasedUri() {
        List<CloudRoute> routes = List.of(route);
        Assertions.assertThrows(NotFoundException.class, () -> UriUtil.findRoute(routes, PORT_BASED_URI));
    }

    @Test
    void testRouteMatchesWithHostBasedUriWithPort() {
        boolean actualResult = UriUtil.routeMatchesUri(route, HOST_BASED_URI_WITH_PORT);
        Assertions.assertFalse(actualResult);
    }

    @Test
    void testRouteMatchesWithHostBasedUriWithoutPort() {
        boolean actualResult = UriUtil.routeMatchesUri(route, HOST_BASED_URI_WITHOUT_PORT);
        Assertions.assertTrue(actualResult);
    }

    @Test
    void testRouteMatchesWithPortBasedUri() {
        boolean actualResult = UriUtil.routeMatchesUri(route, PORT_BASED_URI);
        Assertions.assertFalse(actualResult);
    }

    @Test
    void testStripSchemeWithScheme() {
        String actual = UriUtil.stripScheme(PORT_BASED_URI);
        Assertions.assertEquals(PORT_BASED_URI_WITHOUT_SCHEME, actual);
    }

    @Test
    void testStripSchemeWithoutScheme() {
        String actual = UriUtil.stripScheme(PORT_BASED_URI_WITHOUT_SCHEME);
        Assertions.assertEquals(PORT_BASED_URI_WITHOUT_SCHEME, actual);
    }

}
