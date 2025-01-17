/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.csv;

import static org.apache.commons.csv.TokenMatchers.hasContent;
import static org.apache.commons.csv.TokenMatchers.hasType;
import static org.apache.commons.csv.TokenMatchers.isReady;
import static org.apache.commons.csv.TokenMatchers.matches;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TokenMatchersTest {

    private Token token;

    @Before
    public void setUp() {
        token = new Token();
        token.type = Token.Type.TOKEN;
        token.isReady = true;
        token.content.append("content");
    }

    @Test
    public void testHasType() {
        assertFalse(hasType(Token.Type.COMMENT).matches(token));
        assertFalse(hasType(Token.Type.EOF).matches(token));
        assertFalse(hasType(Token.Type.EORECORD).matches(token));
        assertTrue(hasType(Token.Type.TOKEN).matches(token));
    }

    @Test
    public void testHasContent() {
        assertFalse(hasContent("This is not the token's content").matches(token));
        assertTrue(hasContent("content").matches(token));
    }

    @Test
    public void testIsReady() {
        assertTrue(isReady().matches(token));
        token.isReady = false;
        assertFalse(isReady().matches(token));
    }

    @Test
    public void testMatches() {
        assertTrue(matches(Token.Type.TOKEN, "content").matches(token));
        assertFalse(matches(Token.Type.EOF, "content").matches(token));
        assertFalse(matches(Token.Type.TOKEN, "not the content").matches(token));
        assertFalse(matches(Token.Type.EORECORD, "not the content").matches(token));
    }

}
