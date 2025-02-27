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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.junit.Test;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;
import org.junit.Assert;



/**
 * A generic abstract test class, implementations overwrite this class,
 * providing an implementation of the getEmptyGraph method.
 *
 * @author reto, szalay, mir, hhn
 */
public abstract class GraphTest {

    private final IRI uriRef1 =
            new IRI("http://example.org/ontology#res1");
    private final IRI uriRef2 =
            new IRI("http://example.org/ontology#res2");
    private final IRI uriRef3 =
            new IRI("http://example.org/ontology#res3");
    private final IRI uriRef4 =
            new IRI("http://example.org/ontology#res4");
    private final IRI xmlLiteralType =
            new IRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral");
    private Literal literal1 = new PlainLiteralImpl("literal1");
    private Literal literal2 = new PlainLiteralImpl("literal2");
    private BlankNode bnode1 = new BlankNode();
    private BlankNode bnode2 = new BlankNode();
    private Triple trpl1 = new TripleImpl(uriRef2, uriRef2, literal1);
    private Triple trpl2 = new TripleImpl(uriRef1, uriRef2, uriRef1);
    private Triple trpl3 = new TripleImpl(bnode2, uriRef3, literal2);
    private Triple trpl4 = new TripleImpl(uriRef3, uriRef4, literal2);
    
    /**
     * Subclasses implement this method to provide implementation instances of
     * Graph. This method may be called an arbitrary amount of time,
     * independently whether previously returned Graph are still in use or not.
     *
     * @return an empty Graph of the implementation to be tested
     */
    protected abstract Graph getEmptyGraph();
    
    @Test
    public void testAddCountAndGetTriples() {
        Graph graph = getEmptyGraph();
        Assert.assertEquals(0, graph.size());
        final TripleImpl triple1 = new TripleImpl(uriRef1, uriRef2, uriRef1);
        graph.add(triple1);
        Assert.assertEquals(1, graph.size());
        Iterator<Triple> tripleIter = graph.filter(uriRef1, uriRef2, uriRef1);
        Assert.assertTrue(tripleIter.hasNext());
        Triple tripleGot = tripleIter.next();
        Assert.assertEquals(triple1, tripleGot);
        Assert.assertFalse(tripleIter.hasNext());
        BlankNode bnode = new BlankNode() {};
        graph.add(new TripleImpl(bnode, uriRef1, uriRef3));
        graph.add(new TripleImpl(bnode, uriRef1, uriRef4));
        tripleIter = graph.filter(null, uriRef1, null);
        Set<BlankNodeOrIRI> subjectInMatchingTriples = new HashSet<BlankNodeOrIRI>();
        Set<RDFTerm> objectsInMatchingTriples = new HashSet<RDFTerm>();
        while (tripleIter.hasNext()) {
            Triple triple = tripleIter.next();
            subjectInMatchingTriples.add(triple.getSubject());
            objectsInMatchingTriples.add(triple.getObject());
        }
        Assert.assertEquals(1, subjectInMatchingTriples.size());
        Assert.assertEquals(2, objectsInMatchingTriples.size());
        Set<RDFTerm> expectedObjects = new HashSet<RDFTerm>();
        expectedObjects.add(uriRef3);
        expectedObjects.add(uriRef4);
        Assert.assertEquals(expectedObjects, objectsInMatchingTriples);
        graph.add(new TripleImpl(bnode, uriRef4, bnode));
        tripleIter = graph.filter(null, uriRef4, null);
        Assert.assertTrue(tripleIter.hasNext());
        Triple retrievedTriple = tripleIter.next();
        Assert.assertFalse(tripleIter.hasNext());
        Assert.assertEquals(retrievedTriple.getSubject(), retrievedTriple.getObject());
        tripleIter = graph.filter(uriRef1, uriRef2, null);
        Assert.assertTrue(tripleIter.hasNext());
        retrievedTriple = tripleIter.next();
        Assert.assertFalse(tripleIter.hasNext());
        Assert.assertEquals(retrievedTriple.getSubject(), retrievedTriple.getObject());
    }
    
