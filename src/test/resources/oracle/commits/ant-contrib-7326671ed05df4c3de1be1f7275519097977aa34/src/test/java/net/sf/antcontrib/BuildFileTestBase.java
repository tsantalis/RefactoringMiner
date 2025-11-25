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
package net.sf.antcontrib;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;

/**
 * More methods for BuildFileTest.
 *
 * @author <a href="mailto:danson@germane-software.com">Dale Anson</a>
 */
public abstract class BuildFileTestBase extends BuildFileTest {
    /**
     * run a target, expect a build exception.
     *
     * @param target target to run
     */
    protected void expectBuildException(String target) {
        expectSpecificBuildException(target, "no specific reason", null);
    }

    /**
     * Assert that the given message has NOT been logged with a priority &gt;= INFO
     * when running the given target.
     *
     * @param target Description of the Parameter
     * @param log    Description of the Parameter
     */
    protected void expectLogNotContaining(String target, String log) {
        executeTarget(target);
        assertLogNotContaining(log);
    }

    /**
     * Assert that the given substring is NOT in the log messages.
     *
     * @param substring String
     */
    protected void assertLogNotContaining(String substring) {
        String realLog = getLog();
        assertFalse("expecting log to NOT contain \"" + substring + "\" log was \""
                        + realLog + "\"",
                realLog.contains(substring));
    }

    /**
     * Set up to run the named project.
     * <p>Overrides BuildFileTest.configureProject to first
     * attempt to make a File out of the filename parameter, if the resulting
     * file does not exists, then attempt to locate the file in the classpath.
     * This way, test xml files can be placed alongside of their corresponding
     * class file and can be easily found.</p>
     *
     * @param filename name of project file to run
     * @throws BuildException Description of the Exception
     */
    protected void configureProject(String filename) throws BuildException {
        // find the build file
        File f = new File(filename);
        if (!f.exists()) {
            URL url = getClass().getClassLoader().getResource(filename);
            if (url == null) {
                throw new BuildException("Can't find " + filename);
            }
            f = new File(url.getPath());
            if (!f.exists()) {
                throw new BuildException("Can't find " + filename);
            }
        }
        super.configureProject(f.getAbsolutePath());
    }

    /**
     * run a target, expect an exception string containing the substring we look
     * for (case sensitive match).
     *
     * @param target   target to run
     * @param cause    information string to reader of report
     * @param contains substring of the build exception to look for
     */
    protected void expectBuildExceptionStackTraceContaining(String target, String cause,
                                                            String contains) {
        try {
            executeTarget(target);
        } catch (BuildException ex) {
            //buildException = ex;  // buildException has private access in super
            StringWriter stacktrace = new StringWriter();
            PrintWriter writer = new PrintWriter(stacktrace, true);
            ex.printStackTrace(writer);
            String trace = stacktrace.toString();
            if ((null != contains) && (!trace.contains(contains))) {
                fail("Should throw BuildException because '" + cause + "' with message containing '"
                     + contains + "' (actual message '" + trace + "' instead)");
            }
            return;
        }
        fail("Should throw BuildException because: " + cause);
    }
}
