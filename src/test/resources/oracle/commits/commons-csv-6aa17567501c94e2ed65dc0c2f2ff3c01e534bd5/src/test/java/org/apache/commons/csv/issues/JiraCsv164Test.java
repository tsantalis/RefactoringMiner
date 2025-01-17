/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.csv.issues;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Test;

public class JiraCsv164Test {

    @Test
    public void testJiraCsv154_withCommentMarker() throws IOException {
        final String comment = "This is a header comment";
        final CSVFormat format = CSVFormat.EXCEL.withHeader("H1", "H2").withCommentMarker('#')
                .withHeaderComments(comment);
        final StringBuilder out = new StringBuilder();
        try (final CSVPrinter printer = format.print(out)) {
            printer.print("A");
            printer.print("B");
        }
        final String s = out.toString();
        assertTrue(s, s.contains(comment));
    }

    @Test
    public void testJiraCsv154_withHeaderComments() throws IOException {
        final String comment = "This is a header comment";
        final CSVFormat format = CSVFormat.EXCEL.withHeader("H1", "H2").withHeaderComments(comment)
                .withCommentMarker('#');
        final StringBuilder out = new StringBuilder();
        try (final CSVPrinter printer = format.print(out)) {
            printer.print("A");
            printer.print("B");
        }
        final String s = out.toString();
        assertTrue(s, s.contains(comment));
    }

}
