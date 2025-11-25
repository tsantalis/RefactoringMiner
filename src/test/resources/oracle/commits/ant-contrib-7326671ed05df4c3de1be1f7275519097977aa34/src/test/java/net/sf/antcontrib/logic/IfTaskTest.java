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
 * Testcase for &lt;if&gt;.
 */
public class IfTaskTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("logic/if.xml");
    }

    /**
     * Method testNoCondition.
     */
    @Test
    public void testNoCondition() {
        expectSpecificBuildException("noCondition", "no condition",
                "You must nest a condition into <if>");
    }

    /**
     * Method testTwoConditions.
     */
    @Test
    public void testTwoConditions() {
        expectSpecificBuildException("twoConditions", "two conditions",
                "You must not nest more than one "
                        + "condition into <if>");
    }

    /**
     * Method testNothingToDo.
     */
    @Test
    public void testNothingToDo() {
        expectLog("nothingToDo", "");
    }

    /**
     * Method testTwoThens.
     */
    @Test
    public void testTwoThens() {
        expectSpecificBuildException("twoThens", "two <then>s",
                "You must not nest more than one "
                        + "<then> into <if>");
    }

    /**
     * Method testTwoElses.
     */
    @Test
    public void testTwoElses() {
        expectSpecificBuildException("twoElses", "two <else>s",
                "You must not nest more than one "
                        + "<else> into <if>");
    }

    /**
     * Method testNormalOperation.
     */
    @Test
    public void testNormalOperation() {
        executeTarget("normalOperation");
        assertLogContaining("In then");
        assertLogContaining("some value");
        assertLogNotContaining("${inner}");
        assertLogNotContaining("In else");
    }

    /**
     * Method testNormalOperation2.
     */
    @Test
    public void testNormalOperation2() {
        executeTarget("normalOperation2");
        assertLogContaining("In else");
        assertLogNotContaining("In then");
    }

    /**
     * Method testNoConditionInElseif.
     */
    @Test
    public void testNoConditionInElseif() {
        expectSpecificBuildException("noConditionInElseif", "no condition",
                "You must nest a condition into <elseif>");
    }

    /**
     * Method testTwoConditionInElseif.
     */
    @Test
    public void testTwoConditionInElseif() {
        expectSpecificBuildException("twoConditionsInElseif", "two conditions",
                "You must not nest more than one "
                        + "condition into <elseif>");
    }

    /**
     * Method testNormalOperationElseif.
     */
    @Test
    public void testNormalOperationElseif() {
        executeTarget("normalOperationElseif");
        assertLogContaining("In elseif");
        assertLogNotContaining("In then");
        assertLogNotContaining("In else-branch");
    }

    /**
     * Method testNormalOperationElseif2.
     */
    @Test
    public void testNormalOperationElseif2() {
        executeTarget("normalOperationElseif2");
        assertLogContaining("In else-branch");
        assertLogNotContaining("In then");
        assertLogNotContaining("In elseif");
    }
}
