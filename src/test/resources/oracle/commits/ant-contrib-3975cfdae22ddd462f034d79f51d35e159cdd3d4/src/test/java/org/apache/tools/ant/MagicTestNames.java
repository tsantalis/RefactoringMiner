/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant;

/**
 * Magic names used within Ant's tests.
 *
 * @since Ant 1.10.6
 */
public final class MagicTestNames {
    /**
     * Magic property that makes unit tests based on BuildFileTest
     * or BuildFileRule ignore externally set basedir
     * (typically by Surefire/Failsafe)
     *
     * Value: {@value}
     * @since Ant 1.10.6
     */
    public static final String TEST_BASEDIR_IGNORE = "ant.test.basedir.ignore";

    /**
     * Magic property that makes unit tests based on BuildFileTest
     * or BuildFileRule use build files in alternative locations
     * (relative to "root" directory)
     *
     * Value: {@value}
     * @since Ant 1.10.6
     */
    public static final String TEST_ROOT_DIRECTORY = "root";

    /**
     * Property for ant process ID set in unit tests by BuildFileTest
     * or BuildFileRule.
     *
     * Value: {@value}
     */
    public static final String TEST_PROCESS_ID = "ant.processid";

    /**
     * Property for ant thread name set in unit tests by BuildFileTest
     * or BuildFileRule.
     *
     * Value: {@value}
     */
    public static final String TEST_THREAD_NAME = "ant.threadname";
}
