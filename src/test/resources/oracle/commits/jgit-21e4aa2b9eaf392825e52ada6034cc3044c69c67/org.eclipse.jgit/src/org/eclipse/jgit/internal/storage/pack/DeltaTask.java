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

package org.eclipse.jgit.internal.storage.pack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ThreadSafeProgressMonitor;
import org.eclipse.jgit.storage.pack.PackConfig;

final class DeltaTask implements Callable<Object> {
	static final class Block {
		private static final int MIN_TOP_PATH = 50 << 20;

		final List<DeltaTask> tasks;
		final int threads;
		final PackConfig config;
		final ObjectReader templateReader;
		final DeltaCache dc;
		final ThreadSafeProgressMonitor pm;
		final ObjectToPack[] list;
		final int beginIndex;
		final int endIndex;

		private long totalWeight;

		Block(int threads, PackConfig config, ObjectReader reader,
				DeltaCache dc, ThreadSafeProgressMonitor pm,
				ObjectToPack[] list, int begin, int end) {
			this.tasks = new ArrayList<DeltaTask>(threads);
			this.threads = threads;
			this.config = config;
			this.templateReader = reader;
			this.dc = dc;
			this.pm = pm;
			this.list = list;
			this.beginIndex = begin;
			this.endIndex = end;
		}

		synchronized DeltaWindow stealWork(DeltaTask forThread) {
			for (;;) {
				DeltaTask maxTask = null;
				Slice maxSlice = null;
				int maxWork = 0;

				for (DeltaTask task : tasks) {
					Slice s = task.remaining();
					if (s != null && maxWork < s.size()) {
						maxTask = task;
						maxSlice = s;
						maxWork = s.size();
					}
				}
				if (maxTask == null)
					return null;
				if (maxTask.tryStealWork(maxSlice))
					return forThread.initWindow(maxSlice);
			}
		}

		void partitionTasks() {
			ArrayList<WeightedPath> topPaths = computeTopPaths();
			Iterator<WeightedPath> topPathItr = topPaths.iterator();
			int nextTop = 0;
			long weightPerThread = totalWeight / threads;
			for (int i = beginIndex; i < endIndex;) {
				DeltaTask task = new DeltaTask(this);
				long w = 0;

				// Assign the thread one top path.
				if (topPathItr.hasNext()) {
					WeightedPath p = topPathItr.next();
					w += p.weight;
					task.add(p.slice);
				}

				// Assign the task thread ~average weight.
				int s = i;
				for (; w < weightPerThread && i < endIndex;) {
					if (nextTop < topPaths.size()
							&& i == topPaths.get(nextTop).slice.beginIndex) {
						if (s < i)
							task.add(new Slice(s, i));
						s = i = topPaths.get(nextTop++).slice.endIndex;
					} else
						w += list[i++].getWeight();
				}

				// Round up the slice to the end of a path.
				if (s < i) {
					int h = list[i - 1].getPathHash();
					while (i < endIndex) {
						if (h == list[i].getPathHash())
							i++;
						else
							break;
					}
					task.add(new Slice(s, i));
				}
				if (!task.slices.isEmpty())
					tasks.add(task);
			}
			while (topPathItr.hasNext()) {
				WeightedPath p = topPathItr.next();
				DeltaTask task = new DeltaTask(this);
				task.add(p.slice);
				tasks.add(task);
			}

			topPaths = null;
		}

		private ArrayList<WeightedPath> computeTopPaths() {
			ArrayList<WeightedPath> topPaths = new ArrayList<WeightedPath>(
					threads);
			int cp = beginIndex;
			int ch = list[cp].getPathHash();
			long cw = list[cp].getWeight();
			totalWeight = list[cp].getWeight();

			for (int i = cp + 1; i < endIndex; i++) {
				ObjectToPack o = list[i];
				if (ch != o.getPathHash()) {
					if (MIN_TOP_PATH < cw) {
						if (topPaths.size() < threads) {
							Slice s = new Slice(cp, i);
							topPaths.add(new WeightedPath(cw, s));
							if (topPaths.size() == threads)
								Collections.sort(topPaths);
						} else if (topPaths.get(0).weight < cw) {
							Slice s = new Slice(cp, i);
							WeightedPath p = new WeightedPath(cw, s);
							topPaths.set(0, p);
							if (p.compareTo(topPaths.get(1)) > 0)
								Collections.sort(topPaths);
						}
					}
					cp = i;
					ch = o.getPathHash();
					cw = 0;
				}
				if (o.isEdge() || o.doNotAttemptDelta())
					continue;
				cw += o.getWeight();
				totalWeight += o.getWeight();
			}

			// Sort by starting index to identify gaps later.
			Collections.sort(topPaths, new Comparator<WeightedPath>() {
				public int compare(WeightedPath a, WeightedPath b) {
					return a.slice.beginIndex - b.slice.beginIndex;
				}
			});
			return topPaths;
		}
	}

	static final class WeightedPath implements Comparable<WeightedPath> {
		final long weight;
		final Slice slice;

		WeightedPath(long weight, Slice s) {
			this.weight = weight;
			this.slice = s;
		}

		public int compareTo(WeightedPath o) {
			int cmp = Long.signum(weight - o.weight);
			if (cmp != 0)
				return cmp;
			return slice.beginIndex - o.slice.beginIndex;
		}
	}

	static final class Slice {
		final int beginIndex;
		final int endIndex;

		Slice(int b, int e) {
			beginIndex = b;
			endIndex = e;
		}

		final int size() {
			return endIndex - beginIndex;
		}
	}

	private final Block block;
	private final LinkedList<Slice> slices;

	private ObjectReader or;
	private DeltaWindow dw;

	DeltaTask(Block b) {
		this.block = b;
		this.slices = new LinkedList<Slice>();
	}

	void add(Slice s) {
		if (!slices.isEmpty()) {
			Slice last = slices.getLast();
			if (last.endIndex == s.beginIndex) {
				slices.removeLast();
				slices.add(new Slice(last.beginIndex, s.endIndex));
				return;
			}
		}
		slices.add(s);
	}

	public Object call() throws Exception {
		or = block.templateReader.newReader();
		try {
			DeltaWindow w;
			for (;;) {
				synchronized (this) {
					if (slices.isEmpty())
						break;
					w = initWindow(slices.removeFirst());
				}
				runWindow(w);
			}
			while ((w = block.stealWork(this)) != null)
				runWindow(w);
		} finally {
			block.pm.endWorker();
			or.release();
			or = null;
		}
		return null;
	}

	DeltaWindow initWindow(Slice s) {
		DeltaWindow w = new DeltaWindow(block.config, block.dc,
				or, block.pm,
				block.list, s.beginIndex, s.endIndex);
		synchronized (this) {
			dw = w;
		}
		return w;
	}

	private void runWindow(DeltaWindow w) throws IOException {
		try {
			w.search();
		} finally {
			synchronized (this) {
				dw = null;
			}
		}
	}

	synchronized Slice remaining() {
		if (!slices.isEmpty())
			return slices.getLast();
		DeltaWindow d = dw;
		return d != null ? d.remaining() : null;
	}

	synchronized boolean tryStealWork(Slice s) {
		if (!slices.isEmpty() && slices.getLast().beginIndex == s.beginIndex) {
			slices.removeLast();
			return true;
		}
		DeltaWindow d = dw;
		return d != null ? d.tryStealWork(s) : false;
	}
}
