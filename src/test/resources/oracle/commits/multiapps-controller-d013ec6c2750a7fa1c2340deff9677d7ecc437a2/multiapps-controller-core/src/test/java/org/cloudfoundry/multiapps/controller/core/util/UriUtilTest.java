package org.cloudfoundry.multiapps.controller.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

class UriUtilTest {

    private final CloudRoute ROUTE_TO_MATCH = ImmutableCloudRoute.builder()
                                                                 .host("valid-host")
                                                                 .domain(ImmutableCloudDomain.builder()
                                                                                             .name("valid-domain")
                                                                                             .build())
                                                                 .path("/valid/path")
                                                                 .build();
    private final List<CloudRoute> ROUTES_LIST = Arrays.asList(ROUTE_TO_MATCH);

    static Stream<Arguments> testMatchRoute() {
        return Stream.of(Arguments.of("https://valid-host.valid-domain", false, false),
                         Arguments.of("https://valid-host.valid-domain/valid/path", false, true),
                         Arguments.of("https://valid-host.valid-domain/valid/path", true, false),
                         Arguments.of("https://valid-domain/valid/path", true, false));
    }

    @MethodSource
    @ParameterizedTest
    void testMatchRoute(String uri, boolean noHostname, boolean shouldMatch) {
        CloudRouteSummary routeSummary = new ApplicationURI(uri, noHostname).toCloudRouteSummary();

        if (shouldMatch) {
            CloudRoute matchedRoute = UriUtil.matchRoute(ROUTES_LIST, routeSummary);
            Assertions.assertEquals(ROUTE_TO_MATCH, matchedRoute);
        } else {
            Assertions.assertThrows(NotFoundException.class, () -> UriUtil.matchRoute(ROUTES_LIST, routeSummary));
        }
    }

    static Stream<Arguments> testStripScheme() {
        return Stream.of(Arguments.of("https://host.domain/with/path", "host.domain/with/path"),
                         Arguments.of("host.domain/with/path", "host.domain/with/path"),
                         Arguments.of("https://valid-domain:4000", "valid-domain:4000"),
                         Arguments.of("valid-domain:4000", "valid-domain:4000"));
    }

    @MethodSource
    @ParameterizedTest
    void testStripScheme(String uri, String strippedUri) {
        Assertions.assertEquals(strippedUri, UriUtil.stripScheme(uri));
    }
}
