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
import org.apache.tools.ant.types.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Testcase for &lt;outofdate&gt;.
 *
 * @author <a href="mailto:peterreilly@users.sf.net">Peter Reilly</a>
 */
public class OutOfDateTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/logic/outofdate.xml");
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    /**
     * Method testSimple.
     */
    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
    }

    /**
     * Method testVerbose.
     */
    @Test
    public void testVerbose() {
        buildRule.executeTarget("verbose");
        assertThat(buildRule.getLog(),
                containsString("outofdate with regard to"));
    }

    /**
     * Method testDelete.
     */
    @Test
    public void testDelete() {
        buildRule.executeTarget("delete");
    }

    /**
     * Method testDeleteAll.
     */
    @Test
    public void testDeleteAll() {
        buildRule.executeTarget("delete-all");
    }

    /**
     * Method testDeleteQuiet.
     */
    @Test
    public void testDeleteQuiet() {
        buildRule.executeTarget("init");
        buildRule.executeTarget("delete-quiet");
        assertThat(buildRule.getLog(), not(containsString("Deleting")));
    }

    /**
     * Method testFileset.
     */
    @Test
    public void testFileset() {
        buildRule.executeTarget("outofdate.init");
        buildRule.executeTarget("outofdate.test");
        assertThat(buildRule.getLog(),containsString("outofdate triggered"));
        // switch \ to / if present
        String outofdateSources =
                buildRule.getProject().getProperty("outofdate.sources").replace('\\', '/');
        assertThat("newer.text empty", outofdateSources, containsString("newer.text"));
        assertThat("file.notdone", outofdateSources, containsString("outofdate/source/1/2/file.notdone"));
        assertThat("file.done", outofdateSources, not(containsString("outofdate/source/1/2/file.done")));
        assertThat("done.y", outofdateSources, not(containsString("outofdate/source/1/done.y")));
        assertThat("partial.y", outofdateSources, containsString("outofdate/source/1/partial.y"));
        String outofdateTargets =
                buildRule.getProject().getProperty("outofdate.targets");
        assertThat(outofdateTargets, containsString("outofdate.xml"));
        assertThat(outofdateTargets, containsString("outofdate/gen/1/2/file.notdone"));
        assertThat(outofdateTargets, containsString("outofdate/gen/1/partial.h"));
        assertThat(outofdateTargets, not(containsString("outofdate/gen/1/partial.c")));
        assertThat(outofdateTargets, not(containsString("outofdate/gen/1/done.h")));

        Path sourcesPath = buildRule.getProject().getReference("outofdate.sources.path");
        assertNotNull(sourcesPath);
        String[] sources = sourcesPath.list();
        assertEquals(3, sources.length);
        Path targetsPath = buildRule.getProject().getReference("outofdate.targets.path");
        String[] targets = targetsPath.list();
        assertNotNull(targetsPath);
        assertEquals(3, targets.length);
    }

    /**
     * Method testEmptySources.
     */
    @Test
    public void testEmptySources() {
        buildRule.executeTarget("empty-sources");
    }
}
