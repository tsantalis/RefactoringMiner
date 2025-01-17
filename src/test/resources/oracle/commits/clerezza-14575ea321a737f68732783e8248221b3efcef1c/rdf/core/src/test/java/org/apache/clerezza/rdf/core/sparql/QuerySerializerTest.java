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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.sparql.query.BinaryOperation;
import org.apache.clerezza.rdf.core.sparql.query.BuiltInCall;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.clerezza.rdf.core.sparql.query.LiteralExpression;
import org.apache.clerezza.rdf.core.sparql.query.ResourceOrVariable;
import org.apache.clerezza.rdf.core.sparql.query.TriplePattern;
import org.apache.clerezza.rdf.core.sparql.query.UriRefExpression;
import org.apache.clerezza.rdf.core.sparql.query.UriRefOrVariable;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleAskQuery;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleBasicGraphPattern;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleDescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleGroupGraphPattern;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleOptionalGraphPattern;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleOrderCondition;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleSelectQuery;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleTriplePattern;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author hasan
 */
public class QuerySerializerTest {

    @Test
    public void testSelectQuery() {

        final String queryString = "SELECT ?title FROM <http://example.org/library>" +
                " WHERE { <http://example.org/book/book1>" +
                " <http://purl.org/dc/elements/1.1/title> ?title . }";

        SimpleSelectQuery selectQuery = new SimpleSelectQuery();
        Variable variable = new Variable("title");
        selectQuery.addSelection(variable);
        IRI defaultGraph = new IRI("http://example.org/library");
        selectQuery.addDefaultGraph(defaultGraph);
        ResourceOrVariable subject = new ResourceOrVariable(
                new IRI("http://example.org/book/book1"));
        UriRefOrVariable predicate = new UriRefOrVariable(
                new IRI("http://purl.org/dc/elements/1.1/title"));
        ResourceOrVariable object = new ResourceOrVariable(variable);
        TriplePattern triplePattern = new SimpleTriplePattern(subject, predicate, object);
        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(triplePattern);

        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);
        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(bgp);
        selectQuery.setQueryPattern(queryPattern);

