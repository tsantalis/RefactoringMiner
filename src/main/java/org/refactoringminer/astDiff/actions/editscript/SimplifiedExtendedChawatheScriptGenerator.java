package org.refactoringminer.astDiff.actions.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.actions.model.MoveIn;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.HashMap;
import java.util.Map;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class SimplifiedExtendedChawatheScriptGenerator implements ExtendedEditScriptGenerator {

	@Override
	public EditScript computeActions(ExtendedMultiMappingStore ms, Map<String, TreeContext> parentContextMap, Map<String, TreeContext> childContextMap) {
		EditScript actions = new ExtendedChawatheScriptGenerator().computeActions(ms,parentContextMap,childContextMap);
		//return actions;
		return simplify(actions);
	}

	private static EditScript simplify(EditScript actions) {
		Map<Tree, Action> addedTrees = new HashMap<>();
		Map<Tree, Action> deletedTrees = new HashMap<>();
		Map<Tree, Action> MoveInTrees = new HashMap<>();
		Map<Tree, Action> MoveOutTrees = new HashMap<>();

		for (Action a: actions)
			if (a instanceof Insert)
				addedTrees.put(a.getNode(), a);
			else if (a instanceof Delete)
				deletedTrees.put(a.getNode(), a);
			else if (a instanceof MoveIn)
				MoveInTrees.put(a.getNode(), a);
			else if (a instanceof MoveOut)
				MoveOutTrees.put(a.getNode(), a);

		for (Tree t : addedTrees.keySet()) {
			if (addedTrees.containsKey(t.getParent())
					&& addedTrees.keySet().containsAll(t.getParent().getDescendants()))
				actions.remove(addedTrees.get(t));
			else {
				if (t.getChildren().size() > 0 && addedTrees.keySet().containsAll(t.getDescendants())) {
					Insert originalAction = (Insert) addedTrees.get(t);
					TreeInsert ti = new TreeInsert(originalAction.getNode(),
							originalAction.getParent(), originalAction.getPosition());
					int index = actions.lastIndexOf(originalAction);
					actions.add(index, ti);
					actions.remove(index +  1);
				}
			}
		}

		for (Tree t : deletedTrees.keySet()) {
			if (deletedTrees.containsKey(t.getParent())
					&& deletedTrees.keySet().containsAll(t.getParent().getDescendants()))
				actions.remove(deletedTrees.get(t));
			else {
				if (t.getChildren().size() > 0 && deletedTrees.keySet().containsAll(t.getDescendants())) {
					Delete originalAction = (Delete) deletedTrees.get(t);
					TreeDelete ti = new TreeDelete(originalAction.getNode());
					int index = actions.lastIndexOf(originalAction);
					actions.add(index, ti);
					actions.remove(index +  1);
				}
			}
		}
//		for (Tree t : MoveInTrees.keySet()) {
//			if (MoveInTrees.containsKey(t.getParent())
//					&& MoveInTrees.keySet().containsAll(t.getParent().getDescendants())) {
//				actions.remove(MoveInTrees.get(t));
//			}
//			else {
//				if (!t.getChildren().isEmpty() && deletedTrees.keySet().containsAll(t.getDescendants())) {
//					//TODO:
//				}
//			}
//		}
//		for (Tree t : MoveOutTrees.keySet()) {
//			if (MoveOutTrees.containsKey(t.getParent())
//					&& MoveOutTrees.keySet().containsAll(t.getParent().getDescendants())) {
//				actions.remove(MoveOutTrees.get(t));
//			}
//			else {
//				if (!t.getChildren().isEmpty() && deletedTrees.keySet().containsAll(t.getDescendants())) {
//					//TODO:
//				}
//			}
//		}
		return actions;
	}
}
