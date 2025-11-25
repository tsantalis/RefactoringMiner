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

import org.apache.tools.ant.types.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testcase for &lt;outofdate&gt;.
 *
 * @author <a href="mailto:peterreilly@users.sf.net">Peter Reilly</a>
 */
public class OutOfDateTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("logic/outofdate.xml");
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        executeTarget("cleanup");
    }

    /**
     * Method testSimple.
     */
    @Test
    public void testSimple() {
        executeTarget("simple");
    }

    /**
     * Method testVerbose.
     */
    @Test
    public void testVerbose() {
        expectLogContaining("verbose", "outofdate with regard to");
    }

    /**
     * Method testDelete.
     */
    @Test
    public void testDelete() {
        executeTarget("delete");
    }

    /**
     * Method testDeleteAll.
     */
    @Test
    public void testDeleteAll() {
        executeTarget("delete-all");
    }

    /**
     * Method testDeleteQuiet.
     */
    @Test
    public void testDeleteQuiet() {
        executeTarget("init");
        executeTarget("delete-quiet");
        assertLogNotContaining("Deleting");
    }

    /**
     * Method testFileset.
     */
    @Test
    public void testFileset() {
        executeTarget("outofdate.init");
        executeTarget("outofdate.test");
        assertLogContaining("outofdate triggered");
        // switch \ to / if present
        String outofdateSources =
                getProject().getProperty("outofdate.sources").replace('\\', '/');
        assertTrue("newer.text empty", outofdateSources.contains("newer.text"));
        assertTrue("file.notdone", outofdateSources.contains("outofdate/source/1/2/file.notdone"));
        assertTrue("file.done", !outofdateSources.contains("outofdate/source/1/2/file.done"));
        assertTrue("done.y", !outofdateSources.contains("outofdate/source/1/done.y"));
        assertTrue("partial.y", outofdateSources.contains("outofdate/source/1/partial.y"));
        String outofdateTargets =
                getProject().getProperty("outofdate.targets");
        assertTrue(outofdateTargets.contains("outofdate.xml"));
        assertTrue(outofdateTargets.contains("outofdate/gen/1/2/file.notdone"));
        assertTrue(outofdateTargets.contains("outofdate/gen/1/partial.h"));
        assertTrue(!outofdateTargets.contains("outofdate/gen/1/partial.c"));
        assertTrue(!outofdateTargets.contains("outofdate/gen/1/done.h"));

        Path sourcesPath = getProject().getReference("outofdate.sources.path");
        assertNotNull(sourcesPath);
        String[] sources = sourcesPath.list();
        assertEquals(3, sources.length);
        Path targetsPath = getProject().getReference("outofdate.targets.path");
        String[] targets = targetsPath.list();
        assertNotNull(targetsPath);
        assertEquals(3, targets.length);
    }

    /**
     * Method testEmptySources.
     */
    @Test
    public void testEmptySources() {
        executeTarget("empty-sources");
    }
}
