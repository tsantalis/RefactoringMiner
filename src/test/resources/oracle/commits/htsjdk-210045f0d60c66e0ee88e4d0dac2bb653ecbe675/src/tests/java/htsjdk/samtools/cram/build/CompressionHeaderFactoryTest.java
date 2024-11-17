package htsjdk.samtools.cram.build;

import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.encoding.readfeatures.Substitution;
import htsjdk.samtools.cram.structure.CompressionHeader;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.EncodingID;
import htsjdk.samtools.cram.structure.EncodingKey;
import htsjdk.samtools.cram.structure.ReadTag;
import htsjdk.samtools.cram.structure.SubstitutionMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vadim on 07/01/2016.
 */
public class CompressionHeaderFactoryTest {
    @Test
    public void testAllEncodingsPresent() {
        final CompressionHeader header = new CompressionHeaderFactory().build(new ArrayList<>(), new SubstitutionMatrix(new long[256][256]), true);
        for (final EncodingKey key : EncodingKey.values()) {
            switch (key) {
                // skip test marks and unused series:
                case TV_TestMark:
                case TM_TestMark:
                case BB_bases:
                case QQ_scores:
                    Assert.assertFalse(header.encodingMap.containsKey(key), "Unexpected encoding key found: " + key.name());
                    continue;
            }
            Assert.assertTrue(header.encodingMap.containsKey(key), "Encoding key not found: " + key.name());
            Assert.assertNotNull(header.encodingMap.get(key));
            Assert.assertFalse(header.encodingMap.get(key).id == EncodingID.NULL);
        }
    }

    @Test
    public void testAP_delta() {
        boolean sorted = true;
        CompressionHeader header = new CompressionHeaderFactory().build(new ArrayList<>(), new SubstitutionMatrix(new long[256][256]), sorted);
        Assert.assertEquals(header.APDelta, sorted);

        sorted = false;
        header = new CompressionHeaderFactory().build(new ArrayList<>(), new SubstitutionMatrix(new long[256][256]), sorted);
        Assert.assertEquals(header.APDelta, sorted);
    }

    @Test
    public void testGetDataForTag() {
        final CompressionHeaderFactory factory = new CompressionHeaderFactory();
        final List<CramCompressionRecord> records = new ArrayList<>();
        final CramCompressionRecord record = new CramCompressionRecord();
        final int tagID = ReadTag.name3BytesToInt("ACi".getBytes());
        final byte[] data = new byte[]{1, 2, 3, 4};
        final ReadTag tag = new ReadTag(tagID, data, ValidationStringency.STRICT);
        record.tags = new ReadTag[]{tag};
        records.add(record);

        final byte[] dataForTag = factory.getDataForTag(records, tagID);
        Assert.assertEquals(dataForTag, data);
    }

    @Test
    public void test_buildFrequencies() {
        final CramCompressionRecord record = new CramCompressionRecord();
        final Substitution s = new Substitution();
        s.setPosition(1);
        final byte refBase = 'A';
        final byte readBase = 'C';

        s.setBase(readBase);
        s.setReferenceBase(refBase);
        s.setCode((byte) 1);
        record.readFeatures = new ArrayList<>();
        record.readFeatures.add(s);
        record.readLength = 2;

        final List<CramCompressionRecord> records = new ArrayList<>();
        records.add(record);

        final long[][] frequencies = CompressionHeaderFactory.buildFrequencies(records);
        for (int i = 0; i < frequencies.length; i++) {
            for (int j = 0; j < frequencies[i].length; j++) {
                if (i != refBase && j != readBase) {
                    Assert.assertEquals(frequencies[i][j], 0);
                }
            }

        }
        Assert.assertEquals(frequencies[refBase][readBase], 1);
    }

    @Test
    public void test_getBestExternalCompressor() {
        try {
            Assert.assertNotNull(CompressionHeaderFactory.getBestExternalCompressor(null));
            Assert.fail("NPE expected for null data");
        } catch (final NullPointerException e) {

        }
        Assert.assertNotNull(CompressionHeaderFactory.getBestExternalCompressor("".getBytes()));
        Assert.assertNotNull(CompressionHeaderFactory.getBestExternalCompressor("qwe".getBytes()));
    }

