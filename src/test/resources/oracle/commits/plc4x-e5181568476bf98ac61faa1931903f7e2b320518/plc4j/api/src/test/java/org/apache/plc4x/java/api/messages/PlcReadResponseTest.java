/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
package org.apache.plc4x.java.api.messages;

import org.apache.plc4x.java.api.messages.items.ReadRequestItem;
import org.apache.plc4x.java.api.messages.items.ReadResponseItem;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class PlcReadResponseTest {

    @Test
    void constuctor() {
        new PlcReadResponse(mock(PlcReadRequest.class), mock(ReadResponseItem.class));
        new PlcReadResponse(mock(PlcReadRequest.class), (List) Collections.singletonList(mock(ReadResponseItem.class)));
    }

    @Test
    void getValue() {
        new PlcReadResponse(mock(PlcReadRequest.class), (List) Collections.singletonList(mock(ReadResponseItem.class, RETURNS_DEEP_STUBS)))
            .getValue(mock(ReadRequestItem.class));
    }

}