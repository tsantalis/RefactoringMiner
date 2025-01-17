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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class CertBundleCertProviderTest {
    @Rule
    public OpenShiftServer server = new OpenShiftServer(true, true);

    private OpenShiftClient client;
    private CertProvider certProvider;

    @Before
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

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBadKey() {
        new CertSpec.Builder()
                .setProvider("certBundle")
                .setSecretName("mycerts")
                .setTlsKey("/%^$lkg")
                .setTlsCert("d29ybGQ=")
                .build()
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateBadCert() {
        new CertSpec.Builder()
                .setProvider("certBundle")
                .setSecretName("mycerts")
                .setTlsKey("d29ybGQ=")
                .setTlsCert("/%^$lkg")
                .build()
                .validate();
    }
}
