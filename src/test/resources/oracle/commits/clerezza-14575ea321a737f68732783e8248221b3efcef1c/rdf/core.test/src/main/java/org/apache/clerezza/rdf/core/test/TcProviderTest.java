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
package org.apache.clerezza.rdf.core.test;

import java.util.HashSet;
import java.util.Iterator;

import java.util.Set;
import org.junit.Test;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;

import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import static org.junit.Assert.*;

/**
 * 
 * @author mir,rbn
 */
public abstract class TcProviderTest {

    protected final IRI uriRefA = generateUri("a");
    protected final IRI uriRefA1 = generateUri("a1");
    protected final IRI uriRefB = generateUri("b");
    protected final IRI uriRefB1 = generateUri("b1");
    protected final IRI uriRefC = generateUri("c");

    protected final IRI graphIRI = generateUri("myGraph");
    protected final IRI otherGraphIRI = new IRI(graphIRI.getUnicodeString());

    @Test
    public void testCreateImmutableGraph() {
        TcProvider simpleTcmProvider = getInstance();
        Graph mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefA, uriRefA, uriRefA));

        ImmutableGraph createdGraph = simpleTcmProvider.createImmutableGraph(uriRefA, mGraph);

        Iterator<Triple> iteratorInput = mGraph.iterator();
        Iterator<Triple> iteratorCreated = createdGraph.iterator();
        assertEquals(iteratorInput.next(), iteratorCreated.next());
        assertFalse(iteratorCreated.hasNext());

        try {
            simpleTcmProvider.createImmutableGraph(uriRefA, mGraph);
            assertTrue(false);
        } catch (EntityAlreadyExistsException e) {
            assertTrue(true);
        }
        simpleTcmProvider.deleteGraph(uriRefA);
    }

    @Test
    public void testCreateGraph() {
        TcProvider simpleTcmProvider = getInstance();
        Graph mGraph = simpleTcmProvider.createGraph(uriRefA);
        assertTrue(mGraph.isEmpty());

        try {
            simpleTcmProvider.createGraph(uriRefA);
            assertTrue(false);
        } catch (EntityAlreadyExistsException e) {
            assertTrue(true);
        }
        simpleTcmProvider.deleteGraph(uriRefA);
    }

    @Test
    public void testGetImmutableGraph() {
        TcProvider simpleTcmProvider = getInstance();
        // add Graphs
        Graph mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefA, uriRefA, uriRefA));
        simpleTcmProvider.createImmutableGraph(uriRefA, mGraph);
        mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefA1, uriRefA1, uriRefA1));
        simpleTcmProvider.createImmutableGraph(uriRefA1, mGraph);
        mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefB, uriRefB, uriRefB));
        simpleTcmProvider.createImmutableGraph(uriRefB, mGraph);
        mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefB1, uriRefB1, uriRefB1));
        simpleTcmProvider.createImmutableGraph(uriRefB1, mGraph);

        ImmutableGraph bGraph = simpleTcmProvider.getImmutableGraph(uriRefB);
        Iterator<Triple> iterator = bGraph.iterator();
        assertEquals(new TripleImpl(uriRefB, uriRefB, uriRefB), iterator.next());
        assertFalse(iterator.hasNext());
        simpleTcmProvider.deleteGraph(uriRefA);
        simpleTcmProvider.deleteGraph(uriRefA1);
        simpleTcmProvider.deleteGraph(uriRefB);
        simpleTcmProvider.deleteGraph(uriRefB1);
    }

    @Test
    public void testGetGraph() {
        TcProvider simpleTcmProvider = getInstance();
        // add Graphs
        Graph mGraph = simpleTcmProvider.createGraph(uriRefA);
        mGraph.add(new TripleImpl(uriRefA, uriRefA, uriRefA));
        mGraph = simpleTcmProvider.createGraph(uriRefA1);
        mGraph.add(new TripleImpl(uriRefA1, uriRefA1, uriRefA1));
        mGraph = simpleTcmProvider.createGraph(uriRefB);
        mGraph.add(new TripleImpl(uriRefB, uriRefB, uriRefA));
        mGraph.add(new TripleImpl(uriRefB, uriRefB, uriRefB));
        mGraph.remove(new TripleImpl(uriRefB, uriRefB, uriRefA));
        assertEquals(1, mGraph.size());
        mGraph = simpleTcmProvider.createGraph(uriRefB1);
        mGraph.add(new TripleImpl(uriRefB1, uriRefB1, uriRefB1));

        Graph bGraph = simpleTcmProvider.getGraph(uriRefB);
        Iterator<Triple> iterator = bGraph.iterator();
        assertEquals(new TripleImpl(uriRefB, uriRefB, uriRefB), iterator.next());
        assertFalse(iterator.hasNext());
        simpleTcmProvider.deleteGraph(uriRefA);
        simpleTcmProvider.deleteGraph(uriRefA1);
        simpleTcmProvider.deleteGraph(uriRefB);
        simpleTcmProvider.deleteGraph(uriRefB1);
        
    }

    @Test
    public void testGetTriples() {
        TcProvider simpleTcmProvider = getInstance();
        // add Graphs
        Graph mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefA, uriRefA, uriRefA));
        simpleTcmProvider.createImmutableGraph(uriRefA, mGraph);
        mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefB, uriRefB, uriRefB));
        simpleTcmProvider.createImmutableGraph(uriRefB, mGraph);
        // add Graphs
        mGraph = simpleTcmProvider.createGraph(uriRefA1);
        mGraph.add(new TripleImpl(uriRefA1, uriRefA1, uriRefA1));
        mGraph = simpleTcmProvider.createGraph(uriRefB1);
        mGraph.add(new TripleImpl(uriRefB1, uriRefB1, uriRefB1));

        // get a ImmutableGraph
        Graph tripleCollection = simpleTcmProvider.getGraph(uriRefA);
        // get a Graph
        Graph tripleCollection2 = simpleTcmProvider.getGraph(uriRefB1);

        Iterator<Triple> iterator = tripleCollection.iterator();
        assertEquals(new TripleImpl(uriRefA, uriRefA, uriRefA), iterator.next());
        assertFalse(iterator.hasNext());

        iterator = tripleCollection2.iterator();
        assertEquals(new TripleImpl(uriRefB1, uriRefB1, uriRefB1), iterator.next());
        assertFalse(iterator.hasNext());
        simpleTcmProvider.deleteGraph(uriRefA);
        simpleTcmProvider.deleteGraph(uriRefA1);
        simpleTcmProvider.deleteGraph(uriRefB);
        simpleTcmProvider.deleteGraph(uriRefB1);
    }

    @Test
    public void testDeleteEntity() {
        TcProvider simpleTcmProvider = getInstance();
        Graph mGraph = new SimpleGraph();
        mGraph.add(new TripleImpl(uriRefA, uriRefA, uriRefA));
        ImmutableGraph graph = mGraph.getImmutableGraph();
        simpleTcmProvider.createImmutableGraph(uriRefA, graph);
        simpleTcmProvider.createImmutableGraph(uriRefC, graph);

        simpleTcmProvider.deleteGraph(uriRefA);
        try {
            simpleTcmProvider.getGraph(uriRefA);
            assertTrue(false);
        } catch (NoSuchEntityException e) {
            assertTrue(true);
        }

        // Check that graph is still available under uriRefC
        ImmutableGraph cGraph = simpleTcmProvider.getImmutableGraph(uriRefC);
        assertNotNull(cGraph);
        simpleTcmProvider.deleteGraph(uriRefC);
    }

    /**
     * Subclasses implement this method to provide implementation instances of
     * <code>TcProvider</code>. The first call within a test method has to
     * return a empty TcProvider. Subsequent calls within the test method
     * should instantiate a new provider, but load the previously added data from
     * its "persistent" store.
     *
     * @return a TcProvider of the implementation to be tested.
     */
    protected abstract TcProvider getInstance();

