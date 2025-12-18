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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.sf.antcontrib.BuildFileTestBase;

import org.apache.tools.ant.BuildException;

import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;trycatch&gt;.
 */
public class TryCatchTaskTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("logic/trycatch.xml");
    }

    /**
     * Method testFullTest.
     */
    @Test
    public void testFullTest() {
        executeTarget("fullTest");
        assertEquals("Tada!", getProject().getProperty("foo"));
        Object e = getProject().getReference("bar");
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
        executeTarget("twoCatches");
    }

    /**
     * Method testTwoFinallys.
     */
    @Test
    public void testTwoFinallys() {
        expectSpecificBuildException("twoFinallys", "two finally children",
                "You must not specify more than one <finally>");
    }

    /**
     * Method testTwoTrys.
     */
    @Test
    public void testTwoTrys() {
        expectSpecificBuildException("twoTrys", "two try children",
                "You must not specify more than one <try>");
    }

    /**
     * Method testNoTry.
     */
    @Test
    public void testNoTry() {
        expectSpecificBuildException("noTry", "no try child",
                "A nested <try> element is required");
    }

    /**
     * Method testNoException.
     */
    @Test
    public void testNoException() {
        executeTarget("noException");
        assertLogContaining("Tada!");
        assertLogNotContaining("In <catch>");
        assertTrue(getLog().indexOf("In <finally>") > getLog().indexOf("Tada!"));
        assertNull(getProject().getProperty("foo"));
        assertNull(getProject().getReference("bar"));
    }
}
