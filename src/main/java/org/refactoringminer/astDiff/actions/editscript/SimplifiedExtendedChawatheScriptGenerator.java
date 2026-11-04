package org.refactoringminer.astDiff.actions.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

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

	//an insert (delete) whose parent is inserted (deleted) with all its descendants is dropped, and an insert (delete) of a tree with all its descendants becomes a tree insert (delete);
	//whether a tree has all its descendants inserted (deleted) is computed once per tree, bottom-up, and the script is rebuilt in one pass
	private static EditScript simplify(EditScript actions) {
		Map<Tree, Action> addedTrees = new HashMap<>();
		Map<Tree, Action> deletedTrees = new HashMap<>();
		for (Action a: actions)
			if (a instanceof Insert)
				addedTrees.put(a.getNode(), a);
			else if (a instanceof Delete)
				deletedTrees.put(a.getNode(), a);

		Set<Action> removed = Collections.newSetFromMap(new IdentityHashMap<>());
		Map<Action, Action> replaced = new IdentityHashMap<>();
		Map<Tree, Boolean> addedDescendants = descendantsInKeys(addedTrees);
		for (Map.Entry<Tree, Action> entry : addedTrees.entrySet()) {
			Tree t = entry.getKey();
			if (addedTrees.containsKey(t.getParent()) && addedDescendants.get(t.getParent()))
				removed.add(entry.getValue());
			else if (t.getChildren().size() > 0 && addedDescendants.get(t)) {
				Insert originalAction = (Insert) entry.getValue();
				replaced.put(originalAction, new TreeInsert(originalAction.getNode(), originalAction.getParent(), originalAction.getPosition()));
			}
		}
		Map<Tree, Boolean> deletedDescendants = descendantsInKeys(deletedTrees);
		for (Map.Entry<Tree, Action> entry : deletedTrees.entrySet()) {
			Tree t = entry.getKey();
			if (deletedTrees.containsKey(t.getParent()) && deletedDescendants.get(t.getParent()))
				removed.add(entry.getValue());
			else if (t.getChildren().size() > 0 && deletedDescendants.get(t))
				replaced.put(entry.getValue(), new TreeDelete(entry.getValue().getNode()));
		}
		if (removed.isEmpty() && replaced.isEmpty())
			return actions;
		EditScript simplified = new EditScript();
		for (Action a : actions)
			if (!removed.contains(a))
				simplified.add(replaced.getOrDefault(a, a));
		return simplified;
	}

	//for every tree of the map, whether all its descendants are keys of the map, i.e., all its children are keys whose descendants are all keys
	private static Map<Tree, Boolean> descendantsInKeys(Map<Tree, Action> trees) {
		Map<Tree, Boolean> result = new IdentityHashMap<>();
		Map<Tree, Boolean> allInKeys = new IdentityHashMap<>();
		for (Tree t : trees.keySet()) {
			descendantsInKeys(t, trees, allInKeys, result);
			if (t.getParent() != null && trees.containsKey(t.getParent()))
				descendantsInKeys(t.getParent(), trees, allInKeys, result);
		}
		return result;
	}

	//post-order without recursion, since the trees of long expressions are deep
	private static boolean descendantsInKeys(Tree root, Map<Tree, Action> trees, Map<Tree, Boolean> allInKeys, Map<Tree, Boolean> result) {
		Boolean known = result.get(root);
		if (known != null)
			return known;
		Deque<Tree> stack = new ArrayDeque<>();
		Deque<Tree> order = new ArrayDeque<>();
		stack.push(root);
		while (!stack.isEmpty()) {
			Tree t = stack.pop();
			if (result.containsKey(t))
				continue;
			order.push(t);
			for (Tree child : t.getChildren())
				if (!result.containsKey(child))
					stack.push(child);
		}
		while (!order.isEmpty()) {
			Tree t = order.pop();
			if (result.containsKey(t))
				continue;
			boolean descendants = true;
			for (Tree child : t.getChildren())
				if (!allInKeys.get(child)) {
					descendants = false;
					break;
				}
			result.put(t, descendants);
			allInKeys.put(t, descendants && trees.containsKey(t));
		}
		return result.get(root);
	}

}
