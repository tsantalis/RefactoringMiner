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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Testcase for &lt;if&gt;.
 */
public class IfTaskTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/logic/if.xml");
    }

    /**
     * Method testNoCondition.
     */
    @Test
    public void testNoCondition() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must nest a condition into <if>");
        buildRule.executeTarget("noCondition");
    }

    /**
     * Method testTwoConditions.
     */
    @Test
    public void testTwoConditions() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not nest more than one "
                + "condition into <if>");
        buildRule.executeTarget("twoConditions");
    }

    /**
     * Method testNothingToDo.
     */
    @Test
    public void testNothingToDo() {
        buildRule.executeTarget("nothingToDo");
        assertEquals("", buildRule.getLog());
    }

    /**
     * Method testTwoThens.
     */
    @Test
    public void testTwoThens() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not nest more than one "
                + "<then> into <if>");
        buildRule.executeTarget("twoThens");
    }

    /**
     * Method testTwoElses.
     */
    @Test
    public void testTwoElses() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not nest more than one "
                + "<else> into <if>");
        buildRule.executeTarget("twoElses");
    }

    /**
     * Method testNormalOperation.
     */
    @Test
    public void testNormalOperation() {
        buildRule.executeTarget("normalOperation");
        String log = buildRule.getLog();
        assertThat(log, both(containsString("In then"))
                .and(containsString("some value")));
        assertThat(log, both(not(containsString("${inner}")))
                .and(not(containsString("In else"))));
    }

    /**
     * Method testNormalOperation2.
     */
    @Test
    public void testNormalOperation2() {
        buildRule.executeTarget("normalOperation2");
        assertThat(buildRule.getLog(), both(containsString("In else"))
                .and(not(containsString("In then"))));
    }

    /**
     * Method testNoConditionInElseif.
     */
    @Test
    public void testNoConditionInElseif() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must nest a condition into <elseif>");
        buildRule.executeTarget("noConditionInElseif");
    }

    /**
     * Method testTwoConditionInElseif.
     */
    @Test
    public void testTwoConditionInElseif() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not nest more than one "
                + "condition into <elseif>");
        buildRule.executeTarget("twoConditionsInElseif");
    }

    /**
     * Method testNormalOperationElseif.
     */
    @Test
    public void testNormalOperationElseif() {
        buildRule.executeTarget("normalOperationElseif");
        String log = buildRule.getLog();
        assertThat(log, containsString("In elseif"));
        assertThat(log, both(not(containsString("In then")))
                .and(not(containsString("In else-branch"))));
    }

    /**
     * Method testNormalOperationElseif2.
     */
    @Test
    public void testNormalOperationElseif2() {
        buildRule.executeTarget("normalOperationElseif2");
        String log = buildRule.getLog();
        assertThat(log, containsString("In else-branch"));
        assertThat(log, both(not(containsString("In then")))
                .and(not(containsString("In elseif"))));
    }
}
