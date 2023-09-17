/*
 * Copyright (C) 2011, Google Inc.
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

package org.eclipse.jgit.revwalk;

import java.io.IOException;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.storage.pack.PackWriter;

/**
 * Iterates through an open object list.
 * <p>
 * A cached object list should be constructed by enumerating from a single
 * stable commit back to the beginning of the project, using an ObjectWalk:
 *
 * <pre>
 * ObjectWalk walk = new ObjectWalk(repository);
 * walk.markStart(walk.parseCommit(listName));
 *
 * RevCommit commit;
 * while ((commit = walk.next()) != null)
 * 	list.addCommit(commit);
 *
 * RevObject object;
 * while ((object == walk.nextObject()) != null)
 * 	list.addObject(object, walk.getPathHasCode());
 * </pre>
 * <p>
 * {@link PackWriter} relies on the list covering a single commit, and going all
 * the way back to the root. If a list contains multiple starting commits the
 * PackWriter will include all of those objects, even if the client did not ask
 * for them, or should not have been given the objects.
 */
public abstract class ObjectListIterator {
	private final ObjectWalk walk;

	/**
	 * Initialize the list iterator.
	 *
	 * @param walk
	 *            the revision pool the iterator will use when allocating the
	 *            returned objects.
	 */
	protected ObjectListIterator(ObjectWalk walk) {
		this.walk = walk;
	}

	/**
	 * Lookup an object from the revision pool.
	 *
	 * @param id
	 *            the object to allocate.
	 * @param type
	 *            the type of the object. The type must be accurate, as it is
	 *            used to allocate the proper RevObject instance.
	 * @return the object.
	 */
	protected RevObject lookupAny(AnyObjectId id, int type) {
		return walk.lookupAny(id, type);
	}

	/**
	 * Pop the next most recent commit.
	 * <p>
	 * Commits should be returned in descending commit time order, or in
	 * topological order. Either ordering is acceptable for a list to use.
	 *
	 * @return next most recent commit; null if traversal is over.
	 * @throws IOException
	 *             the list cannot be read.
	 */
	public abstract RevCommit next() throws IOException;

	/**
	 * Pop the next most recent object.
	 * <p>
	 * Only RevTree and RevBlob may be returned from this method, as these are
	 * the only non-commit types reachable from a RevCommit. Lists may return
	 * the objects clustered by type, or clustered by order of first-discovery
	 * when walking from the most recent to the oldest commit.
	 *
	 * @return the next object. Null at the end of the list.
	 * @throws IOException
	 *             the list cannot be read.
	 */
	public abstract RevObject nextObject() throws IOException;

	/**
	 * Get the current object's path hash code.
	 * <p>
	 * The path hash code should be cached from the ObjectWalk.
	 *
	 * @return path hash code; any integer may be returned.
	 */
	public abstract int getPathHashCode();

	/** Release the resources associated with this iterator. */
	public abstract void release();
}
