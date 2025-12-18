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

import static org.junit.Assert.assertTrue;

import net.sf.antcontrib.BuildFileTestBase;

import org.junit.Before;
import org.junit.Test;

/**
 * Testcase for &lt;shellscript&gt;.
 *
 * @author <a href="mailto:peterreilly@users.sf.net">Peter Reilly</a>
 */
public class ShellScriptTest extends BuildFileTestBase {
    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        configureProject("platform/shellscript.xml");
        staticInitialize();
    }

    /**
     * Method testShHello.
     */
    @Test
    public void testShHello() {
        if (hasSh) {
            expectLogContaining("sh.hello", "hello world");
        }
    }

    /**
     * Method testBashHello.
     */
    @Test
    public void testBashHello() {
        if (hasBash) {
            expectLogContaining("bash.hello", "hello world");
        }
    }

    /**
     * Method testShInputString.
     */
    @Test
    public void testShInputString() {
        if (hasSh) {
            expectLogContaining("sh.inputstring", "hello world");
        }
    }

    /**
     * Method testShProperty.
     */
    @Test
    public void testShProperty() {
        if (hasSh) {
            expectLogContaining("sh.property", "this is a property");
        }
    }

    /**
     * Method testPythonHello.
     */
    @Test
    public void testPythonHello() {
        if (hasPython) {
            expectLogContaining("python.hello", "hello world");
        }
    }

    /**
     * Method testPerlHello.
     */
    @Test
    public void testPerlHello() {
        if (hasPerl) {
            expectLogContaining("perl.hello", "hello world");
        }
    }

    /**
     * Method testNoShell.
     */
    @Test
    public void testNoShell() {
        expectBuildExceptionContaining(
                "noshell", "Execute failed", "a shell that should not exist");
    }

    /**
     * Method testSed.
     */
    @Test
    public void testSed() {
        if (hasSed) {
            expectLogContaining("sed.test", "BAR bar bar bar BAR bar");
        }
    }

    /**
     * Method testSetProperty.
     */
    @Test
    public void testSetProperty() {
        if (hasSh) {
            executeTarget("sh.set.property");
            assertPropertyEquals("sh.set.property", "hello world");
        }
    }

    /**
     * Method testTmpSuffix.
     */
    @Test
    public void testTmpSuffix() {
        if (hasSh) {
            expectLogContaining("sh.tmp.suffix", ".bat");
        }
    }

    /**
     * Method testCmd.
     */
    @Test
    public void testCmd() {
        if (hasCmd) {
            expectLogContaining("cmd.test", "hello world");
        }
    }

    /**
     * Method testDir.
     */
    @Test
    public void testDir() {
        if (hasBash) {
            executeTarget("dir.test");
            assertTrue(getProject().getProperty("dir.test.property").contains("subdir"));
        }
    }

    /**
     * Method testCommand.
     */
    @Test
    public void testCommand() {
        expectBuildExceptionContaining(
                "command.test", "Attribute failed",
                "Attribute command is not supported");
    }

    /**
     * Field initialized.
     */
    private static boolean initialized = false;

    /**
     * Field hasSh.
     */
    private static boolean hasSh = false;

    /**
     * Field hasBash.
     */
    private static boolean hasBash = false;

    /**
     * Field hasPython.
     */
    private static boolean hasPython = false;

    /**
     * Field hasPerl.
     */
    private static boolean hasPerl = false;

    /**
     * Field hasSed.
     */
    private static boolean hasSed = false;

    /**
     * Field hasCmd.
     */
    private static boolean hasCmd = false;

    /**
     * Field staticMonitor.
     */
    private static final Object STATIC_MONITOR = new Object();

    /**
     * check if the env contains the shells
     * sh, bash, python and perl;
     * assume cmd.exe exists for windows.
     */
    private void staticInitialize() {
        synchronized (STATIC_MONITOR) {
            if (initialized) {
                return;
            }
            initialized = true;
            hasSh = hasShell("hassh");
            hasBash = hasShell("hasbash");
            hasPerl = hasShell("hasperl");
            hasPython = hasShell("haspython");
            hasSed = hasShell("hassed");
            hasCmd = hasShell("hascmd");

        }
    }

    /**
     * Method hasShell.
     *
     * @param target String
     * @return boolean
     */
    private boolean hasShell(String target) {
        try {
            executeTarget(target);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
