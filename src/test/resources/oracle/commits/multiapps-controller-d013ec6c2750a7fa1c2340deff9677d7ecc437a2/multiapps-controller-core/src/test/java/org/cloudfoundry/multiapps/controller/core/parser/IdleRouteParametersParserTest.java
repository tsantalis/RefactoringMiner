package org.cloudfoundry.multiapps.controller.core.parser;

import static org.cloudfoundry.multiapps.controller.core.util.TestData.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IdleRouteParametersParserTest {

    private static final String DEFAULT_HOST = "default-host";
    private static final String DEFAULT_DOMAIN = "default-domain";

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testParseIdleRoutes() {
        return Stream.of(
// @formatter:off
            Arguments.of(List.of(routeParameter("foo.bar.com")),
                         List.of(idleRouteParameter("foo-idle.bar.com")), 
                         new Expectation(Expectation.Type.SET, Set.of(routeSummary("foo-idle", "bar.com", "")))),            
            Arguments.of(List.of(routeParameter("foo-quux.test.com/abc"), routeParameter("bar-quux.test.com/def")),
                         List.of(idleRouteParameter("idle-route.test.com/test")), 
                         new Expectation(Expectation.Type.SET, Set.of(routeSummary("idle-route", "test.com", "/test")))),
            Arguments.of(List.of(routeParameter("foo-quux.test.com/abc"), routeParameter("bar-quux.test.com/def")),
                         Collections.emptyList(), 
                         new Expectation(Expectation.Type.SET, Set.of(routeSummary(DEFAULT_HOST, DEFAULT_DOMAIN, "/abc"),
                                                                      routeSummary(DEFAULT_HOST, DEFAULT_DOMAIN, "/def")))),
            Arguments.of(List.of(routeParameter("foo.bar.com")),
                         null,
                         new Expectation(Expectation.Type.SET, Set.of(routeSummary(DEFAULT_HOST, DEFAULT_DOMAIN, "")))),
            Arguments.of(null,
                         List.of(idleRouteParameter("https://bar-quux.test.com/def")), 
                         new Expectation(Expectation.Type.SET, Set.of(routeSummary("bar-quux", "test.com", "/def")))),
            // even if normal routes are without host - idle route will have default-host
            Arguments.of(List.of(routeParameter("https://foo-quux.test.com/abc", true), routeParameter("https://bar-quux.test.com/def")),
                         Collections.emptyList(), 
                         new Expectation(Expectation.Type.SET, Set.of(routeSummary(DEFAULT_HOST, DEFAULT_DOMAIN, "/abc"),
                                                                      routeSummary(DEFAULT_HOST, DEFAULT_DOMAIN, "/def")))),
            // if idle routes are without host - processed idle route will remain without host
            Arguments.of(List.of(routeParameter("foo.bar.com", false)),
                         List.of(idleRouteParameter("foo-idle.bar.com/abc", true)),
                         new Expectation(Expectation.Type.SET, Set.of(routeSummary("", "foo-idle.bar.com", "/abc"))))
// @formatter:on
        );
    }

    @MethodSource
    @ParameterizedTest
    void testParseIdleRoutes(List<Map<String, Object>> routes, List<Map<String, Object>> idleRoutes, Expectation expectation) {
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put(SupportedParameters.ROUTES, routes);
        parametersMap.put(SupportedParameters.IDLE_ROUTES, idleRoutes);
        
        tester.test(() -> new IdleRouteParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(List.of(parametersMap)), expectation);
    }

    static Stream<Arguments> testParseIdleHostsDomainsWithoutRoutes() {
// @formatter:off
        return Stream.of(
            Arguments.of(List.of("test-host-1", "test-host-2"), List.of("test-domain.com"),  List.of("idle-host"), List.of("idle-domain.com"),
                         new Expectation(List.of("idle-host.idle-domain.com").toString())),
            Arguments.of(List.of("test-host-1"), List.of("test-domain.com"), List.of("idle-host", "idle-host-2"), List.of("idle-domain.com", "idle-domain.net"),
                         new Expectation(List.of("idle-host.idle-domain.com", "idle-host-2.idle-domain.com", "idle-host.idle-domain.net", "idle-host-2.idle-domain.net").toString()))
// @formatter:on
        );
    }

    @MethodSource
    @ParameterizedTest
    void testParseIdleHostsDomainsWithoutRoutes(List<String> hosts, List<String> domains, List<String> idleHosts, List<String> idleDomains,
                                                Expectation expectation) {
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put(SupportedParameters.HOSTS, hosts);
        parametersMap.put(SupportedParameters.DOMAINS, domains);
        parametersMap.put(SupportedParameters.IDLE_HOSTS, idleHosts);
        parametersMap.put(SupportedParameters.IDLE_DOMAINS, idleDomains);

        tester.test(() -> new IdleRouteParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(List.of(parametersMap)), expectation);
    }

    @Test
    void testIgnoreIdleHostsDomains() {
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put(SupportedParameters.ROUTES,
                          List.of(routeParameter("foo-quux.test.com/abc"), routeParameter("bar-quux.test.com/def")));
        parametersMap.put(SupportedParameters.IDLE_HOSTS, List.of("idle-1", "idle-2"));
        parametersMap.put(SupportedParameters.IDLE_DOMAINS, List.of("domain-1", "domain-2"));

        tester.test(() -> new IdleRouteParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(List.of(parametersMap)),
                    new Expectation(Expectation.Type.SET,
                                    Set.of(routeSummary(DEFAULT_HOST, DEFAULT_DOMAIN, "/abc"),
                                           routeSummary(DEFAULT_HOST, DEFAULT_DOMAIN, "/def"))));
    }
}
