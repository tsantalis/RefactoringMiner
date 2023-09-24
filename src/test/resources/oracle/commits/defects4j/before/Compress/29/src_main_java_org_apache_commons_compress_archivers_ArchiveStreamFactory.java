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
package org.apache.commons.compress.archivers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.dump.DumpArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Factory to create Archive[In|Out]putStreams from names or the first bytes of
 * the InputStream. In order to add other implementations, you should extend
 * ArchiveStreamFactory and override the appropriate methods (and call their
 * implementation from super of course).
 * 
 * Compressing a ZIP-File:
 * 
 * <pre>
 * final OutputStream out = new FileOutputStream(output); 
 * ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, out);
 * 
 * os.putArchiveEntry(new ZipArchiveEntry("testdata/test1.xml"));
 * IOUtils.copy(new FileInputStream(file1), os);
 * os.closeArchiveEntry();
 *
 * os.putArchiveEntry(new ZipArchiveEntry("testdata/test2.xml"));
 * IOUtils.copy(new FileInputStream(file2), os);
 * os.closeArchiveEntry();
 * os.close();
 * </pre>
 * 
 * Decompressing a ZIP-File:
 * 
 * <pre>
 * final InputStream is = new FileInputStream(input); 
 * ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, is);
 * ZipArchiveEntry entry = (ZipArchiveEntry)in.getNextEntry();
 * OutputStream out = new FileOutputStream(new File(dir, entry.getName()));
 * IOUtils.copy(in, out);
 * out.close();
 * in.close();
 * </pre>
 * @Immutable provided that the deprecated method setEntryEncoding is not used.
 */
public class ArchiveStreamFactory {

    /**
     * Constant (value {@value}) used to identify the AR archive format.
     * @since 1.1
     */
    public static final String AR = "ar";
    /**
     * Constant (value {@value}) used to identify the ARJ archive format.
     * Not supported as an output stream type.
     * @since 1.6
     */
    public static final String ARJ = "arj";
    /**
     * Constant (value {@value}) used to identify the CPIO archive format.
     * @since 1.1
     */
    public static final String CPIO = "cpio";
    /**
     * Constant (value {@value}) used to identify the Unix DUMP archive format.
     * Not supported as an output stream type.
     * @since 1.3
     */
    public static final String DUMP = "dump";
    /**
     * Constant (value {@value}) used to identify the JAR archive format.
     * @since 1.1
     */
    public static final String JAR = "jar";
    /**
     * Constant used to identify the TAR archive format.
     * @since 1.1
     */
    public static final String TAR = "tar";
    /**
     * Constant (value {@value}) used to identify the ZIP archive format.
     * @since 1.1
     */
    public static final String ZIP = "zip";
    /**
     * Constant (value {@value}) used to identify the 7z archive format.
     * @since 1.8
     */
    public static final String SEVEN_Z = "7z";

    /**
     * Entry encoding, null for the platform default.
     */
    private final String encoding;

    /**
     * Entry encoding, null for the default.
     */
    private volatile String entryEncoding = null;

    /**
     * Create an instance using the platform default encoding.
     */
    public ArchiveStreamFactory() {
        this(null);
    }

    /**
     * Create an instance using the specified encoding.
     *
     * @param encoding the encoding to be used.
     *
     * @since 1.10
     */
    public ArchiveStreamFactory(String encoding) {
        super();
        this.encoding = encoding;
        // Also set the original field so can continue to use it.
        this.entryEncoding = encoding;
    }

    /**
     * Returns the encoding to use for arj, jar, zip, dump, cpio and tar
     * files, or null for the archiver default.
     *
     * @return entry encoding, or null for the archiver default
     * @since 1.5
     */
    public String getEntryEncoding() {
        return entryEncoding;
    }

