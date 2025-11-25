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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Testcase for &lt;propertycopy&gt;.
 */
public class PropertyCopyTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/property/propertycopy.xml");
    }

    /**
     * Runs a propertyCopy without a specified name attribute.
     */
    @Test
    public void testMissingName() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must specify a property to set.");
        buildRule.executeTarget("missingName");
    }

    /**
     * Method testMissingFrom.
     */
    @Test
    public void testMissingFrom() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Missing the 'from' attribute.");
        buildRule.executeTarget("missingFrom");
    }

    /**
     * Method testNonSilent.
     */
    @Test
    public void testNonSilent() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Property 'bar' is not defined.");
        buildRule.executeTarget("nonSilent");
    }

    /**
     * Method testSilent.
     */
    @Test
    public void testSilent() {
        buildRule.executeTarget("silent");
        assertNull(buildRule.getProject().getProperty("foo"));
    }

    /**
     * Method testNormal.
     */
    @Test
    public void testNormal() {
        buildRule.executeTarget("normal");
        assertEquals(buildRule.getProject().getProperty("displayName"),
                "My Organization");
    }
}
