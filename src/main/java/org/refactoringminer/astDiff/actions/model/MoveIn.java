package org.refactoringminer.astDiff.actions.model;

import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.tree.Tree;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class MoveIn extends TreeAddition {
	private String srcFile;

	public MoveIn(Tree node, Tree parent, String srcFile, int pos) {
		super(node, parent, pos);
		this.srcFile = srcFile;
	}

	public String getSrcFile() {
		return srcFile;
	}

	@Override
	public String getName() {
		return "M";
	}

	@Override
	public String toString()  {
		return "moved from file: " + getSrcFile();
	}
}
