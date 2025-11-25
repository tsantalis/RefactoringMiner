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
package net.sf.antcontrib.property;

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;propertycopy&gt;.
 */
public class PropertyCopyTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("property/propertycopy.xml");
    }

    /**
     * Runs a propertyCopy without a specified name attribute.
     */
    @Test
    public void testMissingName() {
        expectSpecificBuildException("missingName", "missing name",
                "You must specify a property to set.");
    }

    /**
     * Method testMissingFrom.
     */
    @Test
    public void testMissingFrom() {
        expectSpecificBuildException("missingFrom", "missing from",
                "Missing the 'from' attribute.");
    }

    /**
     * Method testNonSilent.
     */
    @Test
    public void testNonSilent() {
        expectSpecificBuildException("nonSilent", "from doesn't exist",
                "Property 'bar' is not defined.");
    }

    /**
     * Method testSilent.
     */
    @Test
    public void testSilent() {
        executeTarget("silent");
        assertPropertyEquals("foo", null);
    }

    /**
     * Method testNormal.
     */
    @Test
    public void testNormal() {
        executeTarget("normal");
        assertPropertyEquals("displayName", "My Organization");
    }
}
