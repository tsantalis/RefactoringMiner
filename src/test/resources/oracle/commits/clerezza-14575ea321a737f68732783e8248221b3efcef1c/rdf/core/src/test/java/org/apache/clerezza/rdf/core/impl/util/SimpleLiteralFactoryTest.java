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
package org.apache.clerezza.rdf.core.impl.util;

import junit.framework.Assert;
import org.apache.clerezza.rdf.core.impl.util.SimpleLiteralFactory;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.IRI;
import org.junit.Test;

/**
 *
 * @author reto
 */
public class SimpleLiteralFactoryTest {

    final private static IRI xsdInteger = 
            new IRI("http://www.w3.org/2001/XMLSchema#integer");
    final private static IRI xsdInt =
            new IRI("http://www.w3.org/2001/XMLSchema#int");
    final private static IRI xsdLong =
            new IRI("http://www.w3.org/2001/XMLSchema#long");

    SimpleLiteralFactory simpleLiteralFactory = new SimpleLiteralFactory();

    @Test
    public void longToXsdIntegerAndBackToMany() {
        long value = 14l;
        Literal tl = simpleLiteralFactory.createTypedLiteral(value);
        Assert.assertEquals(xsdLong, tl.getDataType());
        long longValue = simpleLiteralFactory.createObject(Long.class, tl);
        Assert.assertEquals(value, longValue);
        int intValue = simpleLiteralFactory.createObject(Integer.class, tl);
        Assert.assertEquals(value, intValue);
    }

    @Test
    public void intToXsdIntAndBackToMany() {
        int value = 14;
        Literal tl = simpleLiteralFactory.createTypedLiteral(value);
        Assert.assertEquals(xsdInt, tl.getDataType());
        long longValue = simpleLiteralFactory.createObject(Long.class, tl);
        Assert.assertEquals(value, longValue);
        int intValue = simpleLiteralFactory.createObject(Integer.class, tl);
        Assert.assertEquals(value, intValue);
    }
}
