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
package org.apache.clerezza.rdf.utils.smushing;

import java.util.Iterator;
import java.util.Set;
import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author reto
 */
public class SameAsSmushTest {
    
    private final IRI uriA = new IRI("http://example.org/A");
    private final IRI uriB = new IRI("http://example.org/B");
    private final IRI uriC = new IRI("http://example.org/C");
    
    private final Literal lit = new PlainLiteralImpl("That's me (and you)");

    private Graph sameAsStatements = new SimpleGraph();
    {
        sameAsStatements.add(new TripleImpl(uriA, OWL.sameAs, uriB));
    }
    
    private Graph  dataGraph = new SimpleGraph();
    {
        dataGraph.add(new TripleImpl(uriA, FOAF.knows, uriB));
        dataGraph.add(new TripleImpl(uriB, RDFS.label, lit));
        dataGraph.add(new TripleImpl(uriA, RDFS.label, lit));
    }

    @Test
    public void simple()  {
        SameAsSmusher smusher = new SameAsSmusher() {

            @Override
            protected IRI getPreferedIRI(Set<IRI> uriRefs) {
                if (!uriRefs.contains(uriA)) throw new RuntimeException("not the set we excpect");
                if (!uriRefs.contains(uriB)) throw new RuntimeException("not the set we excpect");
                return uriC;
            }
            
        };
        Assert.assertEquals(3, dataGraph.size());
        smusher.smush(dataGraph, sameAsStatements, true);
        Assert.assertEquals(4, dataGraph.size());
        Assert.assertTrue(dataGraph.filter(null, OWL.sameAs, null).hasNext());
        //exactly one statement with literal 
        Iterator<Triple> litStmts = dataGraph.filter(null, null, lit);
        Assert.assertTrue(litStmts.hasNext());
        Triple litStmt = litStmts.next();
        Assert.assertFalse(litStmts.hasNext());
        Iterator<Triple> knowsStmts = dataGraph.filter(null, FOAF.knows, null);
        Assert.assertTrue(knowsStmts.hasNext());
        Triple knowStmt = knowsStmts.next();
        Assert.assertEquals(knowStmt.getSubject(), knowStmt.getObject());
        Assert.assertEquals(litStmt.getSubject(), knowStmt.getObject());
        Assert.assertEquals(litStmt.getSubject(), dataGraph.filter(null, OWL.sameAs, null).next().getObject());
        Assert.assertEquals(knowStmt.getSubject(), uriC);
    }

}