//    @Test
//    public void testGetNames() {
//        Graph mGraph = new SimpleGraph();
//        mGraph.add(new TripleImpl(uriRefB, uriRefB, uriRefB));
//        simpleTcmProvider.createGraph(uriRefB, mGraph.getGraph());
//        
//        mGraph = new SimpleGraph();
//        mGraph.add(new TripleImpl(uriRefA, uriRefA, uriRefA));
//        ImmutableGraph graph = mGraph.getGraph();
//        simpleTcmProvider.createGraph(uriRefA, graph);
//        simpleTcmProvider.createGraph(uriRefC, graph);
//
//        Set<IRI> names = simpleTcmProvider.getNames(graph);
//
//        assertTrue(names.contains(uriRefA));
//        assertTrue(names.contains(uriRefC));
//        assertEquals(2, names.size());
//
//        assertFalse(names.contains(uriRefB));
//    }

    @Test
    public void testCreateGraphExtended() throws Exception {

        TcProvider provider = getInstance();
        Graph graph = provider.createGraph(graphIRI);
        assertNotNull(graph);
        //get a new provider and check that graph is there
        provider = getInstance();
        graph = provider.getGraph(graphIRI);
        assertNotNull(graph);
        //check that there is no such graph, but only the mgraph
        boolean expThrown = false;
        try {
            ImmutableGraph g = provider.getImmutableGraph(graphIRI);
        } catch(NoSuchEntityException e) {
            expThrown = true;
        }

        assertTrue(expThrown);
        provider.deleteGraph(graphIRI);
    }

    @Test
    public void testCreateImmutableGraphExtended() throws Exception {

        TcProvider provider = getInstance();
        ImmutableGraph graph = provider.createImmutableGraph(graphIRI, null);

        assertNotNull(graph);

        //get a new provider and check that graph is there
        provider = getInstance();
        graph = provider.getImmutableGraph(graphIRI);
        assertNotNull(graph);

        //check that there is no such mgraph, but only the graph
        boolean expThrown = false;

        try {
            Graph g = provider.getMGraph(graphIRI);
        } catch(NoSuchEntityException e) {
            expThrown = true;
        }

        assertTrue(expThrown);
        provider.deleteGraph(graphIRI);
    }

    @Test
    public void testCreateGraphNoDuplicateNames() throws Exception {

        TcProvider provider = getInstance();
        ImmutableGraph graph = provider.createImmutableGraph(graphIRI, null);
        assertNotNull(graph);
        boolean expThrown = false;
        try {
            ImmutableGraph other = provider.createImmutableGraph(otherGraphIRI, null);
        } catch(EntityAlreadyExistsException eaee) {
            expThrown = true;
        }
        assertTrue(expThrown);
        provider.deleteGraph(graphIRI);
    }

    @Test
    public void testCreateGraphNoDuplicateNames2() throws Exception {

        TcProvider provider = getInstance();
        Graph graph = provider.createGraph(graphIRI);
        assertNotNull(graph);
        boolean expThrown = false;
        try {
            Graph other = provider.createGraph(otherGraphIRI);
        } catch(EntityAlreadyExistsException eaee) {
            expThrown = true;
        }
        assertTrue(expThrown);
        provider.deleteGraph(graphIRI);
    }

    @Test
    public void testCreateGraphWithInitialCollection() throws Exception {

        Triple t1 = createTestTriple();

        TcProvider provider = getInstance();

        ImmutableGraph graph = provider.createImmutableGraph(graphIRI, createTestTripleCollection(t1));

        assertEquals(1, graph.size());
        assertTrue(graph.contains(t1));
        provider.deleteGraph(graphIRI);
    }

    @Test
    public void testGraphIsNotMutable() throws Exception {

        Triple t1 = createTestTriple();
        Set<Triple> t = new HashSet<Triple>();
        t.add(t1);

        TcProvider provider = getInstance();

        ImmutableGraph graph = provider.createImmutableGraph(graphIRI, createTestTripleCollection(t1));

        boolean expThrown = false;

        try {
            graph.add(t1);
        } catch(UnsupportedOperationException uoe) {
            expThrown = true;
        }

        assertTrue(expThrown);
        expThrown = false;

        try {
            graph.remove(t1);
        } catch(UnsupportedOperationException uoe) {
            expThrown = true;
        }

        assertTrue(expThrown);
        expThrown = false;

        try {
            graph.addAll(t);
        } catch(UnsupportedOperationException uoe) {
            expThrown = true;
        }

        assertTrue(expThrown);

        expThrown = false;

        try {
            graph.clear();
        } catch(UnsupportedOperationException uoe) {
            expThrown = true;
        }

        assertTrue(expThrown);

        expThrown = false;

        try {
            graph.removeAll(t);
        } catch(UnsupportedOperationException uoe) {
            expThrown = true;
        }

        assertTrue(expThrown);
        provider.deleteGraph(graphIRI);
    }

