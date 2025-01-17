/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeycloakRealmParamsTest {
    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    private KubernetesClient client;

    @Before
    public void setup() {
        client = server.getClient();
    }

    @Test
    public void testRequiredEnvironment() {
        client.configMaps().createNew()
                .editOrNewMetadata()
                .withName("myconfig")
                .endMetadata()
                .addToData("identityProviderUrl", "https://localhost:8443/auth")
                .addToData("identityProviderClientId", "myclient")
                .addToData("identityProviderClientSecret", "mysecret")
                .done();

        KeycloakRealmParams params = KeycloakRealmParams.fromKube(client, "myconfig");
        assertEquals("https://localhost:8443/auth", params.getIdentityProviderUrl());
        assertEquals("myclient", params.getIdentityProviderClientId());
        assertEquals("mysecret", params.getIdentityProviderClientSecret());
    }
}
