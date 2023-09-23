/*
 * Copyright (C) 2010, Google Inc.
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

package org.eclipse.jgit.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.pack.ObjectReuseAsIs;

/**
 * Reads an {@link ObjectDatabase} for a single thread.
 * <p>
 * Readers that can support efficient reuse of pack encoded objects should also
 * implement the companion interface {@link ObjectReuseAsIs}.
 */
public abstract class ObjectReader {
	/** Type hint indicating the caller doesn't know the type. */
	public static final int OBJ_ANY = -1;

	/**
	 * Construct a new reader from the same data.
	 * <p>
	 * Applications can use this method to build a new reader from the same data
	 * source, but for an different thread.
	 *
	 * @return a brand new reader, using the same data source.
	 */
	public abstract ObjectReader newReader();

	/**
	 * Obtain a unique abbreviation (prefix) of an object SHA-1.
	 *
	 * This method uses a reasonable default for the minimum length. Callers who
	 * don't care about the minimum length should prefer this method.
	 *
	 * The returned abbreviation would expand back to the argument ObjectId when
	 * passed to {@link #resolve(AbbreviatedObjectId)}, assuming no new objects
	 * are added to this repository between calls.
	 *
	 * @param objectId
	 *            object identity that needs to be abbreviated.
	 * @return SHA-1 abbreviation.
	 * @throws IOException
	 *             the object store cannot be read.
	 */
	public AbbreviatedObjectId abbreviate(AnyObjectId objectId)
			throws IOException {
		return abbreviate(objectId, 7);
	}

	/**
	 * Obtain a unique abbreviation (prefix) of an object SHA-1.
	 *
	 * The returned abbreviation would expand back to the argument ObjectId when
	 * passed to {@link #resolve(AbbreviatedObjectId)}, assuming no new objects
	 * are added to this repository between calls.
	 *
	 * The default implementation of this method abbreviates the id to the
	 * minimum length, then resolves it to see if there are multiple results.
	 * When multiple results are found, the length is extended by 1 and resolve
	 * is tried again.
	 *
	 * @param objectId
	 *            object identity that needs to be abbreviated.
	 * @param len
	 *            minimum length of the abbreviated string. Must be in the range
	 *            [2, {@value Constants#OBJECT_ID_STRING_LENGTH}].
	 * @return SHA-1 abbreviation. If no matching objects exist in the
	 *         repository, the abbreviation will match the minimum length.
	 * @throws IOException
	 *             the object store cannot be read.
	 */
	public AbbreviatedObjectId abbreviate(AnyObjectId objectId, int len)
			throws IOException {
		if (len == Constants.OBJECT_ID_STRING_LENGTH)
			return AbbreviatedObjectId.fromObjectId(objectId);

		AbbreviatedObjectId abbrev = objectId.abbreviate(len);
		Collection<ObjectId> matches = resolve(abbrev);
		while (1 < matches.size() && len < Constants.OBJECT_ID_STRING_LENGTH) {
			abbrev = objectId.abbreviate(++len);
			List<ObjectId> n = new ArrayList<ObjectId>(8);
			for (ObjectId candidate : matches) {
				if (abbrev.prefixCompare(candidate) == 0)
					n.add(candidate);
			}
			if (1 < n.size())
				matches = n;
			else
				matches = resolve(abbrev);
		}
		return abbrev;
	}

	/**
	 * Resolve an abbreviated ObjectId to its full form.
	 *
	 * This method searches for an ObjectId that begins with the abbreviation,
	 * and returns at least some matching candidates.
	 *
	 * If the returned collection is empty, no objects start with this
	 * abbreviation. The abbreviation doesn't belong to this repository, or the
	 * repository lacks the necessary objects to complete it.
	 *
	 * If the collection contains exactly one member, the abbreviation is
	 * (currently) unique within this database. There is a reasonably high
	 * probability that the returned id is what was previously abbreviated.
	 *
	 * If the collection contains 2 or more members, the abbreviation is not
	 * unique. In this case the implementation is only required to return at
	 * least 2 candidates to signal the abbreviation has conflicts. User
	 * friendly implementations should return as many candidates as reasonably
	 * possible, as the caller may be able to disambiguate further based on
	 * context. However since databases can be very large (e.g. 10 million
	 * objects) returning 625,000 candidates for the abbreviation "0" is simply
	 * unreasonable, so implementors should draw the line at around 256 matches.
	 *
	 * @param id
	 *            abbreviated id to resolve to a complete identity. The
	 *            abbreviation must have a length of at least 2.
	 * @return candidates that begin with the abbreviated identity.
	 * @throws IOException
	 *             the object store cannot be read.
	 */
	public abstract Collection<ObjectId> resolve(AbbreviatedObjectId id)
			throws IOException;