//    This tests can not pass, because equals in AbstractGraph is not implemented
//    yet.
//    @Test
//    public void testGraphHasName() throws Exception {
//
//        TcProvider provider = getInstance();
//
//        Graph triples = createTestTripleCollection(createTestTriple());
//        ImmutableGraph graph = provider.createGraph(graphIRI, triples);
//
//        provider = getInstance();
//        Set<IRI> names = provider.getNames(graph);
//        assertTrue(names.contains(graphIRI));
//    }
//
//    @Test
//    public void testCreateSameGraphWithDifferentNames() throws Exception {
//
//        Graph triples = createTestTripleCollection(createTestTriple());
//
//        TcProvider provider = getInstance();
//        IRI name1 = new IRI("http://myGraph1");
//        ImmutableGraph graph = provider.createGraph(name1, triples);
//
//        IRI name2 = new IRI("http://myGraph2");
//        ImmutableGraph secondGraph = provider.createGraph(name2, triples);
//
//        Set<IRI> names = provider.getNames(graph);
//        assertNotNull(names);
//        assertEquals(2, names.size());
//    }

    @Test
    public void testGraphDeletion() throws Exception {

        Graph triples = createTestTripleCollection(createTestTriple());

        TcProvider provider = getInstance();
        IRI name1 = new IRI("http://myGraph1");
        ImmutableGraph graph = provider.createImmutableGraph(name1, triples);

        IRI name2 = new IRI("http://myGraph2");
        ImmutableGraph secondGraph = provider.createImmutableGraph(name2, triples);

        //if we delete graph with name1, the second graph should still be there
        provider.deleteGraph(name1);

        provider = getInstance();
        ImmutableGraph firstGraph = provider.getImmutableGraph(name2);
        assertNotNull(firstGraph);

        //check second name is not there
        boolean expThrown = false;

        try {
            ImmutableGraph g = provider.getImmutableGraph(name1);
        } catch(NoSuchEntityException nses) {
            expThrown = true;
        }

        assertTrue(expThrown);
        provider.deleteGraph(name2);
    }



    @Test
    public void testGetTriplesGraph() throws Exception {
        TcProvider provider = getInstance();

        Graph graph = provider.createGraph(graphIRI);

        Graph tc = provider.getGraph(graphIRI);
        assertNotNull(tc);
        provider.deleteGraph(graphIRI);
    }

    private Triple createTestTriple() {
        BlankNodeOrIRI subject = new BlankNode() {};
        IRI predicate = new IRI("http://test.com/");
        BlankNodeOrIRI object = new IRI("http://test.com/myObject");
        return new TripleImpl(subject, predicate, object);
    }

    private Graph createTestTripleCollection(Triple t) {
        Set<Triple> ts = new HashSet<Triple>();
        ts.add(t);
        return new SimpleGraph(ts);
    }

    protected IRI generateUri(String name) {
        return new IRI("http://example.org/" + name);
    }
    
}
