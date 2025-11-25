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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Testcase for &lt;osfamily&gt;.
 */
public class OsFamilyTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/platform/osfamily.xml");
    }

    /**
     * Method testConsistency.
     */
    @Test
    public void testConsistency() {
        buildRule.executeTarget("consistency");
        assertEquals(buildRule.getProject().getProperty("consistent"), "true");
    }

    /**
     * Method testMissingProperty.
     */
    @Test
    public void testMissingProperty() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The attribute 'property' is required "
                + "for the OsFamily task.");
        buildRule.executeTarget("missingProperty");
    }
}