	/**
	 * Does the requested object exist in this database?
	 *
	 * @param objectId
	 *            identity of the object to test for existence of.
	 * @return true if the specified object is stored in this database.
	 * @throws IOException
	 *             the object store cannot be accessed.
	 */
	public boolean has(AnyObjectId objectId) throws IOException {
		return has(objectId, OBJ_ANY);
	}

	/**
	 * Does the requested object exist in this database?
	 *
	 * @param objectId
	 *            identity of the object to test for existence of.
	 * @param typeHint
	 *            hint about the type of object being requested;
	 *            {@link #OBJ_ANY} if the object type is not known, or does not
	 *            matter to the caller.
	 * @return true if the specified object is stored in this database.
	 * @throws IncorrectObjectTypeException
	 *             typeHint was not OBJ_ANY, and the object's actual type does
	 *             not match typeHint.
	 * @throws IOException
	 *             the object store cannot be accessed.
	 */
	public boolean has(AnyObjectId objectId, int typeHint) throws IOException {
		try {
			open(objectId, typeHint);
			return true;
		} catch (MissingObjectException notFound) {
			return false;
		}
	}

	/**
	 * Open an object from this database.
	 *
	 * @param objectId
	 *            identity of the object to open.
	 * @return a {@link ObjectLoader} for accessing the object.
	 * @throws MissingObjectException
	 *             the object does not exist.
	 * @throws IOException
	 *             the object store cannot be accessed.
	 */
	public ObjectLoader open(AnyObjectId objectId)
			throws MissingObjectException, IOException {
		return open(objectId, OBJ_ANY);
	}

	/**
	 * Open an object from this database.
	 *
	 * @param objectId
	 *            identity of the object to open.
	 * @param typeHint
	 *            hint about the type of object being requested;
	 *            {@link #OBJ_ANY} if the object type is not known, or does not
	 *            matter to the caller.
	 * @return a {@link ObjectLoader} for accessing the object.
	 * @throws MissingObjectException
	 *             the object does not exist.
	 * @throws IncorrectObjectTypeException
	 *             typeHint was not OBJ_ANY, and the object's actual type does
	 *             not match typeHint.
	 * @throws IOException
	 *             the object store cannot be accessed.
	 */
	public abstract ObjectLoader open(AnyObjectId objectId, int typeHint)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException;

	/**
	 * Asynchronous object opening.
	 *
	 * @param <T>
	 *            type of identifier being supplied.
	 * @param objectIds
	 *            objects to open from the object store. The supplied collection
	 *            must not be modified until the queue has finished.
	 * @param reportMissing
	 *            if true missing objects are reported by calling failure with a
	 *            MissingObjectException. This may be more expensive for the
	 *            implementation to guarantee. If false the implementation may
	 *            choose to report MissingObjectException, or silently skip over
	 *            the object with no warning.
	 * @return queue to read the objects from.
	 */
	public <T extends ObjectId> AsyncObjectLoaderQueue<T> open(
			Iterable<T> objectIds, final boolean reportMissing) {
		final Iterator<T> idItr = objectIds.iterator();
		return new AsyncObjectLoaderQueue<T>() {
			private T cur;

			public boolean next() throws MissingObjectException, IOException {
				if (idItr.hasNext()) {
					cur = idItr.next();
					return true;
				} else {
					return false;
				}
			}

			public T getCurrent() {
				return cur;
			}

			public ObjectId getObjectId() {
				return cur;
			}

			public ObjectLoader open() throws IOException {
				return ObjectReader.this.open(cur, OBJ_ANY);
			}

			public boolean cancel(boolean mayInterruptIfRunning) {
				return true;
			}

			public void release() {
				// Since we are sequential by default, we don't
				// have any state to clean up if we terminate early.
			}
		};
	}

