/*
 * Copyright (C) 2023, Google LLC. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.Deflater;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.NB;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.eclipse.jgit.util.TemporaryBuffer.Heap;

/**
 * Helper class to create packs for tests.
 */
public class InMemoryPack {

  private final Heap tinyPack;

  public InMemoryPack() {
    this(1024);
  }

  public InMemoryPack(int size) {
    this.tinyPack = new TemporaryBuffer.Heap(size);
  }

  public InMemoryPack header(int cnt)
      throws IOException {
    final byte[] hdr = new byte[8];
    NB.encodeInt32(hdr, 0, 2);
    NB.encodeInt32(hdr, 4, cnt);

    tinyPack.write(Constants.PACK_SIGNATURE);
    tinyPack.write(hdr, 0, 8);
    return this;
  }

  public InMemoryPack write(int i) throws IOException {
    tinyPack.write(i);
    return this;
  }

  public InMemoryPack deflate(byte[] content)
      throws IOException {
    Deflater deflater = new Deflater();
    byte[] buf = new byte[128];
    deflater.setInput(content, 0, content.length);
    deflater.finish();
    do {
      final int n = deflater.deflate(buf, 0, buf.length);
      if (n > 0)
        tinyPack.write(buf, 0, n);
    } while (!deflater.finished());
    return this;
  }

  public InMemoryPack copyRaw(AnyObjectId o) throws IOException {
    o.copyRawTo(tinyPack);
    return this;
  }

  public InMemoryPack digest() throws IOException {
    MessageDigest md = Constants.newMessageDigest();
    md.update(tinyPack.toByteArray());
    tinyPack.write(md.digest());
    return this;
  }

  public InputStream toInputStream() throws IOException {
    return new ByteArrayInputStream(tinyPack.toByteArray());
  }

  public byte[] toByteArray() throws IOException {
    return tinyPack.toByteArray();
  }
}
