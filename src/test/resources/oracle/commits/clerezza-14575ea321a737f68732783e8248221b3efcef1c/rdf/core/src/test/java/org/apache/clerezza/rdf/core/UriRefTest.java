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

import org.apache.clerezza.commons.rdf.IRI;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.junit.Test;
import junit.framework.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author reto
 */
public class UriRefTest {
    
    private Logger logger = LoggerFactory.getLogger(UriRefTest.class);
    
    @Test
    public void uriRefEqualityTest() {
        try {
            String uriRefString = "http://example.org/üöä";
            IRI uriRef1 = new IRI(uriRefString);
            IRI uriRef2 = new IRI(uriRefString);
            Assert.assertEquals(uriRef1, uriRef2);
            IRI uriRef3 =
                    new IRI(URLEncoder.encode(uriRefString, "utf-8"));
            Assert.assertFalse(uriRef1.equals(uriRef3));
        } catch (UnsupportedEncodingException ex) {
            logger.error("Exception {} ", ex);
        }
    }
    
    @Test
    public void toStringTest() {
        String uriRefString = "http://example.org/üöä";
        IRI uriRef = new IRI(uriRefString);
        Assert.assertEquals("<"+uriRefString+">", uriRef.toString());
    }

}