	/**
	 * Get only the size of an object.
	 * <p>
	 * The default implementation of this method opens an ObjectLoader.
	 * Databases are encouraged to override this if a faster access method is
	 * available to them.
	 *
	 * @param objectId
	 *            identity of the object to open.
	 * @param typeHint
	 *            hint about the type of object being requested;
	 *            {@link #OBJ_ANY} if the object type is not known, or does not
	 *            matter to the caller.
	 * @return size of object in bytes.
	 * @throws MissingObjectException
	 *             the object does not exist.
	 * @throws IncorrectObjectTypeException
	 *             typeHint was not OBJ_ANY, and the object's actual type does
	 *             not match typeHint.
	 * @throws IOException
	 *             the object store cannot be accessed.
	 */
	public long getObjectSize(AnyObjectId objectId, int typeHint)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		return open(objectId, typeHint).getSize();
	}

	/**
	 * Asynchronous object size lookup.
	 *
	 * @param <T>
	 *            type of identifier being supplied.
	 * @param objectIds
	 *            objects to get the size of from the object store. The supplied
	 *            collection must not be modified until the queue has finished.
	 * @param reportMissing
	 *            if true missing objects are reported by calling failure with a
	 *            MissingObjectException. This may be more expensive for the
	 *            implementation to guarantee. If false the implementation may
	 *            choose to report MissingObjectException, or silently skip over
	 *            the object with no warning.
	 * @return queue to read object sizes from.
	 */
	public <T extends ObjectId> AsyncObjectSizeQueue<T> getObjectSize(
			Iterable<T> objectIds, final boolean reportMissing) {
		final Iterator<T> idItr = objectIds.iterator();
		return new AsyncObjectSizeQueue<T>() {
			private T cur;

			private long sz;

			public boolean next() throws MissingObjectException, IOException {
				if (idItr.hasNext()) {
					cur = idItr.next();
					sz = getObjectSize(cur, OBJ_ANY);
					return true;
				} else {
					return false;
				}
			}

			public T getCurrent() {
				return cur;
			}

			public ObjectId getObjectId() {
				return cur;
			}

			public long getSize() {
				return sz;
			}

			public boolean cancel(boolean mayInterruptIfRunning) {
				return true;
			}

			public void release() {
				// Since we are sequential by default, we don't
				// have any state to clean up if we terminate early.
			}
		};
	}

	/**
	 * Advice from a {@link RevWalk} that a walk is starting from these roots.
	 *
	 * @param walk
	 *            the revision pool that is using this reader.
	 * @param roots
	 *            starting points of the revision walk. The starting points have
	 *            their headers parsed, but might be missing bodies.
	 * @throws IOException
	 *             the reader cannot initialize itself to support the walk.
	 */
	public void walkAdviceBeginCommits(RevWalk walk, Collection<RevCommit> roots)
			throws IOException {
		// Do nothing by default, most readers don't want or need advice.
	}

	/**
	 * Advice from an {@link ObjectWalk} that trees will be traversed.
	 *
	 * @param ow
	 *            the object pool that is using this reader.
	 * @param min
	 *            the first commit whose root tree will be read.
	 * @param max
	 *            the last commit whose root tree will be read.
	 * @throws IOException
	 *             the reader cannot initialize itself to support the walk.
	 */
	public void walkAdviceBeginTrees(ObjectWalk ow, RevCommit min, RevCommit max)
			throws IOException {
		// Do nothing by default, most readers don't want or need advice.
	}

	/** Advice from that a walk is over. */
	public void walkAdviceEnd() {
		// Do nothing by default, most readers don't want or need advice.
	}

	/**
	 * Release any resources used by this reader.
	 * <p>
	 * A reader that has been released can be used again, but may need to be
	 * released after the subsequent usage.
	 */
	public void release() {
		// Do nothing.
	}
}
