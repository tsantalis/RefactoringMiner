package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

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
            // [0]
            Arguments.of("valid-host.domain", true, "valid-host.domain", null),
            // [1]
            Arguments.of("should_correct$this$host.domain", false, "should-correct-this-host.domain", null),
            // [2]
            Arguments.of("host_can_be_corrected.domain.com", false, "host-can-be-corrected.domain.com", null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidate(String inputRoute, boolean isValid, String correctedRoute, String expectedException) {
        assertEquals(isValid, validator.isValid(inputRoute, null));
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testAttemptToCorrect(String inputRoute, boolean isValid, String correctedRoute, String expectedException) {
        if (!validator.canCorrect())
            return;
        try {
            String result = validator.attemptToCorrect(inputRoute, null);
            assertEquals(correctedRoute, result);
        } catch (Exception e) {
            assertNotNull(e.getMessage(), expectedException);
            assertEquals("Exception's message doesn't match up!", e.getMessage(), expectedException);
        }
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
