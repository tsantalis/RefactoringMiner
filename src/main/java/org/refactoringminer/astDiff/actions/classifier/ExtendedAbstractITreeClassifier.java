package org.refactoringminer.astDiff.actions.classifier;

import java.util.*;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ASTDiff;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public abstract class ExtendedAbstractITreeClassifier implements ExtendedTreeClassifier {
	protected final Diff diff;
	protected final Set<Tree> srcUpdTrees = new HashSet<>();
	protected final Set<Tree> dstUpdTrees = new HashSet<>();
	protected final Set<Tree> srcMvTrees = new HashSet<>();
	protected final Set<Tree> dstMvTrees = new HashSet<>();
	protected final Set<Tree> srcDelTrees = new HashSet<>();
	protected final Set<Tree> dstAddTrees = new HashSet<>();
	protected final Map<Tree, List<Action>> dstMmTrees = new HashMap<>();
	protected final Map<Tree, List<Action>> srcMmTrees = new HashMap<>();
	protected final Map<Tree, List<Action>> dstMoveInTreeMap = new HashMap<>();
	protected final Map<Tree, List<Action>> srcMoveOutTreeMap = new HashMap<>();

	public ExtendedAbstractITreeClassifier(ASTDiff diff) {
		this.diff = diff;
	}

	protected abstract void classify();

	public Set<Tree> getUpdatedSrcs() {
		return srcUpdTrees;
	}

	public Set<Tree> getUpdatedDsts() {
		return dstUpdTrees;
	}

	public Set<Tree> getMovedSrcs() {
		return srcMvTrees;
	}

	public Set<Tree> getMovedDsts() {
		return dstMvTrees;
	}

	public Set<Tree> getDeletedSrcs() {
		return srcDelTrees;
	}

	public Set<Tree> getInsertedDsts() {
		return dstAddTrees;
	}

	public Map<Tree, List<Action>> getMultiMapSrc() {
		return srcMmTrees;
	}

	public Map<Tree, List<Action>> getMultiMapDst() {
		return dstMmTrees;
	}

	public Map<Tree, List<Action>> getDstMoveInTreeMap() {
		return dstMoveInTreeMap;
	}

	public Map<Tree, List<Action>> getSrcMoveOutTreeMap() {
		return srcMoveOutTreeMap;
	}
}
