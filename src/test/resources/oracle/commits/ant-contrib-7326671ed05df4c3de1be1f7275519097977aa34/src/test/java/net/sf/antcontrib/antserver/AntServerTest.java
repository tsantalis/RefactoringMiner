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

import static org.junit.Assert.assertEquals;

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Place class description here.
 *
 * @author <a href="mailto:mattinger@yahoo.com">Matthew Inger</a>
 */
public class AntServerTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("antserver/antservertest.xml");
    }

    /**
     * Method tearDown.
     */
    @After
    public void tearDown() {
        executeTarget("cleanup");
    }

    /**
     * Method test1.
     */
    @Test
    public void test1() {
        String[] expected = new String[]
                {
                        "Test1 Successfully Called",
                        "[test1_remote]"
                };

        expectLogContaining("test1", expected);
    }

    /**
     * Method test2.
     */
    @Test
    public void test2() {
        String[] expected = new String[]
                {
                        "Test2 Successfully Called",
                        "[test2_remote]"
                };

        expectLogContaining("test2", expected);
    }

    /**
     * Method test3.
     */
    @Test
    public void test3() {
        String[] expected = new String[]
                {
                        "Test3 Successfully Called",
                        "[test3_remote]"
                };

        expectLogContaining("test3", expected);
    }

    /**
     * Method test4.
     */
    @Test
    public void test4() {
        String[] expected = new String[]
                {
                        "Test4 Successfully Called",
                        "[test4_remote]"
                };

        expectLogContaining("test4", expected);
    }

    /**
     * Method test5.
     */
    @Test
    public void test5() {
        this.executeTarget("test5");
    }

    /**
     * Assert that the given message has been logged with a priority
     * &gt;= INFO when running the given target.
     *
     * @param target String
     * @param logs   String[]
     */
    protected void expectLogContaining(String target,
                                       String[] logs) {
        executeTarget(target);
        String realLog = getLog();

        int cnt = 0;
        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            if (realLog.contains(log)) {
                cnt++;
            }
            if (sb.length() != 0) {
                sb.append(" and ");
            }
            sb.append("\"").append(log).append("\"");
        }

        assertEquals("expecting log to contain " + sb.toString()
                + " log was \"" + realLog + "\"", cnt, logs.length);
    }
}
