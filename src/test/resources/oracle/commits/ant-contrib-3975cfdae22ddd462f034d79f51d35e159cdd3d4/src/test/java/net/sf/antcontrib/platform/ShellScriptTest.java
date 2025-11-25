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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Testcase for &lt;shellscript&gt;.
 *
 * @author <a href="mailto:peterreilly@users.sf.net">Peter Reilly</a>
 */
public class ShellScriptTest {
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Method setUp.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/test/resources/platform/shellscript.xml");
        staticInitialize();
    }

    /**
     * Method testShHello.
     */
    @Test
    public void testShHello() {
        assumeTrue("No shell", hasSh);
        buildRule.executeTarget("sh.hello");
        assertThat(buildRule.getLog(), containsString("hello world"));
    }

    /**
     * Method testBashHello.
     */
    @Test
    public void testBashHello() {
        assumeTrue("No Bourne Again shell", hasBash);
        buildRule.executeTarget("bash.hello");
        assertThat(buildRule.getLog(), containsString("hello world"));
    }

    /**
     * Method testShInputString.
     */
    @Test
    public void testShInputString() {
        assumeTrue("No shell", hasSh);
        buildRule.executeTarget("sh.inputstring");
        assertThat(buildRule.getLog(), containsString("hello world"));
    }

    /**
     * Method testShProperty.
     */
    @Test
    public void testShProperty() {
        assumeTrue("No shell", hasSh);
        buildRule.executeTarget("sh.property");
        assertThat(buildRule.getLog(), containsString("this is a property"));
    }

    /**
     * Method testPythonHello.
     */
    @Test
    public void testPythonHello() {
        assumeTrue("No Python", hasPython);
        buildRule.executeTarget("python.hello");
        assertThat(buildRule.getLog(), containsString("hello world"));
    }

    /**
     * Method testPerlHello.
     */
    @Test
    public void testPerlHello() {
        assumeTrue("No Perl", hasPerl);
        buildRule.executeTarget("perl.hello");
        assertThat(buildRule.getLog(), containsString("hello world"));
    }

    /**
     * Method testNoShell.
     */
    @Test
    public void testNoShell() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("a shell that should not exist");
        buildRule.executeTarget("noshell");
    }

    /**
     * Method testSed.
     */
    @Test
    public void testSed() {
        assumeTrue("No Stream EDitor", hasSed);
        buildRule.executeTarget("sed.test");
        assertThat(buildRule.getLog(), containsString("BAR bar bar bar BAR bar"));
    }

    /**
     * Method testSetProperty.
     */
    @Test
    public void testSetProperty() {
        assumeTrue("No shell", hasSh);
        buildRule.executeTarget("sh.set.property");
        assertEquals(buildRule.getProject().getProperty("sh.set.property"), "hello world");
    }

    /**
     * Method testTmpSuffix.
     */
    @Test
    public void testTmpSuffix() {
        assumeTrue("No shell", hasSh);
        buildRule.executeTarget("sh.tmp.suffix");
        assertThat(buildRule.getLog(), containsString(".bat"));
    }

    /**
     * Method testCmd.
     */
    @Test
    public void testCmd() {
        assumeTrue("No Command Prompt", hasCmd);
        assertThat(buildRule.getLog(), containsString("hello world"));
    }

    /**
     * Method testDir.
     */
    @Test
    public void testDir() {
        assumeTrue("No Bourne Again shell", hasBash);
        buildRule.executeTarget("dir.test");
        assertThat(buildRule.getProject().getProperty("dir.test.property"),
                containsString("subdir"));
    }

    /**
     * Method testCommand.
     */
    @Test
    public void testCommand() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Attribute command is not supported");
        buildRule.executeTarget("command.test");
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
            buildRule.executeTarget(target);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
