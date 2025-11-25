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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import net.sf.antcontrib.BuildFileTestBase;

/**
 */
public class VariableTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("property/variabletest.xml");
    }

    /**
     * Method test1.
     */
    @Test
    public void test1() {
        expectPropertySet("test1", "x", "6");
    }

    /**
     * Method test2.
     */
    @Test
    public void test2() {
        expectPropertySet("test2", "x", "12");
    }

    /**
     * Method test3.
     */
    @Test
    public void test3() {
        expectPropertySet("test3", "x", "6 + 12");
    }

    /**
     * Method test4.
     */
    @Test
    public void test4() {
        expectPropertySet("test4", "x", "6");
    }

    /**
     * Method test5.
     */
    @Test
    public void test5() {
        expectPropertySet("test5", "str", "I am a string.");
    }

    /**
     * Method test6.
     */
    @Test
    public void test6() {
        expectPropertySet("test6", "x", "Today is blue.");
    }

    /**
     * Method test7.
     */
    @Test
    public void test7() {
        expectPropertySet("test7", "x", "6");
    }

    /* TODO depends on the Antelope <if>, need to adjust to use the ant-contrib <if> */

    /**
     * Method test8.
     */
    @Ignore
    public void test8() {
        expectPropertySet("test8", "x", "12");
        expectLogContaining("test8", "12");
    }

    /**
     * Method test9.
     */
    @Test
    public void test9() {
        expectPropertyUnset("test9", "i");
    }

    /**
     * Method test10.
     */
    @Test
    public void test10() {
        expectPropertySet("test10", "x", "xxx");
    }
}