        Assert.assertTrue(selectQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    @Test
    public void testConstructQuery() {

        final String queryString = "CONSTRUCT { <http://example.org/person#Alice> " +
                "<http://www.w3.org/2001/vcard-rdf/3.0#FN> ?name . } " +
                "WHERE { ?x <http://xmlns.com/foaf/0.1/name> ?name . }";

        ResourceOrVariable s = new ResourceOrVariable(
                new IRI("http://example.org/person#Alice"));
        UriRefOrVariable p = new UriRefOrVariable(
                new IRI("http://www.w3.org/2001/vcard-rdf/3.0#FN"));
        ResourceOrVariable o = new ResourceOrVariable(new Variable("name"));
        Set<TriplePattern> constructTriplePatterns = new HashSet<TriplePattern>();
        constructTriplePatterns.add(new SimpleTriplePattern(s, p, o));
        SimpleConstructQuery constructQuery = new SimpleConstructQuery(constructTriplePatterns);

        s = new ResourceOrVariable(new Variable("x"));
        p = new UriRefOrVariable(new IRI("http://xmlns.com/foaf/0.1/name"));
        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(new SimpleTriplePattern(s, p, o));

        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);
        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(bgp);
        constructQuery.setQueryPattern(queryPattern);

        Assert.assertTrue(constructQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    @Test
    public void testDescribeQuery() {

        final String queryString = "DESCRIBE <http://example.org/book/book1>";

        SimpleDescribeQuery describeQuery = new SimpleDescribeQuery();
        describeQuery.addResourceToDescribe(new ResourceOrVariable(
                new IRI("http://example.org/book/book1")));

        Assert.assertTrue(describeQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    @Test
    public void testAskQuery() {

        final String queryString = "ASK WHERE { ?x <http://xmlns.com/foaf/0.1/name> " +
                "\"Alice\"^^<http://www.w3.org/2001/XMLSchema#string> . }";

        ResourceOrVariable s = new ResourceOrVariable(new Variable("x"));
        UriRefOrVariable p = new UriRefOrVariable(
                new IRI("http://xmlns.com/foaf/0.1/name"));
        ResourceOrVariable o = new ResourceOrVariable(
                LiteralFactory.getInstance().createTypedLiteral("Alice"));

        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(new SimpleTriplePattern(s, p, o));
        SimpleAskQuery askQuery = new SimpleAskQuery();

        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);
        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(bgp);
        askQuery.setQueryPattern(queryPattern);

        Assert.assertTrue(askQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    /**
     * Ignoring: given that triplePatterns is a Set I don't see what is supposed 
     * to guarantee the expected ordering.
     */
    @Ignore
    @Test
    public void testFilter() {

        final String queryString = "SELECT ?title ?price WHERE { " +
                "?x <http://purl.org/dc/elements/1.1/title> ?title . " +
                "?x <http://example.org/ns#price> ?price . " +
                "FILTER ((?price) < (\"30.5\"^^<http://www.w3.org/2001/XMLSchema#double>)) " +
                "}";

        Variable price = new Variable("price");
        Variable title = new Variable("title");
        SimpleSelectQuery selectQuery = new SimpleSelectQuery();
        selectQuery.addSelection(title);
        selectQuery.addSelection(price);

        Variable x = new Variable("x");
        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(new SimpleTriplePattern(x,
                new IRI("http://example.org/ns#price"), price));
        triplePatterns.add(new SimpleTriplePattern(x,
                new IRI("http://purl.org/dc/elements/1.1/title"), title));

        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);
        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(bgp);
        BinaryOperation constraint = new BinaryOperation("<",
                price, new LiteralExpression(LiteralFactory.getInstance().createTypedLiteral(30.5)));
        queryPattern.addConstraint(constraint);
        selectQuery.setQueryPattern(queryPattern);

        Assert.assertTrue(selectQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    @Test
    public void testUriRefExpression() {

        final String queryString = "SELECT ?resource WHERE { " +
                "?resource <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?myType . " +
                "FILTER ((?resource) = (<http://example.org/ontology#special>)) " +
                "}";

        Variable resource = new Variable("resource");
        SimpleSelectQuery selectQuery = new SimpleSelectQuery();
        selectQuery.addSelection(resource);

        Variable myType = new Variable("myType");
        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(new SimpleTriplePattern(resource,
                new IRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), myType));

        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);
        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(bgp);
        BinaryOperation constraint = new BinaryOperation("=",
                resource, new UriRefExpression(new IRI("http://example.org/ontology#special")));
        queryPattern.addConstraint(constraint);
        selectQuery.setQueryPattern(queryPattern);

        Assert.assertTrue(selectQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    @Test
    public void testOrderBy() {

        final String queryString = "SELECT * WHERE { ?a ?b ?c . } ORDER BY DESC(?c)";

        Variable a = new Variable("a");
        Variable b = new Variable("b");
        Variable c = new Variable("c");
        SimpleSelectQuery selectQuery = new SimpleSelectQuery();
        selectQuery.setSelectAll();
        selectQuery.addSelection(a);
        selectQuery.addSelection(b);
        selectQuery.addSelection(c);

        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(new SimpleTriplePattern(a, b, c));
        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);
        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(bgp);
        selectQuery.setQueryPattern(queryPattern);
        selectQuery.addOrderCondition(new SimpleOrderCondition(c, false));

        Assert.assertTrue(selectQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    @Test
    public void testOptional() {

        final String queryString = "SELECT ?title ?price WHERE { " +
                "?x <http://purl.org/dc/elements/1.1/title> ?title . " +
                "OPTIONAL { ?x <http://example.org/ns#price> ?price . } " +
                "}";

        Variable title = new Variable("title");
        Variable price = new Variable("price");
        SimpleSelectQuery selectQuery = new SimpleSelectQuery();
        selectQuery.addSelection(title);
        selectQuery.addSelection(price);

        Variable x = new Variable("x");
        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(new SimpleTriplePattern(x,
                new IRI("http://purl.org/dc/elements/1.1/title"), title));

        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);

        Set<TriplePattern> triplePatternsOpt = new HashSet<TriplePattern>();
        triplePatternsOpt.add(new SimpleTriplePattern(x,
                new IRI("http://example.org/ns#price"), price));

        SimpleBasicGraphPattern bgpOpt =
                new SimpleBasicGraphPattern(triplePatternsOpt);

        SimpleGroupGraphPattern ggpOpt = new SimpleGroupGraphPattern();
        ggpOpt.addGraphPattern(bgpOpt);

        SimpleOptionalGraphPattern ogp = new SimpleOptionalGraphPattern(bgp, ggpOpt);

        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(ogp);
        selectQuery.setQueryPattern(queryPattern);

        Assert.assertTrue(selectQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }

    @Test
    public void testRegex() {

        final String queryString = "SELECT ?p WHERE { " +
                "<http://localhost/testitem> ?p ?x . " +
                "FILTER REGEX(?x,\".*uni.*\"^^<http://www.w3.org/2001/XMLSchema#string>) }";

        Variable p = new Variable("p");
        SimpleSelectQuery selectQuery = new SimpleSelectQuery();
        selectQuery.addSelection(p);

        Variable x = new Variable("x");
        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();
        triplePatterns.add(new SimpleTriplePattern(
                new IRI("http://localhost/testitem"), p, x));

        SimpleBasicGraphPattern bgp = new SimpleBasicGraphPattern(triplePatterns);
        SimpleGroupGraphPattern queryPattern = new SimpleGroupGraphPattern();
        queryPattern.addGraphPattern(bgp);

        List<Expression> arguments = new ArrayList<Expression>();
        arguments.add(x);
        arguments.add(new LiteralExpression(LiteralFactory.getInstance().
                createTypedLiteral(".*uni.*")));
        BuiltInCall constraint = new BuiltInCall("REGEX", arguments);
        queryPattern.addConstraint(constraint);
        selectQuery.setQueryPattern(queryPattern);
        Assert.assertTrue(selectQuery.toString()
                .replaceAll("( |\n)+", " ").trim().equals(queryString));
    }
}
