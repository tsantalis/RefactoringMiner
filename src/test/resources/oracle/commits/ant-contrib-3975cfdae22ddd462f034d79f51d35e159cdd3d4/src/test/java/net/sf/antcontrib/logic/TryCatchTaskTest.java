/*
 * Copyright (c) 2001-2005 Ant-Contrib project.  All rights reserved.
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Testcase for &lt;trycatch&gt;.
 */
public class TryCatchTaskTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/logic/trycatch.xml");
    }

    /**
     * Method testFullTest.
     */
    @Test
    public void testFullTest() {
        buildRule.executeTarget("fullTest");
        assertEquals("Tada!", buildRule.getProject().getProperty("foo"));
        Object e = buildRule.getProject().getReference("bar");
        assertNotNull(e);
        assertTrue(e instanceof BuildException);
        assertEquals("Tada!", ((BuildException) e).getMessage());
    }

    /**
     * Method testTwoCatches.
     */
    @Test
    public void testTwoCatches() {
        //  two catch blocks were not supported prior to TryCatchTask.java v 1.4.
        buildRule.executeTarget("twoCatches");
    }

    /**
     * Method testTwoFinallys.
     */
    @Test
    public void testTwoFinallys() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one <finally>");
        buildRule.executeTarget("twoFinallys");
    }

    /**
     * Method testTwoTrys.
     */
    @Test
    public void testTwoTrys() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one <try>");
        buildRule.executeTarget("twoTrys");
    }

    /**
     * Method testNoTry.
     */
    @Test
    public void testNoTry() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("A nested <try> element is required");
        buildRule.executeTarget("noTry");
    }

    /**
     * Method testNoException.
     */
    @Test
    public void testNoException() {
        buildRule.executeTarget("noException");
        assertThat(buildRule.getLog(), containsString("Tada!"));
        assertThat(buildRule.getLog(), not(containsString("In <catch>")));
        assertTrue(buildRule.getLog().indexOf("In <finally>")
                > buildRule.getLog().indexOf("Tada!"));
        assertNull(buildRule.getProject().getProperty("foo"));
        assertNull(buildRule.getProject().getReference("bar"));
    }
}
