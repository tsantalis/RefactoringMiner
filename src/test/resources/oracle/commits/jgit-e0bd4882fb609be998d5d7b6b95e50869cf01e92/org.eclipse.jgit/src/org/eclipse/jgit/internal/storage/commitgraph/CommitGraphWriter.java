/*
 * Copyright (C) 2021, Tencent.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.commitgraph;

import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.CHUNK_ID_BLOOM_FILTER_DATA;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.CHUNK_ID_BLOOM_FILTER_INDEX;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.CHUNK_ID_COMMIT_DATA;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.CHUNK_ID_EXTRA_EDGE_LIST;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.CHUNK_ID_OID_FANOUT;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.CHUNK_ID_OID_LOOKUP;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.CHUNK_LOOKUP_WIDTH;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.COMMIT_DATA_WIDTH;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.COMMIT_GRAPH_MAGIC;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.GRAPH_EXTRA_EDGES_NEEDED;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.GRAPH_LAST_EDGE;
import static org.eclipse.jgit.internal.storage.commitgraph.CommitGraphConstants.GRAPH_NO_PARENT;
import static org.eclipse.jgit.lib.Constants.COMMIT_GENERATION_NOT_COMPUTED;
import static org.eclipse.jgit.lib.Constants.COMMIT_GENERATION_UNKNOWN;
import static org.eclipse.jgit.lib.Constants.OBJECT_ID_LENGTH;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.io.CancellableDigestOutputStream;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.NB;

/**
 * Writes a commit-graph formatted file.
 *
 * @since 6.5
 */
public class CommitGraphWriter {

	private static final int COMMIT_GRAPH_VERSION_GENERATED = 1;

	private static final int OID_HASH_VERSION = 1;

	private static final int GRAPH_FANOUT_SIZE = 4 * 256;

	private static final int GENERATION_NUMBER_MAX = 0x3FFFFFFF;

	private static final int MAX_CHANGED_PATHS = 512;

	private final int hashsz;

	private final GraphCommits graphCommits;

	private final boolean generateChangedPathFilters;

	/**
	 * Create commit-graph writer for these commits.
	 *
	 * @param graphCommits
	 *            the commits which will be writen to the commit-graph.
	 */
	public CommitGraphWriter(@NonNull GraphCommits graphCommits) {
		this(graphCommits, false);
	}

	/**
	 * Create commit-graph writer for these commits.
	 *
	 * @param graphCommits
	 *            the commits which will be writen to the commit-graph.
	 * @param generateChangedPathFilters
	 *            whether changed path filters are generated
	 */
	public CommitGraphWriter(@NonNull GraphCommits graphCommits,
			boolean generateChangedPathFilters) {
		this.graphCommits = graphCommits;
		this.hashsz = OBJECT_ID_LENGTH;
		this.generateChangedPathFilters = generateChangedPathFilters;
	}

	/**
	 * Write commit-graph to the supplied stream.
	 *
	 * @param monitor
	 *            progress monitor to report the number of items written.
	 * @param commitGraphStream
	 *            output stream of commit-graph data. The stream should be
	 *            buffered by the caller. The caller is responsible for closing
	 *            the stream.
	 * @return statistics gathered during the run
	 * @throws IOException
	 *             if an error occurred
	 */
	public Stats write(@NonNull ProgressMonitor monitor,
			@NonNull OutputStream commitGraphStream) throws IOException {
		Stats stats = new Stats();
		if (graphCommits.size() == 0) {
			return stats;
		}

		List<ChunkHeader> chunks = createChunks(stats);
		long expectedSize = calculateExpectedSize(chunks);
		long writeCount = 256 + 2 * graphCommits.size()
				+ graphCommits.getExtraEdgeCnt();
		monitor.beginTask(
				MessageFormat.format(JGitText.get().writingOutCommitGraph,
						Integer.valueOf(chunks.size())),
				(int) writeCount);

		try (CancellableDigestOutputStream out = new CancellableDigestOutputStream(
				monitor, commitGraphStream)) {
			writeHeader(out, chunks.size());
			writeChunkLookup(out, chunks);
			writeChunks(monitor, out, chunks);
			writeCheckSum(out);
			if (expectedSize != out.length()) {
				throw new IllegalStateException(String.format(
						"Commit-graph: expected %d bytes but out has %d bytes", //$NON-NLS-1$
						expectedSize, out.length()));
			}
		} catch (InterruptedIOException e) {
			throw new IOException(JGitText.get().commitGraphWritingCancelled,
					e);
		} finally {
			monitor.endTask();
		}
		return stats;
	}

