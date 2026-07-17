package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RouteValidatorTest {

    private static final RouteValidator validator = new RouteValidator();

    static Stream<Arguments> getParameters() {
        return Stream.of(
// @formatter:off
            // [1] basic host and domain route; is already valid, no correction needed
            Arguments.of("valid-host.domain", null, true, "valid-host.domain", null),
            // [2] route with host and domain; contains invalid characters, can be corrected
            Arguments.of("should_correct$this$host.domain", null, false, "should-correct-this-host.domain", null),
            // [3] route with host and domain, and explicit no-hostname parameter; host should be corrected
            Arguments.of("host_can_be_corrected.domain.com", Boolean.FALSE, false, "host-can-be-corrected.domain.com", null),
            // [4] no-hostname parameter provided is of invalid type; should raise exception during validation
            Arguments.of("doesnt.matter.com", 1, false, null, MessageFormat.format(Messages.COULD_NOT_PARSE_BOOLEAN_FLAG, SupportedParameters.NO_HOSTNAME)),
            // [5] a route without hostname; domain contains invalid character, can be corrected
            Arguments.of("domain_can_be_corrected.com", Boolean.TRUE, false, "domain-can-be-corrected.com", null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidate(String inputRoute, Object noHostname, boolean isValid, String correctedRoute, String expectedException) {
        Map<String, Object> routeContext = constructContext(noHostname);
        try {
            assertEquals(isValid, validator.isValid(inputRoute, routeContext));
            assertNull(expectedException, "Expected an exception but test passed!");
        } catch (Exception e) {
            assertNotNull(expectedException, "Didn't expect an exception, but got " + e.getMessage());
            assertEquals(expectedException, e.getMessage(), "Exception's message doesn't match up!");
        }
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testAttemptToCorrect(String inputRoute, Object noHostname, boolean isValid, String correctedRoute, String expectedException) {
        if (!validator.canCorrect())
            return;
        try {
            Map<String, Object> routeContext = constructContext(noHostname);
            String result = validator.attemptToCorrect(inputRoute, routeContext);
            assertNull(expectedException, "Expected an exception but test passed!");
            assertEquals(correctedRoute, result);
        } catch (Exception e) {
            assertNotNull(expectedException, "Didn't expect an exception, but got " + e.getMessage());
            assertEquals(expectedException, e.getMessage(), "Exception's message doesn't match up!");
        }
    }

    private Map<String, Object> constructContext(Object noHostname) {
        if (noHostname == null) {
            return Collections.emptyMap();
        }

        return Map.of(SupportedParameters.NO_HOSTNAME, noHostname);
    }

    @Test
    void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @Test
    void testGetParameterName() {
        assertEquals("route", validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(Module.class));
    }

}
