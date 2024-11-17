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
import htsjdk.samtools.cram.digest.ContentDigests;
import htsjdk.samtools.cram.encoding.ExternalCompressor;
import htsjdk.samtools.cram.encoding.writer.DataWriterFactory;
import htsjdk.samtools.cram.encoding.writer.Writer;
import htsjdk.samtools.cram.io.DefaultBitOutputStream;
import htsjdk.samtools.cram.io.ExposedByteArrayOutputStream;
import htsjdk.samtools.cram.structure.Block;
import htsjdk.samtools.cram.structure.BlockContentType;
import htsjdk.samtools.cram.structure.CompressionHeader;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.Slice;
import htsjdk.samtools.cram.structure.SubstitutionMatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerFactory {
    private final SAMFileHeader samFileHeader;
    private int recordsPerSlice = 10000;
    private boolean preserveReadNames = true;
    private long globalRecordCounter = 0;

    public ContainerFactory(final SAMFileHeader samFileHeader, final int recordsPerSlice) {
        this.samFileHeader = samFileHeader;
        this.recordsPerSlice = recordsPerSlice;
    }

    public Container buildContainer(final List<CramCompressionRecord> records)
            throws IllegalArgumentException, IllegalAccessException,
            IOException {
        return buildContainer(records, null);
    }

    Container buildContainer(final List<CramCompressionRecord> records,
                             final SubstitutionMatrix substitutionMatrix)
            throws IllegalArgumentException, IllegalAccessException,
            IOException {
        // get stats, create compression header and slices
        final long time1 = System.nanoTime();
        final CompressionHeader header = new CompressionHeaderFactory().build(records,
                substitutionMatrix, samFileHeader.getSortOrder() == SAMFileHeader.SortOrder.coordinate);
        header.APDelta = true;
        final long time2 = System.nanoTime();

        header.readNamesIncluded = preserveReadNames;
        header.APDelta = true;

        final List<Slice> slices = new ArrayList<Slice>();

        final Container container = new Container();
        container.header = header;
        container.nofRecords = records.size();
        container.globalRecordCounter = globalRecordCounter;
        container.bases = 0;
        container.blockCount = 0;

        final long time3 = System.nanoTime();
        long lastGlobalRecordCounter = container.globalRecordCounter;
        for (int i = 0; i < records.size(); i += recordsPerSlice) {
            final List<CramCompressionRecord> sliceRecords = records.subList(i,
                    Math.min(records.size(), i + recordsPerSlice));
            final Slice slice = buildSlice(sliceRecords, header);
            slice.globalRecordCounter = lastGlobalRecordCounter;
            lastGlobalRecordCounter += slice.nofRecords;
            container.bases += slice.bases;
            slices.add(slice);

            // assuming one sequence per container max:
            if (container.sequenceId == -1 && slice.sequenceId != -1)
                container.sequenceId = slice.sequenceId;
        }

        final long time4 = System.nanoTime();

        container.slices = slices.toArray(new Slice[slices.size()]);
        calculateAlignmentBoundaries(container);

        container.buildHeaderTime = time2 - time1;
        container.buildSlicesTime = time4 - time3;

        globalRecordCounter += records.size();
        return container;
    }

    private static void calculateAlignmentBoundaries(final Container container) {
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (final Slice s : container.slices) {
            if (s.sequenceId != SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
                start = Math.min(start, s.alignmentStart);
                end = Math.max(end, s.alignmentStart + s.alignmentSpan);
            }
        }

        if (start < Integer.MAX_VALUE) {
            container.alignmentStart = start;
            container.alignmentSpan = end - start;
        }
    }

    private static Slice buildSlice(final List<CramCompressionRecord> records,
                                    final CompressionHeader header)
            throws IllegalArgumentException, IllegalAccessException,
            IOException {
        final Map<Integer, ExposedByteArrayOutputStream> map = new HashMap<Integer, ExposedByteArrayOutputStream>();
        for (final int id : header.externalIds) {
            map.put(id, new ExposedByteArrayOutputStream());
        }

        final DataWriterFactory dataWriterFactory = new DataWriterFactory();
        final ExposedByteArrayOutputStream bitBAOS = new ExposedByteArrayOutputStream();
        final DefaultBitOutputStream bitOutputStream = new DefaultBitOutputStream(bitBAOS);

        final Slice slice = new Slice();
        slice.nofRecords = records.size();

        int minAlStart = Integer.MAX_VALUE;
        int maxAlEnd = SAMRecord.NO_ALIGNMENT_START;
        {
            // @formatter:off
            /*
             * 1) Count slice bases.
			 * 2) Decide if the slice is single ref, unmapped or multi reference.
			 * 3) Detect alignment boundaries for the slice if not multi reference.
			 */
            // @formatter:on
            slice.sequenceId = Slice.UNMAPPED_OR_NO_REFERENCE;
            final ContentDigests hasher = ContentDigests.create(ContentDigests.ALL);
            for (final CramCompressionRecord record : records) {
                slice.bases += record.readLength;
                hasher.add(record);

                if (slice.sequenceId != Slice.MULTI_REFERENCE
                        && record.alignmentStart != SAMRecord.NO_ALIGNMENT_START
                        && record.sequenceId != SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
                    switch (slice.sequenceId) {
                        case Slice.UNMAPPED_OR_NO_REFERENCE:
                            slice.sequenceId = record.sequenceId;
                            break;
                        case Slice.MULTI_REFERENCE:
                            break;

                        default:
                            if (slice.sequenceId != record.sequenceId)
                                slice.sequenceId = Slice.UNMAPPED_OR_NO_REFERENCE;
                            break;
                    }

                    minAlStart = Math.min(record.alignmentStart, minAlStart);
                    maxAlEnd = Math.max(record.getAlignmentEnd(), maxAlEnd);
                }
            }

            slice.sliceTags = hasher.getAsTags();
        }

        if (slice.sequenceId == Slice.MULTI_REFERENCE
                || minAlStart == Integer.MAX_VALUE) {
            slice.alignmentStart = SAMRecord.NO_ALIGNMENT_START;
            slice.alignmentSpan = 0;
        } else {
            slice.alignmentStart = minAlStart;
            slice.alignmentSpan = maxAlEnd - minAlStart + 1;
        }

        final Writer writer = dataWriterFactory.buildWriter(bitOutputStream, map, header, slice.sequenceId);
        int prevAlStart = slice.alignmentStart;
        for (final CramCompressionRecord record : records) {
            record.alignmentDelta = record.alignmentStart - prevAlStart;
            prevAlStart = record.alignmentStart;
            writer.write(record);
        }

        bitOutputStream.close();
        slice.coreBlock = Block.buildNewCore(bitBAOS.toByteArray());

        slice.external = new HashMap<Integer, Block>();
        for (final Integer key : map.keySet()) {
            final ExposedByteArrayOutputStream os = map.get(key);

            final Block externalBlock = new Block();
            externalBlock.setContentId(key);
            externalBlock.setContentType(BlockContentType.EXTERNAL);

            final ExternalCompressor compressor = header.externalCompressors.get(key);
            final byte[] rawData = os.toByteArray();
            final byte[] compressed = compressor.compress(rawData);
            externalBlock.setContent(rawData, compressed);
            externalBlock.setMethod(compressor.getMethod());
            slice.external.put(key, externalBlock);
        }

        return slice;
    }

    public boolean isPreserveReadNames() {
        return preserveReadNames;
    }

    public void setPreserveReadNames(final boolean preserveReadNames) {
        this.preserveReadNames = preserveReadNames;
    }
}
