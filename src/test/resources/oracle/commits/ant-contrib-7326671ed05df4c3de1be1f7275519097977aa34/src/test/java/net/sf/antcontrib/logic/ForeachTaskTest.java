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

import static org.junit.Assert.assertTrue;

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;foreach&gt;.
 */
public class ForeachTaskTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("logic/foreach.xml");
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        executeTarget("teardown");
    }

    /**
     * Method testSimpleList.
     */
    @Test
    public void testSimpleList() {
        simpleTest("simpleList");
    }

    /**
     * Method testDelimiter.
     */
    @Test
    public void testDelimiter() {
        simpleTest("delimiter");
    }

    /**
     * Method testFileset.
     */
    @Test
    public void testFileset() {
        simpleTest("fileset");
        assertLogContaining("The nested fileset element is deprecated,"
                + " use a nested path instead");
    }

    /**
     * Method testFilesetAndList.
     */
    @Test
    public void testFilesetAndList() {
        simpleTest("filesetAndList");
        assertLogContaining("The nested fileset element is deprecated,"
                + " use a nested path instead");
    }

    /**
     * Method testNoList.
     */
    @Test
    public void testNoList() {
        expectSpecificBuildException("noList", "neither list nor fileset",
                "You must have a list or path to iterate through");
    }

    /**
     * Method testNoTarget.
     */
    @Test
    public void testNoTarget() {
        expectSpecificBuildException("noTarget", "no target",
                "You must supply a target to perform");
    }

    /**
     * Method testNoParam.
     */
    @Test
    public void testNoParam() {
        expectSpecificBuildException("noParam", "no param",
                "You must supply a property name to set on each iteration in param");
    }

    /**
     * Method testNestedParam.
     */
    @Test
    public void testNestedParam() {
        expectLogContaining("nestedParam", "Called with param: rincewind");
    }

    /**
     * Method testNestedReference.
     */
    @Test
    public void testNestedReference() {
        expectLogContaining("nestedReference", "Called with param: twoflower");
    }

    /**
     * Method testPath.
     */
    @Test
    public void testPath() {
        simpleTest("path");
    }

    /**
     * Method testPathAndList.
     */
    @Test
    public void testPathAndList() {
        simpleTest("pathAndList");
    }

    /**
     * Method simpleTest.
     *
     * @param target String
     */
    private void simpleTest(String target) {
        executeTarget(target);
        int last = -1;
        for (int i = 1; i < 4; i++) {
            int thisIdx = getLog().indexOf("Called with param: " + i);
            assertTrue(thisIdx > last);
            last = thisIdx;
        }
    }
}
