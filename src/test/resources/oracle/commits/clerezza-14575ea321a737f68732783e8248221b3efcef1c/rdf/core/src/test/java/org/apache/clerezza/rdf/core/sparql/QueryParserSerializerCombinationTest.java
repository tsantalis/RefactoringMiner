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

import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hasan
 */
public class QueryParserSerializerCombinationTest {
    
    public QueryParserSerializerCombinationTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testPatternOrderPreservation() throws Exception {
        String queryString =
                "SELECT ?property ?range ?property_description ?subproperty ?subproperty_description \n"
                + "WHERE\n"
                + "{ ?property <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#ObjectProperty> .\n"
                + "{ { ?property <http://www.w3.org/2000/01/rdf-schema#domain> ?superclass .\n"
                + "<http://example.org/ontologies/market_ontology.owl#Company> <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?superclass .\n"
                + "}  UNION { ?property <http://www.w3.org/2000/01/rdf-schema#domain> ?dunion .\n"
                + "?dunion <http://www.w3.org/2002/07/owl#unionOf> ?dlist .\n"
                + "?dlist <http://jena.hpl.hp.com/ARQ/list#member> ?superclass .\n"
                + "<http://example.org/ontologies/market_ontology.owl#Company> <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?superclass .\n"
                + "} } { { ?property <http://www.w3.org/2000/01/rdf-schema#range> ?superrange .\n"
                + "?range <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?superrange .\n"
                + "FILTER (! (isBLANK(?range)))\n"
                + "}  UNION { ?property <http://www.w3.org/2000/01/rdf-schema#range> ?range .\n"
                + "FILTER (! (isBLANK(?range)))\n"
                + "} }  OPTIONAL { ?somesub <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?range .\n"
                + "FILTER (((?somesub) != (<http://www.w3.org/2002/07/owl#Nothing>)) && ((?somesub) != (?range)))\n"
                + "}  OPTIONAL { ?subproperty <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> ?property .\n"
                 + " OPTIONAL { ?subproperty <http://purl.org/dc/elements/1.1/description> ?subproperty_description .\n"
                + "} FILTER (((?subproperty) != (<http://www.w3.org/2002/07/owl#bottomObjectProperty>)) && ((?subproperty) != (?property)))\n"
                + "}  OPTIONAL { ?property <http://purl.org/dc/elements/1.1/description> ?property_description .\n"
                + "} FILTER ((?property) != (<http://www.w3.org/2002/07/owl#bottomObjectProperty>))\n"
                + "FILTER ((?range) != (<http://www.w3.org/2002/07/owl#Nothing>))\n"
                + "FILTER (! (BOUND(?somesub)))\n"
                + "} \n";

        Query query = QueryParser.getInstance().parse(queryString);
        Assert.assertEquals(queryString.replaceAll("\\s", "").trim(), query.toString().replaceAll("\\s", "").trim());
    }

    @Test
    public void testParsingAndSerializationStability() throws Exception {
        String queryString =
                "PREFIX mo: <http://example.org/ontologies/market_ontology.owl#>\n"
                + "PREFIX list: <http://jena.hpl.hp.com/ARQ/list#>\n"
                + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
                + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
                + "SELECT ?property ?range ?property_description ?subproperty ?subproperty_description\n"
                + "WHERE {\n"
                + "    ?property a owl:ObjectProperty .\n"
                + "    FILTER (?property != owl:bottomObjectProperty) .\n"
                + "    {\n"
                + "        {\n"
                + "            ?property rdfs:domain ?superclass .\n"
                + "            mo:Company rdfs:subClassOf ?superclass .\n"
                + "        }\n"
                + "        UNION\n"
                + "        {\n"
                + "            ?property rdfs:domain ?dunion .\n"
                + "            ?dunion owl:unionOf ?dlist .\n"
                + "            ?dlist list:member ?superclass .\n"
                + "            mo:Company rdfs:subClassOf ?superclass .\n"
                + "        }\n"
                + "    }\n"
                + "    {\n"
                + "        {\n"
                + "            ?property rdfs:range ?superrange .\n"
                + "            ?range rdfs:subClassOf ?superrange .\n"
                + "            FILTER (!isBlank(?range)) .\n"
                + "        }\n"
                + "        UNION\n"
                + "        {\n"
                + "            ?property rdfs:range ?range .\n"
                + "            FILTER (!isBlank(?range)) .\n"
                + "        }\n"
                + "    } .\n"
                + "    FILTER (?range != owl:Nothing) .\n"
                + "    OPTIONAL { ?somesub rdfs:subClassOf ?range . FILTER(?somesub != owl:Nothing && ?somesub != ?range)}\n"
                + "    FILTER (!bound(?somesub)) .\n"
                + "    OPTIONAL {\n"
                + "        ?subproperty rdfs:subPropertyOf ?property .\n"
                + "        FILTER(?subproperty != owl:bottomObjectProperty && ?subproperty != ?property)\n"
                + "        OPTIONAL { ?subproperty dc:description ?subproperty_description . }\n"
                + "    }\n"
                + "    OPTIONAL { ?property dc:description ?property_description . }\n"
                + "} ";

        Query query1 = QueryParser.getInstance().parse(queryString);
        Thread.sleep(5000l);
        Query query2 = QueryParser.getInstance().parse(queryString);
        Assert.assertEquals(query1.toString(), query2.toString());
    }
}