	private List<ChunkHeader> createChunks(Stats stats)
			throws MissingObjectException,
			IncorrectObjectTypeException, CorruptObjectException, IOException {
		List<ChunkHeader> chunks = new ArrayList<>();
		chunks.add(new ChunkHeader(CHUNK_ID_OID_FANOUT, GRAPH_FANOUT_SIZE));
		chunks.add(new ChunkHeader(CHUNK_ID_OID_LOOKUP,
				hashsz * graphCommits.size()));
		chunks.add(new ChunkHeader(CHUNK_ID_COMMIT_DATA,
				(hashsz + 16) * graphCommits.size()));
		if (graphCommits.getExtraEdgeCnt() > 0) {
			chunks.add(new ChunkHeader(CHUNK_ID_EXTRA_EDGE_LIST,
					graphCommits.getExtraEdgeCnt() * 4));
		}
		if (generateChangedPathFilters) {
			BloomFilterChunks bloomFilterChunks = computeBloomFilterChunks(
					stats);
			chunks.add(new ChunkHeader(CHUNK_ID_BLOOM_FILTER_INDEX,
					bloomFilterChunks.index));
			chunks.add(new ChunkHeader(CHUNK_ID_BLOOM_FILTER_DATA,
					bloomFilterChunks.data));
		}
		return Collections.unmodifiableList(chunks);
	}

	private static long calculateExpectedSize(List<ChunkHeader> chunks) {
		int chunkLookup = (chunks.size() + 1) * CHUNK_LOOKUP_WIDTH;
		long chunkContent = chunks.stream().mapToLong(c -> c.size).sum();
		return /* header */ 8 + chunkLookup + chunkContent + /* CRC */ 20;
	}

	private void writeHeader(CancellableDigestOutputStream out, int numChunks)
			throws IOException {
		byte[] headerBuffer = new byte[8];
		NB.encodeInt32(headerBuffer, 0, COMMIT_GRAPH_MAGIC);
		byte[] buff = { (byte) COMMIT_GRAPH_VERSION_GENERATED,
				(byte) OID_HASH_VERSION, (byte) numChunks, (byte) 0 };
		System.arraycopy(buff, 0, headerBuffer, 4, 4);
		out.write(headerBuffer, 0, 8);
		out.flush();
	}

	private void writeChunkLookup(CancellableDigestOutputStream out,
			List<ChunkHeader> chunks) throws IOException {
		int numChunks = chunks.size();
		long chunkOffset = 8 + (numChunks + 1) * CHUNK_LOOKUP_WIDTH;
		byte[] buffer = new byte[CHUNK_LOOKUP_WIDTH];
		for (ChunkHeader chunk : chunks) {
			NB.encodeInt32(buffer, 0, chunk.id);
			NB.encodeInt64(buffer, 4, chunkOffset);
			out.write(buffer);
			chunkOffset += chunk.size;
		}
		NB.encodeInt32(buffer, 0, 0);
		NB.encodeInt64(buffer, 4, chunkOffset);
		out.write(buffer);
	}

	private void writeChunks(ProgressMonitor monitor,
			CancellableDigestOutputStream out, List<ChunkHeader> chunks)
			throws IOException {
		for (ChunkHeader chunk : chunks) {
			int chunkId = chunk.id;

			switch (chunkId) {
			case CHUNK_ID_OID_FANOUT:
				writeFanoutTable(out);
				break;
			case CHUNK_ID_OID_LOOKUP:
				writeOidLookUp(out);
				break;
			case CHUNK_ID_COMMIT_DATA:
				writeCommitData(monitor, out);
				break;
			case CHUNK_ID_EXTRA_EDGE_LIST:
				writeExtraEdges(out);
				break;
			case CHUNK_ID_BLOOM_FILTER_INDEX:
			case CHUNK_ID_BLOOM_FILTER_DATA:
				if (!chunk.data.isPresent()) {
					throw new IllegalStateException(
							"data for this chunk must be precomputed"); //$NON-NLS-1$
				}
				chunk.data.get().writeTo(out);
				break;
			default:
				throw new IllegalStateException(
						"Don't know how to write chunk " + chunkId); //$NON-NLS-1$
			}
		}
	}

	private void writeCheckSum(CancellableDigestOutputStream out)
			throws IOException {
		out.write(out.getDigest());
		out.flush();
	}

