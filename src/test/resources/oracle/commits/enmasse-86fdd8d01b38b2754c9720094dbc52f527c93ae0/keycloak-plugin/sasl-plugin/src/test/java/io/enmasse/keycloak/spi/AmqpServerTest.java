/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.keycloak.spi;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AmqpServerTest {

    @Test
    public void testEmptyGroupsGeneratesEmptyPermissions() {
        AmqpServer amqpServer = new AmqpServer(null, 0, null, false);
        Map<String, String[]> props = amqpServer.getPermissionsFromGroups(Collections.emptySet());
        assertNotNull(props);
        assertTrue(props.isEmpty());

        props = amqpServer.getPermissionsFromGroups(Collections.singleton("foo"));
        assertNotNull(props);
        assertTrue(props.isEmpty());

        props = amqpServer.getPermissionsFromGroups(Collections.singleton("foo_bar"));
        assertNotNull(props);
        assertTrue(props.isEmpty());

        props = amqpServer.getPermissionsFromGroups(Collections.singleton("send"));
        assertNotNull(props);
        assertTrue(props.isEmpty());

    }

    @Test
    public void testMultiplePermissionsForSameAddress() {
        AmqpServer amqpServer = new AmqpServer(null, 0, null, false);
        Map<String, String[]> props = amqpServer.getPermissionsFromGroups(new HashSet<>(Arrays.asList("send_foo", "consume_foo")));
        assertNotNull(props);
        assertEquals(1, props.size());
        assertNotNull(props.get("foo"));
        assertEquals(2, props.get("foo").length);
        assertTrue(Arrays.stream(props.get("foo")).anyMatch(e -> e.equals("send")));
        assertTrue(Arrays.stream(props.get("foo")).anyMatch(e -> e.equals("recv")));
    }



    @Test
    public void testMultiplePermissionsWithUrlEncoding() {
        AmqpServer amqpServer = new AmqpServer(null, 0, null, false);
        Map<String, String[]> props = amqpServer.getPermissionsFromGroups(new HashSet<>(Arrays.asList("send_foo%2Fbar", "view_%66oo%2Fbar", "recv_foo")));
        assertNotNull(props);
        assertEquals(2, props.size());
        assertNotNull(props.get("foo/bar"));
        assertEquals(2, props.get("foo/bar").length);
        assertTrue(Arrays.stream(props.get("foo/bar")).anyMatch(e -> e.equals("send")));
        assertEquals(2, props.get("foo/bar").length);
        assertTrue(Arrays.stream(props.get("foo/bar")).anyMatch(e -> e.equals("view")));

        assertNotNull(props.get("foo"));
        assertEquals(1, props.get("foo").length);
        assertTrue(Arrays.stream(props.get("foo")).anyMatch(e -> e.equals("recv")));
    }
}
