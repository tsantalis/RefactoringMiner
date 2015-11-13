package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.LinkedList;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch.Diff;
import br.ufmg.dcc.labsoft.refdetector.model.builder.DiffUtils;

public class SourceCode {

	private final String source;
	private final boolean empty;
	private final int linesCount;
	private final long[] bigramHashes;
	
	public SourceCode() {
		this("", true);
	}

	public SourceCode(String source) {
		this(source, false);
	}

	public SourceCode(String source, boolean empty) {
		this.source = source;
		this.empty = empty;
		if (empty) {
			linesCount = 0;
			this.bigramHashes = new long[0];
		} else {
			List<String> lines = DiffUtils.splitLines(source);
			linesCount = lines.size();
			this.bigramHashes = DiffUtils.computeBigrams(DiffUtils.computeHashes(source));
		}
	}

	public double similarity(SourceCode other) {
		return similarity(other, 1);
	}
	
	public double similarity(SourceCode other, int factor) {
		if (empty || other.empty) {
			return 0.0;
		}
		return DiffUtils.computeSimilarity(this.bigramHashes, factor, other.bigramHashes, false);
	}

	public double partialSimilarity(SourceCode other) {
		if (empty || other.empty) {
			return 0.0;
		}
		return DiffUtils.computeSimilarity(this.bigramHashes, other.bigramHashes, true);
	}
	
	public SourceCodeDiff diff(SourceCode other) {
		LinkedList<Diff> diffs = DiffUtils.tokenBasedDiff(this.source, other.source);
		return new SourceCodeDiff(diffs);
	}

	public SourceCodeDiff lineDiff(SourceCode other) {
		LinkedList<Diff> diffs = DiffUtils.lineBasedDiff(this.source, other.source);
		return new SourceCodeDiff(diffs);
	}

	public int linesCount() {
		return linesCount;
	}
	
	@Override
	public String toString() {
		return source;
	}

	public boolean isEmpty() {
		return empty;
	}
	
}
