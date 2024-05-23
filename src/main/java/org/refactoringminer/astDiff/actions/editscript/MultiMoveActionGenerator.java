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
		for (Tree srcTree : srcTrees) {
			if (srcTree == null)
				continue;
			for (Tree dstTree : dstTrees) {
				if (dstTree == null)
					continue;
				boolean updated = false;
				if (srcTree.isLeaf() && dstTree.isLeaf())
					updated = (srcTree.getMetrics().hash != dstTree.getMetrics().hash);
				MultiMove action = new MultiMove(srcTree, dstTree, -1, counter + 1, updated);
				if (!actions.contains(action)) {
					actions.add(action);
					if (!actionMapSrc.containsKey(srcTree))
						actionMapSrc.put(srcTree, new ArrayList<>());
					if (!actionMapDst.containsKey(dstTree))
						actionMapDst.put(dstTree, new ArrayList<>());

					actionMapSrc.get(srcTree).add(action);
					actionMapDst.get(dstTree).add(action);
				}
			}
		}
		counter += 1;
	}

	protected static EditScript toEditScript(List<Action> actions) {
		EditScript result = new EditScript();
		for (Action action : actions) {
			result.add(action);
		}
		return result;
	}
}
