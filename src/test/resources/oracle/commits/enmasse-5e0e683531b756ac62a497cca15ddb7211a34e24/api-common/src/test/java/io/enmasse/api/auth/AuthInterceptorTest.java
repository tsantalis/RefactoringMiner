/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AuthInterceptorTest {

    private File tokenFile;
    private AuthInterceptor handler;
    private ContainerRequestContext mockRequestContext;
    private UriInfo mockUriInfo;
    private AuthApi mockAuthApi;

    @BeforeEach
    public void setUp() throws IOException {
        tokenFile = File.createTempFile("token", "");
        mockAuthApi = mock(AuthApi.class);
        when(mockAuthApi.getNamespace()).thenReturn("myspace");
        handler = new AuthInterceptor(mockAuthApi, new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return "/healthz".equals(s);
            }
        });
        mockRequestContext = mock(ContainerRequestContext.class);

        mockUriInfo = mock(UriInfo.class);
        when(mockUriInfo.getAbsolutePath()).thenReturn(URI.create("https://localhost:443/apis/enmasse.io/v1alpha1/addressspaces"));
        when(mockUriInfo.getPath()).thenReturn("/apis/enmasse.io/v1alpha1/addressspaces");
        when(mockRequestContext.getUriInfo()).thenReturn(mockUriInfo);
    }


    private void assertAuthenticatedAs(ContainerRequestContext requestContext, String userName) {
        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        handler.filter(requestContext);
        verify(requestContext).setSecurityContext(contextCaptor.capture());
        SecurityContext context = contextCaptor.getValue();
        assertThat(RbacSecurityContext.getUserName(context.getUserPrincipal()), is(userName));
    }

    @Test
    public void testNoAuthorizationHeader() {
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        assertAuthenticatedAs(mockRequestContext, "system:anonymous");
    }

    @Test
    public void testBasicAuth() throws IOException {
        Files.write(Paths.get(tokenFile.getAbsolutePath()), "valid_token".getBytes());
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNzCg==");

        assertAuthenticatedAs(mockRequestContext, "system:anonymous");
    }

    @Test
    public void testInvalidToken() {
        TokenReview returnedTokenReview = new TokenReview(null, null, false);
        when(mockAuthApi.performTokenReview("invalid_token")).thenReturn(returnedTokenReview);
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid_token");
        assertThrows(NotAuthorizedException.class, () -> handler.filter(mockRequestContext));
    }

    @Test
    public void testValidTokenButNotAuthorized() {
        TokenReview returnedTokenReview = new TokenReview("foo", "myid", true);
        when(mockAuthApi.performTokenReview("valid_token")).thenReturn(returnedTokenReview);
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReview("foo", false);
        when(mockAuthApi.performSubjectAccessReviewResource(eq("foo"), any(), any(), eq("create"), any())).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid_token");
        when(mockRequestContext.getMethod()).thenReturn(HttpMethod.POST);

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        handler.filter(mockRequestContext);
        verify(mockRequestContext).setSecurityContext(contextCaptor.capture());
        SecurityContext context = contextCaptor.getValue();

        assertNotNull(context);
        assertThat(context.getAuthenticationScheme(), is("RBAC"));
        RbacSecurityContext rbacSecurityContext = (RbacSecurityContext) context;
        assertThat(RbacSecurityContext.getUserName(rbacSecurityContext.getUserPrincipal()), is("foo"));
        assertThat(RbacSecurityContext.getUserId(rbacSecurityContext.getUserPrincipal()), is("myid"));
        assertFalse(rbacSecurityContext.isUserInRole(RbacSecurityContext.rbacToRole("myspace", ResourceVerb.create, "configmaps", "")));
    }

    @Test
    public void testHealthAuthz() {
        when(mockUriInfo.getPath()).thenReturn("/healthz");
        handler.filter(mockRequestContext);
    }

    @Test
    public void testCertAuthorization() {
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReview("me", true);
        when(mockAuthApi.performSubjectAccessReviewResource(eq("me"), any(), any(), eq("create"), any())).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString("X-Remote-User")).thenReturn("me");

        HttpServerRequest request = mock(HttpServerRequest.class);
        HttpConnection connection = mock(HttpConnection.class);
        when(request.isSSL()).thenReturn(true);
        when(request.connection()).thenReturn(connection);

        handler.setRequest(request);

        handler.filter(mockRequestContext);

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(mockRequestContext).setSecurityContext(contextCaptor.capture());
        SecurityContext context = contextCaptor.getValue();

        assertThat(context.getAuthenticationScheme(), is("RBAC"));
        RbacSecurityContext rbacSecurityContext = (RbacSecurityContext) context;
        assertThat(RbacSecurityContext.getUserName(rbacSecurityContext.getUserPrincipal()), is("me"));
        assertTrue(rbacSecurityContext.isUserInRole(RbacSecurityContext.rbacToRole("myspace", ResourceVerb.create, "addressspaces", "enmasse.io")));
    }

    @Test
    public void testCertAuthorizationFailed() throws SSLPeerUnverifiedException {
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReview("system:anonymous", false);
        when(mockAuthApi.performSubjectAccessReviewResource(eq("system:anonymous"), any(), any(), eq("create"), eq("enmasse.io"))).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString("X-Remote-User")).thenReturn("me");

        HttpServerRequest request = mock(HttpServerRequest.class);
        HttpConnection connection = mock(HttpConnection.class);
        when(request.isSSL()).thenReturn(true);
        when(request.connection()).thenReturn(connection);
        when(connection.peerCertificateChain()).thenThrow(new SSLPeerUnverifiedException(""));

        handler.setRequest(request);

        handler.filter(mockRequestContext);

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(mockRequestContext).setSecurityContext(contextCaptor.capture());
        SecurityContext context = contextCaptor.getValue();

        assertThat(context.getAuthenticationScheme(), is("RBAC"));
        RbacSecurityContext rbacSecurityContext = (RbacSecurityContext) context;
        assertThat(RbacSecurityContext.getUserName(rbacSecurityContext.getUserPrincipal()), is("system:anonymous"));
        assertFalse(rbacSecurityContext.isUserInRole(RbacSecurityContext.rbacToRole("myspace", ResourceVerb.create, "addressspaces", "enmasse.io")));
    }

    @Test
    public void testAuthorized() throws IOException {
        TokenReview returnedTokenReview = new TokenReview("foo", "myid", true);
        when(mockAuthApi.performTokenReview("valid_token")).thenReturn(returnedTokenReview);
        SubjectAccessReview returnedSubjectAccessReview = new SubjectAccessReview("foo", true);
        when(mockAuthApi.performSubjectAccessReviewResource(eq("foo"), any(), any(), eq("create"), any())).thenReturn(returnedSubjectAccessReview);
        when(mockRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid_token");
        when(mockRequestContext.getMethod()).thenReturn(HttpMethod.POST);

        handler.filter(mockRequestContext);

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(mockRequestContext).setSecurityContext(contextCaptor.capture());
        SecurityContext context = contextCaptor.getValue();

        assertThat(context.getAuthenticationScheme(), is("RBAC"));
        RbacSecurityContext rbacSecurityContext = (RbacSecurityContext) context;
        assertThat(RbacSecurityContext.getUserName(rbacSecurityContext.getUserPrincipal()), is("foo"));
        assertTrue(rbacSecurityContext.isUserInRole(RbacSecurityContext.rbacToRole("myspace", ResourceVerb.create, "configmaps", "")));
    }
}
