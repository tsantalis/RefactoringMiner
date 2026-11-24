package org.eel.kitchen.jsonschema;

import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

public final class FormValidationTest
{
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FormValidation servlet;

    // FIXME: why doesn't it work???
    /*
    @BeforeTest
    public void init()
    {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        servlet = new FormValidation();
    }
    */

    @Test
    public void missingBothParametersReturns401()
        throws ServletException, IOException
    {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        servlet = new FormValidation();
        servlet.doPost(request, response);
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Missing parameters");
    }

    @Test
    public void missingSchemaParameterReturns401()
        throws ServletException, IOException
    {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        servlet = new FormValidation();
        when(request.getParameter("schema")).thenReturn("{}");
        servlet.doPost(request, response);
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Missing parameters");
    }

    @Test
    public void missingDataParameterReturns401()
        throws ServletException, IOException
    {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        servlet = new FormValidation();
        when(request.getParameter("data")).thenReturn("{}");
        servlet.doPost(request, response);
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Missing parameters");
    }
}
