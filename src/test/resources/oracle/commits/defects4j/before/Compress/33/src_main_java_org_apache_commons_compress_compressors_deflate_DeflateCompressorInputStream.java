/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.deflate;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;

/**
 * Deflate decompressor.
 * @since 1.9
 */
public class DeflateCompressorInputStream extends CompressorInputStream {
    
    private final InputStream in;

    /**
     * Creates a new input stream that decompresses Deflate-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     *
     */
    public DeflateCompressorInputStream(InputStream inputStream) {
        this(inputStream, new DeflateParameters());
    }

    /**
     * Creates a new input stream that decompresses Deflate-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     * @param       parameters parameters
     */
    public DeflateCompressorInputStream(InputStream inputStream,
                                        DeflateParameters parameters) {
        in = new InflaterInputStream(inputStream, new Inflater(!parameters.withZlibHeader()));
    }
    
    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        int ret = in.read();
        count(ret == -1 ? 0 : 1);
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int ret = in.read(buf, off, len);
        count(ret);
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        in.close();
    }
    
    /**
     * Checks if the signature matches what is expected for a zlib / deflated file
     *  with the zlib header.
     * 
     * @param signature
     *            the bytes to check
     * @param length
     *            the number of bytes to check
     * @return true, if this stream is zlib / deflate compressed with a header
     * stream, false otherwise
     * 
     * @since 1.9
     */
}
