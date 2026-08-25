package org.refactoringminer.astDiff.actions.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class MultiMoveActionGenerator implements ExtendedEditScriptGenerator {
	protected Map<Tree, List<MultiMove>> actionMapSrc = new HashMap<>();
	protected Map<Tree, List<MultiMove>> actionMapDst = new HashMap<>();
	private final List<Action> actions = new ArrayList<>();
	private static int counter = 0;

	@Override
	public EditScript computeActions(ExtendedMultiMappingStore mappings, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
		addMultiMappings(mappings);
		return toEditScript(actions);
	}

	private void addMultiMappings(ExtendedMultiMappingStore mappings) {
		Map<Tree, Set<Tree>> dstToSrcMulti = mappings.dstToSrcMultis();
		for (Map.Entry<Tree, Set<Tree>> entry : dstToSrcMulti.entrySet()) {
			Set<Tree> srcTrees = entry.getValue();
			Set<Tree> dstTrees = mappings.getDsts(srcTrees.iterator().next());
			this.addMapping(srcTrees, dstTrees);
		}
	}

	public void addMapping(Set<Tree> srcTrees, Set<Tree> dstTrees) {
		if (isOrderPreservingEquivalentGroup(srcTrees, dstTrees))
			return;
		for (Tree srcTree : srcTrees) {
			for (Tree dstTree : dstTrees) {
				if (!shouldAddMultiMove(srcTree, dstTree, srcTrees, dstTrees))
					continue;
				MultiMove action = new MultiMove(srcTree, dstTree, -1, counter + 1, false);
				if (!actions.contains(action)) {
					actions.add(action);
					actionMapSrc.computeIfAbsent(srcTree, key -> new ArrayList<>()).add(action);
					actionMapDst.computeIfAbsent(dstTree, key -> new ArrayList<>()).add(action);
				}
			}
		}
		counter += 1;
	}

	private boolean shouldAddMultiMove(Tree srcTree, Tree dstTree, Set<Tree> srcTrees, Set<Tree> dstTrees) {
		return srcTree != null
				&& dstTree != null
				&& srcTree.getMetrics().hash == dstTree.getMetrics().hash
				&& isComparableMultiMove(srcTree, dstTree)
				&& !hasMatchingAlternative(srcTree, dstTrees, dstTree)
				&& !hasMatchingAlternative(dstTree, srcTrees, srcTree);
	}

	private boolean isOrderPreservingEquivalentGroup(Set<Tree> srcTrees, Set<Tree> dstTrees) {
		if (srcTrees.size() != dstTrees.size() || srcTrees.isEmpty())
			return false;
		List<Tree> sortedSrcTrees = sortedByPosition(srcTrees);
		List<Tree> sortedDstTrees = sortedByPosition(dstTrees);
		for (int i = 0; i < sortedSrcTrees.size(); i++) {
			Tree srcTree = sortedSrcTrees.get(i);
			Tree dstTree = sortedDstTrees.get(i);
			if (srcTree == null || dstTree == null)
				return false;
			if (!sameType(srcTree, dstTree) || srcTree.getMetrics().hash != dstTree.getMetrics().hash)
				return false;
		}
		return true;
	}

	private List<Tree> sortedByPosition(Set<Tree> trees) {
		List<Tree> sortedTrees = new ArrayList<>(trees);
		sortedTrees.sort(Comparator.comparingInt(Tree::getPos).thenComparingInt(Tree::getEndPos));
		return sortedTrees;
	}

	private boolean isComparableMultiMove(Tree srcTree, Tree dstTree) {
		if (!sameType(srcTree, dstTree) || !sameParentType(srcTree, dstTree))
			return false;
		if (srcTree.isLeaf() || dstTree.isLeaf())
			return Objects.equals(srcTree.getLabel(), dstTree.getLabel());
		return true;
	}

	private boolean hasMatchingAlternative(Tree tree, Set<Tree> candidates, Tree excludedCandidate) {
		for (Tree candidate : candidates) {
			if (candidate == excludedCandidate)
				continue;
			if (sameType(tree, candidate) && tree.getMetrics().hash == candidate.getMetrics().hash)
				return true;
		}
		return false;
	}

	private boolean sameParentType(Tree srcTree, Tree dstTree) {
		Tree srcParent = srcTree.getParent();
		Tree dstParent = dstTree.getParent();
		return srcParent != null && dstParent != null && sameType(srcParent, dstParent);
	}

	private boolean sameType(Tree srcTree, Tree dstTree) {
		return Objects.equals(srcTree.getType(), dstTree.getType());
	}

	protected static EditScript toEditScript(List<Action> actions) {
		EditScript result = new EditScript();
		for (Action action : actions) {
			result.add(action);
		}
		return result;
	}
}
