/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.common;

import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DefaultExceptionMapperTest {

    @Test
    public void testToResponse() {
        int code = 410;
        Response.Status status = Response.Status.fromStatusCode(code);
        String message = "Some error message";
        WebApplicationException exception = new WebApplicationException(message, status);

        Response response = new DefaultExceptionMapper().toResponse(exception);
        assertEquals(code, response.getStatus());
        assertEquals(status.getReasonPhrase(), response.getStatusInfo().getReasonPhrase());
        assertTrue(response.getEntity() instanceof Status);
        Status responseEntity = (Status) response.getEntity();
        assertEquals(status.getReasonPhrase(), responseEntity.getReason());
        assertEquals(message, responseEntity.getMessage());
    }

    @Test
    public void testToResponseStatus422() {
        int code = 422;
        io.fabric8.kubernetes.api.model.Status status = new StatusBuilder().withCode(code).build();
        String message = "Some error message";
        KubernetesClientException kubernetesClientException = new KubernetesClientException(message, code, status);

        Response response = new DefaultExceptionMapper().toResponse(kubernetesClientException);
        assertEquals(code, response.getStatus());
        // can't check for response.getStatusInfo().getReasonPhrase() here because 422 isn't known in Response.Status 
        assertTrue(response.getEntity() instanceof Status);
        Status responseEntity = (Status) response.getEntity();
        assertEquals("Unprocessable Entity", responseEntity.getReason());
        assertEquals(message, responseEntity.getMessage());
    }

}