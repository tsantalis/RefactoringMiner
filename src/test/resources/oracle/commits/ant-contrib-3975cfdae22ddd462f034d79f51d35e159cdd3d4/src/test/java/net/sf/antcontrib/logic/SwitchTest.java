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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Testcase for &lt;switch&gt;.
 */
public class SwitchTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/logic/switch.xml");
    }

    /**
     * Method testNoValue.
     */
    @Test
    public void testNoValue() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Value is missing");
        buildRule.executeTarget("noValue");
    }

    /**
     * Method testNoChildren.
     */
    @Test
    public void testNoChildren() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No cases supplied");
        buildRule.executeTarget("noChildren");
    }

    /**
     * Method testTwoDefaults.
     */
    @Test
    public void testTwoDefaults() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot specify multiple default cases");
        buildRule.executeTarget("twoDefaults");
    }

    /**
     * Method testNoMatch.
     */
    @Test
    public void testNoMatch() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No case matched the value foo"
                + " and no default has been specified.");
        buildRule.executeTarget("noMatch");
    }

    /**
     * Method testCaseNoValue.
     */
    @Test
    public void testCaseNoValue() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Value is required for case.");
        buildRule.executeTarget("caseNoValue");
    }

    /**
     * Method testDefault.
     */
    @Test
    public void testDefault() {
        buildRule.executeTarget("testDefault");
        String log = buildRule.getLog();
        assertThat(log, both(containsString("In default"))
                .and(containsString("baz")));
        assertThat(log, both(not(containsString("${inner}")))
                .and(not(containsString("In case"))));
    }

    /**
     * Method testCase.
     */
    @Test
    public void testCase() {
        buildRule.executeTarget("testCase");
        String log = buildRule.getLog();
        assertThat(log, both(containsString("In case"))
                .and(containsString("baz")));
        assertThat(log, both(not(containsString("${inner}")))
                .and(not(containsString("In default"))));
    }

    /**
     * Method testCaseSensitive.
     */
    @Test
    public void testCaseSensitive() {
        buildRule.executeTarget("testCaseSensitive");
        assertThat(buildRule.getLog(), both(containsString("In default"))
                .and(not(containsString("In case"))));
    }

    /**
     * Method testCaseInSensitive.
     */
    @Test
    public void testCaseInSensitive() {
        buildRule.executeTarget("testCaseInSensitive");
        assertThat(buildRule.getLog(), both(containsString("In case"))
                .and(not(containsString("In default"))));
    }
}
