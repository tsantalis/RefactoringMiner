package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.LinkedList;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch.Diff;
import br.ufmg.dcc.labsoft.refdetector.model.builder.DiffUtils;

public class SourceCode {

	private List<String> lines;
	
	public SourceCode(String source) {
		this.lines = DiffUtils.splitLines(source);
	}

	public SourceCode(List<String> lines) {
		this.lines = lines;
	}

	public SourceCodeDiff diff(SourceCode other) {
		LinkedList<Diff> diffs = DiffUtils.tokenBasedDiff(DiffUtils.joinLines(this.lines), DiffUtils.joinLines(other.lines));
		return new SourceCodeDiff(diffs);
	}

	public SourceCodeDiff lineDiff(SourceCode other) {
		LinkedList<Diff> diffs = DiffUtils.lineBasedDiff(DiffUtils.joinLines(this.lines), DiffUtils.joinLines(other.lines));
		return new SourceCodeDiff(diffs);
	}

	public int linesCount() {
		return this.lines.size();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line);
			sb.append('\n');
		}
		return sb.toString();
	}
}
