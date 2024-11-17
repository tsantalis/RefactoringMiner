/**
 * ****************************************************************************
 * Copyright 2013 EMBL-EBI
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package htsjdk.samtools.cram.build;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.encoding.reader.CramRecordReader;
import htsjdk.samtools.cram.encoding.reader.DataReaderFactory;
import htsjdk.samtools.cram.encoding.reader.DataReaderFactory.DataReaderWithStats;
import htsjdk.samtools.cram.io.DefaultBitInputStream;
import htsjdk.samtools.cram.structure.CompressionHeader;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.Slice;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.Log.LogLevel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ContainerParser {
    private static final Log log = Log.getInstance(ContainerParser.class);

    private final SAMFileHeader samFileHeader;
    private final Map<String, Long> nanosecondsMap = new TreeMap<String, Long>();

    public ContainerParser(final SAMFileHeader samFileHeader) {
        this.samFileHeader = samFileHeader;
    }

    public List<CramCompressionRecord> getRecords(final Container container,
                                                  ArrayList<CramCompressionRecord> records, ValidationStringency validationStringency) throws IllegalArgumentException,
            IllegalAccessException {
        final long time1 = System.nanoTime();
        if (records == null)
            records = new ArrayList<CramCompressionRecord>(container.nofRecords);

        for (final Slice slice : container.slices)
            records.addAll(getRecords(slice, container.header, validationStringency));

        final long time2 = System.nanoTime();

        container.parseTime = time2 - time1;

        if (log.isEnabled(LogLevel.DEBUG)) {
            for (final String key : nanosecondsMap.keySet()) {
                log.debug(String.format("%s: %dms.", key, nanosecondsMap.get(key) / 1000000));
            }
        }

        return records;
    }

    ArrayList<CramCompressionRecord> getRecords(ArrayList<CramCompressionRecord> records,
                                                final Slice slice, final CompressionHeader header, ValidationStringency validationStringency) throws IllegalArgumentException,
            IllegalAccessException {
        String seqName = SAMRecord.NO_ALIGNMENT_REFERENCE_NAME;
        switch (slice.sequenceId) {
            case SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX:
            case -2:
                break;

            default:
                final SAMSequenceRecord sequence = samFileHeader
                        .getSequence(slice.sequenceId);
                seqName = sequence.getSequenceName();
                break;
        }

        final DataReaderFactory dataReaderFactory = new DataReaderFactory();
        final Map<Integer, InputStream> inputMap = new HashMap<Integer, InputStream>();
        for (final Integer exId : slice.external.keySet()) {
            log.debug("Adding external data: " + exId);
            inputMap.put(exId, new ByteArrayInputStream(slice.external.get(exId)
                    .getRawContent()));
        }

        long time;
        final CramRecordReader reader = new CramRecordReader(validationStringency);
        dataReaderFactory.buildReader(reader, new DefaultBitInputStream(
                        new ByteArrayInputStream(slice.coreBlock.getRawContent())),
                inputMap, header, slice.sequenceId);

        if (records == null)
            records = new ArrayList<CramCompressionRecord>(slice.nofRecords);

        long readNanos = 0;
        int prevStart = slice.alignmentStart;
        for (int i = 0; i < slice.nofRecords; i++) {
            final CramCompressionRecord record = new CramCompressionRecord();
            record.sliceIndex = slice.index;
            record.index = i;

            time = System.nanoTime();
            reader.read(record);
            readNanos += System.nanoTime() - time;

            if (record.sequenceId == slice.sequenceId) {
                record.sequenceName = seqName;
                record.sequenceId = slice.sequenceId;
            } else {
                if (record.sequenceId == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX)
                    record.sequenceName = SAMRecord.NO_ALIGNMENT_REFERENCE_NAME;
                else {
                    record.sequenceName = samFileHeader.getSequence(record.sequenceId)
                            .getSequenceName();
                }
            }

            records.add(record);

            if (header.APDelta) {
                prevStart += record.alignmentDelta;
                record.alignmentStart = prevStart;
            }
        }
        log.debug("Slice records read time: " + readNanos / 1000000);

        final Map<String, DataReaderWithStats> statMap = dataReaderFactory.getStats(reader);
        for (final String key : statMap.keySet()) {
            final long value;
            if (!nanosecondsMap.containsKey(key)) {
                nanosecondsMap.put(key, 0L);
                value = 0;
            } else
                value = nanosecondsMap.get(key);
            nanosecondsMap.put(key, value + statMap.get(key).nanos);
        }
        return records;
    }

    List<CramCompressionRecord> getRecords(final Slice slice, final CompressionHeader header, ValidationStringency validationStringency)
            throws IllegalArgumentException, IllegalAccessException {
        return getRecords(null, slice, header, validationStringency);
    }
}
