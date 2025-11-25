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
package net.sf.antcontrib.logic;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Since AntCallBack is basically a copy and paste of antcall, the only testing
 * done here is on the extra features provided by antcallback. It is assumed
 * that changes to antcall will be propagated to antcallback and that antcall
 * has it's own unit tests (which turns out to have been a bad assumption,
 * I can't find any unit tests for antcall).
 *
 * @author <a href="mailto:danson@germane-software.com">Dale Anson</a>
 */
public class AntCallBackTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    /**
     * The JUnit setup method.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/logic/antcallbacktest.xml");
    }

    /**
     * A unit test for JUnit.
     */
    @Test
    public void test1() {
        buildRule.executeTarget("test1");
        expectPropertySet("prop1", "prop1");
    }

    /**
     * A unit test for JUnit.
     */
    @Test
    public void test2() {
        buildRule.executeTarget("test2");
        expectPropertySet("prop1", "prop1");
        expectPropertySet("prop2", "prop2");
        expectPropertySet("prop3", "prop3");
    }

    /**
     * A unit test for JUnit.
     */
    @Test
    public void test3() {
        buildRule.executeTarget("test3");
        expectPropertySet("prop1", "prop1");
        expectPropertySet("prop2", "prop2");
        expectPropertySet("prop3", "prop3");
    }

    /**
     * A unit test for JUnit.
     */
    @Test
    public void test4() {
        buildRule.executeTarget("test4");
        expectPropertyUnset("prop1");
        expectPropertySet("prop2", "prop2");
        expectPropertySet("prop3", "prop3");
    }

    /**
     * A unit test for JUnit.
     */
    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        expectPropertySet("prop1", "blah");
        expectPropertySet("prop2", "prop2");
        expectPropertySet("prop3", "prop3");
    }

    private void expectPropertySet(String property, String value) {
        assertEquals("property " + property, value,
                buildRule.getProject().getProperty(property));
    }

    private void expectPropertyUnset(String property) {
        String result = buildRule.getProject().getProperty(property);
        assertNull("Expected property " + property
                + " to be unset, but it is set to the value: " + result, result);
    }
}
