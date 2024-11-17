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
import htsjdk.samtools.cram.ref.ReferenceContext;
import htsjdk.samtools.cram.structure.AlignmentSpan;
import htsjdk.samtools.cram.encoding.reader.CramRecordReader;
import htsjdk.samtools.cram.structure.CompressionHeader;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.Slice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerParser {
    private final SAMFileHeader samFileHeader;

    public ContainerParser(final SAMFileHeader samFileHeader) {
        this.samFileHeader = samFileHeader;
    }

    /**
     * Iterate through all of this container's {@link Slice}s to derive a map of reference sequence IDs
     * to {@link AlignmentSpan}s.  Used to create BAI Indexes.
     *
     * @param container the container to iterate through
     * @param validationStringency stringency for validating records, passed to
     * {@link Slice#getMultiRefAlignmentSpans(CompressionHeader, ValidationStringency)}
     * @return the map of map of reference sequence IDs to AlignmentSpans.
     */
    public Map<ReferenceContext, AlignmentSpan> getSpans(final Container container, final ValidationStringency validationStringency) {
        final Map<ReferenceContext, AlignmentSpan> containerSpanMap  = new HashMap<>();
        for (final Slice slice : container.slices) {
            switch (slice.getReferenceContext().getType()) {
                case UNMAPPED_UNPLACED_TYPE:
                    containerSpanMap.put(ReferenceContext.UNMAPPED_UNPLACED_CONTEXT, AlignmentSpan.UNMAPPED_SPAN);
                    break;
                case MULTIPLE_REFERENCE_TYPE:
                    final Map<ReferenceContext, AlignmentSpan> spans = slice.getMultiRefAlignmentSpans(container.header, validationStringency);
                    for (final Map.Entry<ReferenceContext, AlignmentSpan> entry : spans.entrySet()) {
                        containerSpanMap.merge(entry.getKey(), entry.getValue(), AlignmentSpan::add);
                    }
                    break;
                default:
                    final AlignmentSpan alignmentSpan = new AlignmentSpan(slice.alignmentStart, slice.alignmentSpan, slice.nofRecords);
                    containerSpanMap.merge(slice.getReferenceContext(), alignmentSpan, AlignmentSpan::add);
                    break;
            }
        }
        return containerSpanMap;
    }

    public List<CramCompressionRecord> getRecords(final Container container,
                                                  ArrayList<CramCompressionRecord> records,
                                                  final ValidationStringency validationStringency) {
        if (container.isEOF()) {
            return Collections.emptyList();
        }

        if (records == null) {
            records = new ArrayList<>(container.nofRecords);
        }

        for (final Slice slice : container.slices) {
            records.addAll(getRecords(slice, container.header, validationStringency));
        }

        return records;
    }

    private ArrayList<CramCompressionRecord> getRecords(final Slice slice,
                                                        final CompressionHeader header,
                                                        final ValidationStringency validationStringency) {
        final ReferenceContext sliceContext = slice.getReferenceContext();
        String seqName = SAMRecord.NO_ALIGNMENT_REFERENCE_NAME;
        if (sliceContext.isMappedSingleRef()) {
            final SAMSequenceRecord sequence = samFileHeader.getSequence(sliceContext.getSequenceId());
            seqName = sequence.getSequenceName();

        }

        final CramRecordReader reader = slice.createCramRecordReader(header, validationStringency);

        final ArrayList<CramCompressionRecord> records = new ArrayList<>(slice.nofRecords);

        int prevAlignmentStart = slice.alignmentStart;
        for (int i = 0; i < slice.nofRecords; i++) {
            final CramCompressionRecord record = new CramCompressionRecord();
            record.sliceIndex = slice.index;
            record.index = i;

            // read the new record and update the running prevAlignmentStart
            prevAlignmentStart = reader.read(record, prevAlignmentStart);

            if (sliceContext.isMappedSingleRef() && record.sequenceId == sliceContext.getSequenceId()) {
                record.sequenceName = seqName;
            } else {
                if (record.sequenceId == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
                    record.sequenceName = SAMRecord.NO_ALIGNMENT_REFERENCE_NAME;
                } else {
                    record.sequenceName = samFileHeader.getSequence(record.sequenceId)
                            .getSequenceName();
                }
            }

            records.add(record);
        }

        return records;
    }
}
