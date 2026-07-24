package org.cloudfoundry.multiapps.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SLExceptionTest {

    @Test
    public void testProperlyConstructedPattern() {
        String msg = "Executing Activiti task \"{1}\" of process \"{0}\"";
        SLException slException = new SLException(msg, "{foo}", "bar");
        SLException slException2 = new SLException(slException);

        assertEquals("Nested SLException's messages should match", slException.getMessage(), slException2.getMessage());
    }

    @Test
    public void testExceptionWrappedBySLException() {
        String msg = "This is a }{ json: [{\"0\": \"1\"}]";
        Exception exception = new Exception(msg);
        SLException slException = new SLException(exception);

        assertEquals("SLException should reuse it's cause's message", exception.getMessage(), slException.getMessage());
    }
}