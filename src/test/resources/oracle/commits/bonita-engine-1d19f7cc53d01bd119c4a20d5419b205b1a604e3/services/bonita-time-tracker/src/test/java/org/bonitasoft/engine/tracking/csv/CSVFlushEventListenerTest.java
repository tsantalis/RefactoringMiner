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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.bonitasoft.engine.commons.io.IOUtil;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.tracking.AbstractTimeTrackerTest;
import org.bonitasoft.engine.tracking.FlushEvent;
import org.bonitasoft.engine.tracking.Record;
import org.junit.After;
import org.junit.Test;

public class CSVFlushEventListenerTest extends AbstractTimeTrackerTest {

    @After
    public void after() {
        final FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.matches("(.*)bonita_timetracker(.*).csv");
            }
        };

        final File temp_dir = new File(IOUtil.TMP_DIRECTORY);
        final String[] list = temp_dir.list(filter);
        for (final String fileName : list) {
            IOUtil.deleteFile(new File(temp_dir, fileName), 1, 0);
        }
    }

    @Test
    public void should_work_if_output_folder_is_a_folder() {
        final TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        new CSVFlushEventListener(logger, IOUtil.TMP_DIRECTORY, ";");
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_if_output_folder_unknown() {
        final TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        new CSVFlushEventListener(logger, "unknownFolder", ";");
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_if_outputfolder_is_a_file() throws Exception {
        final TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        final File file = IOUtil.createTempFile("test", ".txt", new File(IOUtil.TMP_DIRECTORY));
        file.createNewFile();

        new CSVFlushEventListener(logger, file.getAbsolutePath(), ";");
    }

    @Test
    public void flushedCsv() throws Exception {
        final TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        final CSVFlushEventListener csvFlushEventListener = new CSVFlushEventListener(logger, System.getProperty("java.io.tmpdir"), ";");
        final Record rec1 = new Record(System.currentTimeMillis(), "rec", "rec1Desc", 100);
        final Record rec2 = new Record(System.currentTimeMillis(), "rec", "rec2Desc", 200);

        final CSVFlushEventListenerResult csvFlushResult = csvFlushEventListener.flush(new FlushEvent(System.currentTimeMillis(), Arrays.asList(rec1, rec2)));


        final File csvFile = csvFlushResult.getOutputFile();
        final List<List<String>> csvValues = CSVUtil.readCSV(true, csvFile, ";");
        assertEquals(2, csvValues.size());
        checkCSVRecord(rec1, csvValues.get(0));
        checkCSVRecord(rec2, csvValues.get(1));

        final List<Record> records = csvFlushResult.getFlushEvent().getRecords();
        assertEquals(2, records.size());
        checkRecord(rec1, records.get(0));
        checkRecord(rec2, records.get(1));
    }

    private void checkCSVRecord(final Record record, final List<String> csvValues) {
        // timestamp, year, month, day, hour, minute, second, milisecond, duration, name, description]
        assertEquals(11, csvValues.size());

        final long timestamp = record.getTimestamp();
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH) + 1;
        final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        final int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        final int minute = cal.get(Calendar.MINUTE);
        final int second = cal.get(Calendar.SECOND);
        final int milisecond = cal.get(Calendar.MILLISECOND);

        assertEquals(timestamp, Long.valueOf(csvValues.get(0)).longValue());
        assertEquals(year, Integer.valueOf(csvValues.get(1)).intValue());
        assertEquals(month, Integer.valueOf(csvValues.get(2)).intValue());
        assertEquals(dayOfMonth, Integer.valueOf(csvValues.get(3)).intValue());
        assertEquals(hourOfDay, Integer.valueOf(csvValues.get(4)).intValue());
        assertEquals(minute, Integer.valueOf(csvValues.get(5)).intValue());
        assertEquals(second, Integer.valueOf(csvValues.get(6)).intValue());
        assertEquals(milisecond, Integer.valueOf(csvValues.get(7)).intValue());

        assertEquals(record.getDuration(), Long.valueOf(csvValues.get(8)).longValue());
        assertEquals(record.getName(), csvValues.get(9));
        assertEquals(record.getDescription(), csvValues.get(10));

    }

}