    @Test
    public void testRemoveAllTriples() {
        Graph graph = getEmptyGraph();
        Assert.assertEquals(0, graph.size());
        graph.add(new TripleImpl(uriRef1, uriRef2, uriRef3));
        graph.add(new TripleImpl(uriRef2, uriRef3, uriRef4));
        Assert.assertEquals(2, graph.size());
        graph.clear();
        Assert.assertEquals(0, graph.size());
    }

    @Test
    public void testUseTypedLiterals() {
        Graph graph = getEmptyGraph();
        Assert.assertEquals(0, graph.size());
        Literal value = new TypedLiteralImpl("<elem>value</elem>",xmlLiteralType);
        final TripleImpl triple1 = new TripleImpl(uriRef1, uriRef2, value);
        graph.add(triple1);
        Iterator<Triple> tripleIter = graph.filter(uriRef1, uriRef2, null);
        Assert.assertTrue(tripleIter.hasNext());
        RDFTerm gotValue = tripleIter.next().getObject();
        Assert.assertEquals(value, gotValue);
    }

    @Test
    public void testUseLanguageLiterals() {
        Graph graph = getEmptyGraph();
        Assert.assertEquals(0, graph.size());
        Language language = new Language("it");
        Literal value = new PlainLiteralImpl("<elem>value</elem>",language);
        final TripleImpl triple1 = new TripleImpl(uriRef1, uriRef2, value);
        graph.add(triple1);
        Iterator<Triple> tripleIter = graph.filter(uriRef1, uriRef2, null);
        Assert.assertTrue(tripleIter.hasNext());
        RDFTerm gotValue = tripleIter.next().getObject();
        Assert.assertEquals(value, gotValue);
        Assert.assertEquals(language, ((Literal)gotValue).getLanguage());
    }

    @Test
    public void testRemoveViaIterator() {
        Graph graph = getEmptyGraph();
        Assert.assertEquals(0, graph.size());
        final TripleImpl triple1 = new TripleImpl(uriRef1, uriRef2, uriRef1);
        graph.add(triple1);
        final TripleImpl triple2 = new TripleImpl(uriRef1, uriRef2, uriRef4);
        graph.add(triple2);
        Assert.assertEquals(2, graph.size());
        Iterator<Triple> iterator = graph.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        Assert.assertEquals(0, graph.size());
    }

    @Test
    public void testGetSize() throws Exception {
        Graph graph = getEmptyGraph();
        // The test graph must always be empty after test fixture setup
        Assert.assertEquals(0, graph.size());
    }


