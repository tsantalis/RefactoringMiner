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

package org.apache.directory.api.dsmlv2;


import org.apache.directory.api.dsmlv2.abandonRequest.AbandonRequestTest;
import org.apache.directory.api.dsmlv2.addRequest.AddRequestTest;
import org.apache.directory.api.dsmlv2.addResponse.AddResponseTest;
import org.apache.directory.api.dsmlv2.authRequest.AuthRequestTest;
import org.apache.directory.api.dsmlv2.authResponse.AuthResponseTest;
import org.apache.directory.api.dsmlv2.batchRequest.BatchRequestTest;
import org.apache.directory.api.dsmlv2.batchResponse.BatchResponseTest;
import org.apache.directory.api.dsmlv2.compareRequest.CompareRequestTest;
import org.apache.directory.api.dsmlv2.compareResponse.CompareResponseTest;
import org.apache.directory.api.dsmlv2.delRequest.DelRequestTest;
import org.apache.directory.api.dsmlv2.delResponse.DelResponseTest;
import org.apache.directory.api.dsmlv2.errorResponse.ErrorResponseTest;
import org.apache.directory.api.dsmlv2.extendedRequest.ExtendedRequestTest;
import org.apache.directory.api.dsmlv2.extendedResponse.ExtendedResponseTest;
import org.apache.directory.api.dsmlv2.modDNRequest.ModifyDNRequestTest;
import org.apache.directory.api.dsmlv2.modDNResponse.ModifyDNResponseTest;
import org.apache.directory.api.dsmlv2.modifyRequest.ModifyRequestTest;
import org.apache.directory.api.dsmlv2.modifyResponse.ModifyResponseTest;
import org.apache.directory.api.dsmlv2.searchRequest.SearchRequestTest;
import org.apache.directory.api.dsmlv2.searchResponse.SearchResponseTest;
import org.apache.directory.api.dsmlv2.searchResponse.searchResultDone.SearchResultDoneTest;
import org.apache.directory.api.dsmlv2.searchResponse.searchResultEntry.SearchResultEntryTest;
import org.apache.directory.api.dsmlv2.searchResponse.searchResultReference.SearchResultReferenceTest;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

/**
 * This is the complete Test Suite for DSMLv2 Parser (Request and Response)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(JUnitPlatform.class)
@SelectClasses(
    {
        AbandonRequestTest.class,
        AddRequestTest.class,
        AddResponseTest.class,
        AuthRequestTest.class,
        AuthResponseTest.class,
        BatchRequestTest.class,
        BatchResponseTest.class,
        CompareRequestTest.class,
        CompareResponseTest.class,
        DelRequestTest.class,
        DelResponseTest.class,
        ErrorResponseTest.class,
        ExtendedRequestTest.class,
        ExtendedResponseTest.class,
        ModifyDNRequestTest.class,
        ModifyDNResponseTest.class,
        ModifyRequestTest.class,
        ModifyResponseTest.class,
        SearchRequestTest.class,
        SearchResponseTest.class,
        SearchResultDoneTest.class,
        SearchResultEntryTest.class,
        SearchResultReferenceTest.class
})
public class AllTests
{
}
