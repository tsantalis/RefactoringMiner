/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.ConfigMapListBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ReflectorTest {
    Reflector<ConfigMap, ConfigMapList> reflector;
    ListerWatcher<ConfigMap, ConfigMapList> testLister;
    WorkQueue<ConfigMap> testStore;
    Processor<ConfigMap> testProc;

    @BeforeEach
    public void setup() {
        testLister = mock(ListerWatcher.class);
        testProc = mock(Processor.class);
        testStore = new EventCache<>(new HasMetadataFieldExtractor<>());

        Reflector.Config<ConfigMap, ConfigMapList> config = new Reflector.Config<>();
        config.setClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        config.setListerWatcher(testLister);
        config.setWorkQueue(testStore);
        config.setExpectedType(ConfigMap.class);
        config.setResyncInterval(Duration.ofSeconds(1));
        config.setProcessor(testProc);
        reflector = new Reflector<>(config);
    }

    @Test
    public void testReflector() throws Exception {
        when(testLister.list(any())).thenReturn(new ConfigMapListBuilder()
                .editOrNewMetadata()
                .withResourceVersion("3")
                .endMetadata()
                .addToItems(configMap("a1", "a2", "1"))
                .addToItems(configMap("a1", "a3", "3"))
                .addToItems(configMap("b1", "b2", "2"))
                .build());

        ArgumentCaptor<io.fabric8.kubernetes.client.Watcher<ConfigMap>> captor = ArgumentCaptor.forClass(io.fabric8.kubernetes.client.Watcher.class);

        reflector.run();
        verify(testProc, times(1)).process(any());
        reflector.run();
        verify(testProc, times(1)).process(any());
        assertStoreSize(2);
        verify(testLister).watch(captor.capture(), any());
        verify(testLister).list(any());

        assertConfigMap("a1", "a3");
        assertConfigMap("b1", "b2");

        io.fabric8.kubernetes.client.Watcher watcher = captor.getValue();
        watcher.eventReceived(io.fabric8.kubernetes.client.Watcher.Action.MODIFIED, configMap("a1", "a4", "5"));
        reflector.run();
        assertStoreSize(2);
        assertConfigMap("a1", "a4");
        assertConfigMap("b1", "b2");
        watcher.eventReceived(io.fabric8.kubernetes.client.Watcher.Action.ADDED, configMap("c1", "c4", "5"));
        reflector.run();
        assertStoreSize(3);
        assertConfigMap("a1", "a4");
        assertConfigMap("b1", "b2");
        assertConfigMap("c1", "c4");
        watcher.eventReceived(io.fabric8.kubernetes.client.Watcher.Action.DELETED, configMap("b1", "b2", "6"));
        reflector.run();
        assertStoreSize(2);
        assertConfigMap("a1", "a4");
        assertConfigMap("c1", "c4");

        reflector.run();
        reflector.run();
        reflector.run();
        reflector.run();
        verify(testProc, times(4)).process(any());
        reflector.run();
        verify(testProc, times(4)).process(any());
    }

    public void assertStoreSize(int expectedSize) throws InterruptedException {
        assertThat("Store contains " + testStore.listKeys(), testStore.listKeys().size(), is(expectedSize));
    }

    private void assertConfigMap(String name, String expectedValue) throws InterruptedException {
        String actual = findValue(testStore.list(), name);
        assertNotNull(actual);
        assertThat(actual, is(expectedValue));
    }

    private static String findValue(List<ConfigMap> list, String name) {
        String found = null;
        for (ConfigMap map : list) {
            if (map.getMetadata().getName().equals(name)) {
                found = map.getData().get("data");
                break;
            }
        }
        return found;
    }

    private static ConfigMap configMap(String name, String data, String resourceVersion) {
        return new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withResourceVersion(resourceVersion)
                .endMetadata()
                .withData(Collections.singletonMap("data", data))
                .build();
    }
}
