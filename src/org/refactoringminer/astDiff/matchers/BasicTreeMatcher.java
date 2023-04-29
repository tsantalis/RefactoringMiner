package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.List;


/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class BasicTreeMatcher implements TreeMatcher {
	@Override
	public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		basicMatcher(src, dst, mappingStore);
	}

	private void basicMatcher(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		mappingStore.add(process(src, dst));
	}

	public static MappingStore process(Tree src, Tree dst) {
		MappingStore match;
		match = new CustomGreedy(0).match(src, dst);
		CustomBottomUpMatcher customBottomUpMatcher = new CustomBottomUpMatcher();
		customBottomUpMatcher.match(src, dst, match);
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
						}
					}
				}
			}
		}
		for (Pair<Tree, Tree> treeTreePair : removeList) {
			match.removeMapping(treeTreePair.first, treeTreePair.second);
		}
	}
}
