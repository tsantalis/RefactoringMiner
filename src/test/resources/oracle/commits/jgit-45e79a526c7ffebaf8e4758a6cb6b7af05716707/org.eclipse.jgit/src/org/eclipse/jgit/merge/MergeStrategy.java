/*
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2009, Matthias Sohn <matthias.sohn@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.merge;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.lib.Repository;

/**
 * A method of combining two or more trees together to form an output tree.
 * <p>
 * Different strategies may employ different techniques for deciding which paths
 * (and ObjectIds) to carry from the input trees into the final output tree.
 */
public abstract class MergeStrategy {
	/** Simple strategy that sets the output tree to the first input tree. */
	public static final MergeStrategy OURS = new StrategyOneSided("ours", 0);

	/** Simple strategy that sets the output tree to the second input tree. */
	public static final MergeStrategy THEIRS = new StrategyOneSided("theirs", 1);

	/** Simple strategy to merge paths, without simultaneous edits. */
	public static final ThreeWayMergeStrategy SIMPLE_TWO_WAY_IN_CORE = new StrategySimpleTwoWayInCore();

	/** Simple strategy to merge paths. It tries to merge also contents. Multiple merge bases are not supported */
	public static final ThreeWayMergeStrategy RESOLVE = new StrategyResolve();

	private static final HashMap<String, MergeStrategy> STRATEGIES = new HashMap<String, MergeStrategy>();

	static {
		register(OURS);
		register(THEIRS);
		register(SIMPLE_TWO_WAY_IN_CORE);
		register(RESOLVE);
	}

	/**
	 * Register a merge strategy so it can later be obtained by name.
	 *
	 * @param imp
	 *            the strategy to register.
	 * @throws IllegalArgumentException
	 *             a strategy by the same name has already been registered.
	 */
	public static void register(final MergeStrategy imp) {
		register(imp.getName(), imp);
	}

	/**
	 * Register a merge strategy so it can later be obtained by name.
	 *
	 * @param name
	 *            name the strategy can be looked up under.
	 * @param imp
	 *            the strategy to register.
	 * @throws IllegalArgumentException
	 *             a strategy by the same name has already been registered.
	 */
	public static synchronized void register(final String name,
			final MergeStrategy imp) {
		if (STRATEGIES.containsKey(name))
			throw new IllegalArgumentException(MessageFormat.format(JGitText.get().mergeStrategyAlreadyExistsAsDefault, name));
		STRATEGIES.put(name, imp);
	}

	/**
	 * Locate a strategy by name.
	 *
	 * @param name
	 *            name of the strategy to locate.
	 * @return the strategy instance; null if no strategy matches the name.
	 */
	public static synchronized MergeStrategy get(final String name) {
		return STRATEGIES.get(name);
	}

	/**
	 * Get all registered strategies.
	 *
	 * @return the registered strategy instances. No inherit order is returned;
	 *         the caller may modify (and/or sort) the returned array if
	 *         necessary to obtain a reasonable ordering.
	 */
	public static synchronized MergeStrategy[] get() {
		final MergeStrategy[] r = new MergeStrategy[STRATEGIES.size()];
		STRATEGIES.values().toArray(r);
		return r;
	}

	/** @return default name of this strategy implementation. */
	public abstract String getName();

	/**
	 * Create a new merge instance.
	 *
	 * @param db
	 *            repository database the merger will read from, and eventually
	 *            write results back to.
	 * @return the new merge instance which implements this strategy.
	 */
	public abstract Merger newMerger(Repository db);
}
