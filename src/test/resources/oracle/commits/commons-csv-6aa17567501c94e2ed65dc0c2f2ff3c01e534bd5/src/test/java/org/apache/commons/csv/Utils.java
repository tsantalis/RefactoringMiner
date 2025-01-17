/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.commons.csv;

import java.util.List;

import org.junit.Assert;

/**
 * Utility methods for test cases
 */
final class Utils {

    private Utils() {
    }

    /**
     * Checks if the 2d array has the same contents as the list of records.
     *
     * @param message the message to be displayed
     * @param expected the 2d array of expected results
     * @param actual the List of {@link CSVRecord} entries, each containing an array of values
     */
    public static void compare(final String message, final String[][] expected, final List<CSVRecord> actual) {
        Assert.assertEquals(message+"  - outer array size", expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            Assert.assertArrayEquals(message + " (entry " + i + ")", expected[i], actual.get(i).values());
        }
    }
}
