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
package org.apache.clerezza.rdf.core;


import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.IRI;
import java.util.Arrays;
import java.util.Date;
import org.junit.Test;

import junit.framework.Assert;

/**
 *
 * @author reto
 */
public class LiteralFactoryTest {
    
    /**
     * Test that a NoConvertorException thrown for an unsupported convertor
     */
    @Test(expected=NoConvertorException.class)
    public void unavailableConvertor() {
        Object value = new Object() {};
        LiteralFactory.getInstance().createTypedLiteral(value);
    }

    /**
     * Test conversion of byte[] to literal an back
     */
    @Test
    public void byteArrayConversion() {
        byte[] bytes = new byte[5];
        for (byte i = 0; i < bytes.length; i++) {
            bytes[i] = i;
        }
        Literal literal = LiteralFactory.getInstance().createTypedLiteral(bytes);
        Assert.assertEquals(new IRI("http://www.w3.org/2001/XMLSchema#base64Binary"), 
                literal.getDataType());
        //we are using bytes.getClass() but there should be a way to get
        //that instance of Class without getting it from an instance
        //but this is java-bug 4071439 (would like byte[].class or byte.class.getArrayType())
        byte[] bytesBack = LiteralFactory.getInstance().createObject(bytes.getClass(), literal);
        Assert.assertTrue(Arrays.equals(bytes, bytesBack));

    }

    /**
     * Test conversion of java.util.Date to literal an back
     */
    @Test
    public void dateConversion() {
        Date date = new Date();
        Literal literal = LiteralFactory.getInstance().createTypedLiteral(date);
        Assert.assertEquals(new IRI("http://www.w3.org/2001/XMLSchema#dateTime"),
                literal.getDataType());
        Date dateBack = LiteralFactory.getInstance().createObject(Date.class, literal);
        Assert.assertEquals(date.getTime(), dateBack.getTime());

    }

    /**
     * Test conversion of String to literal an back
     */
    @Test
    public void stringConversion() {
        String value = "Hello world";
        Literal literal = LiteralFactory.getInstance().createTypedLiteral(value);
        Assert.assertEquals(new IRI("http://www.w3.org/2001/XMLSchema#string"),
                literal.getDataType());
        String valueBack = LiteralFactory.getInstance().createObject(String.class, literal);
        Assert.assertEquals(value, valueBack);

    }

    /**
     * Test conversion of Integer to literal an back
     */
    @Test
    public void intConversion() {
        int value = 3;
        Literal literal = LiteralFactory.getInstance().createTypedLiteral(value);
        Assert.assertEquals(new IRI("http://www.w3.org/2001/XMLSchema#int"),
                literal.getDataType());
        Integer valueBack = LiteralFactory.getInstance().createObject(Integer.class, literal);
        Assert.assertEquals(value, valueBack.intValue());

    }

    /**
     * Test conversion of Long to literal an back
     */
    @Test
    public void longConversion() {
        long value = 332314646;
        Literal literal = LiteralFactory.getInstance().createTypedLiteral(value);
        Assert.assertEquals(new IRI("http://www.w3.org/2001/XMLSchema#long"),
                literal.getDataType());
        Long valueBack = LiteralFactory.getInstance().createObject(Long.class, literal);
        Assert.assertEquals(value, valueBack.longValue());

    }


}