    /**
     * Sets the encoding to use for arj, jar, zip, dump, cpio and tar files. Use null for the archiver default.
     * 
     * @param entryEncoding the entry encoding, null uses the archiver default.
     * @since 1.5
     * @deprecated 1.10 use {@link #ArchiveStreamFactory(String)} to specify the encoding
     * @throws IllegalStateException if the constructor {@link #ArchiveStreamFactory(String)} 
     * was used to specify the factory encoding.
     */
    @Deprecated
    public void setEntryEncoding(String entryEncoding) {
        // Note: this does not detect new ArchiveStreamFactory(null) but that does not set the encoding anyway
        if (encoding != null) {
            throw new IllegalStateException("Cannot overide encoding set by the constructor");
        }
        this.entryEncoding = entryEncoding;
    }

    /**
     * Create an archive input stream from an archiver name and an input stream.
     * 
     * @param archiverName the archive name,
     * i.e. {@value #AR}, {@value #ARJ}, {@value #ZIP}, {@value #TAR}, {@value #JAR}, {@value #CPIO}, {@value #DUMP} or {@value #SEVEN_Z}
     * @param in the input stream
     * @return the archive input stream
     * @throws ArchiveException if the archiver name is not known
     * @throws StreamingNotSupportedException if the format cannot be
     * read from a stream
     * @throws IllegalArgumentException if the archiver name or stream is null
     */
    public ArchiveInputStream createArchiveInputStream(
            final String archiverName, final InputStream in)
            throws ArchiveException {

        if (archiverName == null) {
            throw new IllegalArgumentException("Archivername must not be null.");
        }

        if (in == null) {
            throw new IllegalArgumentException("InputStream must not be null.");
        }

        if (AR.equalsIgnoreCase(archiverName)) {
            return new ArArchiveInputStream(in);
        }
        if (ARJ.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new ArjArchiveInputStream(in, entryEncoding);
            } else {
                return new ArjArchiveInputStream(in);
            }
        }
        if (ZIP.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new ZipArchiveInputStream(in, entryEncoding);
            } else {
                return new ZipArchiveInputStream(in);
            }
        }
        if (TAR.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new TarArchiveInputStream(in, entryEncoding);
            } else {
                return new TarArchiveInputStream(in);
            }
        }
        if (JAR.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new JarArchiveInputStream(in, entryEncoding);
            } else {
                return new JarArchiveInputStream(in);
            }
        }
        if (CPIO.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new CpioArchiveInputStream(in, entryEncoding);
            } else {
                return new CpioArchiveInputStream(in);
            }
        }
        if (DUMP.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new DumpArchiveInputStream(in, entryEncoding);
            } else {
                return new DumpArchiveInputStream(in);
            }
        }
        if (SEVEN_Z.equalsIgnoreCase(archiverName)) {
            throw new StreamingNotSupportedException(SEVEN_Z);
        }

        throw new ArchiveException("Archiver: " + archiverName + " not found.");
    }

    /**
     * Create an archive output stream from an archiver name and an output stream.
     * 
     * @param archiverName the archive name,
     * i.e. {@value #AR}, {@value #ZIP}, {@value #TAR}, {@value #JAR} or {@value #CPIO} 
     * @param out the output stream
     * @return the archive output stream
     * @throws ArchiveException if the archiver name is not known
     * @throws StreamingNotSupportedException if the format cannot be
     * written to a stream
     * @throws IllegalArgumentException if the archiver name or stream is null
     */
    public ArchiveOutputStream createArchiveOutputStream(
            final String archiverName, final OutputStream out)
            throws ArchiveException {
        if (archiverName == null) {
            throw new IllegalArgumentException("Archivername must not be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("OutputStream must not be null.");
        }

        if (AR.equalsIgnoreCase(archiverName)) {
            return new ArArchiveOutputStream(out);
        }
        if (ZIP.equalsIgnoreCase(archiverName)) {
            ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out);
            if (entryEncoding != null) {
                zip.setEncoding(entryEncoding);
            }
            return zip;
        }
        if (TAR.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new TarArchiveOutputStream(out, entryEncoding);
            } else {
                return new TarArchiveOutputStream(out);
            }
        }
        if (JAR.equalsIgnoreCase(archiverName)) {
                return new JarArchiveOutputStream(out);
        }
        if (CPIO.equalsIgnoreCase(archiverName)) {
            if (entryEncoding != null) {
                return new CpioArchiveOutputStream(out, entryEncoding);
            } else {
                return new CpioArchiveOutputStream(out);
            }
        }
        if (SEVEN_Z.equalsIgnoreCase(archiverName)) {
            throw new StreamingNotSupportedException(SEVEN_Z);
        }
        throw new ArchiveException("Archiver: " + archiverName + " not found.");
    }

    /**
     * Create an archive input stream from an input stream, autodetecting
     * the archive type from the first few bytes of the stream. The InputStream
     * must support marks, like BufferedInputStream.
     * 
     * @param in the input stream
     * @return the archive input stream
     * @throws ArchiveException if the archiver name is not known
     * @throws StreamingNotSupportedException if the format cannot be
     * read from a stream
     * @throws IllegalArgumentException if the stream is null or does not support mark
     */
    public ArchiveInputStream createArchiveInputStream(final InputStream in)
            throws ArchiveException {
        if (in == null) {
            throw new IllegalArgumentException("Stream must not be null.");
        }

        if (!in.markSupported()) {
            throw new IllegalArgumentException("Mark is not supported.");
        }

        final byte[] signature = new byte[12];
        in.mark(signature.length);
        try {
            int signatureLength = IOUtils.readFully(in, signature);
            in.reset();
            if (ZipArchiveInputStream.matches(signature, signatureLength)) {
                if (entryEncoding != null) {
                    return new ZipArchiveInputStream(in, entryEncoding);
                } else {
                    return new ZipArchiveInputStream(in);
                }
            } else if (JarArchiveInputStream.matches(signature, signatureLength)) {
                if (entryEncoding != null) {
                    return new JarArchiveInputStream(in, entryEncoding);
                } else {
                    return new JarArchiveInputStream(in);
                }
            } else if (ArArchiveInputStream.matches(signature, signatureLength)) {
                return new ArArchiveInputStream(in);
            } else if (CpioArchiveInputStream.matches(signature, signatureLength)) {
                if (entryEncoding != null) {
                    return new CpioArchiveInputStream(in, entryEncoding);
                } else {
                    return new CpioArchiveInputStream(in);
                }
            } else if (ArjArchiveInputStream.matches(signature, signatureLength)) {
                    return new ArjArchiveInputStream(in);
            } else if (SevenZFile.matches(signature, signatureLength)) {
                throw new StreamingNotSupportedException(SEVEN_Z);
            }

            // Dump needs a bigger buffer to check the signature;
            final byte[] dumpsig = new byte[32];
            in.mark(dumpsig.length);
            signatureLength = IOUtils.readFully(in, dumpsig);
            in.reset();
            if (DumpArchiveInputStream.matches(dumpsig, signatureLength)) {
                return new DumpArchiveInputStream(in, entryEncoding);
            }

            // Tar needs an even bigger buffer to check the signature; read the first block
            final byte[] tarheader = new byte[512];
            in.mark(tarheader.length);
            signatureLength = IOUtils.readFully(in, tarheader);
            in.reset();
            if (TarArchiveInputStream.matches(tarheader, signatureLength)) {
                return new TarArchiveInputStream(in, entryEncoding);
            }
            // COMPRESS-117 - improve auto-recognition
            if (signatureLength >= 512) {
                TarArchiveInputStream tais = null;
                try {
                    tais = new TarArchiveInputStream(new ByteArrayInputStream(tarheader));
                    // COMPRESS-191 - verify the header checksum
                    if (tais.getNextTarEntry().isCheckSumOK()) {
                        return new TarArchiveInputStream(in, encoding);
                    }
                } catch (Exception e) { // NOPMD
                    // can generate IllegalArgumentException as well
                    // as IOException
                    // autodetection, simply not a TAR
                    // ignored
                } finally {
                    IOUtils.closeQuietly(tais);
                }
            }
        } catch (IOException e) {
            throw new ArchiveException("Could not use reset and mark operations.", e);
        }

        throw new ArchiveException("No Archiver found for the stream signature");
    }

}
