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
package net.sf.antcontrib.math;

import org.junit.Before;
import org.junit.Test;

import net.sf.antcontrib.BuildFileTestBase;

/**
 * MathTest class.
 *
 * @author <a href="mailto:danson@germane-software.com">Dale Anson</a>
 */
public class MathTest extends BuildFileTestBase {
    /**
     * The JUnit setup method.
     */
    @Before
    public void setUp() {
        configureProject("math/mathtest.xml");
    }

    /**
     * A unit test for JUnit.
     */
    @Test
    public void test1() {
        expectPropertySet("test1", "result", "18");
    }
}
