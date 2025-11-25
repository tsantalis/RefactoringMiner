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
package net.sf.antcontrib.antserver;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Place class description here.
 *
 * @author <a href="mailto:mattinger@yahoo.com">Matthew Inger</a>
 */
public class AntServerTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/antserver/antservertest.xml");
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    /**
     * Method test1.
     */
    @Test
    public void test1() {
        buildRule.executeTarget("test1");
        assertThat(buildRule.getLog(),
                both(containsString("Test1 Successfully Called"))
                        .and(containsString("[test1_remote]")));
    }

    /**
     * Method test2.
     */
    @Test
    public void test2() {
        buildRule.executeTarget("test2");
        assertThat(buildRule.getLog(),
                both(containsString("Test2 Successfully Called"))
                        .and(containsString("[test2_remote]")));
    }

    /**
     * Method test3.
     */
    @Test
    public void test3() {
        buildRule.executeTarget("test3");
        assertThat(buildRule.getLog(),
                both(containsString("Test3 Successfully Called"))
                        .and(containsString("[test3_remote]")));
    }

    /**
     * Method test4.
     */
    @Test
    public void test4() {
        buildRule.executeTarget("test4");
        assertThat(buildRule.getLog(),
                both(containsString("Test4 Successfully Called"))
                        .and(containsString("[test4_remote]")));
    }

    /**
     * Method test5.
     */
    @Test
    public void test5() {
        buildRule.executeTarget("test5");
    }
}
