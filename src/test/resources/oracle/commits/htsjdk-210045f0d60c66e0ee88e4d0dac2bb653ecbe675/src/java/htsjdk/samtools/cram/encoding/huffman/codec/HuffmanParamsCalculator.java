package htsjdk.samtools.cram.encoding.huffman.codec;

import htsjdk.samtools.cram.common.MutableInt;
import htsjdk.samtools.cram.encoding.huffman.HuffmanCode;
import htsjdk.samtools.cram.encoding.huffman.HuffmanTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A utility class to calculate Huffman encoding parameters based on the values to be encoded.
 */
class HuffmanParamsCalculator {
    private final HashMap<Integer, MutableInt> countMap = new HashMap<>();
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
        for (int i = 0; i < intValues.length; i++) {
            intValues[i] = values[i];
        }

        return intValues;
    }

    public byte[] valuesAsBytes() {
        final byte[] byteValues = new byte[values.length];
        for (int i = 0; i < byteValues.length; i++) {
            byteValues[i] = (byte) (0xFF & values[i]);
        }

        return byteValues;
    }

    public Byte[] valuesAsAutoBytes() {
        final Byte[] byteValues = new Byte[values.length];
        for (int i = 0; i < byteValues.length; i++) {
            byteValues[i] = (byte) (0xFF & values[i]);
        }

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

    private static Integer[] autobox(final int[] array) {
        final Integer[] newArray = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = array[i];
        }
        return newArray;
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
            if (result != 0) {
                return result;
            }
            return length - o.length;
        }
    }

}
