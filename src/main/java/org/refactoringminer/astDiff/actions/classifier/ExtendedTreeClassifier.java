package org.refactoringminer.astDiff.actions.classifier;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * An interface to partition the nodes of an AST into sets of updated, deleted, moved, and updated nodes.
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @see Tree
 */
public interface ExtendedTreeClassifier extends TreeClassifier {
	Map<Tree, Action> getMultiMapSrc();
	Map<Tree, Action> getMultiMapDst();
	Map<Tree, Action> getDstMoveInTreeMap();
	Map<Tree, Action> getSrcMoveOutTreeMap();
}
