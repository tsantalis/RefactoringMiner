/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.artemis.sasl_delegation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.impl.ProtonSaslPlainImpl;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.core.settings.impl.HierarchicalObjectRepository;
import org.apache.activemq.artemis.spi.core.security.jaas.CertificateCallback;
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal;
import org.apache.activemq.artemis.spi.core.security.jaas.UserPrincipal;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SaslDelegatingLoginTest {

    private SaslDelegatingLogin loginModule;
    private Map<String, Object> options = new HashMap<>();
    private Map<String, String> validLogins = new HashMap<>();
    private Map<String, List<String>> groups = new HashMap<>();
    private int port;
    private AuthServer authServer;
    private Vertx vertx;
    private String verticleId;
    private String mechName;
    private SaslGroupBasedSecuritySettingsPlugin securityPlugin = new SaslGroupBasedSecuritySettingsPlugin();

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        options.clear();
        loginModule = new SaslDelegatingLogin();

        Map<String, String> initMap = new HashMap<>();
        initMap.put("name", "test");
        initMap.put("useGroupsFromSaslDelegation", "true");
        securityPlugin.init(initMap);
        HierarchicalRepository<Set<Role>> repo = new HierarchicalObjectRepository<>();
        repo.setDefault(Collections.emptySet());
        securityPlugin.setSecurityRepository(repo);

        CompletableFuture<Integer> portFuture = new CompletableFuture<>();
        mechName = ProtonSaslPlainImpl.MECH_NAME;
        authServer = new AuthServer(portFuture);
        vertx = Vertx.vertx();
        CompletableFuture<String> idFuture = new CompletableFuture<>();
        vertx.deployVerticle(authServer, result -> {
            if(result.succeeded()) {
                idFuture.complete(result.result());
            } else {
                idFuture.completeExceptionally(result.cause());
            }
        });
        verticleId = idFuture.get();
        port = portFuture.get();
        options.put("hostname", "127.0.0.1");
        options.put("port", port);
        options.put("security_settings", "test");
        options.put("default_roles_authenticated", "all");
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    // successful credentials login with roles
    @Test
    public void testSuccessfulCredentialLogin() throws Exception {
        Subject subject = new Subject();
        validLogins.put("user", "password");
        groups.put("user", Arrays.asList("send_a","recv_b"));
        loginModule.initialize(subject, createCallbackHandler("user", "password".toCharArray()), Collections.emptyMap(), options);
        assertTrue("Login unexpectedly failed", loginModule.login());
        assertEquals("No principals should be added until after the commit", 0, subject.getPrincipals().size());
        assertTrue("Commit unexpectedly failed", loginModule.commit());
        assertEquals("Unexpected user principal names", Collections.singleton("user"), subject.getPrincipals(UserPrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));
        assertEquals("Unexpected role principal names", new HashSet<>(Arrays.asList("send_a","recv_b","all")), subject.getPrincipals(RolePrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));
    }

    // unsuccessful credentials login
    @Test
    public void testUnsuccessfulCredentialLogin() throws Exception {
        Subject subject = new Subject();
        validLogins.put("user", "password2");
        groups.put("user", Arrays.asList("a","b"));
        loginModule.initialize(subject, createCallbackHandler("user", "password".toCharArray()), Collections.emptyMap(), options);
        assertFalse("Login unexpectedly succeeded", loginModule.login());
        assertEquals("No principals should be added until after the commit", 0, subject.getPrincipals().size());
        loginModule.commit();
        assertEquals("Unexpected user principal names", Collections.emptySet(), subject.getPrincipals(UserPrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));
        assertEquals("Unexpected role principal names", Collections.emptySet(), subject.getPrincipals(RolePrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));

    }

    // no matching sasl mechanism
    @Test
    public void testUnsuccessfulCredentialLoginWithBadMechanism() throws Exception {
        mechName = "WIBBLE";
        Subject subject = new Subject();
        validLogins.put("user", "password");
        groups.put("user", Arrays.asList("a","b"));
        loginModule.initialize(subject, createCallbackHandler("user", "password".toCharArray()), Collections.emptyMap(), options);
        try {
            loginModule.login();
            fail("Login should not succeed if there are no matching mechanisms");
        } catch (LoginException e) {
            // pass
        }
    }

    // successful cert login
    @Test
    public void testSuccessfulCertLogin() throws Exception {

        options.put("valid_cert_users", "foo:a,b;bar:c");

        Subject subject = new Subject();

        loginModule.initialize(subject, createCallbackHandler(null, null, generateCertificate(CERT_FOO)), Collections.emptyMap(), options);
        assertTrue("Login unexpectedly failed", loginModule.login());
        assertEquals("No principals should be added until after the commit", 0, subject.getPrincipals().size());
        assertTrue("Commit unexpectedly failed", loginModule.commit());
        assertEquals("Unexpected user principal names", Collections.singleton("foo"), subject.getPrincipals(UserPrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));
        assertEquals("Unexpected role principal names", new HashSet<>(Arrays.asList("a","b")), subject.getPrincipals(RolePrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));

        subject = new Subject();

        loginModule.initialize(subject, createCallbackHandler(null, null, generateCertificate(CERT_BAR)), Collections.emptyMap(), options);
        assertTrue("Login unexpectedly failed", loginModule.login());
        assertEquals("No principals should be added until after the commit", 0, subject.getPrincipals().size());
        assertTrue("Commit unexpectedly failed", loginModule.commit());
        assertEquals("Unexpected user principal names", Collections.singleton("bar"), subject.getPrincipals(UserPrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));
        assertEquals("Unexpected role principal names", Collections.singleton("c"), subject.getPrincipals(RolePrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));

    }

    // unsuccessful cert login
    @Test
    public void testUnsuccessfulCertLogin() throws Exception {

        Map<String, Object> options = new HashMap<>();
        options.put("valid_cert_users", "foo:a,b;bar:c");

        Subject subject = new Subject();

        loginModule.initialize(subject, createCallbackHandler(null, null, generateCertificate(CERT_BANANA)), Collections.emptyMap(), options);
        assertFalse("Login unexpectedly succeeded", loginModule.login());
        assertEquals("No principals should be added until after the commit", 0, subject.getPrincipals().size());
        loginModule.commit();
        assertEquals("Unexpected user principal names", Collections.emptySet(), subject.getPrincipals(UserPrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));
        assertEquals("Unexpected role principal names", Collections.emptySet(), subject.getPrincipals(RolePrincipal.class).stream().map(Principal::getName).collect(Collectors.toSet()));

    }


    private CallbackHandler createCallbackHandler(String user, char[] password, X509Certificate... certificates) {
        return callbacks -> {
            for(Callback callback : callbacks) {
                if(callback instanceof NameCallback && user != null) {
                    ((NameCallback)callback).setName(user);
                } else if(callback instanceof PasswordCallback && password != null) {
                    ((PasswordCallback)callback).setPassword(password);
                } else if(callback instanceof CertificateCallback && (certificates.length != 0)) {
                    ((CertificateCallback)callback).setCertificates(certificates);
                }
            }
        };
    }

    private X509Certificate generateCertificate(String cert) throws CertificateException {
        return X509Certificate.getInstance(cert.getBytes(StandardCharsets.US_ASCII));
    }

    private static final Symbol ADDRESS_AUTHZ_CAPABILITY = Symbol.valueOf("ADDRESS-AUTHZ");
    private static final Symbol ADDRESS_AUTHZ_PROPERTY = Symbol.valueOf("address-authz");

    private final class AuthServer extends AbstractVerticle {



        private ProtonServer server;
        private final CompletableFuture<Integer> portFuture;
        private final SaslAuthenticator saslAuthenticator;

        private AuthServer(CompletableFuture<Integer> portFuture) {
            this.portFuture = portFuture;
            saslAuthenticator = new SaslAuthenticator();
        }

        private void connectHandler(ProtonConnection connection) {
            String containerId = "auth-server";
            connection.setContainer(containerId);
            connection.openHandler(conn -> {
                Map<Symbol, Object> props = new HashMap<>();
                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", saslAuthenticator.getUser());
                claims.put("preferred_username", saslAuthenticator.getUser());
                props.put(Symbol.valueOf("authenticated-identity"),claims);
                if(connection.getRemoteDesiredCapabilities() != null && Arrays.asList(connection.getRemoteDesiredCapabilities()).contains(ADDRESS_AUTHZ_CAPABILITY)) {
                    connection.setOfferedCapabilities(new Symbol[] { ADDRESS_AUTHZ_CAPABILITY });
                    if(groups.containsKey(saslAuthenticator.getUser())) {
                        props.put(ADDRESS_AUTHZ_PROPERTY, getPermissionsFromGroups(groups.get(saslAuthenticator.getUser())));
                    }
                }

                if(groups.containsKey(saslAuthenticator.getUser())) {
                    props.put(Symbol.valueOf("groups"), groups.get(saslAuthenticator.getUser()));
                }
                connection.setProperties(props);
                connection.open();
                connection.close();
            }).closeHandler(conn -> {
                connection.close();
                connection.disconnect();
            }).disconnectHandler(protonConnection -> {
                connection.disconnect();
            });

        }

        Map<String, String[]> getPermissionsFromGroups(List<String> groups) {
            Map<String, Set<String>> authMap = new HashMap<>();
            for(String group : groups) {
                String[] parts = group.split("_", 2);
                if(parts[0] != null) {
                    Set<String> permissions = authMap.computeIfAbsent(parts[1], a -> new HashSet<>());
                    permissions.add(parts[0]);
                }
            }
            return authMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(new String[e.getValue().size()])));
        }


        @Override
        public void start() {
            ProtonServerOptions options = new ProtonServerOptions();

            server = ProtonServer.create(vertx, options);
            server.saslAuthenticatorFactory(() -> saslAuthenticator);
            server.connectHandler(this::connectHandler);

            server.listen(0, "127.0.0.1", event -> {
                if(event.failed()) {
                    portFuture.completeExceptionally(event.cause());
                } else {
                    portFuture.complete(server.actualPort());
                }
            });
        }

        @Override
        public void stop() {
            if (server != null) {
                server.close();
            }
        }

    }

    private static final String CERT_FOO = "-----BEGIN CERTIFICATE-----\n" +
        "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\n" +
        "W0s=\n" +
        "-----END CERTIFICATE-----\n";

    private static final String CERT_BAR = "-----BEGIN CERTIFICATE-----\n" +
        "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\n" +
        "d4s=\n" +
        "-----END CERTIFICATE-----\n";

    private static final String CERT_BANANA = "-----BEGIN CERTIFICATE-----\n" +
        "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\n" +
        "DQkhI+BdvtzGTwE=\n" +
        "-----END CERTIFICATE-----\n";

    private class SaslAuthenticator implements ProtonSaslAuthenticator {

        private Sasl sasl;
        private String user;
        private boolean succeeded;

        private SaslAuthenticator() {
        }

        @Override
        public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
            sasl = transport.sasl();
            sasl.server();
            sasl.allowSkip(false);

            sasl.setMechanisms(mechName);

        }

        @Override
        public void process(Handler<Boolean> completionHandler) {
            String[] remoteMechanisms = sasl.getRemoteMechanisms();

            if (remoteMechanisms.length > 0) {
                byte[] response;
                if(sasl.pending()>0) {
                    response = new byte[sasl.pending()];
                    sasl.recv(response, 0, response.length);
                } else {
                    response = new byte[0];
                }

                int authzidNullPosition = findNullPosition(response, 0);
                if (authzidNullPosition < 0) {
                    throw new IllegalArgumentException("Invalid PLAIN encoding, authzid null terminator not found");
                }

                int authcidNullPosition = findNullPosition(response, authzidNullPosition + 1);
                if (authcidNullPosition < 0) {
                    throw new IllegalArgumentException("Invalid PLAIN encoding, authcid null terminator not found");
                }

                String username = new String(response, authzidNullPosition + 1, authcidNullPosition - authzidNullPosition - 1, StandardCharsets.UTF_8);
                int passwordLen = response.length - authcidNullPosition - 1;
                String password = new String(response, authcidNullPosition + 1, passwordLen, StandardCharsets.UTF_8);

                if(validLogins.containsKey(username)) {
                    if(password.equals(validLogins.get(username))) {
                        this.user = username;
                        succeeded = true;
                        sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
                    } else {
                        sasl.done(Sasl.SaslOutcome.PN_SASL_AUTH);
                    }
                } else {
                    sasl.done(Sasl.SaslOutcome.PN_SASL_AUTH);
                }
                completionHandler.handle(true);
            } else {
                completionHandler.handle(false);
            }
        }

        private int findNullPosition(byte[] response, int startPosition) {
            int position = startPosition;
            while (position < response.length) {
                if (response[position] == (byte) 0) {
                    return position;
                }
                position++;
            }
            return -1;
        }

        @Override
        public boolean succeeded() {
            return succeeded;
        }

        public String getUser() {
            return user;
        }
    }
}
