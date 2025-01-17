/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class ResourceCheckerTest {
    ResourceChecker<String> controller;
    Watcher<String> watcher;

    @BeforeEach
    public void setup() {
        watcher = mock(Watcher.class);
        controller = new ResourceChecker<>(watcher, Duration.ofMillis(1));
    }

    @Test
    public void testResourcesUpdated() throws Exception {
        controller.doWork();
        verify(watcher, never()).onUpdate(any());

        List<String> items = Arrays.asList("hello", "there");
        controller.onInit(() -> items);
        controller.onUpdate();
        controller.doWork();
        verify(watcher).onUpdate(eq(items));
    }
}
