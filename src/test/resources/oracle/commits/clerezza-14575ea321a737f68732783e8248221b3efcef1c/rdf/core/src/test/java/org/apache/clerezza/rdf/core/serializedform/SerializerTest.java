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

import org.apache.clerezza.commons.rdf.Graph;
import java.io.OutputStream;

import junit.framework.Assert;

import org.apache.clerezza.rdf.core.*;
import org.junit.Test;

/**
 *
 * @author mir
 */
public class SerializerTest {

    private static boolean providerAInvoked;
    private static boolean providerBInvoked;
    private SerializingProvider serializingProviderA = new SerializingProviderA();
    private SerializingProvider serializingProviderB = new SerializingProviderB();

    @Test
    public void registerOneProvider() {
        Serializer serializer = new Serializer(null);
        serializer.bindSerializingProvider(serializingProviderA);
        providerAInvoked = false;
        serializer.serialize(null, null, "application/x-fantasy2+rdf");
        Assert.assertTrue(providerAInvoked);
    }
    
    @Test
    public void registerAndUnregisterSecond() {
        Serializer serializer = new Serializer(null);
        serializer.bindSerializingProvider(serializingProviderA);
        serializer.bindSerializingProvider(serializingProviderB);
        providerAInvoked = false;
        providerBInvoked = false;
        serializer.serialize(null, null, "application/x-fantasy2+rdf");
        Assert.assertFalse(providerAInvoked);
        Assert.assertTrue(providerBInvoked);
        providerAInvoked = false;
        providerBInvoked = false;
        serializer.serialize(null, null, "application/x-fantasy1+rdf");
        Assert.assertTrue(providerAInvoked);
        Assert.assertFalse(providerBInvoked);
        serializer.unbindSerializingProvider(serializingProviderB);
        providerAInvoked = false;
        providerBInvoked = false;
        serializer.serialize(null, null, "application/x-fantasy2+rdf");
        Assert.assertTrue(providerAInvoked);
        Assert.assertFalse(providerBInvoked);
        
    }

    @SupportedFormat({"application/x-fantasy1+rdf", "application/x-fantasy2+rdf"})
    static class SerializingProviderA implements SerializingProvider {

        @Override
        public void serialize(OutputStream serializedGraph, Graph tc, String formatIdentifier) {
            providerAInvoked = true;
        }
    };
    @SupportedFormat("application/x-fantasy2+rdf")
    static class SerializingProviderB implements SerializingProvider {

        @Override
        public void serialize(OutputStream serializedGraph, Graph tc, String formatIdentifier) {
            providerBInvoked = true;
        }
    };
}
