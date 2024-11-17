/*
 * The MIT License
 *
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package htsjdk.samtools;

import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.reference.InMemoryReferenceSequenceFile;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.Log.LogLevel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CramFileWriterTest {

    @BeforeClass
    public void initClass() {
        Log.setGlobalLogLevel(LogLevel.ERROR);
    }

    @Test(description = "Test for lossy CRAM compression invariants.")
    public void lossyCramInvariantsTest() {
        doTest(createRecords(1000));
    }

    @Test(description = "Tests a writing records with null SAMFileHeaders")
    public void writeRecordsWithNullHeader() throws Exception {

        final List<SAMRecord> samRecs = createRecords(50);
        for (SAMRecord rec : samRecs) {
            rec.setHeader(null);
        }
        doTest(samRecs);
    }

    @Test(description = "Tests a unmapped record with sequence and quality fields")
    public void unmappedWithSequenceAndQualityField() throws Exception {
        unmappedSequenceAndQualityFieldHelper(true);
    }

    @Test(description = "Tests a unmapped record with no sequence or quality fields")
    public void unmappedWithNoSequenceAndQualityField() throws Exception {
        unmappedSequenceAndQualityFieldHelper(false);
    }

    private void unmappedSequenceAndQualityFieldHelper(boolean unmappedHasBasesAndQualities) throws Exception {
        List<SAMRecord> list = new ArrayList<SAMRecord>(2);
        final SAMRecordSetBuilder builder = new SAMRecordSetBuilder();
        if (builder.getHeader().getReadGroups().isEmpty()) {
            throw new Exception("Read group expected in the header");
        }

        builder.setUnmappedHasBasesAndQualities(unmappedHasBasesAndQualities);

        builder.addUnmappedFragment("test1");
        builder.addUnmappedPair("test2");

        list.addAll(builder.getRecords());

        Collections.sort(list, new SAMRecordCoordinateComparator());

        doTest(list);
    }

    private List<SAMRecord> createRecords(int count) {
        List<SAMRecord> list = new ArrayList<SAMRecord>(count);
        final SAMRecordSetBuilder builder = new SAMRecordSetBuilder();
        if (builder.getHeader().getReadGroups().isEmpty()) {
            throw new IllegalStateException("Read group expected in the header");
        }

        int posInRef = 1;
        for (int i = 0; i < count / 2; i++) {
            builder.addPair(Integer.toString(i), 0, posInRef += 1,
                    posInRef += 3);
        }
        list.addAll(builder.getRecords());

        Collections.sort(list, new SAMRecordCoordinateComparator());

        return list;
    }

    private SAMFileHeader createSAMHeader(SAMFileHeader.SortOrder sortOrder) {
        final SAMFileHeader header = new SAMFileHeader();
        header.setSortOrder(sortOrder);
        header.addSequence(new SAMSequenceRecord("chr1", 123));
        SAMReadGroupRecord readGroupRecord = new SAMReadGroupRecord("1");
        header.addReadGroup(readGroupRecord);
        return header;
    }

    private ReferenceSource createReferenceSource() {
        byte[] refBases = new byte[1024 * 1024];
        Arrays.fill(refBases, (byte) 'A');
        InMemoryReferenceSequenceFile rsf = new InMemoryReferenceSequenceFile();
        rsf.add("chr1", refBases);
        return new ReferenceSource(rsf);
    }

    private void writeRecordsToCRAM(CRAMFileWriter writer, List<SAMRecord> samRecords) {
        for (SAMRecord record : samRecords) {
            writer.addAlignment(record);
        }
        writer.close();
    }

    private void validateRecords(final List<SAMRecord> expectedRecords, ByteArrayInputStream is, ReferenceSource referenceSource) {
        CRAMFileReader cReader = new CRAMFileReader(null, is, referenceSource);

        SAMRecordIterator iterator2 = cReader.getIterator();
        int index = 0;
        while (iterator2.hasNext()) {
            SAMRecord actualRecord = iterator2.next();
            SAMRecord expectedRecord = expectedRecords.get(index++);

            Assert.assertEquals(actualRecord.getReadName(), expectedRecord.getReadName());
            Assert.assertEquals(actualRecord.getFlags(), expectedRecord.getFlags());
            Assert.assertEquals(actualRecord.getAlignmentStart(), expectedRecord.getAlignmentStart());
            Assert.assertEquals(actualRecord.getAlignmentEnd(), expectedRecord.getAlignmentEnd());
            Assert.assertEquals(actualRecord.getReferenceName(), expectedRecord.getReferenceName());
            Assert.assertEquals(actualRecord.getMateAlignmentStart(),
                    expectedRecord.getMateAlignmentStart());
            Assert.assertEquals(actualRecord.getMateReferenceName(),
                    expectedRecord.getMateReferenceName());
            Assert.assertEquals(actualRecord.getReadBases(), expectedRecord.getReadBases());
            Assert.assertEquals(actualRecord.getBaseQualities(), expectedRecord.getBaseQualities());
        }
        cReader.close();
    }

    private void doTest(final List<SAMRecord> samRecords) {
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final ReferenceSource refSource = createReferenceSource();
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        CRAMFileWriter writer = new CRAMFileWriter(os, refSource, header, null);
        writeRecordsToCRAM(writer, samRecords);

        validateRecords(samRecords, new ByteArrayInputStream(os.toByteArray()), refSource);
    }

    @Test(description = "Test CRAMWriter constructor with index stream")
    public void testCRAMWriterWithIndex() {
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final ReferenceSource refSource = createReferenceSource();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream indexStream = new ByteArrayOutputStream();

        final List<SAMRecord> samRecords = createRecords(100);
        CRAMFileWriter writer = new CRAMFileWriter(outStream, indexStream, refSource, header, null);

        writeRecordsToCRAM(writer, samRecords);
        validateRecords(samRecords, new ByteArrayInputStream(outStream.toByteArray()), refSource);
        Assert.assertTrue(indexStream.size() != 0);
    }

    @Test(description = "Test CRAMWriter constructor with presorted==false")
    public void testCRAMWriterNotPresorted() {
        final SAMFileHeader header = createSAMHeader(SAMFileHeader.SortOrder.coordinate);
        final ReferenceSource refSource = createReferenceSource();
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream indexStream = new ByteArrayOutputStream();

        CRAMFileWriter writer = new CRAMFileWriter(outStream, indexStream, false, refSource, header, null);

        // force records to not be coordinate sorted to ensure we're relying on presorted=false
        final List<SAMRecord> samRecords = createRecords(100);
        Collections.sort(samRecords, new SAMRecordCoordinateComparator().reversed());

        writeRecordsToCRAM(writer, samRecords);

        // for validation, restore the sort order of the expected records so they match the order of the written records
        Collections.sort(samRecords, new SAMRecordCoordinateComparator());
        validateRecords(samRecords, new ByteArrayInputStream(outStream.toByteArray()), refSource);
        Assert.assertTrue(indexStream.size() != 0);
    }

    @Test
    public void test_roundtrip_tlen_preserved() throws IOException {
        SamReader reader = SamReaderFactory.make().open(new File("testdata/htsjdk/samtools/cram_tlen_reads.sorted.sam"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ReferenceSource source = new ReferenceSource(new File("testdata/htsjdk/samtools/cram_tlen.fasta"));
        CRAMFileWriter writer = new CRAMFileWriter(baos, source, reader.getFileHeader(), "test.cram");
        SAMRecordIterator iterator = reader.iterator();
        List<SAMRecord> records = new ArrayList<SAMRecord>();
        while (iterator.hasNext()) {
            final SAMRecord record = iterator.next();
            writer.addAlignment(record);
            records.add(record);
        }
        writer.close();

        CRAMFileReader cramReader = new CRAMFileReader(new ByteArrayInputStream(baos.toByteArray()), (File) null, source, ValidationStringency.STRICT);
        iterator = cramReader.getIterator();
        int i = 0;
        while (iterator.hasNext()) {
            SAMRecord record1 = iterator.next();
            SAMRecord record2 = records.get(i++);
            Assert.assertEquals(record1.getInferredInsertSize(), record2.getInferredInsertSize(), record1.getReadName());
        }
        Assert.assertEquals(records.size(), i);
    }

    @Test
    public void testCRAMQuerySort() throws IOException {
        final File input = new File("testdata/htsjdk/samtools/cram_query_sorted.cram");
        final File reference = new File("testdata/htsjdk/samtools/cram_query_sorted.fasta");
        final File outputFile = File.createTempFile("tmp.", ".cram");

        try (final SamReader reader = SamReaderFactory.makeDefault().referenceSequence(reference).open(input);
             final SAMFileWriter writer = new SAMFileWriterFactory().makeWriter(reader.getFileHeader().clone(), false, outputFile, reference)) {
            for (SAMRecord rec : reader) {
                    writer.addAlignment(rec);
            }
        }

        try (final SamReader outReader = SamReaderFactory.makeDefault().referenceSequence(reference).open(outputFile)) {
            String prevName = null;
            for (final SAMRecord rec : outReader) {
                if (prevName == null) {
                    prevName = rec.getReadName();
                    continue;
                }
                // test if the read names are sorted alphabetically:
                Assert.assertTrue(rec.getReadName().compareTo(prevName) >= 0);
            }
        }

    }

}
