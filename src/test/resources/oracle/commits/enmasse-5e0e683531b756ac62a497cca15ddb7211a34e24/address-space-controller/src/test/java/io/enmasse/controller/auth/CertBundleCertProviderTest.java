/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.CertSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExternalResourceSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ExternalResourceSupport.class)
public class CertBundleCertProviderTest {
    @Rule
    public OpenShiftServer server = new OpenShiftServer(true, true);

    private OpenShiftClient client;
    private CertProvider certProvider;

    @BeforeEach
    public void setup() {
        client = server.getOpenshiftClient();

        certProvider = new CertBundleCertProvider(client);
    }

    @Test
    public void testProvideCertNoService() {

        AddressSpace space = new AddressSpace.Builder()
                .setName("myspace")
                .setPlan("myplan")
                .setType("standard")
                .build();

        CertSpec spec = new CertSpec.Builder()
                .setProvider("certBundle")
                .setSecretName("mycerts")
                .build();

        certProvider.provideCert(space, new EndpointInfo("messaging", spec));

        Secret cert = client.secrets().withName("mycerts").get();
        assertNull(cert);
    }

    @Test
    public void testProvideCert() {
        AddressSpace space = new AddressSpace.Builder()
                .setName("myspace")
                .setPlan("myplan")
                .setType("standard")
                .build();

        CertSpec spec = new CertSpec.Builder()
                .setProvider("certBundle")
                .setSecretName("mycerts")
                .setTlsKey("aGVsbG8=")
                .setTlsCert("d29ybGQ=")
                .build();

        space.validate();

        certProvider.provideCert(space, new EndpointInfo("messaging", spec));

        Secret cert = client.secrets().withName("mycerts").get();
        assertNotNull(cert);
        assertThat(cert.getData().get("tls.key"), is(spec.getTlsKey()));
        assertThat(cert.getData().get("tls.crt"), is(spec.getTlsCert()));
    }

    @Test
    public void testValidateBadKey() {
        assertThrows(IllegalArgumentException.class, () -> new CertSpec.Builder()
                .setProvider("certBundle")
                .setSecretName("mycerts")
                .setTlsKey("/%^$lkg")
                .setTlsCert("d29ybGQ=")
                .build()
                .validate());
    }

    @Test
    public void testValidateBadCert() {
        assertThrows(IllegalArgumentException.class, () -> new CertSpec.Builder()
                .setProvider("certBundle")
                .setSecretName("mycerts")
                .setTlsKey("d29ybGQ=")
                .setTlsCert("/%^$lkg")
                .build()
                .validate());
    }
}
