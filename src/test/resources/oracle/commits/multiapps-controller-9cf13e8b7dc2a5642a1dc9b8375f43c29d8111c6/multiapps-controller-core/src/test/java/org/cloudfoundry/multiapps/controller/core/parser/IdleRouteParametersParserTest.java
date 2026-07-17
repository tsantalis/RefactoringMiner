package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IdleUriParametersParserTest {

    private static final String DEFAULT_HOST = "default-host";
    private static final String DEFAULT_DOMAIN = "default-domain";

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testParseIdleRoutes() {
        // @formatter:off
        return Stream.of(
            Arguments.of(List.of("foo.bar.com"), List.of("foo-idle.bar.com"), new Expectation(List.of("foo-idle.bar.com").toString())),
            Arguments.of(List.of("foo-quux.test.com/abc", "bar-quux.test.com/def"), List.of("idle-route.test.com/test"), new Expectation(List.of("idle-route.test.com/test").toString())),
            Arguments.of(List.of("foo-quux.test.com/abc", "bar-quux.test.com/def"), Collections.emptyList(), new Expectation(List.of("default-host.default-domain/abc",
                                                                                                                                                 "default-host.default-domain/def").toString())),
            Arguments.of(List.of("foo.bar.com"), null, new Expectation(List.of("default-host.default-domain").toString())),
            Arguments.of(null, List.of("https://bar-quux.test.com/def"), new Expectation(List.of("bar-quux.test.com/def").toString()))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testParseIdleRoutes(List<String> routes, List<String> idleRoutes, Expectation expectation) {
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put(SupportedParameters.ROUTES, constructRoutesParameter(routes, SupportedParameters.ROUTE));
        parametersMap.put(SupportedParameters.IDLE_ROUTES, constructRoutesParameter(idleRoutes, SupportedParameters.IDLE_ROUTE));

        tester.test(() -> new IdleUriParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(List.of(parametersMap)), expectation);
    }

    private List<Map<String, String>> constructRoutesParameter(List<String> routes, String mapKey) {
        return routes == null ? null
            : routes.stream()
                    .map(route -> Map.of(mapKey, route))
                    .collect(Collectors.toList());
    }

    @Test
    void testIgnoreIdleHostsDomains() {
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put(SupportedParameters.ROUTES,
                          constructRoutesParameter(List.of("foo-quux.test.com/abc", "bar-quux.test.com/def"), SupportedParameters.ROUTE));
        parametersMap.put(SupportedParameters.IDLE_HOSTS, List.of("idle-1", "idle-2"));
        parametersMap.put(SupportedParameters.IDLE_DOMAINS, List.of("domain-1", "domain-2"));

        tester.test(() -> new IdleUriParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(List.of(parametersMap)),
                    new Expectation(List.of("default-host.default-domain/abc", "default-host.default-domain/def")
                                        .toString()));
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

    @ParameterizedTest
    @MethodSource
    void testParseIdleHostsDomainsWithoutRoutes(List<String> hosts, List<String> domains, List<String> idleHosts, List<String> idleDomains,
                                                Expectation expectation) {
        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put(SupportedParameters.HOSTS, hosts);
        parametersMap.put(SupportedParameters.DOMAINS, domains);
        parametersMap.put(SupportedParameters.IDLE_HOSTS, idleHosts);
        parametersMap.put(SupportedParameters.IDLE_DOMAINS, idleDomains);

        tester.test(() -> new IdleUriParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(List.of(parametersMap)), expectation);
    }

}
