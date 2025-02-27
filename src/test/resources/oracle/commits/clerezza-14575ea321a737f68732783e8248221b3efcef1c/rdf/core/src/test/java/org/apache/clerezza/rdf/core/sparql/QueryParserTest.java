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
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.sparql.query.AskQuery;
import org.apache.clerezza.rdf.core.sparql.query.BasicGraphPattern;
import org.apache.clerezza.rdf.core.sparql.query.BuiltInCall;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.DescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.clerezza.rdf.core.sparql.query.GraphPattern;
import org.apache.clerezza.rdf.core.sparql.query.GroupGraphPattern;
import org.apache.clerezza.rdf.core.sparql.query.OptionalGraphPattern;
import org.apache.clerezza.rdf.core.sparql.query.OrderCondition;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.core.sparql.query.QueryWithSolutionModifier;
import org.apache.clerezza.rdf.core.sparql.query.ResourceOrVariable;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.clerezza.rdf.core.sparql.query.TriplePattern;
import org.apache.clerezza.rdf.core.sparql.query.UnaryOperation;
import org.apache.clerezza.rdf.core.sparql.query.UriRefOrVariable;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleTriplePattern;

/**
 *
 * @author hasan
 */
public class QueryParserTest {

    @Test
    public void testSelectQuery() throws ParseException {

// SELECT ?title FROM <http://example.org/library>
// WHERE { <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title . }

        final String variable = "title";
        final String defaultGraph = "http://example.org/library";
        final String subject = "http://example.org/book/book1";
        final String predicate = "http://purl.org/dc/elements/1.1/title";

        StringBuffer queryStrBuf = new StringBuffer();
        queryStrBuf.append("SELECT ?").append(variable)
                .append(" FROM <").append(defaultGraph)
                .append("> WHERE { <").append(subject).append("> <")
                .append(predicate).append("> ?").append(variable).append(" . }");

        Query q = QueryParser.getInstance().parse(queryStrBuf.toString());
        Assert.assertTrue(SelectQuery.class.isAssignableFrom(q.getClass()));
        SelectQuery selectQuery = (SelectQuery) q;
        Assert.assertTrue(selectQuery.getSelection().get(0)
                .equals(new Variable(variable)));
        Assert.assertTrue(selectQuery.getDataSet().getDefaultGraphs().toArray()[0]
                .equals(new IRI(defaultGraph)));

        GraphPattern gp = (GraphPattern) selectQuery.getQueryPattern()
                .getGraphPatterns().toArray()[0];
        Assert.assertTrue(BasicGraphPattern.class.isAssignableFrom(gp.getClass()));
        BasicGraphPattern bgp = (BasicGraphPattern) gp;

        Set<TriplePattern> triplePatterns = bgp.getTriplePatterns();
        Assert.assertTrue(triplePatterns.size()==1);

        ResourceOrVariable s = new ResourceOrVariable(new IRI(subject));
        UriRefOrVariable p = new UriRefOrVariable(new IRI(predicate));
        ResourceOrVariable o = new ResourceOrVariable(new Variable(variable));

        Assert.assertTrue(triplePatterns.contains(
                new SimpleTriplePattern(s, p, o)));
    }

    @Test(expected=ParseException.class)
    public void testInvalidQuery() throws ParseException {
        Query q = QueryParser.getInstance().parse("Hello");
    }

    @Test
    public void testSelectQuerySelection() throws ParseException {
        SelectQuery q = (SelectQuery) QueryParser.getInstance().parse(
                "SELECT ?a ?b WHERE {?a ?x ?b}");
        Set<Variable> selectionSet = new HashSet<Variable>(
                q.getSelection());
        Set<Variable> expected = new HashSet<Variable>();
        expected.add(new Variable("a"));
        expected.add(new Variable("b"));
        Assert.assertEquals(expected, selectionSet);
        Assert.assertFalse(q.isSelectAll());

    }

