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
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * Tests LdapNetworkConnection.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapNetworkConnectionTest
{

    private static Stream<Arguments> data()
    {
        // index 0: connection timout in ms
        // index 1: search time limit in seconds
        // index 2: expected timeout in ms
        return Stream.of(
            Arguments.of( 2000, -1, 2000, "Invalid search time limit, use connection timeout" ),
            Arguments.of( 2000, 0, 30000, "Search time limit is 0, use config default value" ),
            Arguments.of( 2000, 1, 2000, "search time limit < connection timeout, use connection timeout" ),
            Arguments.of( 2000, 5, 5000, "search time limit > connection timeout, use search time limit" ),
            Arguments.of( 2000, Integer.MAX_VALUE, 2147483647000L, "Integer overflow" ),
            Arguments.of( 30000, -1, 30000, "Invalid search time limit, use connection timeout" ),
            Arguments.of( 30000, 0, 30000, "Search time limit is 0, use config default value" ),
            Arguments.of( 30000, 1, 30000, "search time limit < connection timeout, use connection timeout" ),
            Arguments.of( 30000, 29, 30000, "search time limit < connection timeout, use connection timeout" ),
            Arguments.of( 30000, 31, 31000, "search time limit > connection timeout, use search time limit" ),
            Arguments.of( 30000, 60, 60000, "search time limit > connection timeout, use search time limit" ),
            Arguments.of( Long.MAX_VALUE, -1, Long.MAX_VALUE, "Invalid search time limit, use connection timeout" ),
            Arguments.of( Long.MAX_VALUE, 0, 30000, "Search time limit is 0, use config default value" ),
            Arguments.of( Long.MAX_VALUE, 1, Long.MAX_VALUE,
                "search time limit < connection timeout, use connection timeout" ) );
    }


    @ParameterizedTest
    @MethodSource("data")
    public void testGetClientTimeout( long connectionTimeoutInMS, int searchTimeLimitInSeconds,
        long expectedTimeoutInMS, String testDescription ) throws IOException
    {
        LdapNetworkConnection ldapConnection = null;

        try
        {
            ldapConnection = new LdapNetworkConnection( "localhost", 389 );
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
