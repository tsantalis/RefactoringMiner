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
package net.sf.antcontrib.property;

import static org.junit.Assert.assertTrue;

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;propertyselector&gt;.
 */
public class PropertySelectorTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("property/propertyselector.xml");
    }

    /**
     * Method testDefaultGrouping0.
     */
    @Test
    public void testDefaultGrouping0() {
        simpleTest("select.test.grouping.0",
                "module.Module1.id", "module.Module2.id");
    }

    /**
     * Method testDefaultGrouping1.
     */
    @Test
    public void testDefaultGrouping1() {
        simpleTest("select.test.grouping.1",
                "Module1", "Module2");
    }

    /**
     * Method simpleTest.
     *
     * @param target    String
     * @param expected1 String
     * @param expected2 String
     */
    private void simpleTest(String target, String expected1, String expected2) {
        executeTarget(target);
        String order1 = expected1 + "," + expected2;
        String order2 = expected2 + "," + expected1;
        int index1 = getLog().indexOf(order1);
        int index2 = getLog().indexOf(order2);
        assertTrue("Neither '" + order1 + "' nor '" + order2
                        + "' was found in '" + getLog() + "'",
                index1 > -1 || index2 > -1);
    }
}
