/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.clerezza.rdf.core.access;

import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Collections;
import java.util.PropertyPermission;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.providers.WeightedA;
import org.apache.clerezza.rdf.core.access.providers.WeightedDummy;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

/**
 *
 * @author reto
 */
@RunWith(JUnitPlatform.class)
public class SecurityTest {

    private static final Logger LOGGER = getLogger(SecurityTest.class);

    public SecurityTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
        ////needed to unbind because this is injected with META-INF/services - file
        TcManager.getInstance().unbindWeightedTcProvider(new WeightedA());
        TcManager.getInstance().bindWeightedTcProvider(new WeightedDummy());
        TcManager.getInstance().createGraph(new IRI("http://example.org/ImmutableGraph/alreadyexists"));
        TcManager.getInstance().createGraph(new IRI("http://example.org/read/ImmutableGraph"));
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUp() {
        
        Policy.setPolicy(new Policy() {

            @Override
            public PermissionCollection getPermissions(CodeSource codeSource) {
                PermissionCollection result = new Permissions();
                result.add(new TcPermission("http://example.org/permitted", "read"));
                result.add(new TcPermission("http://example.org/ImmutableGraph/alreadyexists", "readwrite"));
                result.add(new TcPermission("http://example.org/read/ImmutableGraph", "read"));
                result.add(new TcPermission("http://example.org/area/allowed/*", "readwrite"));
                result.add(new TcPermission("urn:x-localinstance:/graph-access.graph", "readwrite"));
                //result.add(new AllPermission());
                result.add(new RuntimePermission("*"));
                result.add(new ReflectPermission("suppressAccessChecks"));
                result.add(new PropertyPermission("*", "read"));
                //(java.util.PropertyPermission line.separator read)
                result.add(new FilePermission("/-", "read,write"));
                return result;
            }
        });
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                LOGGER.debug("Checking {}", perm);
                super.checkPermission(perm);
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                LOGGER.debug("Checking {}", perm);
                super.checkPermission(perm, context);
            }

        });
    }

    @AfterEach
    public void tearDown() {
        System.setSecurityManager(null);
    }


    @Test
    public void testAcessGraph() {
        assertThrows(NoSuchEntityException.class, () ->
                TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/permitted")));
    }

    @Test
    public void testNoWildCard() {
        assertThrows(AccessControlException.class, () ->
                TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/permitted/subthing")));
    }

    @Test
    public void testAllowedArea() {
        assertThrows(NoSuchEntityException.class, () ->
                TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/area/allowed/something")));
    }

    @Test
    public void testAcessForbiddenGraph() {
        assertThrows(AccessControlException.class, () ->
                TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/forbidden")));
    }

    @Test
    public void testCustomPermissions() {
        IRI graphUri = new IRI("http://example.org/custom");
        TcManager.getInstance().getTcAccessController().setRequiredReadPermissionStrings(graphUri,
                Collections.singletonList("(java.io.FilePermission \"/etc\" \"write\")"));
        //new FilePermission("/etc", "write").toString()));
        Graph ag = TcManager.getInstance().getGraph(new IRI("urn:x-localinstance:/graph-access.graph"));

        LOGGER.info("Custom permissions graph: {}", ag);
        assertThrows(NoSuchEntityException.class, () ->
                TcManager.getInstance().getMGraph(graphUri));
    }

    @Test
    public void testCustomPermissionsIncorrect() {
        IRI graphUri = new IRI("http://example.org/custom");
        TcManager.getInstance().getTcAccessController().setRequiredReadPermissionStrings(graphUri,
                Collections.singletonList("(java.io.FilePermission \"/etc\" \"write\")"));
        //new FilePermission("/etc", "write").toString()));
        Graph ag = TcManager.getInstance().getGraph(new IRI("urn:x-localinstance:/graph-access.graph"));

        LOGGER.info("Incorrect custom permissions graph: {}", ag);
        assertThrows(AccessControlException.class, () ->
                TcManager.getInstance().createGraph(graphUri));
    }

    @Test
    public void testCustomReadWritePermissions() {
        IRI graphUri = new IRI("http://example.org/read-write-custom");
        TcManager.getInstance().getTcAccessController().setRequiredReadWritePermissionStrings(graphUri,
                Collections.singletonList("(java.io.FilePermission \"/etc\" \"write\")"));
        //new FilePermission("/etc", "write").toString()));
        Graph ag = TcManager.getInstance().getGraph(new IRI("urn:x-localinstance:/graph-access.graph"));

        LOGGER.info("Custom read/write permissions graph: {}", ag);
        TcManager.getInstance().createGraph(graphUri);
    }

    @Test
    public void testCreateMGraph() {
        assertThrows(EntityAlreadyExistsException.class, () ->
                TcManager.getInstance().createGraph(new IRI("http://example.org/ImmutableGraph/alreadyexists")));
    }

    @Test
    public void testCreateMGraphWithoutWritePermission() {
        assertThrows(AccessControlException.class, () ->
                TcManager.getInstance().createGraph(new IRI("http://example.org/read/ImmutableGraph")));
    }

    @Test
    public void testAddTripleToMGraph() {
        Graph graph = TcManager.getInstance().getMGraph(new IRI("http://example.org/read/ImmutableGraph"));
        Triple triple = new TripleImpl(new IRI("http://example.org/definition/isNonLiteral"), new IRI("http://example.org/definition/isTest"), new PlainLiteralImpl("test"));
        assertThrows(ReadOnlyException.class, () -> graph.add(triple));
    }
}
