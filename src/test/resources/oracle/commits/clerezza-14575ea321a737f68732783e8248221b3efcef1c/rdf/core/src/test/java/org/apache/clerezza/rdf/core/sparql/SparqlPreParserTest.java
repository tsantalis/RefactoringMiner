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
package org.apache.clerezza.rdf.core.sparql;

import java.util.HashSet;
import java.util.Set;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcManagerTest;
import org.apache.clerezza.rdf.core.access.providers.WeightedA;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author hasan
 */
public class SparqlPreParserTest {

    private TcManager graphAccess;
    private final WeightedA weightedA = new WeightedA();
    private final static IRI DEFAULT_GRAPH = new IRI("http://example.org/default.graph");
    private final static IRI TEST_GRAPH = new IRI("http://example.org/test.graph");

    @Before
	public void setUp() {
		graphAccess = TcManager.getInstance();
		graphAccess.addWeightedTcProvider(weightedA);
	}

	@After
	public void tearDown() {
		graphAccess = TcManager.getInstance();
		graphAccess.removeWeightedTcProvider(weightedA);
	}

    @Test
    public void testDefaultGraphInSelectQuery() throws ParseException {

        StringBuilder queryStrBuilder = new StringBuilder();
        queryStrBuilder.append(
                "PREFIX : <http://example.org/>\n" +
                "SELECT ?x \n" +
                "{\n" +
                ":order :item/:price ?x\n" +
                "}\n");

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStrBuilder.toString(), DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testAllGraphReferenceInSelectQuery() throws ParseException {

        StringBuilder queryStrBuilder = new StringBuilder();
        queryStrBuilder.append("SELECT DISTINCT ?g { GRAPH ?g { ?s ?p ?o } }\n");

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStrBuilder.toString(), DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs == null);
    }

