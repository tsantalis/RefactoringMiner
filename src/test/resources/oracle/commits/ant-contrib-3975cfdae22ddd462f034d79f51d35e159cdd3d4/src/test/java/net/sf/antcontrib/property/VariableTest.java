/*
 * Copyright (c) 2001-2004 Ant-Contrib project.  All rights reserved.
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
package net.sf.antcontrib.property;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Testcase for &lt;variable&gt;.
 */
public class VariableTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/property/variabletest.xml");
    }

    /**
     * Method test1.
     */
    @Test
    public void test1() {
        buildRule.executeTarget("test1");
        assertEquals(buildRule.getProject().getProperty("x"), "6");
    }

    /**
     * Method test2.
     */
    @Test
    public void test2() {
        buildRule.executeTarget("test2");
        assertEquals(buildRule.getProject().getProperty("x"), "12");
    }

    /**
     * Method test3.
     */
    @Test
    public void test3() {
        buildRule.executeTarget("test3");
        assertEquals(buildRule.getProject().getProperty("x"), "6 + 12");
    }

    /**
     * Method test4.
     */
    @Test
    public void test4() {
        buildRule.executeTarget("test4");
        assertEquals(buildRule.getProject().getProperty("x"), "6");
    }

    /**
     * Method test5.
     */
    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        assertEquals(buildRule.getProject().getProperty("str"), "I am a string.");
    }

    /**
     * Method test6.
     */
    @Test
    public void test6() {
        buildRule.executeTarget("test6");
        assertEquals(buildRule.getProject().getProperty("x"), "Today is blue.");
    }

    /**
     * Method test7.
     */
    @Test
    public void test7() {
        buildRule.executeTarget("test7");
        assertEquals(buildRule.getProject().getProperty("x"), "6");
    }

    /**
     * Method test8.
     */
    @Test
    public void test8() {
        buildRule.executeTarget("test8");
        assertEquals(buildRule.getProject().getProperty("x"), "12");
        assertThat(buildRule.getLog(), containsString("12"));
    }

    /**
     * Method test9.
     */
    @Test
    public void test9() {
        buildRule.executeTarget("test9");
        assertNull(buildRule.getProject().getProperty("i"));
    }

    /**
     * Method test10.
     */
    @Test
    public void test10() {
        buildRule.executeTarget("test10");
        assertEquals(buildRule.getProject().getProperty("x"), "xxx");
    }
}
