/*
 * Copyright (c) 2006 Ant-Contrib project.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.antcontrib.logic;

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;for&gt;.
 */
public class ForTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("logic/for.xml");
    }

    /**
     * Method testLoop.
     */
    @Test
    public void testLoop() {
        expectLogContaining("loop", "i is 10");
    }

    /**
     * Method testStep.
     */
    @Test
    public void testStep() {
        executeTarget("step");
        assertLogContaining("i is 10");
        assertLogNotContaining("i is 3");
    }
}