	private void writeFanoutTable(CancellableDigestOutputStream out)
			throws IOException {
		byte[] tmp = new byte[4];
		int[] fanout = new int[256];
		for (RevCommit c : graphCommits) {
			fanout[c.getFirstByte() & 0xff]++;
		}
		for (int i = 1; i < fanout.length; i++) {
			fanout[i] += fanout[i - 1];
		}
		for (int n : fanout) {
			NB.encodeInt32(tmp, 0, n);
			out.write(tmp, 0, 4);
			out.getWriteMonitor().update(1);
		}
	}

	private void writeOidLookUp(CancellableDigestOutputStream out)
			throws IOException {
		byte[] tmp = new byte[4 + hashsz];

		for (RevCommit c : graphCommits) {
			c.copyRawTo(tmp, 0);
			out.write(tmp, 0, hashsz);
			out.getWriteMonitor().update(1);
		}
	}

	private void writeCommitData(ProgressMonitor monitor,
			CancellableDigestOutputStream out) throws IOException {
		int[] generations = computeGenerationNumbers(monitor);
		int num = 0;
		byte[] tmp = new byte[hashsz + COMMIT_DATA_WIDTH];
		int i = 0;
		for (RevCommit commit : graphCommits) {
			int edgeValue;
			int[] packedDate = new int[2];

			ObjectId treeId = commit.getTree();
			treeId.copyRawTo(tmp, 0);

			RevCommit[] parents = commit.getParents();
			if (parents.length == 0) {
				edgeValue = GRAPH_NO_PARENT;
			} else {
				RevCommit parent = parents[0];
				edgeValue = graphCommits.getOidPosition(parent);
			}
			NB.encodeInt32(tmp, hashsz, edgeValue);
			if (parents.length == 1) {
				edgeValue = GRAPH_NO_PARENT;
			} else if (parents.length == 2) {
				RevCommit parent = parents[1];
				edgeValue = graphCommits.getOidPosition(parent);
			} else if (parents.length > 2) {
				edgeValue = GRAPH_EXTRA_EDGES_NEEDED | num;
				num += parents.length - 1;
			}

			NB.encodeInt32(tmp, hashsz + 4, edgeValue);

			packedDate[0] = 0; // commitTime is an int in JGit now
			packedDate[0] |= generations[i] << 2;
			packedDate[1] = commit.getCommitTime();
			NB.encodeInt32(tmp, hashsz + 8, packedDate[0]);
			NB.encodeInt32(tmp, hashsz + 12, packedDate[1]);

			out.write(tmp);
			out.getWriteMonitor().update(1);
			i++;
		}
	}

	private int[] computeGenerationNumbers(ProgressMonitor monitor)
			throws MissingObjectException {
		int[] generations = new int[graphCommits.size()];
		monitor.beginTask(JGitText.get().computingCommitGeneration,
				graphCommits.size());
		for (RevCommit cmit : graphCommits) {
			monitor.update(1);
			int generation = generations[graphCommits.getOidPosition(cmit)];
			if (generation != COMMIT_GENERATION_NOT_COMPUTED
					&& generation != COMMIT_GENERATION_UNKNOWN) {
				continue;
			}

			Stack<RevCommit> commitStack = new Stack<>();
			commitStack.push(cmit);

			while (!commitStack.empty()) {
				int maxGeneration = 0;
				boolean allParentComputed = true;
				RevCommit current = commitStack.peek();
				RevCommit parent;

				for (int i = 0; i < current.getParentCount(); i++) {
					parent = current.getParent(i);
					generation = generations[graphCommits
							.getOidPosition(parent)];
					if (generation == COMMIT_GENERATION_NOT_COMPUTED
							|| generation == COMMIT_GENERATION_UNKNOWN) {
						allParentComputed = false;
						commitStack.push(parent);
						break;
					} else if (generation > maxGeneration) {
						maxGeneration = generation;
					}
				}

				if (allParentComputed) {
					RevCommit commit = commitStack.pop();
					generation = maxGeneration + 1;
					if (generation > GENERATION_NUMBER_MAX) {
						generation = GENERATION_NUMBER_MAX;
					}
					generations[graphCommits
							.getOidPosition(commit)] = generation;
				}
			}
		}
		monitor.endTask();
		return generations;
	}