    @Test
    public void testAddSingleTriple() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple triple= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        Assert.assertEquals(0, graph.size());
        Assert.assertTrue(graph.add(triple));
        Assert.assertEquals(1, graph.size());
    }


    @Test
    public void testAddSameTripleTwice() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple triple= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        Assert.assertEquals(0, graph.size());
        Assert.assertTrue(graph.add(triple));
        Assert.assertFalse(graph.add(triple)); // ImmutableGraph does not change
        Assert.assertEquals(1, graph.size());
    }


    @Test
    public void testRemoveSingleTriple() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple triple= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        Assert.assertTrue(graph.add(triple));
        Assert.assertTrue(graph.remove(triple));
        Assert.assertEquals(0, graph.size());
    }

    @Test
    public void testRemoveSameTripleTwice() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple tripleAlice= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        final Triple tripleBob= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/bob");
        Assert.assertTrue(graph.add(tripleAlice));
        Assert.assertTrue(graph.add(tripleBob));
        Assert.assertTrue(graph.remove(tripleAlice));
        Assert.assertFalse(graph.remove(tripleAlice));
        Assert.assertEquals(1, graph.size());
    }

    @Test
    public void testGetSameBlankNode() throws Exception {
        Graph graph = getEmptyGraph();
        BlankNode bNode = new BlankNode();
        final IRI HAS_NAME = new IRI("http://example.org/ontology/hasName");
        final PlainLiteralImpl name = new PlainLiteralImpl("http://example.org/people/alice");
        final PlainLiteralImpl name2 = new PlainLiteralImpl("http://example.org/people/bob");
        final Triple tripleAlice = new TripleImpl(bNode, HAS_NAME, name);
        final Triple tripleBob = new TripleImpl(bNode, HAS_NAME, name2);
        Assert.assertTrue(graph.add(tripleAlice));
        Assert.assertTrue(graph.add(tripleBob));
        Iterator<Triple> result = graph.filter(null, HAS_NAME, name);
        Assert.assertEquals(bNode, result.next().getSubject());
    }

    @Test
    public void testContainsIfContained() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple triple= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        Assert.assertTrue(graph.add(triple));
        Assert.assertTrue(graph.contains(triple));
    }


    @Test
    public void testContainsIfEmpty() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple triple= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        Assert.assertFalse(graph.contains(triple));
    }


    @Test
    public void testContainsIfNotContained() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple tripleAdd= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        final Triple tripleTest= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/bob");
        Assert.assertTrue(graph.add(tripleAdd));
        Assert.assertFalse(graph.contains(tripleTest));
    }


    @Test
    public void testFilterEmptyGraph() throws Exception {
        Graph graph = getEmptyGraph();
        Iterator<Triple> i = graph.filter(null, null, null);
        Assert.assertFalse(i.hasNext());
    }


    @Test
    public void testFilterSingleEntry() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple triple= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        Assert.assertTrue(graph.add(triple));

        Iterator<Triple> i = graph.filter(null, null, null);
        Collection<Triple> resultSet= toCollection(i);
        Assert.assertEquals(1, resultSet.size());
        Assert.assertTrue(resultSet.contains(triple));
    }


    @Test
    public void testFilterByObject() throws Exception {
        Graph graph = getEmptyGraph();
        final Triple tripleAlice= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/alice");
        final Triple tripleBob= createTriple(
                "http://example.org/ontology/Person",
                "http://example.org/ontology/hasName",
                "http://example.org/people/bob");
        Assert.assertTrue(graph.add(tripleAlice));
        Assert.assertTrue(graph.add(tripleBob));

        Iterator<Triple> iterator;
        Collection<Triple> resultSet;

        // Find bob
        iterator = graph.filter(null, null,
                new IRI("http://example.org/people/bob"));
        resultSet= toCollection(iterator);
        Assert.assertEquals(1, resultSet.size());
        Assert.assertTrue(resultSet.contains(tripleBob));

        // Find alice
        iterator = graph.filter(null, null,
                new IRI("http://example.org/people/alice"));
        resultSet= toCollection(iterator);
        Assert.assertEquals(1, resultSet.size());
        Assert.assertTrue(resultSet.contains(tripleAlice));

        // Find both
        iterator = graph.filter(null, null, null);
        resultSet= toCollection(iterator);
        Assert.assertEquals(2, resultSet.size());
        Assert.assertTrue(resultSet.contains(tripleAlice));
        Assert.assertTrue(resultSet.contains(tripleBob));
    }
