package org.refactoringminer.astDiff.actions.model;

import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.tree.Tree;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class MoveOut extends TreeAddition {
	private String dstFile;

	public MoveOut(Tree node, Tree parent, String dstfile, int pos) {
		super(node, parent, pos);
		this.dstFile = dstfile;
	}

	public String getDstFile() {
		return dstFile;
	}

	@Override
	public String getName() {
		return "M";
	}

	@Override
	public String toString()  {
		return "moved to file: " + getDstFile();
	}
}