    @Test
    public void testSelectAll() throws ParseException {
        SelectQuery q = (SelectQuery) QueryParser.getInstance().parse(
                "SELECT * WHERE {?a ?x ?b}");
        Set<Variable> selectionSet = new HashSet<Variable>(
                q.getSelection());
        Set<Variable> expected = new HashSet<Variable>();
        expected.add(new Variable("a"));
        expected.add(new Variable("b"));
        expected.add(new Variable("x"));
        Assert.assertEquals(expected, selectionSet);
        Assert.assertTrue(q.isSelectAll());

    }

    @Test
    public void testPlainLiteral() throws ParseException {
        SelectQuery q = (SelectQuery) QueryParser.getInstance().parse(
                "SELECT * WHERE {?a ?x 'tiger' . ?a ?x 'lion'@en . }");

        GraphPattern gp = (GraphPattern) q.getQueryPattern()
                .getGraphPatterns().toArray()[0];
        Assert.assertTrue(BasicGraphPattern.class.isAssignableFrom(gp.getClass()));
        BasicGraphPattern bgp = (BasicGraphPattern) gp;

        Set<TriplePattern> triplePatterns = bgp.getTriplePatterns();
        Assert.assertTrue(triplePatterns.size()==2);

        Assert.assertTrue(triplePatterns.contains(new SimpleTriplePattern(
                new Variable("a"), new Variable("x"),
                new PlainLiteralImpl("tiger"))));

        Assert.assertTrue(triplePatterns.contains(new SimpleTriplePattern(
                new Variable("a"), new Variable("x"),
                new PlainLiteralImpl("lion", new Language("en")))));
    }

    @Test
    public void testOrderBy() throws ParseException {
        SelectQuery q = (SelectQuery) QueryParser.getInstance().parse(
                "SELECT * WHERE {?a ?x ?b} ORDER BY DESC(?b)");

        List<OrderCondition> oc = ((QueryWithSolutionModifier) q).getOrderConditions();
        Assert.assertTrue(oc.size()==1);
        Assert.assertFalse(oc.get(0).isAscending());
        Variable b = new Variable("b");
        Assert.assertEquals(b, oc.get(0).getExpression());
    }

    @Test
    public void testConstructQuery() throws ParseException {

// CONSTRUCT { <http://example.org/person#Alice> <http://www.w3.org/2001/vcard-rdf/3.0#FN> ?name}
// WHERE { ?x <http://xmlns.com/foaf/0.1/name> ?name}


        final String variable1 = "name";
        final String variable2 = "x";
        final String subject1 = "http://example.org/person#Alice";
        final String predicate1 = "http://www.w3.org/2001/vcard-rdf/3.0#FN";
        final String predicate2 = "http://xmlns.com/foaf/0.1/name";

        StringBuffer queryStrBuf = new StringBuffer();
        queryStrBuf.append("CONSTRUCT { <").append(subject1).append("> <")
                .append(predicate1).append("> ?").append(variable1)
                .append("} WHERE { ?").append(variable2).append(" <")
                .append(predicate2).append("> ?").append(variable1).append("}");

        Query q = QueryParser.getInstance().parse(queryStrBuf.toString());
        Assert.assertTrue(ConstructQuery.class.isAssignableFrom(q.getClass()));
        ConstructQuery constructQuery = (ConstructQuery) q;
        Set<TriplePattern> triplePatterns = constructQuery
                .getConstructTemplate();
        Assert.assertTrue(triplePatterns.size()==1);

        ResourceOrVariable s = new ResourceOrVariable(new IRI(subject1));
        UriRefOrVariable p = new UriRefOrVariable(new IRI(predicate1));
        ResourceOrVariable o = new ResourceOrVariable(new Variable(variable1));

        Assert.assertTrue(triplePatterns.contains(
                new SimpleTriplePattern(s, p, o)));

        GraphPattern gp = (GraphPattern) constructQuery.getQueryPattern()
                .getGraphPatterns().toArray()[0];
        Assert.assertTrue(BasicGraphPattern.class.isAssignableFrom(gp.getClass()));
        BasicGraphPattern bgp = (BasicGraphPattern) gp;
        triplePatterns = bgp.getTriplePatterns();
        Assert.assertTrue(triplePatterns.size()==1);

        s = new ResourceOrVariable(new Variable(variable2));
        p = new UriRefOrVariable(new IRI(predicate2));

        Assert.assertTrue(triplePatterns.contains(
                new SimpleTriplePattern(s, p, o)));
    }

