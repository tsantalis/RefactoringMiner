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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;foreach&gt;.
 */
public class TimestampSelectorTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("logic/timestampselector.xml");
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        executeTarget("teardown");
    }

    /**
     * Method testFileStampFL.
     */
    @Test
    public void testFileStampFL() {
        expectLogContaining("filestamp.fl", "file2.txt");
    }

    /**
     * Method testFileStampPR.
     */
    @Test
    public void testFileStampPR() {
        expectLogContaining("filestamp.pr", "file2.txt");
    }

    /**
     * Method testDirStampDL.
     */
    @Test
    public void testDirStampDL() {
        expectLogContaining("dirstamp.dl", "dir2");
    }

    /**
     * Method testDirStampPR.
     */
    @Test
    public void testDirStampPR() {
        expectLogContaining("dirstamp.pr", "dir2");
    }
}
