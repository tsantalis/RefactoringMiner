package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.matchers.*;
import org.refactoringminer.astDiff.matchers.vanilla.CustomBottomUpMatcher;
import org.refactoringminer.astDiff.matchers.vanilla.CustomTopDownMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;


/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class BasicTreeMatcher implements TreeMatcher {

	private int minP = 0;
	public void setMinP(int minP) {
		this.minP = minP;
	}

	public int getMinP() {
		return minP;
	}

	@Override
	public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		basicMatcher(src, dst, mappingStore);
	}

	private void basicMatcher(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		mappingStore.add(process(src, dst));
	}

	public MappingStore process(Tree src, Tree dst) {
		MappingStore match = new CustomTopDownMatcher(minP).match(src, dst);
		new CustomBottomUpMatcher().match(src, dst, match);
		optimizeMappings(match);
		return match;
	}

	private static void optimizeMappings(MappingStore match) {
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
						Tree srcMethodInvocationArguments = TreeUtilFunctions.findChildByType(mapping.first, Constants.METHOD_INVOCATION_ARGUMENTS);
						Tree dstMethodInvocationArguments = TreeUtilFunctions.findChildByType(mapping.second, Constants.METHOD_INVOCATION_ARGUMENTS);
						boolean _notEmptyIsoStructuralArguments = false;
						if (srcMethodInvocationArguments != null && dstMethodInvocationArguments != null)
							_notEmptyIsoStructuralArguments = srcMethodInvocationArguments.isIsoStructuralTo(dstMethodInvocationArguments);
						if (!_notEmptyIsoStructuralArguments) {
							removeList.add(new Pair<>(mapping.first, mapping.second));
							removeList.add(new Pair<>(srcMethodName, dstMethodName));
							removeList.add(new Pair<>(srcMethodInvocationArguments, dstMethodInvocationArguments));
						}
					}
				}
			}
		}
		for (Pair<Tree, Tree> treeTreePair : removeList) {
			if (match.getDstForSrc(treeTreePair.first) != null && match.getDstForSrc(treeTreePair.first).equals(treeTreePair.second))
				match.removeMapping(treeTreePair.first, treeTreePair.second);
		}

		List<Pair<Tree, Tree>> addList = new ArrayList<>();
		for (Mapping mapping : match)
		{
			if (mapping.first.getType().name.equals(Constants.SIMPLE_NAME))
			{
				if (mapping.first.getParent().getType().name.equals(Constants.METHOD_INVOCATION)
						&&
						mapping.second.getParent().getType().name.equals(Constants.METHOD_INVOCATION))
				{
					if (match.getDstForSrc(mapping.first.getParent()) != mapping.second.getParent())
						addList.add(new Pair<>(mapping.first.getParent() , mapping.second.getParent()));
				}
			}
			if (mapping.first.getType().name.equals(Constants.SIMPLE_TYPE))
			{
				if (mapping.first.getParent().getType().name.equals(Constants.CLASS_INSTANCE_CREATION)
					&&
						mapping.second.getParent().getType().name.equals(Constants.CLASS_INSTANCE_CREATION))
				{
					if (match.getDstForSrc(mapping.first.getParent()) != mapping.second.getParent())
						addList.add(new Pair<>(mapping.first.getParent() , mapping.second.getParent()));
				}
			}
		}
		for (Pair<Tree, Tree> treeTreePair : addList) {
			match.removeMapping(treeTreePair.first, match.getSrcForDst(treeTreePair.first));
			match.removeMapping(match.getSrcForDst(treeTreePair.second), treeTreePair.second);
			Tree srcMIR = TreeUtilFunctions.findChildByType(treeTreePair.first, Constants.METHOD_INVOCATION_RECEIVER);
			Tree dstMIR = TreeUtilFunctions.findChildByType(treeTreePair.second, Constants.METHOD_INVOCATION_RECEIVER);
			if (match.isSrcMapped(srcMIR)) {
				match.removeMapping(srcMIR, match.getDstForSrc(srcMIR));
			}
			if (match.isDstMapped(dstMIR)) {
				match.removeMapping(match.getSrcForDst(dstMIR), dstMIR);
			}
		}
		for (Pair<Tree, Tree> treeTreePair : addList) {
			match.addMapping(treeTreePair.first, treeTreePair.second);
			Tree srcMIR = TreeUtilFunctions.findChildByType(treeTreePair.first, Constants.METHOD_INVOCATION_RECEIVER);
			Tree dstMIR = TreeUtilFunctions.findChildByType(treeTreePair.second, Constants.METHOD_INVOCATION_RECEIVER);
			if (srcMIR != null && dstMIR != null)
				match.addMapping(srcMIR, dstMIR);
		}

	}
}
