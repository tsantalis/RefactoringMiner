package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.LinkedList;

import name.fraser.neil.plaintext.diff_match_patch.Diff;
import br.ufmg.dcc.labsoft.refdetector.model.builder.DiffUtils;

public class SourceCodeDiff {

	private LinkedList<Diff> diffs;
	
	public SourceCodeDiff(LinkedList<Diff> diffs) {
		this.diffs = diffs;
	}

	public SourceCode deletedLines(double threshold) {
		LinkedList<String> lines = DiffUtils.getDeletedXorInsertedLines(diffs, true, threshold);
		return new SourceCode(DiffUtils.joinLines(lines));
	}

	public SourceCode insertedLines(double threshold) {
		LinkedList<String> lines = DiffUtils.getDeletedXorInsertedLines(diffs, false, threshold);
		return new SourceCode(DiffUtils.joinLines(lines));
	}

	public String toString() {
		return DiffUtils.toHtml(diffs);
	}

}
