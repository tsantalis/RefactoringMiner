/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XOAUTH2SaslServerMechanismTest implements XOAUTH2SaslServerMechanism.TokenVerifier {
    private KeycloakSession keycloakSession;
    private Config.Scope config;
    private boolean verifyToken;
    private KeycloakSessionFactory keycloakSessionFactory;

    @BeforeEach
    public void setup() {
        keycloakSession = mock(KeycloakSession.class);
        keycloakSessionFactory = mock(KeycloakSessionFactory.class);
        when(keycloakSessionFactory.create()).thenReturn(keycloakSession);
        KeycloakTransactionManager txnManager = mock(KeycloakTransactionManager.class);
        when(keycloakSession.getTransactionManager()).thenReturn(txnManager);
        RealmProvider realms = mock(RealmProvider.class);
        when(keycloakSession.realms()).thenReturn(realms);
        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("realm");
        when(realms.getRealmByName(eq("realm"))).thenReturn(realm);
        ClientModel clientModel = mock(ClientModel.class);
        when(clientModel.isEnabled()).thenReturn(true);
        when(realm.getClientByClientId("client")).thenReturn(clientModel);
        UserProvider userProvider = mock(UserProvider.class);
        UserModel user = mock(UserModel.class);
        when(userProvider.getUserByUsername(eq("user"), eq(realm))).thenReturn(user);
        when(keycloakSession.userStorageManager()).thenReturn(userProvider);
        UserCredentialManager userCredentialManager = mock(UserCredentialManager.class);
        when(keycloakSession.userCredentialManager()).thenReturn(userCredentialManager);

        UserSessionProvider userSessionProvider = mock(UserSessionProvider.class);
        when(keycloakSession.sessions()).thenReturn(userSessionProvider);
        UserSessionModel userSessionModel = mock(UserSessionModel.class);
        when(userSessionProvider.getUserSessionWithPredicate(any(), any(), anyBoolean(), any())).thenReturn(userSessionModel);
        when(userSessionModel.getStarted()).thenReturn(Time.currentTime());
        when(userSessionModel.getLastSessionRefresh()).thenReturn(Time.currentTime());
        when(realm.getSsoSessionMaxLifespan()).thenReturn(36000);
        when(realm.getSsoSessionIdleTimeout()).thenReturn(36000);
        when(userSessionModel.getUser()).thenReturn(user);

        config = mock(Config.Scope.class);
        when(config.get(eq("baseUri"), anyString())).thenReturn("https://localhost:8443/auth");
    }

    private byte[] createInitialResponse(final String user, final String token) {
        String initialResponseString="user="+user+"\1auth=Bearer "+token+"\1\1";
        return initialResponseString.getBytes(StandardCharsets.US_ASCII);
    }

    // unknown realm
    @Test
    public void testUnknownRealm() {
        final SaslServerMechanism.Instance instance =
                (new XOAUTH2SaslServerMechanism(this)).newInstance(keycloakSessionFactory, "unknownRealm", config);
        byte[] response = instance.processResponse(createInitialResponse("user", "token"));
        assertTrue(response == null || response.length == 0);
        assertTrue(instance.isComplete());
        assertFalse(instance.isAuthenticated());

    }


    // Invalid token
    @Test
    public void testWrongPassword() {
        final SaslServerMechanism.Instance instance =
                (new XOAUTH2SaslServerMechanism(this)).newInstance(keycloakSessionFactory, "realm", config);
        this.verifyToken = false;
        byte[] response = instance.processResponse(createInitialResponse("user", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3N0xUZ3BvbFJmU1NqQkNEeTljU3VEeFpkM2hmMTEyN3R5ZzNweU52LVRBIn0.eyJqdGkiOiJmM2E3ZjFhNS0zZGM2LTQ1YjEtOWIyYi1hZjIzYmYxYTc5NTUiLCJleHAiOjE1MjAwODI2NTMsIm5iZiI6MCwiaWF0IjoxNTIwMDgyMzUzLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvYW1xcCIsImF1ZCI6ImFtcXAiLCJzdWIiOiI4NjA2NDJiMy0wZmE1LTRiYjctOGI0YS0xZGNiOTdlZjhmZDgiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhbXFwIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiY2Y5Njc0YTAtN2FiNy00YmZiLWI4ZTktZjk3MzM5NTY4NzYwIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInByZWZlcnJlZF91c2VybmFtZSI6Imd1ZXN0In0.Ul33UaC4EXdxtMqv6fLyOHRvHNkA3U1F2FDKxo4Rs4gIvmrbyjK_RN_AciVZjtphYM4xXDn3E9acchyLcQB690NCneDVwqUUj5c2ZU5LcZsAARtBC8MPk8ekDhfmm3ppsRnnSYzucDC1Qe-iLtmhj-v3NzdkgxIwzbgL2E7QzUuf8KFSj2Ue322r27tPhKLm2ay3lcauKe_u3LziA6S1sgxdABWzTBP8UhSeKtqY0j6JT50LA7mvVgmEZvdzqgt6EVYmU0ALzbdjQuOJhmlTDH68cPqQI1-MLAreHt7BDLTN0YuthzoFKheZBaIpBvdDuSI_iV0iAe_AlT16ka4rUg"));
        assertTrue(response == null || response.length == 0);
        assertTrue(instance.isComplete());
        assertFalse(instance.isAuthenticated());
    }

    // Validated token
    @Test
    public void testValidatedToken() {
        final SaslServerMechanism.Instance instance =
                (new XOAUTH2SaslServerMechanism(this)).newInstance(keycloakSessionFactory, "realm", config);
        this.verifyToken = true;
        byte[] response = instance.processResponse(createInitialResponse("user", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ3N0xUZ3BvbFJmU1NqQkNEeTljU3VEeFpkM2hmMTEyN3R5ZzNweU52LVRBIn0.eyJqdGkiOiJmM2E3ZjFhNS0zZGM2LTQ1YjEtOWIyYi1hZjIzYmYxYTc5NTUiLCJleHAiOjE1MjAwODI2NTMsIm5iZiI6MCwiaWF0IjoxNTIwMDgyMzUzLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvYW1xcCIsImF1ZCI6ImFtcXAiLCJzdWIiOiI4NjA2NDJiMy0wZmE1LTRiYjctOGI0YS0xZGNiOTdlZjhmZDgiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhbXFwIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiY2Y5Njc0YTAtN2FiNy00YmZiLWI4ZTktZjk3MzM5NTY4NzYwIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInByZWZlcnJlZF91c2VybmFtZSI6Imd1ZXN0In0.Ul33UaC4EXdxtMqv6fLyOHRvHNkA3U1F2FDKxo4Rs4gIvmrbyjK_RN_AciVZjtphYM4xXDn3E9acchyLcQB690NCneDVwqUUj5c2ZU5LcZsAARtBC8MPk8ekDhfmm3ppsRnnSYzucDC1Qe-iLtmhj-v3NzdkgxIwzbgL2E7QzUuf8KFSj2Ue322r27tPhKLm2ay3lcauKe_u3LziA6S1sgxdABWzTBP8UhSeKtqY0j6JT50LA7mvVgmEZvdzqgt6EVYmU0ALzbdjQuOJhmlTDH68cPqQI1-MLAreHt7BDLTN0YuthzoFKheZBaIpBvdDuSI_iV0iAe_AlT16ka4rUg"));
        assertTrue(response == null || response.length == 0);
        assertTrue(instance.isComplete());
        assertTrue(instance.isAuthenticated());
    }

    // incorrect sasl format
    @Test
    public void testBadInitialResponse() {
        final SaslServerMechanism.Instance instance =
                (new XOAUTH2SaslServerMechanism(this)).newInstance(keycloakSessionFactory, "realm", config);
        assertThrows(IllegalArgumentException.class, () -> instance.processResponse("potato".getBytes(StandardCharsets.UTF_8)));
    }


    @Override
    public AccessToken verifyTokenString(RealmModel realm, String tokenString, URI baseUri, KeycloakSession keycloakSession) throws VerificationException {
        if(verifyToken) {
            AccessToken accessToken = new AccessToken();
            accessToken.issuedFor("client");
            return accessToken;
        } else {
            throw new VerificationException();
        }
    }
}
