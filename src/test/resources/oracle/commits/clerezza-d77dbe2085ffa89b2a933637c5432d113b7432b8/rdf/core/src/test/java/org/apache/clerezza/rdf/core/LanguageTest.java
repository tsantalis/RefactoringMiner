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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.clerezza.commons.rdf.Language;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 *
 * @author reto
 */
@RunWith(JUnitPlatform.class)
public class LanguageTest {
    
    @Test
    public void languageEqualityTest() {
        Language lang1 = new Language("DE");
        Language lang2 = new Language("DE");
        assertEquals(lang1, lang2);
        assertEquals(lang1.hashCode(), lang2.hashCode());
        Language lang3 = new Language("EN");
        assertFalse(lang1.equals(lang3));
    }
    
    @Test
    public void toStringTest() {
        final String id = "de";
        Language lang1 = new Language(id);
        assertEquals(lang1.toString(), id);
    }

}
