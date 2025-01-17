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
package org.apache.clerezza.rdf.utils;

import java.util.Iterator;
import org.junit.Test;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.junit.Assert;

/**
 *
 * @author hasan
 */
public class UnionGraphTest {

    private final IRI uriRef1 =
            new IRI("http://example.org/ontology#res1");
    private final IRI uriRef2 =
            new IRI("http://example.org/ontology#res2");
    private final IRI uriRef3 =
            new IRI("http://example.org/ontology#res3");
    private final IRI uriRef4 =
            new IRI("http://example.org/ontology#res4");

    @Test
    public void readAccess() {
        Graph graph = new SimpleGraph();
        Graph graph2 = new SimpleGraph();
        BlankNode bnode = new BlankNode() {
        };
        graph.add(new TripleImpl(uriRef1, uriRef2, uriRef1));
        graph2.add(new TripleImpl(bnode, uriRef1, uriRef3));
        Graph unionGraph = new UnionGraph(graph, graph2);
        Iterator<Triple> unionTriples = unionGraph.iterator();
        Assert.assertTrue(unionTriples.hasNext());
        unionTriples.next();
        Assert.assertTrue(unionTriples.hasNext());
        unionTriples.next();
        Assert.assertFalse(unionTriples.hasNext());
        Assert.assertEquals(2, unionGraph.size());
    }
    
    @Test
    public void writeAccess() {
        Graph graph = new SimpleGraph();
        Graph graph2 = new SimpleGraph();
        BlankNode bnode = new BlankNode() {
        };
        graph2.add(new TripleImpl(bnode, uriRef1, uriRef3));
        Graph unionGraph = new UnionGraph(graph, graph2);
        Assert.assertEquals(1, unionGraph.size());
        unionGraph.add(new TripleImpl(uriRef4, uriRef1, uriRef3));
        Assert.assertEquals(1, graph.size());
        Assert.assertEquals(2, unionGraph.size());
        Assert.assertEquals(1, graph2.size());
    }
}