/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.ldap.client.api;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * Tests LdapNetworkConnection.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(Parameterized.class)
public class LdapNetworkConnectionTest
{

    private long connectionTimeoutInMS;
    private int searchTimeLimitInSeconds;
    private long expectedTimeoutInMS;


    @Parameters(name = "{index}: {0},{1}->{2}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList( new Object[][]
            {
                    // index 0: connection timout in ms
                    // index 1: search time limit in seconds
                    // index 2: expected timeout in ms
                    { 2000, -1, 2000, "Invalid search time limit, use connection timeout" },
                    { 2000, 0, Long.MAX_VALUE, "Search time limit is 0, use max value" },
                    { 2000, 1, 2000, "search time limit < connection timeout, use connection timeout" },
                    { 2000, 5, 5000, "search time limit > connection timeout, use search time limit" },
                    { 2000, Integer.MAX_VALUE, 2147483647000L, "Integer overflow" },
                    { 30000, -1, 30000, "Invalid search time limit, use connection timeout" },
                    { 30000, 0, Long.MAX_VALUE, "Search time limit is 0, use max value" },
                    { 30000, 1, 30000, "search time limit < connection timeout, use connection timeout" },
                    { 30000, 29, 30000, "search time limit < connection timeout, use connection timeout" },
                    { 30000, 31, 31000, "search time limit > connection timeout, use search time limit" },
                    { 30000, 60, 60000, "search time limit > connection timeout, use search time limit" },
                    { Long.MAX_VALUE, -1, Long.MAX_VALUE, "Invalid search time limit, use connection timeout" },
                    { Long.MAX_VALUE, 0, Long.MAX_VALUE, "Search time limit is 0, use max value" },
                    { Long.MAX_VALUE, 1, Long.MAX_VALUE,
                        "search time limit < connection timeout, use connection timeout" }, } );
    }


    public LdapNetworkConnectionTest( long connectionTimeoutInMS, int searchTimeLimitInSeconds,
        long expectedTimeoutInMS, String testDescription )
    {
        this.connectionTimeoutInMS = connectionTimeoutInMS;
        this.searchTimeLimitInSeconds = searchTimeLimitInSeconds;
        this.expectedTimeoutInMS = expectedTimeoutInMS;
    }


    @Test
    @Ignore
    public void testGetClientTimeout() throws IOException
    {
        LdapNetworkConnection ldapConnection = null;
        
        try
        {
            ldapConnection = new LdapNetworkConnection();
            long timeout = ldapConnection.getTimeout( connectionTimeoutInMS, searchTimeLimitInSeconds );
            assertEquals( expectedTimeoutInMS, timeout );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                ldapConnection.close();
            }
        }
    }

}
