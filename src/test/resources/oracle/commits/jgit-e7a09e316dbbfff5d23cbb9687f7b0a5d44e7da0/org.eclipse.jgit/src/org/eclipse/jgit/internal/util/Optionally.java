/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.util;

import java.lang.ref.SoftReference;
import java.util.Optional;

/**
 * Interface representing a reference to a potentially mutable optional object.
 *
 * @param <T>
 *            type of the mutable optional object
 *
 * @since 6.7
 */
public interface Optionally<T> {
	/**
	 * A permanently empty Optionally
	 *
	 * @param <T>
	 *            type of the mutable optional object
	 *
	 */
	public class Empty<T> implements Optionally<T> {
		@Override
		public void clear() {
			// empty
		}

		@Override
		public Optional<T> getOptional() {
			return Optional.empty();
		}
	}

	/**
	 * A permanent(hard) reference to an object
	 *
	 * @param <T>
	 *            type of the mutable optional object
	 *
	 */
	public class Hard<T> implements Optionally<T> {
		/**
		 * The mutable optional object
		 */
		protected T element;

		/**
		 * @param element
		 *            the mutable optional object
		 */
		public Hard(T element) {
			this.element = element;
		}

		@Override
		public void clear() {
			element = null;
		}

		@Override
		public Optional<T> getOptional() {
			return Optional.ofNullable(element);
		}
	}

	/**
	 * A SoftReference Optionally
	 *
	 * @param <T>
	 *            type of the mutable optional object
	 *
	 */
	public class Soft<T> extends SoftReference<T> implements Optionally<T> {
		/**
		 * @param t
		 *            the mutable optional object
		 */
		public Soft(T t) {
			super(t);
		}

		@Override
		public Optional<T> getOptional() {
			return Optional.ofNullable(get());
		}
	}

	/**
	 * The empty Optionally
	 */
	public static final Optionally<?> EMPTY = new Empty<>();

	/**
	 * @param <T>
	 *            type of the empty Optionally
	 * @return the empty Optionally
	 */
	@SuppressWarnings("unchecked")
	public static <T> Optionally<T> empty() {
		return (Optionally<T>) EMPTY;
	}

	/**
	 * Clear the object
	 *
	 */
	void clear();

	/**
	 * Get an Optional representing the current state of the object
	 *
	 * @return the mutable optional object
	 */
	Optional<T> getOptional();
}
