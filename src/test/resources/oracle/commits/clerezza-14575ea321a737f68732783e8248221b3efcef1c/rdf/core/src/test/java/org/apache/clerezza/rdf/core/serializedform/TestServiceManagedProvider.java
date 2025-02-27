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
package org.apache.clerezza.rdf.core.serializedform;

import java.io.InputStream;
import org.apache.clerezza.commons.rdf.Graph;
import org.junit.Assert;
import org.junit.Test;
import org.apache.clerezza.commons.rdf.IRI;

/**
 * This class is listed in
 * META-INF/services/org.apache.clerezza.serializedform.ParsingProvider
 *
 * @author reto
 */
@SupportedFormat("application/x-test+rdf")
public class TestServiceManagedProvider implements ParsingProvider {

    private static boolean parseInvoked;

    @Override
    public void parse(Graph target, InputStream serializedGraph, String formatIdentifier, IRI baseUri) {
        parseInvoked = true;
    }
    
    @Test
    public void registerOneProvider() {
        Parser parser = Parser.getInstance();
        parser.parse(null, "application/x-test+rdf");
        Assert.assertTrue(parseInvoked);
    }
}