/*
    @Test
    public void graphEventTestAddRemove() {
        Graph mGraph = getEmptyGraph();
        TestGraphListener listener = new TestGraphListener();
        mGraph.addGraphListener(listener, new FilterTriple(uriRef1, uriRef2, null));
        mGraph.addGraphListener(listener, new FilterTriple(bnode2, null, literal2));
        mGraph.addGraphListener(listener, new FilterTriple(null, uriRef4, literal2));        
        mGraph.add(trpl1);
        Assert.assertNull(listener.getEvents());        
        mGraph.add(trpl2);
        Assert.assertEquals(1, listener.getEvents().size());
        Assert.assertEquals(trpl2, listener.getEvents().get(0).getTriple());
        Assert.assertTrue(listener.getEvents().get(0) instanceof  AddEvent);
        listener.resetEvents();
        mGraph.remove(trpl2);
        Assert.assertEquals(1, listener.getEvents().size());
        Assert.assertEquals(trpl2, listener.getEvents().get(0).getTriple());
        Assert.assertTrue(listener.getEvents().get(0) instanceof RemoveEvent);
        listener.resetEvents();        
        mGraph.add(trpl3);
        Assert.assertEquals(1, listener.getEvents().size());
        Assert.assertEquals(trpl3, listener.getEvents().get(0).getTriple());
        Assert.assertTrue(listener.getEvents().get(0) instanceof AddEvent);
        listener.resetEvents();        
        mGraph.remove(trpl4);
        Assert.assertNull(listener.getEvents());
    }
    
    @Test
    public void graphEventTestAddAllRemoveAll() {
        Graph mGraph = getEmptyGraph();
        TestGraphListener listener = new TestGraphListener();
        mGraph.addGraphListener(listener, new FilterTriple(uriRef1, uriRef2, null));
        mGraph.addGraphListener(listener, new FilterTriple(bnode2, null, literal2));
        mGraph.addGraphListener(listener, new FilterTriple(null, uriRef4, literal2));
        Graph triples = new SimpleGraph();
        triples.add(trpl1);
        triples.add(trpl2);
        triples.add(trpl3);
        triples.add(trpl4);
        mGraph.addAll(triples);
        List<GraphEvent> cumulatedEvents = listener.getCumulatedEvents();
        Set<Triple> cumulatedTriples = getCumulatedTriples(cumulatedEvents);
        Assert.assertEquals(3, cumulatedEvents.size());
        Assert.assertTrue(cumulatedEvents.get(0) instanceof AddEvent);
        Assert.assertTrue(cumulatedTriples.contains(trpl2));
        Assert.assertTrue(cumulatedTriples.contains(trpl3));
        Assert.assertTrue(cumulatedTriples.contains(trpl4));
        listener.resetCumulatedEvents();
        mGraph.removeAll(triples);
        cumulatedEvents = listener.getCumulatedEvents();
        cumulatedTriples = getCumulatedTriples(cumulatedEvents);
        Assert.assertEquals(3, cumulatedEvents.size());
        Assert.assertTrue(cumulatedEvents.get(0) instanceof RemoveEvent);
        Assert.assertTrue(cumulatedTriples.contains(trpl2));
        Assert.assertTrue(cumulatedTriples.contains(trpl3));
        Assert.assertTrue(cumulatedTriples.contains(trpl4));
    }

    @Test
    public void graphEventTestFilterRemove() {
        Graph mGraph = getEmptyGraph();
        TestGraphListener listener = new TestGraphListener();
        mGraph.addGraphListener(listener, new FilterTriple(uriRef1, uriRef2, null));
        mGraph.addGraphListener(listener, new FilterTriple(bnode2, null, literal2));
        mGraph.addGraphListener(listener, new FilterTriple(null, uriRef4, literal2));
        mGraph.add(trpl1);
        mGraph.add(trpl2);
        mGraph.add(trpl3);
        mGraph.add(trpl4);
        listener.resetCumulatedEvents();
        Iterator<Triple> result = mGraph.filter(null, uriRef2, null);
        while (result.hasNext()) {
            result.next();
            result.remove();
        }
        List<GraphEvent> cumulatedEvents = listener.getCumulatedEvents();
        Assert.assertEquals(1, cumulatedEvents.size());
        Assert.assertTrue(cumulatedEvents.get(0) instanceof RemoveEvent);
        Assert.assertEquals(trpl2, listener.getEvents().get(0).getTriple());
    }

    @Test
    public void graphEventTestIteratorRemove() {
        Graph mGraph = getEmptyGraph();
        TestGraphListener listener = new TestGraphListener();
        mGraph.addGraphListener(listener, new FilterTriple(uriRef1, uriRef2, null));
        mGraph.addGraphListener(listener, new FilterTriple(bnode2, null, literal2));
        mGraph.addGraphListener(listener, new FilterTriple(null, uriRef4, literal2));
        mGraph.add(trpl1);
        mGraph.add(trpl2);
        mGraph.add(trpl3);
        mGraph.add(trpl4);
        listener.resetCumulatedEvents();
        Iterator<Triple> result = mGraph.iterator();
        while (result.hasNext()) {
            result.next();
            result.remove();
        }
        List<GraphEvent> cumulatedEvents = listener.getCumulatedEvents();
        Set<Triple> cumulatedTriples = getCumulatedTriples(cumulatedEvents);
        Assert.assertEquals(3, cumulatedEvents.size());
        Assert.assertTrue(cumulatedEvents.get(0) instanceof RemoveEvent);
        Assert.assertTrue(cumulatedTriples.contains(trpl2));
        Assert.assertTrue(cumulatedTriples.contains(trpl3));
        Assert.assertTrue(cumulatedTriples.contains(trpl4));
    }

    @Test
    public void graphEventTestClear() {
        Graph mGraph = getEmptyGraph();
        TestGraphListener listener = new TestGraphListener();
        mGraph.addGraphListener(listener, new FilterTriple(uriRef1, uriRef2, null));
        mGraph.addGraphListener(listener, new FilterTriple(bnode2, null, literal2));
        mGraph.addGraphListener(listener, new FilterTriple(null, uriRef4, literal2));
        mGraph.add(trpl1);
        mGraph.add(trpl2);
        mGraph.add(trpl3);
        mGraph.add(trpl4);
        listener.resetCumulatedEvents();
        mGraph.clear();
        List<GraphEvent> cumulatedEvents = listener.getCumulatedEvents();
        Set<Triple> cumulatedTriples = getCumulatedTriples(cumulatedEvents);
        Assert.assertEquals(3, cumulatedEvents.size());
        Assert.assertTrue(cumulatedEvents.get(0) instanceof RemoveEvent);
        Assert.assertTrue(cumulatedTriples.contains(trpl2));
        Assert.assertTrue(cumulatedTriples.contains(trpl3));
        Assert.assertTrue(cumulatedTriples.contains(trpl4));
    }

    private Set<Triple> getCumulatedTriples(List<GraphEvent> cumulatedEvents) {
        Set<Triple> triples = new HashSet<Triple>();
        for(GraphEvent event: cumulatedEvents) {
            triples.add(event.getTriple());
        }
        return triples;
    }

    @Test
    public void graphEventTestWithDelay() throws Exception{
        Graph mGraph = getEmptyGraph();
        TestGraphListener listener = new TestGraphListener();
        mGraph.addGraphListener(listener, new FilterTriple(uriRef1, uriRef2, null),
                1000);

        Triple triple0 = new TripleImpl(uriRef2, uriRef2, literal1);
        Triple triple1 = new TripleImpl(uriRef1, uriRef2, uriRef1);
        Triple triple2 = new TripleImpl(uriRef1, uriRef2, literal1);
        Triple triple3 = new TripleImpl(uriRef1, uriRef2, bnode1);
        mGraph.add(triple0);
        mGraph.add(triple1);
        mGraph.add(triple2);
        mGraph.add(triple3);
        Thread.sleep(1500);
        Assert.assertEquals(3, listener.getEvents().size());
        Assert.assertEquals(triple1, listener.getEvents().get(0).getTriple());
        Assert.assertTrue(listener.getEvents().get(0) instanceof AddEvent);
        Assert.assertEquals(triple2, listener.getEvents().get(1).getTriple());
        Assert.assertTrue(listener.getEvents().get(0) instanceof AddEvent);
        Assert.assertEquals(triple3, listener.getEvents().get(2).getTriple());
        Assert.assertTrue(listener.getEvents().get(0) instanceof AddEvent);
    }

    private static class TestGraphListener implements GraphListener {
        private List<GraphEvent> events = null;
        private List<GraphEvent> cumulatedEvents = new ArrayList<GraphEvent>();

        @Override
        public void graphChanged(List<GraphEvent> events) {
            this.events = events;
            Iterator<GraphEvent> eventIter = events.iterator();
            while (eventIter.hasNext()) {
                GraphEvent graphEvent = eventIter.next();
                this.cumulatedEvents.add(graphEvent);
            }            
        }

        public List<GraphEvent> getEvents() {
            return events;
        }

        public List<GraphEvent> getCumulatedEvents() {
            return cumulatedEvents;
        }

        public void resetEvents() {
            events = null;
        }

        public void resetCumulatedEvents() {
            cumulatedEvents = new ArrayList<GraphEvent>();
        }
    }
*/
    private Collection<Triple> toCollection(Iterator<Triple> iterator) {
        Collection<Triple> result = new ArrayList<Triple>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    /**
     * Creates a new <code>Triple</code>.
     * @param subject  the subject.
     * @param predicate  the predicate.
     * @param object  the object.
     * @throws IllegalArgumentException  If an attribute is <code>null</code>.
     */
    private Triple createTriple(String subject, String predicate,
            String object) {
        return new TripleImpl(new IRI(subject), new IRI(predicate),
                new IRI(object));
    }
    
}
