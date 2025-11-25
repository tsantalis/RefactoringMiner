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

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;switch&gt;.
 */
public class SwitchTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("logic/switch.xml");
    }

    /**
     * Method testNoValue.
     */
    @Test
    public void testNoValue() {
        expectSpecificBuildException("noValue", "no value",
                "Value is missing");
    }

    /**
     * Method testNoChildren.
     */
    @Test
    public void testNoChildren() {
        expectSpecificBuildException("noChildren", "no children",
                "No cases supplied");
    }

    /**
     * Method testTwoDefaults.
     */
    @Test
    public void testTwoDefaults() {
        expectSpecificBuildException("twoDefaults", "two defaults",
                "Cannot specify multiple default cases");
    }

    /**
     * Method testNoMatch.
     */
    @Test
    public void testNoMatch() {
        expectSpecificBuildException("noMatch", "no match",
                "No case matched the value foo"
                        + " and no default has been specified.");
    }

    /**
     * Method testCaseNoValue.
     */
    @Test
    public void testCaseNoValue() {
        expectSpecificBuildException("caseNoValue", "<case> no value",
                "Value is required for case.");
    }

    /**
     * Method testDefault.
     */
    @Test
    public void testDefault() {
        executeTarget("testDefault");
        assertLogContaining("In default");
        assertLogContaining("baz");
        assertLogNotContaining("${inner}");
        assertLogNotContaining("In case");
    }

    /**
     * Method testCase.
     */
    @Test
    public void testCase() {
        executeTarget("testCase");
        assertLogContaining("In case");
        assertLogContaining("baz");
        assertLogNotContaining("${inner}");
        assertLogNotContaining("In default");
    }

    /**
     * Method testCaseSensitive.
     */
    @Test
    public void testCaseSensitive() {
        executeTarget("testCaseSensitive");
        assertLogContaining("In default");
        assertLogNotContaining("In case");
    }

    /**
     * Method testCaseInSensitive.
     */
    @Test
    public void testCaseInSensitive() {
        executeTarget("testCaseInSensitive");
        assertLogContaining("In case");
        assertLogNotContaining("In default");
    }
}
