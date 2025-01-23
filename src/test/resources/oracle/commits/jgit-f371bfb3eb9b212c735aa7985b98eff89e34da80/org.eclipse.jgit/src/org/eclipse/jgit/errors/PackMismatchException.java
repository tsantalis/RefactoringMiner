/*
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.errors;

import java.io.IOException;

/**
 * Thrown when a PackFile no longer matches the PackIndex.
 */
public class PackMismatchException extends IOException {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a pack modification error.
	 *
	 * @param why
	 *            description of the type of error.
	 */
	public PackMismatchException(String why) {
		super(why);
	}
}
