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

package org.apache.commons.csv;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * A CSV record parsed from a CSV file.
 *
 * @version $Id$
 */
public class CSVRecord implements Serializable, Iterable<String> {

    private static final long serialVersionUID = 1L;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /** The values of the record */
    private final String[] values;

    /** The column name to index mapping. */
    private final Map<String, Integer> mapping;

    /** The accumulated comments (if any) */
    private final String comment;

    /** The record number. */
    private final long recordNumber;

    CSVRecord(final String[] values, final Map<String, Integer> mapping,
            final String comment, final long recordNumber) {
        this.recordNumber = recordNumber;
        this.values = values != null ? values : EMPTY_STRING_ARRAY;
        this.mapping = mapping;
        this.comment = comment;
    }

    /**
     * Returns a value by index.
     *
     * @param i
     *            a column index (0-based)
     * @return the String at the given index
     */
    public String get(final int i) {
        return values[i];
    }

    /**
     * Returns a value by name.
     *
     * @param name
     *            the name of the column to be retrieved.
     * @return the column value, or {@code null} if the column name is not found
     * @throws IllegalStateException
     *             if no header mapping was provided
     * @throws IllegalArgumentException
     *             if the record is inconsistent
     * @see #isConsistent()
     */
    public String get(final String name) {
        if (mapping == null) {
            throw new IllegalStateException(
                    "No header mapping was specified, the record values can't be accessed by name");
        }
        final Integer index = mapping.get(name);
            return index != null ? values[index.intValue()] : null;
    }

    /**
     * Returns true if this record is consistent, false if not. Currently, the only check is matching the record size to
     * the header size. Some programs can export files that fails this test but still produce parsable files.
     *
     * @return true of this record is valid, false if not
     * @see CSVParserTest#org.apache.commons.csv.CSVParserTest.testMappedButNotSetAsOutlook2007ContactExport()
     */
    public boolean isConsistent() {
        return mapping == null ? true : mapping.size() == values.length;
    }

    /**
     * Checks whether a given column is mapped.
     *
     * @param name
     *            the name of the column to be retrieved.
     * @return whether a given columns is mapped.
     */
    public boolean isMapped(final String name) {
        return mapping != null ? mapping.containsKey(name) : false;
    }

    /**
     * Checks whether a given columns is mapped and has a value.
     *
     * @param name
     *            the name of the column to be retrieved.
     * @return whether a given columns is mapped.
     */
    public boolean isSet(final String name) {
        return isMapped(name) && mapping.get(name).intValue() < values.length;
    }

    /**
     * Returns an iterator over the values of this record.
     *
     * @return an iterator over the values of this record.
     */
    public Iterator<String> iterator() {
        return Arrays.asList(values).iterator();
    }

    String[] values() {
        return values;
    }

    /**
     * Returns the comment for this record, if any.
     *
     * @return the comment for this record, or null if no comment for this
     *         record is available.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns the number of this record in the parsed CSV file.
     *
     * @return the number of this record.
     */
    public long getRecordNumber() {
        return recordNumber;
    }

    /**
     * Returns the number of values in this record.
     *
     * @return the number of values.
     */
    public int size() {
        return values.length;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

}
