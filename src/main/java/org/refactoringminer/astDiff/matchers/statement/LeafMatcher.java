package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.optimizations.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.refactoringminer.astDiff.matchers.vanilla.FixedLeafMoveMatcherThetaE;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.refactoringminer.astDiff.matchers.statement.LRAUtils.LRAify;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class LeafMatcher extends BasicTreeMatcher implements TreeMatcher {
	private boolean overwrite = false;
	public LeafMatcher(Constants LANG1, Constants LANG2) {
		super(LANG1, LANG2);
	}

	public LeafMatcher setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
		return this;
	}

	@Override
	public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		if (src == null || dst == null) return;
		Map<Tree,Tree> srcCopy = new HashMap<>();
		Map<Tree,Tree> dstCopy = new HashMap<>();
		Pair<Tree, Tree> prunedPair = pruneTrees(src, dst, srcCopy, dstCopy);
		MappingStore match;
		try
        {
			if (prunedPair.first.isIsoStructuralTo(prunedPair.second))
			{
				if (!TreeUtilFunctions.isIsomorphicTo(prunedPair.first,prunedPair.second))
				{
					match = new LRAMoveOptimizedIsomorphic().match(prunedPair.first, prunedPair.second);
				}
				else{
					match = new MappingStore(src, dst);
					match.addMappingRecursively(prunedPair.first, prunedPair.second);
				}
			}
			else {
				match = process(prunedPair.first, prunedPair.second);
			}
			if (!overwrite)
				mappingStore.addWithMaps(match, srcCopy, dstCopy);
			else
				mappingStore.replaceWithMaps(match, srcCopy, dstCopy);
		}
		catch (Exception exception)
		{
//			TODO: ADD ERR LOGGING
            System.out.println(exception.getMessage());
        }
	}
	public Pair<Tree,Tree> pruneTrees(Tree src, Tree dst, Map<Tree,Tree> srcCopy, Map<Tree,Tree> dstCopy) {
		Tree prunedSrc = TreeUtilFunctions.deepCopyWithMapPruning(src,srcCopy,LANG1);
		Tree prunedDst = TreeUtilFunctions.deepCopyWithMapPruning(dst,dstCopy,LANG2);
		return new Pair<>(prunedSrc,prunedDst);
	}

	private void specialCases(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		String EXP_STATEMENT_1 = LANG1.EXPRESSION_STATEMENT;
		String VAR_DEC_STATEMENT_1 = LANG1.VARIABLE_DECLARATION_STATEMENT;
		String EXP_STATEMENT_2 = LANG2.EXPRESSION_STATEMENT;
		String VAR_DEC_STATEMENT_2 = LANG2.VARIABLE_DECLARATION_STATEMENT;
		Tree expTree,varTree;
		boolean expFirst;
		Tree assignment_operator = null;
		Tree assignment,varFrag;
		assignment = varFrag = null;
		if (src.getType().name.equals(EXP_STATEMENT_1) && dst.getType().name.equals(VAR_DEC_STATEMENT_2))
		{
			expTree = src;
			varTree = dst;
			expFirst = true;
			if (varTree.getChildren().size() > 1)
			{
				varFrag = varTree.getChild(1);
			}
			if (expTree.getChildren().size() > 0)
			{
				if (expTree.getChild(0).getType().name.equals(LANG1.ASSIGNMENT))
				{
					assignment = expTree.getChild(0);
					for(Tree child : assignment.getChildren())
					{
						if (child.getType().name.equals(LANG1.ASSIGNMENT_OPERATOR) && child.getLabel().equals(LANG1.EQUAL_OPERATOR))
						{
							assignment_operator = child;
							break;
						}
					}
				}
			}
		}
		else if (src.getType().name.equals(VAR_DEC_STATEMENT_1) && dst.getType().name.equals(EXP_STATEMENT_2))
		{
			expTree = dst;
			varTree = src;
			expFirst = false;
			if (varTree.getChildren().size() > 1)
			{
				varFrag = varTree.getChild(1);
			}
			if (expTree.getChildren().size() > 0)
			{
				if (expTree.getChild(0).getType().name.equals(LANG2.ASSIGNMENT))
				{
					assignment = expTree.getChild(0);
					for(Tree child : assignment.getChildren())
					{
						if (child.getType().name.equals(LANG2.ASSIGNMENT_OPERATOR) && child.getLabel().equals(LANG2.EQUAL_OPERATOR))
						{
							assignment_operator = child;
							break;
						}
					}
				}
			}
		}
		else
		{
			//TODO : nothing for now;
			return;
		}
		if (expFirst)
		{
			mappingStore.addMapping(assignment,varFrag);
			mappingStore.addMapping(expTree,varTree);
			mappingStore.addMapping(assignment_operator, TreeUtilFunctions.getFakeTreeInstance());
		}
		else {
			mappingStore.addMapping(varFrag,assignment);
			mappingStore.addMapping(varTree,expTree);
			mappingStore.addMapping(TreeUtilFunctions.getFakeTreeInstance(),assignment_operator);
		}
	}
	static class MoveOptimizedIsomorphic extends CompositeMatchers.CompositeMatcher {
		public MoveOptimizedIsomorphic() {
			super(
					(src, dst, mappings) -> {
						if (TreeUtilFunctions.isIsomorphicTo(src,dst))
							mappings.addMappingRecursively(src,dst);
						return mappings;
					}
					, new IdenticalSubtreeMatcherThetaA()
					, new LcsOptMatcherThetaB()
					, new UnmappedLeavesMatcherThetaC()
					, new InnerNodesMatcherThetaD()
					, new FixedLeafMoveMatcherThetaE()
					, new SafeCrossMoveMatcherThetaF()
			);
		}
	}

    //LeftRightAware (LRA) matcher version of MTD
    class LRAMoveOptimizedIsomorphic extends MoveOptimizedIsomorphic {
        @Override
        public MappingStore match(Tree src, Tree dst) {
            return match(src, dst, new MappingStore(src, dst));
        }

        @Override
        public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
            List<Pair<Tree, Tree>> pairs = LRAify(src, dst, mappings, LANG1, LANG2);
            for (Pair<Tree, Tree> pair : pairs) {
                super.match(pair.first, pair.second, mappings);
            }
            return mappings;
        }
    }
    static class SafeCrossMoveMatcherThetaF extends CrossMoveMatcherThetaF {
        @Override
        public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
            try {
                return super.match(src, dst, mappings);
            }
            catch (Exception e) {
                // Handle the exception gracefully, e.g., log it or ignore it
                return mappings;
            }
        }
    }

}

