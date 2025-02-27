/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.credential.PasswordUserCredentialModel;
import org.mockito.ArgumentMatcher;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlainSaslServerMechanismTest {
    private KeycloakSessionFactory keycloakSessionFactory;
    private KeycloakSession keycloakSession;
    private Config.Scope config;

    @Before
    public void setup() {
        keycloakSessionFactory = mock(KeycloakSessionFactory.class);
        keycloakSession = mock(KeycloakSession.class);
        when(keycloakSessionFactory.create()).thenReturn(keycloakSession);
        KeycloakTransactionManager txnManager = mock(KeycloakTransactionManager.class);
        when(keycloakSession.getTransactionManager()).thenReturn(txnManager);
        RealmProvider realms = mock(RealmProvider.class);
        when(keycloakSession.realms()).thenReturn(realms);
        RealmModel realm = mock(RealmModel.class);
        when(realms.getRealmByName(eq("realm"))).thenReturn(realm);
        UserProvider userProvider = mock(UserProvider.class);
        UserModel user = mock(UserModel.class);
        when(userProvider.getUserByUsername(eq("user"), eq(realm))).thenReturn(user);
        when(keycloakSession.userStorageManager()).thenReturn(userProvider);
        UserCredentialManager userCredentialManager = mock(UserCredentialManager.class);
        when(keycloakSession.userCredentialManager()).thenReturn(userCredentialManager);
        when(userCredentialManager.isValid(eq(realm), eq(user), argThat(new PasswordCredentialMatcher("password")))).thenReturn(true);

        config = mock(Config.Scope.class);
    }

    private byte[] createInitialResponse(final String user, final String password) {
        byte[] userBytes = user.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] response = new byte[2 + userBytes.length + passwordBytes.length];
        System.arraycopy(userBytes,0,response, 1, userBytes.length);
        System.arraycopy(passwordBytes,0,response, 2+userBytes.length, passwordBytes.length);

        return response;
    }

    private static final class PasswordCredentialMatcher implements ArgumentMatcher<PasswordUserCredentialModel>
    {

        private final String password;

        private PasswordCredentialMatcher(final String password) {
            this.password = password;
        }

        @Override
        public boolean matches(final PasswordUserCredentialModel item) {
            return item.getValue().equals(password);
        }
    }

    // unknown realm
    @Test
    public void testUnknownRealm() {
        final SaslServerMechanism.Instance instance =
                (new PlainSaslServerMechanism()).newInstance(keycloakSessionFactory, "unknownRealm", config);
        byte[] response = instance.processResponse(createInitialResponse("user", "password"));
        assertTrue(response == null || response.length == 0);
        assertTrue(instance.isComplete());
        assertFalse(instance.isAuthenticated());

    }

    // known realm, unknown user
    @Test
    public void testUnknownUser() {
        final SaslServerMechanism.Instance instance =
                (new PlainSaslServerMechanism()).newInstance(keycloakSessionFactory, "realm", config);
        byte[] response = instance.processResponse(createInitialResponse("unknown", "password"));
        assertTrue(response == null || response.length == 0);
        assertTrue(instance.isComplete());
        assertFalse(instance.isAuthenticated());

    }

    // known user, wrong password
    @Test
    public void testWrongPassword() {
        final SaslServerMechanism.Instance instance =
                (new PlainSaslServerMechanism()).newInstance(keycloakSessionFactory, "realm", config);
        byte[] response = instance.processResponse(createInitialResponse("user", "wrong"));
        assertTrue(response == null || response.length == 0);
        assertTrue(instance.isComplete());
        assertFalse(instance.isAuthenticated());

    }

    // known user, correct password
    @Test
    public void testCorrectPassword() {
        final SaslServerMechanism.Instance instance =
                (new PlainSaslServerMechanism()).newInstance(keycloakSessionFactory, "realm", config);
        byte[] response = instance.processResponse(createInitialResponse("user", "password"));
        assertTrue(response == null || response.length == 0);
        assertTrue(instance.isComplete());
        assertTrue(instance.isAuthenticated());
    }

    // incorrect sasl format
    @Test(expected = IllegalArgumentException.class)
    public void testBadInitialResponse() {
        final SaslServerMechanism.Instance instance =
                (new PlainSaslServerMechanism()).newInstance(keycloakSessionFactory, "realm", config);
        instance.processResponse("potato".getBytes(StandardCharsets.UTF_8));
    }


}
