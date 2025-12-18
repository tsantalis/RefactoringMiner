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
package net.sf.antcontrib.platform;

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;osfamily&gt;.
 */
public class OsFamilyTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("platform/osfamily.xml");
    }

    /**
     * Method testConsistency.
     */
    @Test
    public void testConsistency() {
        executeTarget("consistency");
        assertPropertyEquals("consistent", "true");
    }

    /**
     * Method testMissingProperty.
     */
    @Test
    public void testMissingProperty() {
        expectSpecificBuildException("missingProperty", "no attribute",
                "The attribute 'property' is required "
                        + "for the OsFamily task.");
    }
}
