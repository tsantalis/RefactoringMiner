package org.refactoringminer.astDiff.actions.classifier;

import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.List;
import java.util.Map;

/**
 * An interface to partition the nodes of an AST into sets of updated, deleted, moved, and updated nodes.
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @see Tree
 */
public interface ExtendedTreeClassifier extends TreeClassifier {
	Map<Tree, List<Action>> getMultiMapSrc();
    Map<Tree, List<Action>> getMultiMapDst();
    Map<Tree, List<Action>> getDstMoveInTreeMap();
    Map<Tree, List<Action>> getSrcMoveOutTreeMap();
}
