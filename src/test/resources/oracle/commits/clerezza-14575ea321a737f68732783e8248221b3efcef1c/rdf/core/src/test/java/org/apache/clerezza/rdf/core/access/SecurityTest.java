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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.providers.WeightedA;
import org.apache.clerezza.rdf.core.access.providers.WeightedDummy;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;

/**
 *
 * @author reto
 */
public class SecurityTest {
    
    public SecurityTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        ////needed to unbind because this is injected with META-INF/services - file
        TcManager.getInstance().unbindWeightedTcProvider(new WeightedA());
        TcManager.getInstance().bindWeightedTcProvider(new WeightedDummy());
        TcManager.getInstance().createGraph(new IRI("http://example.org/ImmutableGraph/alreadyexists"));
        TcManager.getInstance().createGraph(new IRI("http://example.org/read/ImmutableGraph"));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
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
                //System.out.println("Checking "+perm);
                super.checkPermission(perm);
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                //System.out.println("Checking "+perm);
                super.checkPermission(perm, context);
            }

        });
    }

    @After
    public void tearDown() {
        System.setSecurityManager(null);
    }


    @Test(expected=NoSuchEntityException.class)
    public void testAcessGraph() {        
        TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/permitted"));
    }
    
    @Test(expected=AccessControlException.class)
    public void testNoWildCard() {
        TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/permitted/subthing"));
    }
    
    @Test(expected=NoSuchEntityException.class)
    public void testAllowedArea() {
        TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/area/allowed/something"));
    }
    
    @Test(expected=AccessControlException.class)
    public void testAcessForbiddenGraph() {
        TcManager.getInstance().getImmutableGraph(new IRI("http://example.org/forbidden"));
    }

    @Test(expected=NoSuchEntityException.class)
    public void testCustomPermissions() {
        IRI graphUri = new IRI("http://example.org/custom");
        TcManager.getInstance().getTcAccessController().setRequiredReadPermissionStrings(graphUri,
                Collections.singletonList("(java.io.FilePermission \"/etc\" \"write\")"));
        //new FilePermission("/etc", "write").toString()));
        Graph ag = TcManager.getInstance().getGraph(new IRI("urn:x-localinstance:/graph-access.graph"));
        System.out.print(ag.toString());
        TcManager.getInstance().getMGraph(graphUri);
    }

    @Test(expected=AccessControlException.class)
    public void testCustomPermissionsIncorrect() {
        IRI graphUri = new IRI("http://example.org/custom");
        TcManager.getInstance().getTcAccessController().setRequiredReadPermissionStrings(graphUri,
                Collections.singletonList("(java.io.FilePermission \"/etc\" \"write\")"));
        //new FilePermission("/etc", "write").toString()));
        Graph ag = TcManager.getInstance().getGraph(new IRI("urn:x-localinstance:/graph-access.graph"));
        System.out.print(ag.toString());
        TcManager.getInstance().createGraph(graphUri);
    }

    @Test
    public void testCustomReadWritePermissions() {
        IRI graphUri = new IRI("http://example.org/read-write-custom");
        TcManager.getInstance().getTcAccessController().setRequiredReadWritePermissionStrings(graphUri,
                Collections.singletonList("(java.io.FilePermission \"/etc\" \"write\")"));
        //new FilePermission("/etc", "write").toString()));
        Graph ag = TcManager.getInstance().getGraph(new IRI("urn:x-localinstance:/graph-access.graph"));
        System.out.print(ag.toString());
        TcManager.getInstance().createGraph(graphUri);
    }
    
    @Test(expected=EntityAlreadyExistsException.class)
    public void testCreateMGraph() {
        TcManager.getInstance().createGraph(new IRI("http://example.org/ImmutableGraph/alreadyexists"));
    }
    @Test(expected=AccessControlException.class)
    public void testCreateMGraphWithoutWritePermission() {
        TcManager.getInstance().createGraph(new IRI("http://example.org/read/ImmutableGraph"));
    }
    @Test(expected=ReadOnlyException.class)
    public void testAddTripleToMGraph() {
        Graph graph = TcManager.getInstance().getMGraph(new IRI("http://example.org/read/ImmutableGraph"));
        Triple triple = new TripleImpl(new IRI("http://example.org/definition/isNonLiteral"), new IRI("http://example.org/definition/isTest"), new PlainLiteralImpl("test"));
        graph.add(triple);
    }
}