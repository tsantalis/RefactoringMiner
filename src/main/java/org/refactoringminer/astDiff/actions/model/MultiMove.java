package org.refactoringminer.astDiff.actions.model;

import com.github.gumtreediff.actions.model.TreeAddition;
import com.github.gumtreediff.tree.Tree;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class MultiMove extends TreeAddition {
	private int groupId;
	private boolean updated = false;

	public MultiMove(Tree node, Tree parent, int pos, int groupId, boolean updated) {
		super(node, parent, pos);
		this.groupId = groupId;
		this.updated = updated;
	}

	public boolean isUpdated() { return updated;}

	public int getGroupId() {
		return groupId;
	}

	@Override
	public String getName() {
		return "multi-move-tree";
	}
}