    @Test
    public void test_geByteSizeRangeOfTagValues() {
        final List<CramCompressionRecord> records = new ArrayList<>();
        final int tagID = ReadTag.name3BytesToInt("ACi".getBytes());
        // test empty list:
        CompressionHeaderFactory.ByteSizeRange range = CompressionHeaderFactory.geByteSizeRangeOfTagValues(records, tagID);
        Assert.assertNotNull(range);
        Assert.assertEquals(range.min, Integer.MAX_VALUE);
        Assert.assertEquals(range.max, Integer.MIN_VALUE);

        // test single record with a single tag:
        final CramCompressionRecord record = new CramCompressionRecord();
        final byte[] data = new byte[]{1, 2, 3, 4};
        final ReadTag tag = new ReadTag(tagID, data, ValidationStringency.STRICT);
        record.tags = new ReadTag[]{tag};
        records.add(record);

        range = CompressionHeaderFactory.geByteSizeRangeOfTagValues(records, tagID);
        Assert.assertNotNull(range);
        Assert.assertEquals(range.min, 4);
        Assert.assertEquals(range.max, 4);
    }

    @Test
    public void test_getTagType() {
        Assert.assertEquals(CompressionHeaderFactory.getTagType(ReadTag.name3BytesToInt("ACi".getBytes())), 'i');
    }

    @Test
    public void test_getUnusedByte() {
        final byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        int unusedByte = CompressionHeaderFactory.getUnusedByte(data);
        Assert.assertEquals(unusedByte, -1);

        data[5] = 0;
        unusedByte = CompressionHeaderFactory.getUnusedByte(data);
        Assert.assertEquals(unusedByte, 5);
        data[5] = 5;

        data[150] = 0;
        unusedByte = CompressionHeaderFactory.getUnusedByte(data);
        Assert.assertEquals(unusedByte, 150);
    }

    @Test
    public void test_updateSubstitutionCodes() {
        final CramCompressionRecord record = new CramCompressionRecord();
        final Substitution s = new Substitution();
        s.setPosition(1);
        final byte refBase = 'A';
        final byte readBase = 'C';

        s.setBase(readBase);
        s.setReferenceBase(refBase);
        record.readFeatures = new ArrayList<>();
        record.readFeatures.add(s);
        record.readLength = 2;

        final List<CramCompressionRecord> records = new ArrayList<>();
        records.add(record);

        final long[][] frequencies = new long[256][256];
        frequencies[refBase][readBase] = 1;
        SubstitutionMatrix matrix = new SubstitutionMatrix(frequencies);

        Assert.assertTrue(s.getCode() == -1);
        CompressionHeaderFactory.updateSubstitutionCodes(records, matrix);
        Assert.assertFalse(s.getCode() == -1);
        Assert.assertEquals(s.getCode(), matrix.code(refBase, readBase));
    }

    @Test
    public void test_getTagValueByteSize() {
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'i', 1), 4);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'I', 1), 4);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'c', (byte) 1), 1);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'C', -(byte) 1), 1);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 's', (short) 1), 2);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'S', -(short) 1), 2);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'A', 1), 1);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'f', 1f), 4);

        // string values are null-terminated:
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'Z', "blah-blah"), "blah-blah".length() + 1);

        // byte length of an array tag value is: element type (1 byte) + nof bytes (4 bytes) + nof elements * byte size of element
        int elementTypeLength = 1;
        int arraySizeByteLength = 4;
        int arraySize = 3;
        int byteElementSize = 1;
        int int_float_long_elementSize = 4;
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'B', new byte[]{0, 1, 2}), elementTypeLength + arraySizeByteLength + arraySize * byteElementSize);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'B', new int[]{0, 1, 2}), elementTypeLength + arraySizeByteLength + arraySize * int_float_long_elementSize);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'B', new float[]{0, 1, 2}), elementTypeLength + arraySizeByteLength + arraySize * int_float_long_elementSize);
        Assert.assertEquals(CompressionHeaderFactory.getTagValueByteSize((byte) 'B', new long[]{0, 1, 2}), elementTypeLength + arraySizeByteLength + arraySize * int_float_long_elementSize);
    }
}
