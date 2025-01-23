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

	private boolean permanent;

	/**
	 * Construct a pack modification error.
	 *
	 * @param why
	 *            description of the type of error.
	 */
	public PackMismatchException(String why) {
		super(why);
	}

	/**
	 * Set the type of the exception
	 *
	 * @param permanent
	 *            whether the exception is considered permanent
	 * @since 5.9.1
	 */
	public void setPermanent(boolean permanent) {
		this.permanent = permanent;
	}

	/**
	 * Check if this is a permanent problem
	 *
	 * @return if this is a permanent problem and repeatedly scanning the
	 *         packlist couldn't fix it
	 * @since 5.9.1
	 */
	public boolean isPermanent() {
		return permanent;
	}

	@Override
	public String toString() {
		return super.toString() + ", permanent: " + permanent; //$NON-NLS-1$
	}
}