    @Test
    public void testSelectQuery() throws ParseException {

        StringBuilder queryStrBuilder = new StringBuilder();
        queryStrBuilder.append(
                "PREFIX : <http://example.org/>\n" +
                "SELECT ?x (foo(2*3, ?x < ?y) AS ?f) (GROUP_CONCAT(?x ; separator=\"|\") AS ?gc) (sum(distinct *) AS ?total)\n" +
                "FROM " + TEST_GRAPH.toString() + "\n" +
                "{\n" +
                ":order :item/:price ?x\n" +
                "}\n");

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStrBuilder.toString(), DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(TEST_GRAPH));
    }

    @Test
    public void testSimpleDescribe() throws ParseException {

        String queryStr = "DESCRIBE <http://example.org/>";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testLoadingToDefaultGraph() throws ParseException {

        String queryStr = "LOAD SILENT <http://example.org/mydata>";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(new IRI("http://example.org/mydata"));
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testLoadingToGraph() throws ParseException {

        String queryStr = "LOAD SILENT <http://example.org/mydata> INTO GRAPH " + TEST_GRAPH.toString();

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(TEST_GRAPH);
        expected.add(new IRI("http://example.org/mydata"));
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testClearingDefaultGraph() throws ParseException {

        String queryStr = "CLEAR SILENT DEFAULT";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testClearingNamedGraph() throws ParseException {

        String queryStr = "CLEAR SILENT NAMED";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.contains(TcManagerTest.uriRefA));
    }

    @Test
    public void testClearingGraph() throws ParseException {

        String queryStr = "CLEAR SILENT GRAPH " + TEST_GRAPH.toString();

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(TEST_GRAPH));
    }

    @Test
    public void testDroppingDefaultGraph() throws ParseException {

        String queryStr = "DROP SILENT DEFAULT";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testDroppingNamedGraph() throws ParseException {

        String queryStr = "DROP SILENT NAMED";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.contains(TcManagerTest.uriRefA));
    }

    @Test
    public void testDroppingGraph() throws ParseException {

        String queryStr = "DROP SILENT GRAPH " + TEST_GRAPH.toString();

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(TEST_GRAPH));
    }

    @Test
    public void testCreatingGraph() throws ParseException {

        String queryStr = "CREATE SILENT GRAPH " + TEST_GRAPH.toString();

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(TEST_GRAPH));
    }

    @Test
    public void testAddingTriplesFromDefaultGraphToNamedGraph() throws ParseException {

        String queryStr = "ADD SILENT DEFAULT TO GRAPH " + TEST_GRAPH.toString();

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(TEST_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testAddingTriplesFromNamedGraphToDefaultGraph() throws ParseException {

        String queryStr = "ADD SILENT GRAPH " + TEST_GRAPH.toString() + " TO DEFAULT";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(TEST_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testMovingTriplesFromDefaultGraphToNamedGraph() throws ParseException {

        String queryStr = "MOVE SILENT DEFAULT TO GRAPH " + TEST_GRAPH.toString();

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(TEST_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testMovingTriplesFromNamedGraphToDefaultGraph() throws ParseException {

        String queryStr = "MOVE SILENT GRAPH " + TEST_GRAPH.toString() + " TO DEFAULT";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(TEST_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testCopyingTriplesFromDefaultGraphToNamedGraph() throws ParseException {

        String queryStr = "COPY SILENT DEFAULT TO GRAPH " + TEST_GRAPH.toString();

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(TEST_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testCopyingTriplesFromNamedGraphToDefaultGraph() throws ParseException {

        String queryStr = "COPY SILENT GRAPH " + TEST_GRAPH.toString() + " TO DEFAULT";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(TEST_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testInsertDataToDefaultGraph() throws ParseException {

        String queryStr = "PREFIX dc: <http://purl.org/dc/elements/1.1/> INSERT DATA { \n" +
                "<http://example/book1> dc:title \"A new book\" ; dc:creator \"A.N.Other\" . }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testInsertDataToNamedGraph() throws ParseException {

        String queryStr = "PREFIX ns: <http://example.org/ns#>\n" +
                "INSERT DATA { GRAPH " + TEST_GRAPH.toString() + " { <http://example/book1>  ns:price  42 } }";
        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(TEST_GRAPH));
    }

    @Test
    public void testDeleteDataInDefaultGraph() throws ParseException {

        String queryStr = "PREFIX dc: <http://purl.org/dc/elements/1.1/> DELETE DATA { \n" +
                "<http://example/book1> dc:title \"A new book\" ; dc:creator \"A.N.Other\" . }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testDeleteDataInNamedGraph() throws ParseException {

        String queryStr = "PREFIX ns: <http://example.org/ns#>\n" +
                "DELETE DATA { GRAPH " + TEST_GRAPH.toString() + " { <http://example/book1>  ns:price  42 } }";
        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(TEST_GRAPH));
    }

    @Test
    public void testInsertAndDeleteData() throws ParseException {

        String queryStr = "PREFIX ns: <http://example.org/ns#> " +
                "INSERT DATA { <http://example/book1>  ns:price  42 }; " +
                "DELETE DATA { GRAPH " + TEST_GRAPH.toString() + " { <http://example/book1>  ns:price  42 } }";
        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);

        Set<IRI> expected = new HashSet<>();
        expected.add(DEFAULT_GRAPH);
        expected.add(TEST_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testDeleteWhereInDefaultGraph() throws ParseException {

        String queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "DELETE WHERE { ?person foaf:givenName 'Fred'; ?property ?value }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);

        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testDeleteWhereInNamedGraphs() throws ParseException {

        String queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DELETE WHERE " +
                "{ GRAPH <http://example.com/names> { ?person foaf:givenName 'Fred' ; ?property1 ?value1 } " +
                "  GRAPH <http://example.com/addresses> { ?person ?property2 ?value2 } }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);

        Set<IRI> expected = new HashSet<>();
        expected.add(new IRI("http://example.com/names"));
        expected.add(new IRI("http://example.com/addresses"));
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testModifyOperationWithFallbackGraph() throws ParseException {
        String queryStr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> WITH " + TEST_GRAPH.toString() +
                " DELETE { ?person foaf:givenName 'Bill' } INSERT { ?person foaf:givenName 'William' }" +
                " WHERE { ?person foaf:givenName 'Bill' }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Set<IRI> expected = new HashSet<>();
        expected.add(TEST_GRAPH);
        expected.add(DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testDeleteOperationInDefaultGraph() throws ParseException {
        String queryStr = "PREFIX dc:  <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "DELETE { ?book ?p ?v } WHERE { ?book dc:date ?date . " +
                "FILTER ( ?date > \"1970-01-01T00:00:00-02:00\"^^xsd:dateTime ) ?book ?p ?v }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testInsertOperationToNamedGraph() throws ParseException {
        String queryStr = "PREFIX dc:  <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "INSERT { GRAPH <http://example/bookStore2> { ?book ?p ?v } } " +
                "WHERE { GRAPH <http://example/bookStore> { ?book dc:date ?date . " +
                "FILTER ( ?date > \"1970-01-01T00:00:00-02:00\"^^xsd:dateTime ) ?book ?p ?v } }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);

        Set<IRI> expected = new HashSet<>();
        expected.add(new IRI("http://example/bookStore2"));
        expected.add(new IRI("http://example/bookStore"));
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testInsertAndDeleteWithCommonPrefix() throws ParseException {
        String queryStr = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "PREFIX dcmitype: <http://purl.org/dc/dcmitype/>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n\n" +
                "INSERT\n" +
                "  { GRAPH <http://example/bookStore2> { ?book ?p ?v } }\n" +
                "WHERE\n" +
                "  { GRAPH <http://example/bookStore>\n" +
                "    { ?book dc:date ?date . \n" +
                "      FILTER ( ?date < \"2000-01-01T00:00:00-02:00\"^^xsd:dateTime )\n" +
                "      ?book ?p ?v\n" +
                "    }\n" +
                "  } ;\n\n" +
                "WITH <http://example/bookStore>\n" +
                "DELETE\n" +
                " { ?book ?p ?v }\n" +
                "WHERE\n" +
                "  { ?book dc:date ?date ;\n" +
                "          dc:type dcmitype:PhysicalObject .\n" +
                "    FILTER ( ?date < \"2000-01-01T00:00:00-02:00\"^^xsd:dateTime ) \n" +
                "    ?book ?p ?v\n" +
                "  }";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);

        Set<IRI> expected = new HashSet<>();
        expected.add(new IRI("http://example/bookStore2"));
        expected.add(new IRI("http://example/bookStore"));
        Assert.assertTrue(referredGraphs.containsAll(expected));
    }

    @Test
    public void testExistsFunction() throws ParseException {
        String queryStr = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n\n" +
                "SELECT ?person\n" +
                "WHERE \n" +
                "{\n" +
                "  ?person rdf:type foaf:Person .\n" +
                "  FILTER EXISTS { ?person foaf:name ?name }\n" +
                "}";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }

    @Test
    public void testNotExistsFunction() throws ParseException {
        String queryStr = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n\n" +
                "SELECT ?person\n" +
                "WHERE \n" +
                "{\n" +
                "  ?person rdf:type foaf:Person .\n" +
                "  FILTER NOT EXISTS { ?person foaf:name ?name }\n" +
                "}";

        SparqlPreParser parser;
        parser = new SparqlPreParser(TcManager.getInstance());
        Set<IRI> referredGraphs = parser.getReferredGraphs(queryStr, DEFAULT_GRAPH);
        Assert.assertTrue(referredGraphs.toArray()[0].equals(DEFAULT_GRAPH));
    }
}
