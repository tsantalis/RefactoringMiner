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

import htsjdk.samtools.cram.common.MutableInt;
import htsjdk.samtools.cram.encoding.BetaIntegerEncoding;
import htsjdk.samtools.cram.encoding.BitCodec;
import htsjdk.samtools.cram.encoding.ByteArrayLenEncoding;
import htsjdk.samtools.cram.encoding.ByteArrayStopEncoding;
import htsjdk.samtools.cram.encoding.Encoding;
import htsjdk.samtools.cram.encoding.ExternalByteArrayEncoding;
import htsjdk.samtools.cram.encoding.ExternalByteEncoding;
import htsjdk.samtools.cram.encoding.ExternalCompressor;
import htsjdk.samtools.cram.encoding.ExternalIntegerEncoding;
import htsjdk.samtools.cram.encoding.GammaIntegerEncoding;
import htsjdk.samtools.cram.encoding.NullEncoding;
import htsjdk.samtools.cram.encoding.SubexponentialIntegerEncoding;
import htsjdk.samtools.cram.encoding.huffman.HuffmanCode;
import htsjdk.samtools.cram.encoding.huffman.HuffmanTree;
import htsjdk.samtools.cram.encoding.huffman.codec.HuffmanByteEncoding;
import htsjdk.samtools.cram.encoding.huffman.codec.HuffmanIntegerEncoding;
import htsjdk.samtools.cram.encoding.rans.RANS;
import htsjdk.samtools.cram.encoding.readfeatures.Deletion;
import htsjdk.samtools.cram.encoding.readfeatures.HardClip;
import htsjdk.samtools.cram.encoding.readfeatures.Padding;
import htsjdk.samtools.cram.encoding.readfeatures.ReadFeature;
import htsjdk.samtools.cram.encoding.readfeatures.RefSkip;
import htsjdk.samtools.cram.encoding.readfeatures.Substitution;
import htsjdk.samtools.cram.structure.CompressionHeader;
import htsjdk.samtools.cram.structure.CramCompressionRecord;
import htsjdk.samtools.cram.structure.EncodingKey;
import htsjdk.samtools.cram.structure.EncodingParams;
import htsjdk.samtools.cram.structure.ReadTag;
import htsjdk.samtools.cram.structure.SubstitutionMatrix;
import htsjdk.samtools.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CompressionHeaderFactory {
    private static final Charset charset = Charset.forName("US-ASCII");
    private static final Log log = Log.getInstance(CompressionHeaderFactory.class);
    private static final int oqz = ReadTag.nameType3BytesToInt("OQ", 'Z');
    private static final int bqz = ReadTag.nameType3BytesToInt("BQ", 'Z');

    public CompressionHeader build(final List<CramCompressionRecord> records, final SubstitutionMatrix substitutionMatrix, final boolean sorted) {
        final CompressionHeader header = new CompressionHeader();
        header.externalIds = new ArrayList<Integer>();
        int exCounter = 0;

        final int baseID = exCounter++;
        header.externalIds.add(baseID);
        header.externalCompressors.put(baseID,
                ExternalCompressor.createRANS(RANS.ORDER.ONE));

        final int qualityScoreID = exCounter++;
        header.externalIds.add(qualityScoreID);
        header.externalCompressors.put(qualityScoreID,
                ExternalCompressor.createRANS(RANS.ORDER.ONE));

        final int readNameID = exCounter++;
        header.externalIds.add(readNameID);
        header.externalCompressors.put(readNameID, ExternalCompressor.createGZIP());

        final int mateInfoID = exCounter++;
        header.externalIds.add(mateInfoID);
        header.externalCompressors.put(mateInfoID,
                ExternalCompressor.createRANS(RANS.ORDER.ONE));

        header.encodingMap = new TreeMap<EncodingKey, EncodingParams>();
        for (final EncodingKey key : EncodingKey.values())
            header.encodingMap.put(key, NullEncoding.toParam());

        header.tMap = new TreeMap<Integer, EncodingParams>();

        { // bit flags encoding:
            getOptimalIntegerEncoding(header, EncodingKey.BF_BitFlags, 0, records);
        }

        { // compression bit flags encoding:
            getOptimalIntegerEncoding(header, EncodingKey.CF_CompressionBitFlags, 0, records);
        }

        { // ref id:

            getOptimalIntegerEncoding(header, EncodingKey.RI_RefId, -2, records);
        }

        { // read length encoding:
            getOptimalIntegerEncoding(header, EncodingKey.RL_ReadLength, 0, records);
        }

        { // alignment offset:
            if (sorted) { // alignment offset:
                header.APDelta = true;
                getOptimalIntegerEncoding(header, EncodingKey.AP_AlignmentPositionOffset, 0, records);
            } else {
                final int aStartID = exCounter++;
                header.APDelta = false;
                header.encodingMap.put(EncodingKey.AP_AlignmentPositionOffset,
                        ExternalIntegerEncoding.toParam(aStartID));
                header.externalIds.add(aStartID);
                header.externalCompressors.put(aStartID,
                        ExternalCompressor.createRANS(RANS.ORDER.ONE));
                log.debug("Assigned external id to alignment starts: " + aStartID);
            }
        }

        { // read group
            getOptimalIntegerEncoding(header, EncodingKey.RG_ReadGroup, -1, records);
        }

        { // read name encoding:
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                calculator.add(record.readName.length());
            calculator.calculate();

            header.encodingMap.put(EncodingKey.RN_ReadName, ByteArrayLenEncoding.toParam(
                    HuffmanIntegerEncoding.toParam(calculator.values(),
                            calculator.bitLens()), ExternalByteArrayEncoding
                            .toParam(readNameID)));
        }

        { // records to next fragment
            final IntegerEncodingCalculator calc = new IntegerEncodingCalculator(
                    EncodingKey.NF_RecordsToNextFragment.name(), 0);
            for (final CramCompressionRecord r : records) {
                if (r.isHasMateDownStream())
                    calc.addValue(r.recordsToNextFragment);
            }

            final Encoding<Integer> bestEncoding = calc.getBestEncoding();
            header.encodingMap.put(
                    EncodingKey.NF_RecordsToNextFragment,
                    new EncodingParams(bestEncoding.id(), bestEncoding
                            .toByteArray()));
        }

        { // tag count
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                calculator.add(record.tags == null ? 0 : record.tags.length);
            calculator.calculate();

            header.encodingMap.put(EncodingKey.TC_TagCount, HuffmanIntegerEncoding.toParam(
                    calculator.values(), calculator.bitLens()));
        }

        { // tag name and type
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records) {
                if (record.tags == null)
                    continue;
                for (final ReadTag tag : record.tags)
                    calculator.add(tag.keyType3BytesAsInt);
            }
            calculator.calculate();

            header.encodingMap.put(EncodingKey.TN_TagNameAndType, HuffmanIntegerEncoding
                    .toParam(calculator.values(), calculator.bitLens()));
        }

        {

            final Comparator<ReadTag> comparator = new Comparator<ReadTag>() {

                @Override
                public int compare(final ReadTag o1, final ReadTag o2) {
                    return o1.keyType3BytesAsInt - o2.keyType3BytesAsInt;
                }
            };

            final Comparator<byte[]> baComparator = new Comparator<byte[]>() {

                @Override
                public int compare(final byte[] o1, final byte[] o2) {
                    if (o1.length - o2.length != 0)
                        return o1.length - o2.length;

                    for (int i = 0; i < o1.length; i++)
                        if (o1[i] != o2[i])
                            return o1[i] - o2[i];

                    return 0;
                }
            };

            final Map<byte[], MutableInt> map = new TreeMap<byte[], MutableInt>(baComparator);
            final MutableInt noTagCounter = new MutableInt();
            map.put(new byte[0], noTagCounter);
            for (final CramCompressionRecord record : records) {
                if (record.tags == null) {
                    noTagCounter.value++;
                    record.tagIdsIndex = noTagCounter;
                    continue;
                }

                Arrays.sort(record.tags, comparator);
                record.tagIds = new byte[record.tags.length * 3];

                int tagIndex = 0;
                for (int i = 0; i < record.tags.length; i++) {
                    record.tagIds[i * 3] = (byte) record.tags[tagIndex].keyType3Bytes.charAt(0);
                    record.tagIds[i * 3 + 1] = (byte) record.tags[tagIndex].keyType3Bytes.charAt(1);
                    record.tagIds[i * 3 + 2] = (byte) record.tags[tagIndex].keyType3Bytes.charAt(2);
                    tagIndex++;
                }

                MutableInt count = map.get(record.tagIds);
                if (count == null) {
                    count = new MutableInt();
                    map.put(record.tagIds, count);
                }
                count.value++;
                record.tagIdsIndex = count;
            }

            final byte[][][] dic = new byte[map.size()][][];
            int i = 0;
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final byte[] idsAsBytes : map.keySet()) {
                final int nofIds = idsAsBytes.length / 3;
                dic[i] = new byte[nofIds][];
                for (int j = 0; j < idsAsBytes.length; ) {
                    final int idIndex = j / 3;
                    dic[i][idIndex] = new byte[3];
                    dic[i][idIndex][0] = idsAsBytes[j++];
                    dic[i][idIndex][1] = idsAsBytes[j++];
                    dic[i][idIndex][2] = idsAsBytes[j++];
                }
                calculator.add(i, map.get(idsAsBytes).value);
                map.get(idsAsBytes).value = i++;
            }

            calculator.calculate();
            header.encodingMap.put(EncodingKey.TL_TagIdList,
                    HuffmanIntegerEncoding.toParam(calculator.values(), calculator.bitLens()));
            header.dictionary = dic;
        }

        { // tag values
            @SuppressWarnings("UnnecessaryLocalVariable") final int unsortedTagValueExternalID = exCounter;
            header.externalIds.add(unsortedTagValueExternalID);
            header.externalCompressors.put(unsortedTagValueExternalID,
                    ExternalCompressor.createRANS(RANS.ORDER.ONE));

            final Set<Integer> tagIdSet = new HashSet<Integer>();
            for (final CramCompressionRecord record : records) {
                if (record.tags == null)
                    continue;

                for (final ReadTag tag : record.tags)
                    tagIdSet.add(tag.keyType3BytesAsInt);
            }

            for (final int id : tagIdSet) {
                final int externalID;
                final byte type = (byte) (id & 0xFF);
                switch (type) {
                    case 'Z':
                    case 'B':
                        externalID = id;
                        break;

                    default:
                        externalID = unsortedTagValueExternalID;
                        break;
                }

                header.externalIds.add(externalID);
                header.externalCompressors.put(externalID,
                        ExternalCompressor.createRANS(RANS.ORDER.ONE));
                header.tMap.put(id, ByteArrayLenEncoding.toParam(
                        ExternalIntegerEncoding.toParam(externalID),
                        ExternalByteEncoding.toParam(externalID)));
            }
        }

        { // number of read features
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord r : records)
                calculator.add(r.readFeatures == null ? 0 : r.readFeatures.size());
            calculator.calculate();

            header.encodingMap.put(EncodingKey.FN_NumberOfReadFeatures,
                    HuffmanIntegerEncoding.toParam(calculator.values(), calculator.bitLens()));
        }

        { // feature position
            final IntegerEncodingCalculator calc = new IntegerEncodingCalculator("read feature position", 0);
            for (final CramCompressionRecord record : records) {
                int prevPos = 0;
                if (record.readFeatures == null)
                    continue;
                for (final ReadFeature rf : record.readFeatures) {
                    calc.addValue(rf.getPosition() - prevPos);
                    prevPos = rf.getPosition();
                }
            }

            final Encoding<Integer> bestEncoding = calc.getBestEncoding();
            header.encodingMap.put(EncodingKey.FP_FeaturePosition,
                    new EncodingParams(bestEncoding.id(), bestEncoding.toByteArray()));
        }

        { // feature code
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                if (record.readFeatures != null)
                    for (final ReadFeature readFeature : record.readFeatures)
                        calculator.add(readFeature.getOperator());
            calculator.calculate();

            header.encodingMap.put(EncodingKey.FC_FeatureCode, HuffmanByteEncoding.toParam(
                    calculator.valuesAsBytes(), calculator.bitLens));
        }

        { // bases:
            header.encodingMap.put(EncodingKey.BA_Base, ExternalByteEncoding.toParam(baseID));
        }

        { // quality scores:
            header.encodingMap.put(EncodingKey.QS_QualityScore, ExternalByteEncoding.toParam(qualityScoreID));
        }

        { // base substitution code
            if (substitutionMatrix == null) {
                final long[][] frequencies = new long[200][200];
                for (final CramCompressionRecord record : records) {
                    if (record.readFeatures != null)
                        for (final ReadFeature readFeature : record.readFeatures)
                            if (readFeature.getOperator() == Substitution.operator) {
                                final Substitution substitution = ((Substitution) readFeature);
                                final byte refBase = substitution.getReferenceBase();
                                final byte base = substitution.getBase();
                                frequencies[refBase][base]++;
                            }
                }

                header.substitutionMatrix = new SubstitutionMatrix(frequencies);
            } else
                header.substitutionMatrix = substitutionMatrix;

            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                if (record.readFeatures != null)
                    for (final ReadFeature recordFeature : record.readFeatures) {
                        if (recordFeature.getOperator() == Substitution.operator) {
                            final Substitution substitution = ((Substitution) recordFeature);
                            if (substitution.getCode() == -1) {
                                final byte refBase = substitution.getReferenceBase();
                                final byte base = substitution.getBase();
                                substitution.setCode(header.substitutionMatrix.code(refBase, base));
                            }
                            calculator.add(substitution.getCode());
                        }
                    }
            calculator.calculate();

            header.encodingMap.put(EncodingKey.BS_BaseSubstitutionCode,
                    HuffmanIntegerEncoding.toParam(calculator.values, calculator.bitLens));
        }

        { // insertion bases
            header.encodingMap.put(EncodingKey.IN_Insertion, ByteArrayStopEncoding.toParam((byte) 0, baseID));
        }

        { // insertion bases
            header.encodingMap.put(EncodingKey.SC_SoftClip, ByteArrayStopEncoding.toParam((byte) 0, baseID));
        }

        { // deletion length
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                if (record.readFeatures != null)
                    for (final ReadFeature recordFeature : record.readFeatures)
                        if (recordFeature.getOperator() == Deletion.operator)
                            calculator.add(((Deletion) recordFeature).getLength());
            calculator.calculate();

            header.encodingMap.put(EncodingKey.DL_DeletionLength,
                    HuffmanIntegerEncoding.toParam(calculator.values, calculator.bitLens));
        }

        { // hard clip length
            final IntegerEncodingCalculator calculator = new IntegerEncodingCalculator(EncodingKey.HC_HardClip.name(), 0);
            for (final CramCompressionRecord record : records)
                if (record.readFeatures != null)
                    for (final ReadFeature recordFeature : record.readFeatures)
                        if (recordFeature.getOperator() == HardClip.operator)
                            calculator.addValue(((HardClip) recordFeature).getLength());

            final Encoding<Integer> bestEncoding = calculator.getBestEncoding();
            header.encodingMap.put(EncodingKey.HC_HardClip, new EncodingParams(bestEncoding.id(), bestEncoding.toByteArray()));
        }

        { // padding length
            final IntegerEncodingCalculator calculator = new IntegerEncodingCalculator(EncodingKey.PD_padding.name(), 0);
            for (final CramCompressionRecord record : records)
                if (record.readFeatures != null)
                    for (final ReadFeature recordFeature : record.readFeatures)
                        if (recordFeature.getOperator() == Padding.operator)
                            calculator.addValue(((Padding) recordFeature).getLength());

            final Encoding<Integer> bestEncoding = calculator.getBestEncoding();
            header.encodingMap.put(EncodingKey.PD_padding, new EncodingParams(bestEncoding.id(), bestEncoding.toByteArray()));

        }

        { // ref skip length
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                if (record.readFeatures != null)
                    for (final ReadFeature recordFeature : record.readFeatures)
                        if (recordFeature.getOperator() == RefSkip.operator)
                            calculator.add(((RefSkip) recordFeature).getLength());
            calculator.calculate();

            header.encodingMap.put(EncodingKey.RS_RefSkip, HuffmanIntegerEncoding.toParam(calculator.values, calculator.bitLens));
        }

        { // mapping quality score
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                if (!record.isSegmentUnmapped())
                    calculator.add(record.mappingQuality);
            calculator.calculate();

            header.encodingMap.put(EncodingKey.MQ_MappingQualityScore,
                    HuffmanIntegerEncoding.toParam(calculator.values(), calculator.bitLens));
        }

        { // mate bit flags
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                calculator.add(record.getMateFlags());
            calculator.calculate();

            header.encodingMap.put(EncodingKey.MF_MateBitFlags,
                    HuffmanIntegerEncoding.toParam(calculator.values, calculator.bitLens));
        }

        { // next fragment ref id:
            final HuffmanParamsCalculator calculator = new HuffmanParamsCalculator();
            for (final CramCompressionRecord record : records)
                if (record.isDetached())
                    calculator.add(record.mateSequenceID);
            calculator.calculate();

            if (calculator.values.length == 0)
                header.encodingMap.put(EncodingKey.NS_NextFragmentReferenceSequenceID, NullEncoding.toParam());

            header.encodingMap.put(EncodingKey.NS_NextFragmentReferenceSequenceID,
                    HuffmanIntegerEncoding.toParam(calculator.values(),
                            calculator.bitLens()));
            log.debug("NS: "
                    + header.encodingMap.get(EncodingKey.NS_NextFragmentReferenceSequenceID));
        }

        { // next fragment alignment start
            header.encodingMap.put(EncodingKey.NP_NextFragmentAlignmentStart, ExternalIntegerEncoding.toParam(mateInfoID));
        }

        { // template size
            header.encodingMap.put(EncodingKey.TS_InsetSize, ExternalIntegerEncoding.toParam(mateInfoID));
        }

        return header;
    }

    private static int getValue(final EncodingKey key, final CramCompressionRecord record) {
        switch (key) {
            case AP_AlignmentPositionOffset:
                return record.alignmentDelta;
            case BF_BitFlags:
                return record.flags;
            case CF_CompressionBitFlags:
                return record.compressionFlags;
            case FN_NumberOfReadFeatures:
                return record.readFeatures == null ? 0 : record.readFeatures.size();
            case MF_MateBitFlags:
                return record.mateFlags;
            case MQ_MappingQualityScore:
                return record.mappingQuality;
            case NF_RecordsToNextFragment:
                return record.recordsToNextFragment;
            case NP_NextFragmentAlignmentStart:
                return record.mateAlignmentStart;
            case NS_NextFragmentReferenceSequenceID:
                return record.mateSequenceID;
            case RG_ReadGroup:
                return record.readGroupID;
            case RI_RefId:
                return record.sequenceId;
            case RL_ReadLength:
                return record.readLength;
            case TC_TagCount:
                return record.tags == null ? 0 : record.tags.length;

            default:
                throw new RuntimeException("Unexpected encoding key: " + key.name());
        }
    }

    private static void getOptimalIntegerEncoding(final CompressionHeader header, final EncodingKey key, final int minValue,
                                                  final List<CramCompressionRecord> records) {
        final IntegerEncodingCalculator calc = new IntegerEncodingCalculator(key.name(), minValue);
        for (final CramCompressionRecord record : records) {
            final int value = getValue(key, record);
            calc.addValue(value);
        }

        final Encoding<Integer> bestEncoding = calc.getBestEncoding();
        header.encodingMap.put(key, new EncodingParams(bestEncoding.id(), bestEncoding.toByteArray()));
    }

    private static class BitCode implements Comparable<BitCode> {
        final int value;
        final int length;

        public BitCode(final int value, final int length) {
            this.value = value;
            this.length = length;
        }

        @Override
        public int compareTo(@SuppressWarnings("NullableProblems") final BitCode o) {
            final int result = value - o.value;
            if (result != 0)
                return result;
            return length - o.length;
        }
    }

    public static class HuffmanParamsCalculator {
        private final HashMap<Integer, MutableInt> countMap = new HashMap<Integer, MutableInt>();
        private int[] values = new int[]{};
        private int[] bitLens = new int[]{};

        public void add(final int value) {
            MutableInt counter = countMap.get(value);
            if (counter == null) {
                counter = new MutableInt();
                countMap.put(value, counter);
            }
            counter.value++;
        }

        public void add(final Integer value, final int inc) {
            MutableInt counter = countMap.get(value);
            if (counter == null) {
                counter = new MutableInt();
                countMap.put(value, counter);
            }
            counter.value += inc;
        }

        public int[] bitLens() {
            return bitLens;
        }

        public int[] values() {
            return values;
        }

        public Integer[] valuesAsAutoIntegers() {
            final Integer[] intValues = new Integer[values.length];
            for (int i = 0; i < intValues.length; i++)
                intValues[i] = values[i];

            return intValues;
        }

        public byte[] valuesAsBytes() {
            final byte[] byteValues = new byte[values.length];
            for (int i = 0; i < byteValues.length; i++)
                byteValues[i] = (byte) (0xFF & values[i]);

            return byteValues;
        }

        public Byte[] valuesAsAutoBytes() {
            final Byte[] byteValues = new Byte[values.length];
            for (int i = 0; i < byteValues.length; i++)
                byteValues[i] = (byte) (0xFF & values[i]);

            return byteValues;
        }

        public void calculate() {
            final HuffmanTree<Integer> tree;
            {
                final int size = countMap.size();
                final int[] frequencies = new int[size];
                final int[] values = new int[size];

                int i = 0;
                for (final Integer key : countMap.keySet()) {
                    values[i] = key;
                    frequencies[i] = countMap.get(key).value;
                    i++;
                }
                tree = HuffmanCode.buildTree(frequencies, autobox(values));
            }

            final List<Integer> valueList = new ArrayList<Integer>();
            final List<Integer> lens = new ArrayList<Integer>();
            HuffmanCode.getValuesAndBitLengths(valueList, lens, tree);

            // the following sorting is not really required, but whatever:
            final BitCode[] codes = new BitCode[valueList.size()];
            for (int i = 0; i < valueList.size(); i++) {
                codes[i] = new BitCode(valueList.get(i), lens.get(i));
            }
            Arrays.sort(codes);

            values = new int[codes.length];
            bitLens = new int[codes.length];

            for (int i = 0; i < codes.length; i++) {
                final BitCode code = codes[i];
                bitLens[i] = code.length;
                values[i] = code.value;
            }
        }
    }

    private static Integer[] autobox(final int[] array) {
        final Integer[] newArray = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
            newArray[i] = array[i];
        return newArray;
    }

    public static class EncodingLengthCalculator {
        private final BitCodec<Integer> codec;
        private final Encoding<Integer> encoding;
        private long length;

        public EncodingLengthCalculator(final Encoding<Integer> encoding) {
            this.encoding = encoding;
            codec = encoding.buildCodec(null, null);
        }

        public void add(final int value) {
            length += codec.numberOfBits(value);
        }

        public void add(final int value, final int inc) {
            length += inc * codec.numberOfBits(value);
        }

        public long length() {
            return length;
        }
    }

    public static class IntegerEncodingCalculator {
        public final List<EncodingLengthCalculator> calculators = new ArrayList<EncodingLengthCalculator>();
        private int max = 0;
        private int count = 0;
        private final String name;
        private HashMap<Integer, MutableInt> dictionary = new HashMap<Integer, MutableInt>();
        private final int dictionaryThreshold = 100;
        private final int minValue;

        public IntegerEncodingCalculator(final String name, final int dictionaryThreshold, final int minValue) {
            this.name = name;
            this.minValue = minValue;
            // for (int i = 2; i < 10; i++)
            // calculators.add(new EncodingLengthCalculator(
            // new GolombIntegerEncoding(i)));
            //
            // for (int i = 2; i < 20; i++)
            // calculators.add(new EncodingLengthCalculator(
            // new GolombRiceIntegerEncoding(i)));

            calculators.add(new EncodingLengthCalculator(new GammaIntegerEncoding(1 - minValue)));

            for (int i = 2; i < 5; i++)
                calculators.add(new EncodingLengthCalculator(new SubexponentialIntegerEncoding(0 - minValue, i)));

            if (dictionaryThreshold < 1)
                dictionary = null;
            else {
                dictionary = new HashMap<Integer, MutableInt>();
                // int pow = (int) Math.ceil(Math.log(dictionaryThreshold)
                // / Math.log(2f));
                // dictionaryThreshold = 1 << pow ;
                // dictionary = new HashMap<Integer,
                // MutableInt>(dictionaryThreshold, 1);
            }
        }

        public IntegerEncodingCalculator(final String name, final int minValue) {
            this(name, 255, minValue);
        }

        public void addValue(final int value) {
            count++;
            if (value > max)
                max = value;

            for (final EncodingLengthCalculator calculator : calculators)
                calculator.add(value);

            if (dictionary != null) {
                if (dictionary.size() >= dictionaryThreshold - 1)
                    dictionary = null;
                else {
                    MutableInt mutableInt = dictionary.get(value);
                    if (mutableInt == null) {
                        mutableInt = new MutableInt();
                        dictionary.put(value, mutableInt);
                    }
                    mutableInt.value++;
                }

            }

        }

        public Encoding<Integer> getBestEncoding() {
            if (dictionary != null && dictionary.size() == 1) {
                final int value = dictionary.keySet().iterator().next();
                final EncodingParams param = HuffmanIntegerEncoding.toParam(new int[]{value}, new int[]{0});
                final HuffmanIntegerEncoding huffmanEncoding = new HuffmanIntegerEncoding();
                huffmanEncoding.fromByteArray(param.params);
                return huffmanEncoding;
            }

            EncodingLengthCalculator bestCalculator = calculators.get(0);

            for (final EncodingLengthCalculator calculator : calculators) {
                if (calculator.length() < bestCalculator.length())
                    bestCalculator = calculator;
            }

            Encoding<Integer> bestEncoding = bestCalculator.encoding;
            long bits = bestCalculator.length();

            { // check if beta is better:

                final int betaLength = (int) Math.round(Math.log(max - minValue) / Math.log(2) + 0.5);
                if (bits > betaLength * count) {
                    bestEncoding = new BetaIntegerEncoding(-minValue, betaLength);
                    bits = betaLength * count;
                }
            }

            { // try huffman:
                if (dictionary != null) {
                    final HuffmanParamsCalculator huffmanParamsCalculator = new HuffmanParamsCalculator();
                    for (final Integer value : dictionary.keySet())
                        huffmanParamsCalculator.add(value, dictionary.get(value).value);

                    huffmanParamsCalculator.calculate();

                    final EncodingParams param = HuffmanIntegerEncoding.toParam(huffmanParamsCalculator.values(), huffmanParamsCalculator.bitLens());
                    final HuffmanIntegerEncoding huffmanEncoding = new HuffmanIntegerEncoding();
                    huffmanEncoding.fromByteArray(param.params);
                    final EncodingLengthCalculator calculator = new EncodingLengthCalculator(huffmanEncoding);
                    for (final Integer key : dictionary.keySet())
                        calculator.add(key, dictionary.get(key).value);

                    if (calculator.length() < bits) {
                        bestEncoding = huffmanEncoding;
                        bits = calculator.length();
                    }
                }
            }

            byte[] params = bestEncoding.toByteArray();
            params = Arrays.copyOf(params, Math.min(params.length, 20));
            log.debug("Best encoding for " + name + ": " + bestEncoding.id().name() + Arrays.toString(params) + ", bits=" + bits);

            return bestEncoding;
        }
    }
}
