package org.refactoringminer.astDiff.actions;

import java.util.HashMap;
import java.util.Map;

import com.github.gumtreediff.actions.AbstractITreeClassifier;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public abstract class ExtendedAbstractITreeClassifier extends AbstractITreeClassifier implements ExtendedTreeClassifier {
	protected final Map<Tree,Action> dstMmTrees = new HashMap<>();
	protected final Map<Tree, Action> srcMmTrees = new HashMap<>();
	protected final Map<Tree, Action> dstMoveInTreeMap = new HashMap<>();
	protected final Map<Tree, Action> srcMoveOutTreeMap = new HashMap<>();

	public ExtendedAbstractITreeClassifier(ASTDiff diff) {
		super(diff);
	}

	public Map<Tree, Action> getMultiMapSrc() { return srcMmTrees;}
	public Map<Tree, Action> getMultiMapDst() { return dstMmTrees;}
	public Map<Tree, Action> getDstMoveInTreeMap() { return dstMoveInTreeMap; }
	public Map<Tree, Action> getSrcMoveOutTreeMap() { return srcMoveOutTreeMap; }
}
