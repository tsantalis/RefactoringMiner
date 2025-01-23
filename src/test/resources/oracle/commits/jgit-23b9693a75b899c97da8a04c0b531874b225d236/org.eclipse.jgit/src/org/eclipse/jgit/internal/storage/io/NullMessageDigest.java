/*
 * Copyright (C) 2023, SAP SE and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.io;

import java.security.MessageDigest;

import org.eclipse.jgit.lib.Constants;

/**
 * Dummy message digest consisting of only null bytes with the length of an
 * ObjectId. This class can be used to skip computing a real digest.
 */
public final class NullMessageDigest extends MessageDigest {
	private static final byte[] digest = new byte[Constants.OBJECT_ID_LENGTH];

	private static final NullMessageDigest INSTANCE = new NullMessageDigest();

	/**
	 * Get the only instance of NullMessageDigest
	 *
	 * @return the only instance of NullMessageDigest
	 */
	public static MessageDigest getInstance() {
		return INSTANCE;
	}

	private NullMessageDigest() {
		super("null"); //$NON-NLS-1$
	}

	@Override
	protected void engineUpdate(byte input) {
		// empty
	}

	@Override
	protected void engineUpdate(byte[] input, int offset, int len) {
		// empty
	}

	@Override
	protected byte[] engineDigest() {
		return digest;
	}

	@Override
	protected void engineReset() {
		// empty
	}
}
