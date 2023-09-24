/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.codec.net;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringDecoder;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.binary.StringUtils;

/**
 * <p>
 * Codec for the Quoted-Printable section of <a href="http://www.ietf.org/rfc/rfc1521.txt">RFC 1521</a>.
 * </p>
 * <p>
 * The Quoted-Printable encoding is intended to represent data that largely consists of octets that correspond to
 * printable characters in the ASCII character set. It encodes the data in such a way that the resulting octets are
 * unlikely to be modified by mail transport. If the data being encoded are mostly ASCII text, the encoded form of the
 * data remains largely recognizable by humans. A body which is entirely ASCII may also be encoded in Quoted-Printable
 * to ensure the integrity of the data should the message pass through a character- translating, and/or line-wrapping
 * gateway.
 * </p>
 *
 * @see <a href="http://www.ietf.org/rfc/rfc1521.txt"> RFC 1521 MIME (Multipurpose Internet Mail Extensions) Part One:
 *          Mechanisms for Specifying and Describing the Format of Internet Message Bodies </a>
 *
 * @author Apache Software Foundation
 * @since 1.3
 * @version $Id$
 */
public class QuotedPrintableCodec implements BinaryEncoder, BinaryDecoder, StringEncoder, StringDecoder {
    /**
     * The default charset used for string decoding and encoding.
     */
    private final String charset;

    /**
     * BitSet of printable characters as defined in RFC 1521.
     */
    private static final BitSet PRINTABLE_CHARS = new BitSet(256);

    private static final byte ESCAPE_CHAR = '=';

    private static final byte TAB = 9;

    private static final byte SPACE = 32;



    /** Safe line length for quoted printable encoded text. */

    // Static initializer for printable chars collection
    static {
        // alpha characters
        for (int i = 33; i <= 60; i++) {
            PRINTABLE_CHARS.set(i);
        }
        for (int i = 62; i <= 126; i++) {
            PRINTABLE_CHARS.set(i);
        }
        PRINTABLE_CHARS.set(TAB);
        PRINTABLE_CHARS.set(SPACE);
    }

    /**
     * Default constructor.
     */
    public QuotedPrintableCodec() {
        this(CharEncoding.UTF_8);
    }

    /**
     * Constructor which allows for the selection of a default charset
     * 
     * @param charset
     *                  the default string charset to use.
     */
    public QuotedPrintableCodec(String charset) {
        super();
        this.charset = charset;
    }

    /**
     * Encodes byte into its quoted-printable representation.
     * 
     * @param b
     *            byte to encode
     * @param buffer
     *            the buffer to write to
     * @return The number of bytes written to the <code>buffer</code>
     */
    private static final void encodeQuotedPrintable(int b, ByteArrayOutputStream buffer) {
        buffer.write(ESCAPE_CHAR);
        char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
        char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
        buffer.write(hex1);
        buffer.write(hex2);
    }

    /**
     * Return the byte at position <code>index</code> of the byte array and
     * make sure it is unsigned.
     *
     * @param index
     *                  position in the array
     * @param bytes
     *                  the byte array
     * @return the unsigned octet at position <code>index</code> from the array
     */

    /**
     * Write a byte to the buffer.
     *
     * @param b
     *                  byte to write
     * @param encode
     *                  indicates whether the octet shall be encoded
     * @param buffer
     *                  the buffer to write to
     * @return the number of bytes that have been written to the buffer
     */

    /**
     * Checks whether the given byte is whitespace.
     *
     * @param b
     *                  byte to be checked
     * @return <code>true</code> if the byte is either a space or tab character
     */

