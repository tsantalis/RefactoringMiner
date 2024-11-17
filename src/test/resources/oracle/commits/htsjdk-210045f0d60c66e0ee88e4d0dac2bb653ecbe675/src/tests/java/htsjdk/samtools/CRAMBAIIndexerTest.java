package htsjdk.samtools;

import htsjdk.samtools.cram.build.ContainerFactory;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.Slice;
import htsjdk.samtools.seekablestream.SeekableMemoryStream;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vadim on 12/01/2016.
 */
public class CRAMBAIIndexerTest {

    private static CramCompressionRecord createRecord(int recordIndex, int seqId, int start) {
        byte[] bases = "AAAAA".getBytes();
        int readLength = bases.length;

        final CramCompressionRecord record = new CramCompressionRecord();
        record.setSegmentUnmapped(false);
        record.setMultiFragment(false);
        record.sequenceId = seqId;
        record.alignmentStart =start;
        record.readBases = record.qualityScores = bases;
        record.readName = Integer.toString(recordIndex);
        record.readLength = readLength;
        record.readFeatures = Collections.emptyList();

        return record;
    }
    @Test
    public void test_processMultiContainer() throws IOException, IllegalAccessException {
        SAMFileHeader samFileHeader = new SAMFileHeader();
        samFileHeader.addSequence(new SAMSequenceRecord("1", 10));
        samFileHeader.addSequence(new SAMSequenceRecord("2", 10));
        samFileHeader.addSequence(new SAMSequenceRecord("3", 10));
        ByteArrayOutputStream indexBAOS = new ByteArrayOutputStream();
        CRAMIndexer indexer = new CRAMIndexer(indexBAOS, samFileHeader);
        int recordsPerContainer = 3;
        ContainerFactory containerFactory = new ContainerFactory(samFileHeader, recordsPerContainer);
        List<CramCompressionRecord> records = new ArrayList<>();
        records.add(createRecord(0, 0, 1));
        records.add(createRecord(1, 1, 2));
        records.add(createRecord(2, 1, 3));

        final Container container1 = containerFactory.buildContainer(records);
        Assert.assertNotNull(container1);
        Assert.assertEquals(container1.nofRecords, records.size());
        Assert.assertEquals(container1.sequenceId, Slice.MULTI_REFERENCE);

        indexer.processContainer(container1, ValidationStringency.STRICT);

        records.clear();
        records.add(createRecord(3, 1, 3));
        records.add(createRecord(4, 2, 3));
        records.add(createRecord(5, 2, 4));
        final Container  container2 = containerFactory.buildContainer(records);
        Assert.assertNotNull(container2);
        Assert.assertEquals(container2.nofRecords, records.size());
        Assert.assertEquals(container2.sequenceId, Slice.MULTI_REFERENCE);

        indexer.processContainer(container2, ValidationStringency.STRICT);

        indexer.finish();

        BAMIndex index = new CachingBAMFileIndex(new SeekableMemoryStream(indexBAOS.toByteArray(), null), samFileHeader.getSequenceDictionary());
        final BAMIndexMetaData metaData_0 = index.getMetaData(0);
        Assert.assertNotNull(metaData_0);
        Assert.assertEquals(metaData_0.getAlignedRecordCount(), 1);

        final BAMIndexMetaData metaData_1 = index.getMetaData(1);
        Assert.assertNotNull(metaData_1);
        Assert.assertEquals(metaData_1.getAlignedRecordCount(), 3);

        final BAMIndexMetaData metaData_2 = index.getMetaData(2);
        Assert.assertNotNull(metaData_2);
        Assert.assertEquals(metaData_2.getAlignedRecordCount(), 2);
    }

}