    @Test
    public void testDescribeQuery() throws ParseException {

// DESCRIBE <http://example.org/book/book1>

        final String resource = "http://example.org/book/book1";

        StringBuffer queryStrBuf = new StringBuffer();
        queryStrBuf.append("DESCRIBE <").append(resource).append(">");

        Query q = QueryParser.getInstance().parse(queryStrBuf.toString());
        Assert.assertTrue(DescribeQuery.class.isAssignableFrom(q.getClass()));
        DescribeQuery describeQuery = (DescribeQuery) q;
        Assert.assertTrue(describeQuery.getResourcesToDescribe().get(0)
                .getResource().equals(new IRI(resource)));
    }

    @Test
    public void testAskQuery() throws ParseException {

// ASK { ?x <http://xmlns.com/foaf/0.1/name> "Alice" }

        final String variable = "x";
        final String predicate = "http://xmlns.com/foaf/0.1/name";
        final String object = "Alice";

        StringBuffer queryStrBuf = new StringBuffer();
        queryStrBuf.append("ASK { ?").append(variable).append(" <")
                .append(predicate).append("> \"").append(object).append("\" }");

        Query q = QueryParser.getInstance().parse(queryStrBuf.toString());
        Assert.assertTrue(AskQuery.class.isAssignableFrom(q.getClass()));
        AskQuery askQuery = (AskQuery) q;

        GraphPattern gp = (GraphPattern) askQuery.getQueryPattern()
                .getGraphPatterns().toArray()[0];
        Assert.assertTrue(BasicGraphPattern.class.isAssignableFrom(gp.getClass()));
        BasicGraphPattern bgp = (BasicGraphPattern) gp;

        Set<TriplePattern> triplePatterns = bgp.getTriplePatterns();
        Assert.assertTrue(triplePatterns.size()==1);

        Assert.assertTrue(triplePatterns.contains(new SimpleTriplePattern(new Variable(variable),
                new IRI(predicate), new PlainLiteralImpl(object))));
    }

    @Test
    public void testBaseAndPrefix() throws ParseException {

// BASE    <http://example.org/book/>
// PREFIX  dc: <http://purl.org/dc/elements/1.1/>
//
// SELECT  $title
// WHERE   { <book1>  dc:title  ?title }

        final String base = "http://example.org/book/";
        final String prefix = "dc";
        final String prefixUri = "http://purl.org/dc/elements/1.1/";
        final String variable = "title";
        final String subject = "book1";
        final String predicate = "title";

        StringBuffer queryStrBuf = new StringBuffer();
        queryStrBuf.append("BASE <").append(base).append(">")
                .append(" PREFIX ").append(prefix).append(": <")
                .append(prefixUri).append("> SELECT $").append(variable)
                .append(" WHERE { <").append(subject).append("> ")
                .append(prefix).append(":").append(predicate).append(" ?")
                .append(variable).append(" }");

        Query q = QueryParser.getInstance().parse(queryStrBuf.toString());
        Assert.assertTrue(SelectQuery.class.isAssignableFrom(q.getClass()));
        SelectQuery selectQuery = (SelectQuery) q;
        Assert.assertTrue(selectQuery.getSelection().get(0)
                .equals(new Variable(variable)));

        GraphPattern gp = (GraphPattern) selectQuery.getQueryPattern()
                .getGraphPatterns().toArray()[0];
        Assert.assertTrue(BasicGraphPattern.class.isAssignableFrom(gp.getClass()));
        BasicGraphPattern bgp = (BasicGraphPattern) gp;

        Set<TriplePattern> triplePatterns = bgp.getTriplePatterns();
        Assert.assertTrue(triplePatterns.size()==1);

        ResourceOrVariable s = new ResourceOrVariable(new IRI(base+subject));
        UriRefOrVariable p = new UriRefOrVariable(new IRI(prefixUri+predicate));
        ResourceOrVariable o = new ResourceOrVariable(new Variable(variable));

        Assert.assertTrue(triplePatterns.contains(
                new SimpleTriplePattern(s, p, o)));
    }

