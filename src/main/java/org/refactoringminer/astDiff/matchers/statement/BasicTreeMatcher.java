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
	protected final Constants LANG1;
	protected final Constants LANG2;
	
	public BasicTreeMatcher(Constants LANG1, Constants LANG2) {
		this.LANG1 = LANG1;
		this.LANG2 = LANG2;
	}

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
        MappingStore match = new MappingStore(src, dst);
        List<Pair<Tree, Tree>> pairs = LRAUtils.LRAify(src, dst, match, LANG1, LANG2);
        for (Pair<Tree, Tree> pair : pairs) {
            MappingStore mappings = new CustomTopDownMatcher(minP, LANG1, LANG2).match(pair.first, pair.second);
            new CustomBottomUpMatcher(LANG1, LANG2).match(pair.first, pair.second, mappings);
            for (Mapping mapping : mappings) {
                match.addMapping(mapping.first, mapping.second);
            }
            if (pair.first.getType().name.equals(LANG1.VARIABLE_DECLARATION_STATEMENT) && pair.second.getType().name.equals(LANG2.SINGLE_VARIABLE_DECLARATION)) {
            	Tree t1 = null;
            	for (Tree child : pair.first.getChildren()) {
            		if (child.getType().name.equals(LANG1.VARIABLE_DECLARATION_FRAGMENT))
            			t1 = TreeUtilFunctions.findChildByType(child, LANG1.SIMPLE_NAME);
            	}
            	Tree t2 = TreeUtilFunctions.findChildByType(pair.second, LANG2.SIMPLE_NAME);
            	if (t1 != null && t2 != null) {
            		match.addMapping(t1, t2);
            	}
            }
        }
        optimizeMappings(match);
		return match;
	}

    private static Tree lastLeaf(Tree tree) {
		while (!tree.isLeaf())
			tree = tree.getChild(tree.getChildren().size() - 1);
		return tree;
	}

    private void optimizeMappings(MappingStore match) {
		List<Pair<Tree, Tree>> removeList = new ArrayList<>();
		List<Pair<Tree, Tree>> incorrectMethodExpressionReferenceSimpleName = new ArrayList<>();
		for (Mapping mapping : match) {
			if (mapping.first.getType().name.equals(LANG1.METHOD_INVOCATION)) {
				Tree srcMethodName = TreeUtilFunctions.findChildByType(mapping.first, LANG1.SIMPLE_NAME);
				Tree dstMethodName = TreeUtilFunctions.findChildByType(mapping.second, LANG2.SIMPLE_NAME);
				if (srcMethodName == null || dstMethodName == null) continue;
				if (!srcMethodName.getLabel().equals(dstMethodName.getLabel())) {
					Tree srcMethodInvocationReceiver = TreeUtilFunctions.findChildByType(mapping.first, LANG1.METHOD_INVOCATION_RECEIVER);
					Tree dstMethodInvocationReceiver = TreeUtilFunctions.findChildByType(mapping.second, LANG2.METHOD_INVOCATION_RECEIVER);
					if ((srcMethodInvocationReceiver == null && dstMethodInvocationReceiver != null)
							||
							(srcMethodInvocationReceiver != null && dstMethodInvocationReceiver == null)) {
						Tree srcMethodInvocationArguments = TreeUtilFunctions.findChildByType(mapping.first, LANG1.METHOD_INVOCATION_ARGUMENTS);
						Tree dstMethodInvocationArguments = TreeUtilFunctions.findChildByType(mapping.second, LANG2.METHOD_INVOCATION_ARGUMENTS);
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
			if (mapping.first.getType().name.equals(LANG1.SIMPLE_NAME)){
				if (mapping.first.getParent() != null && mapping.second.getParent() != null &&
						mapping.first.getParent().getType().name.equals(LANG1.EXPRESSION_METHOD_REFERENCE) && mapping.second.getParent().getType().name.equals(LANG2.EXPRESSION_METHOD_REFERENCE)) {
					if (mapping.first.positionInParent() != mapping.second.positionInParent())
						incorrectMethodExpressionReferenceSimpleName.add(mapping);
				}
			}

		}
		for (Pair<Tree, Tree> treeTreePair : removeList) {
			if (match.getDstForSrc(treeTreePair.first) != null && match.getDstForSrc(treeTreePair.first).equals(treeTreePair.second))
				match.removeMapping(treeTreePair.first, treeTreePair.second);
		}
		for (Pair<Tree, Tree> treeTreePair : incorrectMethodExpressionReferenceSimpleName) {
			match.removeMapping(treeTreePair.first, treeTreePair.second);
			if (treeTreePair.first.getParent().getChild(1).getType().name.equals(treeTreePair.second.getParent().getChild(1).getType().name))
				match.addMapping(treeTreePair.first.getParent().getChild(1), treeTreePair.second.getParent().getChild(1));
		}

		List<Pair<Tree, Tree>> calleeNames = new ArrayList<>();
		for (Mapping mapping : match) {
			//the receiver is removed or added, i.e., raw_client_->WaitForAllocation(...) -> WaitForAllocation(...),
			//so the name is a leaf callee on one side and the last leaf of the member access callee on the other side,
			//i.e., the field_identifier of a C++ field_expression or the simple_identifier of a Kotlin navigation_suffix
			if (mapping.first.getType().name.equals(LANG1.METHOD_INVOCATION) && mapping.second.getType().name.equals(LANG2.METHOD_INVOCATION) &&
					!mapping.first.isLeaf() && !mapping.second.isLeaf()) {
				Tree callee1 = mapping.first.getChild(0);
				Tree callee2 = mapping.second.getChild(0);
				if (match.isSrcMapped(callee1) || match.isDstMapped(callee2) || callee1.isLeaf() == callee2.isLeaf())
					continue;
				Tree name1 = lastLeaf(callee1);
				Tree name2 = lastLeaf(callee2);
				if (name1.isLeaf() && name2.isLeaf() && name1.getLabel().equals(name2.getLabel()) && !match.isSrcMapped(name1) && !match.isDstMapped(name2))
					calleeNames.add(new Pair<>(name1, name2));
			}
		}
		for (Pair<Tree, Tree> treeTreePair : calleeNames) {
			match.addMapping(treeTreePair.first, treeTreePair.second);
		}

		List<Pair<Tree, Tree>> addList = new ArrayList<>();
		for (Mapping mapping : match)
		{
			if (mapping.first.getType().name.equals(LANG1.SIMPLE_NAME))
			{
				if (mapping.first.getParent() !=  null && mapping.second.getParent() != null &&
						mapping.first.getParent().getType().name.equals(LANG1.METHOD_INVOCATION) &&
						mapping.second.getParent().getType().name.equals(LANG2.METHOD_INVOCATION))
				{
					if (match.getDstForSrc(mapping.first.getParent()) != mapping.second.getParent())
						addList.add(new Pair<>(mapping.first.getParent() , mapping.second.getParent()));
				}
			}
			if (mapping.first.getType().name.equals(LANG1.SIMPLE_TYPE))
			{
				if (mapping.first.getParent().getType().name.equals(LANG1.CLASS_INSTANCE_CREATION)
					&&
						mapping.second.getParent().getType().name.equals(LANG2.CLASS_INSTANCE_CREATION))
				{
					if (match.getDstForSrc(mapping.first.getParent()) != mapping.second.getParent())
						addList.add(new Pair<>(mapping.first.getParent() , mapping.second.getParent()));
				}
			}
		}
		for (Pair<Tree, Tree> treeTreePair : addList) {
			match.removeMapping(treeTreePair.first, match.getSrcForDst(treeTreePair.first));
			match.removeMapping(match.getSrcForDst(treeTreePair.second), treeTreePair.second);
			Tree srcMIR = TreeUtilFunctions.findChildByType(treeTreePair.first, LANG1.METHOD_INVOCATION_RECEIVER);
			Tree dstMIR = TreeUtilFunctions.findChildByType(treeTreePair.second, LANG2.METHOD_INVOCATION_RECEIVER);
			if (match.isSrcMapped(srcMIR)) {
				match.removeMapping(srcMIR, match.getDstForSrc(srcMIR));
			}
			if (match.isDstMapped(dstMIR)) {
				match.removeMapping(match.getSrcForDst(dstMIR), dstMIR);
			}
		}
		for (Pair<Tree, Tree> treeTreePair : addList) {
			match.addMapping(treeTreePair.first, treeTreePair.second);
			Tree srcMIR = TreeUtilFunctions.findChildByType(treeTreePair.first, LANG1.METHOD_INVOCATION_RECEIVER);
			Tree dstMIR = TreeUtilFunctions.findChildByType(treeTreePair.second, LANG2.METHOD_INVOCATION_RECEIVER);
			if (srcMIR != null && dstMIR != null)
				match.addMapping(srcMIR, dstMIR);
		}

	}
}
