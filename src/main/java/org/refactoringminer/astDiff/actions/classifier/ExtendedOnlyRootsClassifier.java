package org.refactoringminer.astDiff.actions.classifier;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.actions.classifier.ExtendedAbstractITreeClassifier;
import org.refactoringminer.astDiff.actions.model.MoveIn;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;

import java.util.HashSet;
import java.util.Set;

/**
 * Partition only root (of a complete subtree) moved, inserted, updated or deleted nodes.
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ExtendedOnlyRootsClassifier extends ExtendedAbstractITreeClassifier {
	public ExtendedOnlyRootsClassifier(ASTDiff diff) {
		super(diff);
	}

	@Override
	public void classify() {
		ASTDiff astDiff = (ASTDiff)diff;
		Set<Tree> insertedDsts = new HashSet<>();
		for (Action a: diff.editScript)
			if (a instanceof Insert)
				insertedDsts.add(a.getNode());

		Set<Tree> deletedSrcs = new HashSet<>();
		for (Action a: diff.editScript)
			if (a instanceof Delete)
				deletedSrcs.add(a.getNode());

		for (Action a: diff.editScript) {
			if (a instanceof TreeDelete)
				srcDelTrees.add(a.getNode());
			else if (a instanceof Delete) {
				if (!(deletedSrcs.containsAll(a.getNode().getDescendants())
						&& deletedSrcs.contains(a.getNode().getParent())))
					srcDelTrees.add(a.getNode());
			}
			else if (a instanceof Insert) {
				if (!(insertedDsts.containsAll(a.getNode().getDescendants())
						&& insertedDsts.contains(a.getNode().getParent())))
					dstAddTrees.add(a.getNode());
			}
			else if (a instanceof TreeInsert)
				dstAddTrees.add(a.getNode());
			else if (a instanceof Update) {
				srcUpdTrees.add(a.getNode());
				dstUpdTrees.addAll(astDiff.getAllMappings().getDsts(a.getNode()));
			}
			else if (a instanceof Move) {
				srcMvTrees.add(a.getNode());
				dstMvTrees.addAll(astDiff.getAllMappings().getDsts(a.getNode()));
			}
			else if (a instanceof MultiMove)
			{
				srcMmTrees.put(a.getNode(),a);
				for (Tree tree : astDiff.getAllMappings().getDsts(a.getNode()))
					dstMmTrees.put(tree,a);
			}
			else if (a instanceof MoveIn)
			{
				dstMoveInTreeMap.put(((MoveIn) a).getParent(),a);
			}
			else if (a instanceof MoveOut)
			{
				srcMoveOutTreeMap.put(((MoveOut) a).getNode(),a);
			}
		}
	}
}
