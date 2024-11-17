package htsjdk.samtools.cram.build;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.structure.AlignmentSpan;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.Slice;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by vadim on 11/01/2016.
 */
public class ContainerParserTest {

    @Test
    public void testSingleRefContainer() throws IOException, IllegalAccessException {
        SAMFileHeader samFileHeader = new SAMFileHeader();
        ContainerFactory factory = new ContainerFactory(samFileHeader, 10);
        List<CramCompressionRecord> records = new ArrayList<>();
        for (int i=0; i<10; i++) {
            CramCompressionRecord record = new CramCompressionRecord();
            record.readBases="AAA".getBytes();
            record.qualityScores="!!!".getBytes();
            record.setSegmentUnmapped(false);
            record.readName=""+i;
            record.sequenceId=0;
            record.setLastSegment(true);
            record.readFeatures = Collections.emptyList();

            records.add(record);
        }

        Container container = factory.buildContainer(records);
        Assert.assertEquals(container.nofRecords, 10);
        Assert.assertEquals(container.sequenceId, 0);

        ContainerParser parser = new ContainerParser(samFileHeader);
        final Map<Integer, AlignmentSpan> referenceSet = parser.getReferences(container, ValidationStringency.STRICT);
        Assert.assertNotNull(referenceSet);
        Assert.assertEquals(referenceSet.size(), 1);
        Assert.assertTrue(referenceSet.containsKey(0));
    }

    @Test
    public void testUnmappedContainer() throws IOException, IllegalAccessException {
        SAMFileHeader samFileHeader = new SAMFileHeader();
        ContainerFactory factory = new ContainerFactory(samFileHeader, 10);
        List<CramCompressionRecord> records = new ArrayList<>();
        for (int i=0; i<10; i++) {
            CramCompressionRecord record = new CramCompressionRecord();
            record.readBases="AAA".getBytes();
            record.qualityScores="!!!".getBytes();
            record.setSegmentUnmapped(true);
            record.readName=""+i;
            record.sequenceId= SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX;
            record.setLastSegment(true);

            records.add(record);
        }

        Container container = factory.buildContainer(records);
        Assert.assertEquals(container.nofRecords, 10);
        Assert.assertEquals(container.sequenceId, SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX);

        ContainerParser parser = new ContainerParser(samFileHeader);
        final Map<Integer, AlignmentSpan> referenceSet = parser.getReferences(container, ValidationStringency.STRICT);
        Assert.assertNotNull(referenceSet);
        Assert.assertEquals(referenceSet.size(), 1);
        Assert.assertTrue(referenceSet.containsKey(SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX));

    }

    @Test
    public void testMappedAndUnmappedContainer() throws IOException, IllegalAccessException {
        SAMFileHeader samFileHeader = new SAMFileHeader();
        ContainerFactory factory = new ContainerFactory(samFileHeader, 10);
        List<CramCompressionRecord> records = new ArrayList<>();
        for (int i=0; i<10; i++) {
            CramCompressionRecord record = new CramCompressionRecord();
            record.readBases="AAA".getBytes();
            record.qualityScores="!!!".getBytes();
            record.readName=""+i;
            record.alignmentStart=i+1;

            record.setMultiFragment(false);
            if (i%2==0) {
                record.sequenceId = SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX;
                record.setSegmentUnmapped(true);
            } else {
                record.sequenceId=0;
                record.readFeatures = Collections.emptyList();
                record.setSegmentUnmapped(false);
            }
            records.add(record);
        }



        Container container = factory.buildContainer(records);
        Assert.assertEquals(container.nofRecords, 10);
        Assert.assertEquals(container.sequenceId, Slice.MULTI_REFERENCE);

        ContainerParser parser = new ContainerParser(samFileHeader);
        final Map<Integer, AlignmentSpan> referenceSet = parser.getReferences(container, ValidationStringency.STRICT);
        Assert.assertNotNull(referenceSet);
        Assert.assertEquals(referenceSet.size(), 2);
        Assert.assertTrue(referenceSet.containsKey(SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX));
        Assert.assertTrue(referenceSet.containsKey(0));
    }

    @Test
    public void testMultirefContainer() throws IOException, IllegalAccessException {
        SAMFileHeader samFileHeader = new SAMFileHeader();
        ContainerFactory factory = new ContainerFactory(samFileHeader, 10);
        List<CramCompressionRecord> records = new ArrayList<>();
        for (int i=0; i<10; i++) {
            CramCompressionRecord record = new CramCompressionRecord();
            record.readBases="AAA".getBytes();
            record.qualityScores="!!!".getBytes();
            record.readName=""+i;
            record.alignmentStart=i+1;
            record.readLength = 3;

            record.setMultiFragment(false);
            if (i < 9) {
                record.sequenceId=i;
                record.readFeatures = Collections.emptyList();
                record.setSegmentUnmapped(false);
            } else {
                record.sequenceId = SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX;
                record.setSegmentUnmapped(true);
            }
            records.add(record);
        }

        Container container = factory.buildContainer(records);
        Assert.assertEquals(container.nofRecords, 10);
        Assert.assertEquals(container.sequenceId, Slice.MULTI_REFERENCE);

        ContainerParser parser = new ContainerParser(samFileHeader);
        final Map<Integer, AlignmentSpan> referenceSet = parser.getReferences(container, ValidationStringency.STRICT);
        Assert.assertNotNull(referenceSet);
        Assert.assertEquals(referenceSet.size(), 10);
        Assert.assertTrue(referenceSet.containsKey(SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX));
        for (int i=0; i<9; i++) {
            Assert.assertTrue(referenceSet.containsKey(i));
            AlignmentSpan span = referenceSet.get(i);
            Assert.assertEquals(span.getCount(), 1);
            Assert.assertEquals(span.getStart(), i+1);
            Assert.assertEquals(span.getSpan(), 3);
        }
    }
}
