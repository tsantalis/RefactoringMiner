package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class LeafMatcher implements TreeMatcher {
	private final boolean overwrite;

	public LeafMatcher(boolean overwrite) {
		this.overwrite = overwrite;
	}

	@Override
	public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		//if (abstractCodeMapping != null)
		//	if (abstractCodeMapping.getFragment1() instanceof AbstractExpression || abstractCodeMapping.getFragment2() instanceof AbstractExpression)
		//		return;
		if (src == null || dst == null) return;
		Map<Tree,Tree> srcCopy = new HashMap<>();
		Map<Tree,Tree> dstCopy = new HashMap<>();
		Pair<Tree, Tree> prunedPair = pruneTrees(src, dst, srcCopy, dstCopy);
		MappingStore match;
		try {
			if (prunedPair.first.isIsoStructuralTo(prunedPair.second))
			{
				match = new MappingStore(src, dst);
				match.addMappingRecursively(prunedPair.first, prunedPair.second);
			}
			else {
				match = extracted(prunedPair.first, prunedPair.second);
			}
			if (!overwrite)
				mappingStore.addWithMaps(match, srcCopy, dstCopy);
			else
				mappingStore.replaceWithMaps(match, srcCopy, dstCopy);
		}
		catch (Exception exception)
		{
			System.out.println(exception.getMessage());
		}
	}

	public static MappingStore extracted(Tree src, Tree dst) {
		MappingStore match;
		match = new CustomGreedy(0, false).match(src, dst);
		CustomBottomUpMatcher customBottomUpMatcher = new CustomBottomUpMatcher();
		customBottomUpMatcher.match(src, dst, match);
		List<Pair<Tree, Tree>> removeList = new ArrayList<>();
		for (Mapping mapping : match) {
			if (mapping.first.getType().name.equals(Constants.METHOD_INVOCATION)) {
				Tree srcMethodName = TreeUtilFunctions.findChildByType(mapping.first, Constants.SIMPLE_NAME);
				Tree dstMethodName = TreeUtilFunctions.findChildByType(mapping.second, Constants.SIMPLE_NAME);
				if (srcMethodName == null || dstMethodName == null) continue;
				if (!srcMethodName.getLabel().equals(dstMethodName.getLabel())) {
					Tree srcMethodInvocationReceiver = TreeUtilFunctions.findChildByType(mapping.first, Constants.METHOD_INVOCATION_RECEIVER);
					Tree dstMethodInvocationReceiver = TreeUtilFunctions.findChildByType(mapping.second, Constants.METHOD_INVOCATION_RECEIVER);
					if ((srcMethodInvocationReceiver == null && dstMethodInvocationReceiver != null)
							||
							(srcMethodInvocationReceiver != null && dstMethodInvocationReceiver == null)) {
						removeList.add(new Pair<>(mapping.first, mapping.second));
						removeList.add(new Pair<>(srcMethodName, dstMethodName));
						break;
					}
				}
			}
		}
		for (Pair<Tree, Tree> treeTreePair : removeList) {
			match.removeMapping(treeTreePair.first, treeTreePair.second);
		}
		return match;
	}

	@Override
	public void match(Tree src, Tree dst, AbstractCodeFragment abstractCodeFragment1, AbstractCodeFragment abstractCodeFragment2, ExtendedMultiMappingStore mappingStore) {
		this.match(src,dst, new LeafMapping(abstractCodeFragment1, abstractCodeFragment2, null, null), mappingStore);
	}

	public Pair<Tree,Tree> pruneTrees(Tree src, Tree dst, Map<Tree,Tree> srcCopy, Map<Tree,Tree> dstCopy) {
		Tree prunedSrc = TreeUtilFunctions.deepCopyWithMapPruning(src,srcCopy);
		Tree prunedDst = TreeUtilFunctions.deepCopyWithMapPruning(dst,dstCopy);
		return new Pair<>(prunedSrc,prunedDst);
	}

	private void specialCases(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		String EXP_STATEMENT =  Constants.EXPRESSION_STATEMENT;
		String VAR_DEC_STATEMENT = Constants.VARIABLE_DECLARATION_STATEMENT;
		Tree expTree,varTree;
		boolean expFirst;
		Tree assignment_operator = null;
		Tree assignment,varFrag;
		assignment = varFrag = null;
		if (src.getType().name.equals(EXP_STATEMENT) && dst.getType().name.equals(VAR_DEC_STATEMENT))
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
				if (expTree.getChild(0).getType().name.equals(Constants.ASSIGNMENT))
				{
					assignment = expTree.getChild(0);
					for(Tree child : assignment.getChildren())
					{
						if (child.getType().name.equals(Constants.ASSIGNMENT_OPERATOR) && child.getLabel().equals(Constants.EQUAL_OPERATOR))
						{
							assignment_operator = child;
							break;
						}
					}
				}
			}
		}
		else if (src.getType().name.equals(VAR_DEC_STATEMENT) && dst.getType().name.equals(EXP_STATEMENT))
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
				if (expTree.getChild(0).getType().name.equals(Constants.ASSIGNMENT))
				{
					assignment = expTree.getChild(0);
					for(Tree child : assignment.getChildren())
					{
						if (child.getType().name.equals(Constants.ASSIGNMENT_OPERATOR) && child.getLabel().equals(Constants.EQUAL_OPERATOR))
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
}
