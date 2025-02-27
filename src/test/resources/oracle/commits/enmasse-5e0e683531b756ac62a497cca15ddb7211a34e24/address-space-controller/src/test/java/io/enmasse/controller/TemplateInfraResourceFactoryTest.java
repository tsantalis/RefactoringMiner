/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceResolver;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExternalResourceSupport;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ExternalResourceSupport.class)
public class TemplateInfraResourceFactoryTest {
    @Rule
    public OpenShiftServer openShiftServer = new OpenShiftServer(false, true);

    private TemplateInfraResourceFactory resourceFactory;
    private NamespacedOpenShiftClient client;

    @BeforeEach
    public void setup() {
        client = openShiftServer.getOpenshiftClient();
        client.secrets().createNew().editOrNewMetadata().withName("certs").endMetadata().addToData("tls.crt", "cert").done();
        AuthenticationServiceResolver authServiceResolver = mock(AuthenticationServiceResolver.class);
        when(authServiceResolver.getHost(any())).thenReturn("example.com");
        when(authServiceResolver.getPort(any())).thenReturn(5671);
        when(authServiceResolver.getCaSecretName(any())).thenReturn(Optional.of("certs"));
        resourceFactory = new TemplateInfraResourceFactory(
                new KubernetesHelper("test",
                        client,
                        client.getConfiguration().getOauthToken(),
                        new File("src/test/resources/templates"),
                        true),
                a -> authServiceResolver,
                true);
    }

    @Test
    public void testGenerateStandard() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("myproject")
                .putAnnotation(AnnotationKeys.INFRA_UUID, "1234")
                .setType("standard")
                .setPlan("standard-unlimited")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("messaging")
                        .setService("messaging")
                        .setCertSpec(new CertSpec("selfsigned", "messaging-secret", null, null))
                        .build())
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("console")
                        .setService("console")
                        .setCertSpec(new CertSpec("selfsigned", "console-secret", null, null))
                        .build())
                .build();

        StandardInfraConfig infraConfig = new StandardInfraConfigBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("test")
                        .build())
                .withNewSpec()
                .withVersion("master")
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources("2Mi")
                        .build())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withNewResources("2Mi", "1Gi")
                        .withAddressFullPolicy("FAIL")
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withNewResources("2Mi")
                        .withLinkCapacity(22)
                        .build())
                .endSpec()
                .build();
        List<HasMetadata> items = resourceFactory.createInfraResources(addressSpace, infraConfig);
        assertEquals(1, items.size());
        ConfigMap map = findItem("ConfigMap", "mymap", items);
        assertEquals("FAIL", map.getData().get("key"));
    }

    private <T> T findItem(String kind, String name, List<HasMetadata> items) {
        T found = null;
        for (HasMetadata item : items) {
            if (kind.equals(item.getKind()) && name.equals(item.getMetadata().getName())) {
                found = (T) item;
                break;
            }
        }
        assertNotNull(found);
        return found;
    }
}
