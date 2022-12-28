
package org.refactoringminer.astDiff.actions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;
import java.util.Set;

/**
 * An interface to partition the nodes of an AST into sets of updated, deleted, moved,
 * and updated nodes.
 * @see Tree
 */
public interface TreeClassifier {
    /**
     * Return the set of updated nodes in the source AST.
     */
    Set<Tree> getUpdatedSrcs();

    /**
     * Return the set of deleted nodes in the source AST.
     */
    Set<Tree> getDeletedSrcs();

    /**
     * Return the set of moved nodes in the source AST.
     */
    Set<Tree> getMovedSrcs();

    /**
     * Return the set of updated nodes in the destination AST.
     */
    Set<Tree> getUpdatedDsts();

    /**
     * Return the set of inserted nodes in the destination AST.
     */
    Set<Tree> getInsertedDsts();

    /**
     * Return the set of moved nodes in the destination AST.
     */
    Set<Tree> getMovedDsts();


    Map<Tree, Action> getMultiMapSrc();

    Map<Tree, Action> getMultiMapDst();

    Map<Tree, Action> getDstMoveInTreeMap();

    Map<Tree, Action> getSrcMoveOutTreeMap();

}
