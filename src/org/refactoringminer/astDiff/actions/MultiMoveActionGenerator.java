package org.refactoringminer.astDiff.actions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.actions.model.MultiMove;

import java.util.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class MultiMoveActionGenerator {
	private Map<Set<Tree>, Set<Tree>> fullMap;
	private Map<Tree,List<MultiMove>> actionMapSrc = new HashMap<>();
	private Map<Tree,List<MultiMove>> actionMapDst = new HashMap<>();

	ArrayList<Action> actions;
	private static int counter = 0;

	public MultiMoveActionGenerator() {
		fullMap = new LinkedHashMap<>();
		actions = new ArrayList<>();
	}

	public ArrayList<Action> generate() {
		//return actions;
		return simplify(actions);
	}

	private ArrayList<Action> simplify(ArrayList<Action> actions) {
		for (Tree t : actionMapSrc.keySet()) {
			if (actionMapSrc.containsKey(t.getParent())
					&& actionMapSrc.keySet().containsAll(t.getParent().getDescendants()))
				removeActionsForThisTreeFromSrc(t);
			else {
				if (t.getChildren().size() > 0 && actionMapSrc.keySet().containsAll(t.getDescendants())) {

				}
			}
		}
		for (Tree t : actionMapDst.keySet()) {
			if (actionMapDst.containsKey(t.getParent())
					&& actionMapDst.keySet().containsAll(t.getParent().getDescendants()))
				removeActionsForThisTreeFromDst(t);
			else {
				if (t.getChildren().size() > 0 && actionMapDst.keySet().containsAll(t.getDescendants())) {
				}
			}
		}
		return actions;
	}

	private void removeActionsForThisTreeFromSrc(Tree t) {
		List<MultiMove> mappedSrc = actionMapSrc.get(t);
		List<Action> removable = new ArrayList<>();
		boolean _flag = false;
		for(MultiMove action : mappedSrc) {
			if (action.isUpdated())
			{
				_flag = true;
				break;
			}
			else {
				Tree actionSrc = action.getNode();
				Tree actionDst = action.getParent();
				if (       actionMapSrc.containsKey(actionSrc.getParent())
						&& actionMapSrc.keySet().containsAll(actionSrc.getParent().getDescendants())
						&& actionMapDst.containsKey(actionDst.getParent())
						&& actionMapDst.keySet().containsAll(actionDst.getParent().getDescendants()))
					removable.add(action);
				else {
					_flag = true;
					break;
				}
			}
		}
		if (!_flag)
			actions.removeAll(removable);
	}

	private void removeActionsForThisTreeFromDst(Tree t) {
		List<MultiMove> mappedDst = actionMapDst.get(t);
		List<Action> removable = new ArrayList<>();
		boolean _flag = false;
		for(MultiMove action : mappedDst) {
			if (action.isUpdated()) {
				_flag = true;
				break;
			}
			else {
				Tree actionSrc = action.getNode();
				Tree actionDst = action.getParent();
				if (       actionMapSrc.containsKey(actionSrc.getParent())
						&& actionMapSrc.keySet().containsAll(actionSrc.getParent().getDescendants())
						&& actionMapDst.containsKey(actionDst.getParent())
						&& actionMapDst.keySet().containsAll(actionDst.getParent().getDescendants()))
					removable.add(action);
				else {
					_flag = true;
					break;
				}
			}
		}
		if (_flag)
			actions.removeAll(removable);
	}

	public void addMapping(Set<Tree> srcTrees, Set<Tree> dstTrees) {
		this.fullMap.put(srcTrees,dstTrees);
		for (Tree srcTree : srcTrees)
		{
			for (Tree dstTree : dstTrees)
			{
				boolean updated = false;
				if (srcTree.isLeaf() && dstTree.isLeaf())
					updated = (srcTree.getMetrics().hash != dstTree.getMetrics().hash);
				MultiMove action = new MultiMove(srcTree,dstTree,-1, counter + 1,updated);
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
}
