/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.ExposeSpec;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExternalResourceSupport;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ExternalResourceSupport.class)
public class EndpointControllerTest {

    private OpenShiftClient client;

    @Rule
    public OpenShiftServer openShiftServer = new OpenShiftServer(false, true);


    @BeforeEach
    public void setup() {
        client = openShiftServer.getOpenshiftClient();
    }

    @Test
    public void testRoutesNotCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .putAnnotation(AnnotationKeys.INFRA_UUID, "1234")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .build())
                .setType("type1")
                .setPlan("myplan")
                .build();


        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging-1234")
                .addToAnnotations(AnnotationKeys.SERVICE_PORT_PREFIX + "amqps", "5671")
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("amqps")
                .withPort(1234)
                .withNewTargetPort("amqps")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();

        client.services().create(service);

        EndpointController controller = new EndpointController(client, false);

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging-1234.test.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertNull(newspace.getStatus().getEndpointStatuses().get(0).getExternalHost());
        assertTrue(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().isEmpty());
    }

    @Test
    public void testExternalLoadBalancerCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .putAnnotation(AnnotationKeys.INFRA_UUID, "1234")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setExposeSpec(new ExposeSpec.Builder()
                                .setType(ExposeSpec.ExposeType.loadbalancer)
                                .setLoadBalancerPorts(Arrays.asList("amqps"))
                                .build())
                        .build())
                .setType("type1")
                .setPlan("myplan")
                .build();


        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging-1234")
                .addToAnnotations(AnnotationKeys.SERVICE_PORT_PREFIX + "amqps", "5671")
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("amqps")
                .withPort(1234)
                .withNewTargetPort("amqps")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();

        client.services().create(service);

        EndpointController controller = new EndpointController(client, true);

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging-1234.test.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().size(), is(1));
    }

    @Test
    public void testExternalRouteCreated() {
        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setNamespace("mynamespace")
                .putAnnotation(AnnotationKeys.INFRA_UUID, "1234")
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("myendpoint")
                        .setService("messaging")
                        .setExposeSpec(new ExposeSpec.Builder()
                                .setType(ExposeSpec.ExposeType.route)
                                .setRouteHost("host1.example.com")
                                .setRouteServicePort("amqps")
                                .setRouteTlsTermination(ExposeSpec.TlsTermination.passthrough)
                                .build())
                        .build())
                .setType("type1")
                .setPlan("myplan")
                .build();


        Service service = new ServiceBuilder()
                .editOrNewMetadata()
                .withName("messaging-1234")
                .addToAnnotations(AnnotationKeys.SERVICE_PORT_PREFIX + "amqps", "5671")
                .addToLabels(LabelKeys.INFRA_UUID, "1234")
                .endMetadata()
                .editOrNewSpec()
                .addNewPort()
                .withName("amqps")
                .withPort(1234)
                .withNewTargetPort("amqps")
                .endPort()
                .addToSelector("component", "router")
                .endSpec()
                .build();

        client.services().create(service);

        EndpointController controller = new EndpointController(client, true);

        AddressSpace newspace = controller.handle(addressSpace);

        assertThat(newspace.getStatus().getEndpointStatuses().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getName(), is("myendpoint"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServiceHost(), is("messaging-1234.test.svc"));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getServicePorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().size(), is(1));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalPorts().get("amqps"), is(443));
        assertThat(newspace.getStatus().getEndpointStatuses().get(0).getExternalHost(), is("host1.example.com"));
    }
}
