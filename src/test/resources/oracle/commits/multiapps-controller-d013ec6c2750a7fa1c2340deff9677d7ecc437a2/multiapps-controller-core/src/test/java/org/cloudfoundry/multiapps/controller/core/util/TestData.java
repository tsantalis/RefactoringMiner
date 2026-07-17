package org.cloudfoundry.multiapps.controller.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;

//TODO: this class repeats code from test class org.cloudfoundry.multiapps.controller.process.steps.TestData; consolidate if we refactor core module and stplit ApplicationUri out of it
public class TestData {

    public static Map<String, Object> routeParameterWithAdditionalValues(String route, boolean noHostname,
                                                                         Map<String, Object> additionalParameters) {
        Map<String, Object> resultParameter = constructRouteParameter(route, false, noHostname);

        if (additionalParameters != null) {
            resultParameter.putAll(additionalParameters);
        }

        return resultParameter;
    }

    public static Map<String, Object> routeParameter(String route) {
        return constructRouteParameter(route, false, null);
    }

    public static Map<String, Object> idleRouteParameter(String route) {
        return constructRouteParameter(route, true, null);
    }

    public static Map<String, Object> routeParameter(String route, Boolean noHostname) {
        return constructRouteParameter(route, false, noHostname);
    }

    public static Map<String, Object> idleRouteParameter(String route, Boolean noHostname) {
        return constructRouteParameter(route, true, noHostname);
    }

    public static Map<String, Object> constructRouteParameter(String route, boolean isIdle, Boolean noHostname) {
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put(isIdle ? SupportedParameters.IDLE_ROUTE : SupportedParameters.ROUTE, route);
        if (noHostname != null) {
            resultMap.put(SupportedParameters.NO_HOSTNAME, noHostname.booleanValue());
        }

        return resultMap;
    }

    // prefix any uri String with NOHOSTNAME_TEST_PREFIX to parse as hostless CloudRouteSummary; makes test input more readable
    public static final String NOHOSTNAME_URI_FLAG = "NOHOSTNAME-";

    public static CloudRouteSummary routeSummary(String uri) {
        return routeSummary(removePrefix(uri), uriIsHostless(uri));
    }

    public static CloudRouteSummary routeSummary(String uri, boolean noHostname) {
        return new ApplicationURI(uri, noHostname).toCloudRouteSummary();
    }

    public static CloudRouteSummary routeSummary(String host, String domain, String path) {
        return ImmutableCloudRouteSummary.builder()
                                         .host(host)
                                         .domain(domain)
                                         .path(path)
                                         .build();
    }

    public static Set<CloudRouteSummary> routeSummarySet(List<String> uriStrings) {
        return routeSummarySet((String[]) uriStrings.toArray());
    }

    public static Set<CloudRouteSummary> routeSummarySet(String... uriStrings) {
        return Stream.of(uriStrings)
                     .map(TestData::routeSummary)
                     .collect(Collectors.toSet());
    }

    private static String removePrefix(String uri) {
        if (!uriIsHostless(uri)) {
            return uri;
        }

        return uri.substring(NOHOSTNAME_URI_FLAG.length());
    }

    private static boolean uriIsHostless(String uri) {
        return uri.startsWith(NOHOSTNAME_URI_FLAG);
    }
}
