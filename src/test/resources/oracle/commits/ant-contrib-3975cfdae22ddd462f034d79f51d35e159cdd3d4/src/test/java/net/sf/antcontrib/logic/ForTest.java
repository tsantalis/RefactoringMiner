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

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Testcase for &lt;for&gt;.
 */
public class ForTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/logic/for.xml");
    }

    /**
     * Method testLoop.
     */
    @Test
    public void testLoop() {
        buildRule.executeTarget("loop");
        assertThat(buildRule.getLog(), containsString("i is 10"));
    }

    /**
     * Method testStep.
     */
    @Test
    public void testStep() {
        buildRule.executeTarget("step");
        assertThat(buildRule.getLog(), both(containsString("i is 10"))
                .and(not(containsString("i is 3"))));
    }
}
