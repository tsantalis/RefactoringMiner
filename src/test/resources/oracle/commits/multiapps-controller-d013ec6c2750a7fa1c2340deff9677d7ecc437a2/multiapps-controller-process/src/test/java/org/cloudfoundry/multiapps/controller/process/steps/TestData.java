package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;

import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;

// TODO: this class repeats code from test class org.cloudfoundry.multiapps.controller.core.util.TestData; remove if we refactor core module and stplit ApplicationUri out of it
public class TestData {

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
