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
package htsjdk.samtools.cram.encoding.huffman.codec;

import htsjdk.samtools.cram.encoding.AbstractBitCodec;
import htsjdk.samtools.cram.io.BitInputStream;
import htsjdk.samtools.cram.io.BitOutputStream;

import java.io.IOException;


class CanonicalHuffmanIntegerCodec extends AbstractBitCodec<Integer> {
    private final HuffmanIntHelper helper;

    /*
     * values[]: the alphabet (provided as Integers) bitLengths[]: the number of
     * bits of symbol's huffman code
     */
    public CanonicalHuffmanIntegerCodec(final int[] values, final int[] bitLengths) {
        helper = new HuffmanIntHelper(values, bitLengths);
    }

    @Override
    public Integer read(final BitInputStream bitInputStream) throws IOException {
        return helper.read(bitInputStream);
    }

    @Override
    public long write(final BitOutputStream bitOutputStream, final Integer object) throws IOException {
        return helper.write(bitOutputStream, object);
    }

    @Override
    public long numberOfBits(final Integer object) {
        final HuffmanBitCode bitCode;
        bitCode = helper.codes.get(object);
        return bitCode.bitLength;
    }

    @Override
    public Integer read(final BitInputStream bitInputStream, final int length) throws IOException {
        throw new RuntimeException("Not implemented");
    }
}