    /**
     * Encodes an array of bytes into an array of quoted-printable 7-bit characters. Unsafe characters are escaped.
     *
     * <p>
     * This function fully implements the quoted-printable encoding specification (rule #1 through rule #5)
     * as defined in RFC 1521 and is suitable for encoding binary data and unformatted text.
     * </p>
     *
     * @param printable
     *                  bitset of characters deemed quoted-printable
     * @param bytes
     *                  array of bytes to be encoded
     * @return array of bytes containing quoted-printable data
     */
    public static final byte[] encodeQuotedPrintable(BitSet printable, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (printable == null) {
            printable = PRINTABLE_CHARS;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // encode up to buffer.length - 3, the last three octets will be treated
        // separately for simplification of note #3
                // up to this length it is safe to add any byte, encoded or not
        for (byte c : bytes) {
            int b = c;
            if (b < 0) {
                b = 256 + b;
            }
            if (printable.get(b)) {
                buffer.write(b);
            } else {
                // rule #3: whitespace at the end of a line *must* be encoded

                // rule #5: soft line break
                encodeQuotedPrintable(b, buffer);
            }
        }

        // rule #3: whitespace at the end of a line *must* be encoded
        // if we would do a soft break line after this octet, encode whitespace

        // note #3: '=' *must not* be the ultimate or penultimate character
        // simplification: if < 6 bytes left, do a soft line break as we may need
        //                 exactly 6 bytes space for the last 2 bytes
            // rule #3: trailing whitespace shall be encoded

        return buffer.toByteArray();
    }

    /**
     * Decodes an array quoted-printable characters into an array of original bytes. Escaped characters are
     * converted back to their original representation.
     *
     * <p>
     * This function fully implements the quoted-printable encoding specification (rule #1 through rule #5) as
     * defined in RFC 1521.
     * </p>
     *
     * @param bytes
     *                  array of quoted-printable characters
     * @return array of original bytes
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful
     */
    public static final byte[] decodeQuotedPrintable(byte[] bytes) throws DecoderException {
        if (bytes == null) {
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            final int b = bytes[i];
            if (b == ESCAPE_CHAR) {
                try {
                    // if the next octet is a CR we have found a soft line break
                    int u = Utils.digit16(bytes[++i]);
                    int l = Utils.digit16(bytes[++i]);
                    buffer.write((char) ((u << 4) + l));
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new DecoderException("Invalid quoted-printable encoding", e);
                }
            } else {
                // every other octet is appended except for CR & LF
                buffer.write(b);
            }
        }
        return buffer.toByteArray();
    }

    /**
     * Encodes an array of bytes into an array of quoted-printable 7-bit characters. Unsafe characters are escaped.
     * 
     * <p>
     * This function fully implements the quoted-printable encoding specification (rule #1 through rule #5)
     * as defined in RFC 1521 and is suitable for encoding binary data and unformatted text.
     * </p>
     * 
     * @param bytes
     *                  array of bytes to be encoded
     * @return array of bytes containing quoted-printable data
     */
    public byte[] encode(byte[] bytes) {
        return encodeQuotedPrintable(PRINTABLE_CHARS, bytes);
    }

    /**
     * Decodes an array of quoted-printable characters into an array of original bytes. Escaped characters are converted
     * back to their original representation.
     * 
     * <p>
     * This function fully implements the quoted-printable encoding specification (rule #1 through rule #2)
     * as defined in RFC 1521.
     * </p>
     * 
     * @param bytes
     *                  array of quoted-printable characters
     * @return array of original bytes
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful
     */
    public byte[] decode(byte[] bytes) throws DecoderException {
        return decodeQuotedPrintable(bytes);
    }

    /**
     * Encodes a string into its quoted-printable form using the default string charset. Unsafe characters are escaped.
     * 
     * <p>
     * This function fully implements the quoted-printable encoding specification (rule #1 through rule #2)
     * as defined in RFC 1521 and is suitable for encoding binary data.
     * </p>
     * 
     * @param pString
     *                  string to convert to quoted-printable form
     * @return quoted-printable string
     * 
     * @throws EncoderException
     *                  Thrown if quoted-printable encoding is unsuccessful
     * 
     * @see #getDefaultCharset()
     */
    public String encode(String pString) throws EncoderException {
        if (pString == null) {
            return null;
        }
        try {
            return encode(pString, getDefaultCharset());
        } catch (UnsupportedEncodingException e) {
            throw new EncoderException(e.getMessage(), e);
        }
    }

    /**
     * Decodes a quoted-printable string into its original form using the specified string charset. Escaped characters
     * are converted back to their original representation.
     * 
     * @param pString
     *                  quoted-printable string to convert into its original form
     * @param charset
     *                  the original string charset
     * @return original string
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful
     * @throws UnsupportedEncodingException
     *                  Thrown if charset is not supported
     */
    public String decode(String pString, String charset) throws DecoderException, UnsupportedEncodingException {
        if (pString == null) {
            return null;
        }
        return new String(decode(StringUtils.getBytesUsAscii(pString)), charset);
    }

    /**
     * Decodes a quoted-printable string into its original form using the default string charset. Escaped characters are
     * converted back to their original representation.
     * 
     * @param pString
     *                  quoted-printable string to convert into its original form
     * @return original string
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful.
     *                  Thrown if charset is not supported.
     * @see #getDefaultCharset()
     */
    public String decode(String pString) throws DecoderException {
        if (pString == null) {
            return null;
        }
        try {
            return decode(pString, getDefaultCharset());
        } catch (UnsupportedEncodingException e) {
            throw new DecoderException(e.getMessage(), e);
        }
    }

    /**
     * Encodes an object into its quoted-printable safe form. Unsafe characters are escaped.
     * 
     * @param pObject
     *                  string to convert to a quoted-printable form
     * @return quoted-printable object
     * @throws EncoderException
     *                  Thrown if quoted-printable encoding is not applicable to objects of this type or if encoding is
     *                  unsuccessful
     */
    public Object encode(Object pObject) throws EncoderException {
        if (pObject == null) {
            return null;
        } else if (pObject instanceof byte[]) {
            return encode((byte[]) pObject);
        } else if (pObject instanceof String) {
            return encode((String) pObject);
        } else {
            throw new EncoderException("Objects of type " + 
                  pObject.getClass().getName() + 
                  " cannot be quoted-printable encoded");
        }
    }

    /**
     * Decodes a quoted-printable object into its original form. Escaped characters are converted back to their original
     * representation.
     * 
     * @param pObject
     *                  quoted-printable object to convert into its original form
     * @return original object
     * @throws DecoderException
     *                  Thrown if the argument is not a <code>String</code> or <code>byte[]</code>. Thrown if a failure condition is
     *                  encountered during the decode process.
     */
    public Object decode(Object pObject) throws DecoderException {
        if (pObject == null) {
            return null;
        } else if (pObject instanceof byte[]) {
            return decode((byte[]) pObject);
        } else if (pObject instanceof String) {
            return decode((String) pObject);
        } else {
            throw new DecoderException("Objects of type " + 
                  pObject.getClass().getName() + 
                  " cannot be quoted-printable decoded");
        }
    }

    /**
     * Returns the default charset used for string decoding and encoding.
     * 
     * @return the default string charset.
     */
    public String getDefaultCharset() {
        return this.charset;
    }

    /**
     * Encodes a string into its quoted-printable form using the specified charset. Unsafe characters are escaped.
     * 
     * <p>
     * This function fully implements the quoted-printable encoding specification (rule #1 through rule #2)
     * as defined in RFC 1521 and is suitable for encoding binary data and unformatted text.
     * </p>
     * 
     * @param pString
     *                  string to convert to quoted-printable form
     * @param charset
     *                  the charset for pString
     * @return quoted-printable string
     * 
     * @throws UnsupportedEncodingException
     *                  Thrown if the charset is not supported
     */
    public String encode(String pString, String charset) throws UnsupportedEncodingException {
        if (pString == null) {
            return null;
        }
        return StringUtils.newStringUsAscii(encode(pString.getBytes(charset)));
    }
}
