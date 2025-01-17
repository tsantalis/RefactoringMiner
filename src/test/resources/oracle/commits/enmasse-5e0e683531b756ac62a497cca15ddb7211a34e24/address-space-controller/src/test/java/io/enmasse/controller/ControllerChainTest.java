/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceStatus;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.metrics.api.Metric;
import io.enmasse.metrics.api.Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ControllerChainTest {
    private TestAddressSpaceApi testApi;
    private Kubernetes kubernetes;

    @BeforeEach
    public void setup() {
        kubernetes = mock(Kubernetes.class);
        testApi = new TestAddressSpaceApi();

        when(kubernetes.getNamespace()).thenReturn("myspace");
    }

    @Test
    public void testController() throws Exception {
        EventLogger testLogger = mock(EventLogger.class);
        Metrics metrics = new Metrics();
        ControllerChain controllerChain = new ControllerChain(kubernetes, testApi, new TestSchemaProvider(), testLogger, metrics, "1.0", Duration.ofSeconds(5), Duration.ofSeconds(5));
        Controller mockController = mock(Controller.class);
        controllerChain.addController(mockController);

        AddressSpace a1 = new AddressSpace.Builder()
                .setName("myspace")
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new AddressSpaceStatus(false))
                .build();

        AddressSpace a2 = new AddressSpace.Builder()
                .setName("myspace2")
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new AddressSpaceStatus(false))
                .build();

        when(mockController.handle(eq(a1))).thenReturn(a1);
        when(mockController.handle(eq(a2))).thenReturn(a2);

        controllerChain.onUpdate(Arrays.asList(a1, a2));

        verify(mockController, times(2)).handle(any());
        verify(mockController).handle(eq(a1));
        verify(mockController).handle(eq(a2));

        List<Metric> metricList = metrics.snapshot();
        assertThat(metricList.size(), is(5));
        assertTrue(a1.getStatus().isReady());
        assertTrue(a2.getStatus().isReady());
    }
}