	private static Optional<HashSet<ByteBuffer>> computeBloomFilterPaths(
			ObjectReader or, RevCommit cmit) throws MissingObjectException,
			IncorrectObjectTypeException, CorruptObjectException, IOException {
		HashSet<ByteBuffer> paths = new HashSet<>();
		try (TreeWalk walk = new TreeWalk(null, or)) {
			walk.setRecursive(true);
			if (cmit.getParentCount() == 0) {
				walk.addTree(new EmptyTreeIterator());
			} else {
				walk.addTree(cmit.getParent(0).getTree());
			}
			walk.addTree(cmit.getTree());
			while (walk.next()) {
				if (walk.idEqual(0, 1)) {
					continue;
				}
				byte[] rawPath = walk.getRawPath();
				paths.add(ByteBuffer.wrap(rawPath));
				for (int i = 0; i < rawPath.length; i++) {
					if (rawPath[i] == '/') {
						paths.add(ByteBuffer.wrap(rawPath, 0, i));
					}
					if (paths.size() > MAX_CHANGED_PATHS) {
						return Optional.empty();
					}
				}
			}
		}
		return Optional.of(paths);
	}

	private BloomFilterChunks computeBloomFilterChunks(Stats stats)
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {

		ByteArrayOutputStream index = new ByteArrayOutputStream();
		ByteArrayOutputStream data = new ByteArrayOutputStream();

		// Allocate scratch buffer for converting integers into
		// big-endian bytes.
		byte[] scratch = new byte[4];

		NB.encodeInt32(scratch, 0, 1); // version 1
		data.write(scratch);
		NB.encodeInt32(scratch, 0, ChangedPathFilter.PATH_HASH_COUNT);
		data.write(scratch);
		NB.encodeInt32(scratch, 0, ChangedPathFilter.BITS_PER_ENTRY);
		data.write(scratch);
		int dataHeaderSize = data.size();

		try (RevWalk rw = new RevWalk(graphCommits.getObjectReader())) {
			for (RevCommit cmit : graphCommits) {
				ChangedPathFilter cpf = cmit.getChangedPathFilter(rw);
				if (cpf != null) {
					stats.changedPathFiltersReused++;
				} else {
					stats.changedPathFiltersComputed++;
					Optional<HashSet<ByteBuffer>> paths = computeBloomFilterPaths(
							graphCommits.getObjectReader(), cmit);
					if (paths.isEmpty()) {
						cpf = ChangedPathFilter.FULL;
					} else {
						cpf = ChangedPathFilter.fromPaths(paths.get());
					}
				}
				cpf.writeTo(data);
				NB.encodeInt32(scratch, 0, data.size() - dataHeaderSize);
				index.write(scratch);
			}
			return new BloomFilterChunks(index, data);
		}
	}

	private void writeExtraEdges(CancellableDigestOutputStream out)
			throws IOException {
		byte[] tmp = new byte[4];
		for (RevCommit commit : graphCommits) {
			RevCommit[] parents = commit.getParents();
			if (parents.length > 2) {
				int edgeValue;
				for (int n = 1; n < parents.length; n++) {
					RevCommit parent = parents[n];
					edgeValue = graphCommits.getOidPosition(parent);
					if (n == parents.length - 1) {
						edgeValue |= GRAPH_LAST_EDGE;
					}
					NB.encodeInt32(tmp, 0, edgeValue);
					out.write(tmp);
					out.getWriteMonitor().update(1);
				}
			}
		}
	}

	private static class ChunkHeader {
		final int id;

		final long size;

		final Optional<ByteArrayOutputStream> data;

		public ChunkHeader(int id, long size) {
			this.id = id;
			this.size = size;
			this.data = Optional.empty();
		}

		ChunkHeader(int id, ByteArrayOutputStream data) {
			this.id = id;
			this.size = data.size();
			this.data = Optional.of(data);
		}
	}

	private static class BloomFilterChunks {
		final ByteArrayOutputStream index;

		final ByteArrayOutputStream data;

		BloomFilterChunks(ByteArrayOutputStream index,
				ByteArrayOutputStream data) {
			this.index = index;
			this.data = data;
		}
	}

	/**
	 * Statistics collected during a single commit graph write.
	 */
	public static class Stats {

		private long changedPathFiltersReused = 0;

		private long changedPathFiltersComputed = 0;

		/**
		 * Returns the number of existing changed path filters that were reused
		 * when writing, for statistical purposes.
		 *
		 * @return count of changed path filters
		 */
		public long getChangedPathFiltersReused() {
			return changedPathFiltersReused;
		}

		/**
		 * Returns the number of changed path filters that were computed from
		 * scratch, for statistical purposes.
		 *
		 * @return count of changed path filters
		 */
		public long getChangedPathFiltersComputed() {
			return changedPathFiltersComputed;
		}
	}
}
