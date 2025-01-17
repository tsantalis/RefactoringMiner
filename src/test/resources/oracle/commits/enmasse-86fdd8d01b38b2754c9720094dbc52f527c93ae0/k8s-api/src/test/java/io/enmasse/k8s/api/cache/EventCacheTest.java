/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventCacheTest {
    @Test
    public void testAdd() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.add(map("k1"));
        assertTrue(queue.hasSynced());
        assertFalse(queue.listKeys().contains("k1"));

        Processor<ConfigMap> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("k1")));
        assertTrue(queue.listKeys().contains("k1"));
        assertTrue(queue.list().contains(map("k1")));
    }

    @Test
    public void testUpdate() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.update(map("k1"));
        assertFalse(queue.listKeys().contains("k1"));
        assertFalse(queue.list().contains(map("k1")));

        Processor<ConfigMap> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("k1")));
        assertTrue(queue.listKeys().contains("k1"));
        assertTrue(queue.list().contains(map("k1")));
    }

    @Test
    public void testRemove() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.add(map("k1"));
        queue.delete(map("k1"));
        assertTrue(queue.hasSynced());
        assertTrue(queue.listKeys().isEmpty());

        Processor<ConfigMap> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("k1")));
        assertTrue(queue.listKeys().isEmpty());
        assertTrue(queue.list().isEmpty());

        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("k1")));
        assertTrue(queue.listKeys().isEmpty());
        assertTrue(queue.list().isEmpty());
    }

    @Test
    public void testEmpty() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        Processor<ConfigMap> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verifyZeroInteractions(mockProc);
        assertFalse(queue.hasSynced());
    }

    @Test
    public void testSync() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.replace(Arrays.asList(map("k1"), map("k2"), map("k3")), "33");
        assertFalse(queue.hasSynced());
        assertFalse(queue.list().isEmpty());

        Processor<ConfigMap> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(null);
        assertTrue(queue.hasSynced());
    }

    public static ConfigMap map(String name) {
        return new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(name)
                .endMetadata()
                .build();
    }
}
