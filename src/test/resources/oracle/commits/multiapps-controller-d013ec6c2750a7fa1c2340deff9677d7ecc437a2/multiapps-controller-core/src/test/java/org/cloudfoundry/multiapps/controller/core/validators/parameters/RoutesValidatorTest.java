package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.NO_HOSTNAME;
import static org.cloudfoundry.multiapps.controller.core.util.TestData.routeParameter;
import static org.cloudfoundry.multiapps.controller.core.util.TestData.routeParameterWithAdditionalValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RoutesValidatorTest {

    private static final RoutesValidator validator = new RoutesValidator();

    static Stream<Arguments> getParameters() {
        return Stream.of(
// @formatter:off
            // [1] two routes; both are valid
            Arguments.of(List.of(routeParameter("valid-host.domain"), 
                                 routeParameter("sub.domain.com", true)), 
                         true,
                         List.of(routeParameter("valid-host.domain"), 
                                 routeParameter("sub.domain.com", true)), 
                         null),
            // [2] three routes; one is invalid; can be corrected
            Arguments.of(List.of(routeParameter("foo.domain.com"), 
                                 routeParameter("bar.domain.com", true), 
                                 routeParameter("baz^but$invalid.domain.com")), 
                         false,
                         List.of(routeParameter("foo.domain.com"), 
                                 routeParameter("bar.domain.com", true), 
                                 routeParameter("baz-but-invalid.domain.com")),
                         null),
            // [3] one route; is invalid; can be corrected
            Arguments.of(List.of(routeParameter("host.domain_can_be_corrected.com")),
                         false, 
                         List.of(routeParameter("host.domain-can-be-corrected.com")),
                         null),
            // [4] one hostless route; is invalid; can be corrected
            Arguments.of(List.of(routeParameter("sub_domain_can_be_corrected.domain.com", true)),
                         false, 
                         List.of(routeParameter("sub-domain-can-be-corrected.domain.com", true)),
                         null),
            // [5] one route containing invalid value for no-hostname flag; results in exception
            Arguments.of(List.of(routeParameterWithAdditionalValues("doesnt.matter.com", false, Map.of(NO_HOSTNAME, "not a boolean"))),
                         false, 
                         null,
                         MessageFormat.format(Messages.COULD_NOT_PARSE_BOOLEAN_FLAG, NO_HOSTNAME)),
            // [6] two routes, one containing a random key/value pair next to route; is ignored and valid
            Arguments.of(List.of(routeParameter("valid-route.com"), 
                                 routeParameterWithAdditionalValues("another-valid-one.com", false, Map.of("UNSUPPORTED-KEY", 1))),
                         false, 
                         List.of(routeParameter("valid-route.com"),
                                 routeParameter("another-valid-one.com", false)),
                         null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidate(List<Map<String, Object>> inputRoutes, boolean isValid, List<Map<String, Object>> expectedCorrectedRoutes,
                      String expectedException) {
        try {
            assertEquals(isValid, validator.isValid(inputRoutes, null));
            assertNull(expectedException, "Expected an exception but test passed!");
        } catch (Exception e) {
            assertNotNull(expectedException, "Didn't expect an exception, but got " + e.getMessage());
            assertEquals(expectedException, e.getMessage(), "Exception's message doesn't match up!");
        }
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testAttemptToCorrect(List<Map<String, Object>> inputRoutes, boolean isValid, List<Map<String, Object>> expectedCorrectedRoutes,
                              String expectedException) {
        try {
            List<Map<String, Object>> correctedRoutes = (List<Map<String, Object>>) validator.attemptToCorrect(inputRoutes, null);
            assertNull(expectedException, "Expected an exception but test passed!");
            assertEquals(expectedCorrectedRoutes, correctedRoutes);
        } catch (Exception e) {
            assertNotNull(expectedException, "Didn't expect an exception, but got " + e.getMessage());
            assertEquals(expectedException, e.getMessage(), "Exception's message doesn't match up!");
        }
    }

    @Test
    void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @Test
    void testGetParameterName() {
        assertEquals(SupportedParameters.ROUTES, validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(Module.class));
    }

}