    @Test
    public void testOptionalAndFilter() throws ParseException {

// PREFIX dc: <http://purl.org/dc/elements/1.1/>
// PREFIX books: <http://example.org/book/>
//
// SELECT ?book ?title
// WHERE
//  { ?book dc:title ?title .
//    OPTIONAL
//     { ?book books:author ?author .}
//     FILTER ( ! bound(?author) )
//  }
        final String prefix1 = "dc";
        final String prefix1Uri = "http://purl.org/dc/elements/1.1/";
        final String prefix2 = "books";
        final String prefix2Uri = "http://example.org/book/";
        final String variable1 = "book";
        final String variable2 = "title";
        final String variable3 = "author";
        final String predicate1 = "title";
        final String predicate2 = "author";

        StringBuffer queryStrBuf = new StringBuffer();
        queryStrBuf.append("PREFIX ").append(prefix1).append(": <").append(prefix1Uri)
                .append("> PREFIX ").append(prefix2).append(": <").append(prefix2Uri)
                .append("> SELECT ?").append(variable1).append(" ?").append(variable2)
                .append(" WHERE { ?").append(variable1).append(" ")
                .append(prefix1).append(":").append(predicate1)
                .append(" ?").append(variable2).append(" . OPTIONAL { ?")
                .append(variable1).append(" ").append(prefix2).append(":")
                .append(predicate2).append(" ?").append(variable3)
                .append(" .} FILTER ( ! bound(?").append(variable3).append(") ) }");

        Query q = QueryParser.getInstance().parse(queryStrBuf.toString());
        Assert.assertTrue(SelectQuery.class.isAssignableFrom(q.getClass()));
        SelectQuery selectQuery = (SelectQuery) q;
        Assert.assertTrue(selectQuery.getSelection().size() == 2);
        Set<Variable> vars = new HashSet<Variable>(2);
        Variable var1 = new Variable(variable1);
        Variable var2 = new Variable(variable2);
        vars.add(var1);
        vars.add(var2);
        Assert.assertTrue(selectQuery.getSelection().containsAll(vars));

        GroupGraphPattern ggp = selectQuery.getQueryPattern();
        List<Expression> constraints = ggp.getFilter();
        Assert.assertTrue(UnaryOperation.class.isAssignableFrom(constraints
                .get(0).getClass()));
        UnaryOperation uop = (UnaryOperation) constraints.get(0);
        Assert.assertTrue(uop.getOperatorString().equals("!"));
        Assert.assertTrue(BuiltInCall.class.isAssignableFrom(uop.getOperand()
                .getClass()));
        BuiltInCall bic = (BuiltInCall) uop.getOperand();
        Assert.assertTrue(bic.getName().equals("BOUND"));
        Variable var3 = new Variable(variable3);
        Assert.assertTrue(bic.getArguements().get(0).equals(var3));

        GraphPattern gp = (GraphPattern) ggp.getGraphPatterns().toArray()[0];
        Assert.assertTrue(OptionalGraphPattern.class.isAssignableFrom(gp.getClass()));
        OptionalGraphPattern ogp = (OptionalGraphPattern) gp;
        Assert.assertTrue(BasicGraphPattern.class.isAssignableFrom(
                ogp.getMainGraphPattern().getClass()));
        BasicGraphPattern bgp = (BasicGraphPattern) ogp.getMainGraphPattern();

        Set<TriplePattern> triplePatterns = bgp.getTriplePatterns();
        Assert.assertTrue(triplePatterns.size() == 1);
        Assert.assertTrue(triplePatterns.contains(new SimpleTriplePattern(var1, new IRI(prefix1Uri + predicate1),
                var2)));

        GraphPattern gp2 = (GraphPattern) ogp.getOptionalGraphPattern()
                .getGraphPatterns().toArray()[0];
        Assert.assertTrue(BasicGraphPattern.class.isAssignableFrom(gp2.getClass()));
        bgp = (BasicGraphPattern) gp2;

        triplePatterns = bgp.getTriplePatterns();
        Assert.assertTrue(triplePatterns.size() == 1);
        Assert.assertTrue(triplePatterns.contains(new SimpleTriplePattern(var1, new IRI(prefix2Uri + predicate2),
                var3)));
    }
}
