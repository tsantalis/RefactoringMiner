package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;

public class TestDataUtil {
    private static Map<String, Object> routeParameter(String route) {
        return routeParameter(route, false, null);
    }

    private static Map<String, Object> idleRouteParameter(String route) {
        return routeParameter(route, true, null);
    }

    private static Map<String, Object> routeParameter(String route, boolean noHostname) {
        return routeParameter(route, false, noHostname);
    }

    private static Map<String, Object> idleRouteParameter(String route, boolean noHostname) {
        return routeParameter(route, true, noHostname);
    }

    private static Map<String, Object> routeParameter(String route, boolean isIdle, Boolean noHostname) {
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put(isIdle ? SupportedParameters.IDLE_ROUTE : SupportedParameters.ROUTE, route);
        if (noHostname != null) {
            resultMap.put(SupportedParameters.NO_HOSTNAME, noHostname.booleanValue());
        }

        return resultMap;
    }

    private static CloudRouteSummary routeSummary(String host, String domain, String path) {
        return ImmutableCloudRouteSummary.builder()
                                         .host(host)
                                         .domain(domain)
                                         .path(path)
                                         .build();
    }
}
