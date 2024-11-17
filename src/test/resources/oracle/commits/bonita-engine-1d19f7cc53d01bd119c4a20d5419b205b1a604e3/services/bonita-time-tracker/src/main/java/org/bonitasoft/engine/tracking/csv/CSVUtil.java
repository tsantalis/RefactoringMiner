/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.tracking.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

public class CSVUtil {

    public static List<List<String>> readCSV(final boolean excludeHeader, final File csvFile, final String csvSeparator) throws FileNotFoundException {
        final List<List<String>> array = new ArrayList<List<String>>();
        final InputStream inputStream = new FileInputStream(csvFile);
        final Scanner scanner = new Scanner(inputStream);
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                final List<String> lineElements = new ArrayList<String>();
                lineElements.addAll(Arrays.asList(line.split(csvSeparator)));
                array.add(lineElements);
            }
        } finally {
            scanner.close();
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (excludeHeader) {
            array.remove(0);
        }
        return array;
    }

    public static void writeCSVRow(final FileWriter writer, final List<String> row, final String csvSeparator) throws IOException {
        boolean first = true;
        for (final String value : row) {
            if (first) {
                writer.append(value);
                first = false;
            } else {
                writer.append(csvSeparator);
                writer.append(value);
            }
        }
        writer.append("\n");
        writer.flush();
    }

    public static void writeCSVRow(final File file, final List<String> row, final String csvSeparator) throws IOException {
        final FileWriter writer = new FileWriter(file, true);
        writeCSVRow(writer, row, csvSeparator);
        writer.close();
    }

    public static void writeCSVRows(final File file, final List<List<String>> array, final String csvSeparator) throws IOException {
        final FileWriter writer = new FileWriter(file, true);
        for (final List<String> row : array) {
            writeCSVRow(writer, row, csvSeparator);
        }
        writer.close();
    }

}
