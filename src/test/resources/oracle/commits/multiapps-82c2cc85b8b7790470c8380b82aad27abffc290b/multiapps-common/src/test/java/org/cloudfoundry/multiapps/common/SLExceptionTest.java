package org.cloudfoundry.multiapps.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SLExceptionTest {

    @Test
    void testProperlyConstructedPattern() {
        String msg = "Executing Activiti task \"{1}\" of process \"{0}\"";
        SLException slException = new SLException(msg, "{foo}", "bar");
        SLException slException2 = new SLException(slException);

        assertEquals(slException.getMessage(), slException2.getMessage(), "Nested SLException's messages should match");
    }

    @Test
    void testExceptionWrappedBySLException() {
        String msg = "This is a }{ json: [{\"0\": \"1\"}]";
        Exception exception = new Exception(msg);
        SLException slException = new SLException(exception);

        assertEquals(exception.getMessage(), slException.getMessage(), "SLException should reuse it's cause's message");
    }
}