package org.refactoringminer.astDiff.matchers.statement;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.decomposition.*;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.matchers.TreeMatcher;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Use this matcher when both code fragments are {@link gr.uom.java.xmi.decomposition.CompositeStatementObject}. <br>
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class CompositeMatcher extends BasicTreeMatcher implements TreeMatcher {

	private final CompositeStatementObject fragment1;
	private final CompositeStatementObject fragment2;

	public CompositeMatcher(AbstractCodeMapping abstractCodeMapping, Constants LANG1, Constants LANG2) {
		super(LANG1, LANG2);
		CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
		fragment1 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment1();
		fragment2 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment2();
	}

	public CompositeMatcher(CompositeStatementObject fragment1, CompositeStatementObject fragment2, Constants LANG1, Constants LANG2) {
		super(LANG1, LANG2);
		this.fragment1 = fragment1;
		this.fragment2 = fragment2;
	}

	@Override
	public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		if (src.getType().name.equals(LANG1.LABELED_STATEMENT) && dst.getType().name.equals(LANG2.LABELED_STATEMENT))
			mappingStore.addMapping(src.getChild(0),dst.getChild(0));
		else
			process(src,dst,mappingStore);
	}
	private void process(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		Map<Tree, Tree> cpyToSrc = new HashMap<>();
		Map<Tree, Tree> cpyToDst = new HashMap<>();
		Tree srcFakeTree = makeFakeTree(src,fragment1, cpyToSrc, LANG1);
		Tree dstFakeTree = makeFakeTree(dst,fragment2, cpyToDst, LANG2);
		ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null,LANG1,LANG2);
		super.match(srcFakeTree,dstFakeTree,tempMapping);
		postOptimizationForComposites(srcFakeTree,dstFakeTree,tempMapping);
		for(Mapping mapping : tempMapping) {
//			if (mapping.first == srcFakeTree) continue;
			mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
		}
	}

	private void postOptimizationForComposites(Tree srcFakeTree, Tree dstFakeTree, ExtendedMultiMappingStore mappingStore) {
		if (srcFakeTree.isIsoStructuralTo(dstFakeTree)) return;
		if (srcFakeTree.getType().name.equals(LANG1.IF_STATEMENT) && dstFakeTree.getType().name.equals(LANG2.IF_STATEMENT)) {
			findSoloMappedSimpleNameInCondition(srcFakeTree,dstFakeTree,mappingStore);

		}
	}

	private void findSoloMappedSimpleNameInCondition(Tree srcFakeTree, Tree dstFakeTree, ExtendedMultiMappingStore mappingStore) {
		List<Mapping> candidates = new ArrayList<>();
		for (Mapping mapping : mappingStore) {
			if (mapping.first.getType().name.equals(LANG1.SIMPLE_NAME)) {
				Tree p1 = getInfixParent(mapping.first, LANG1);
				Tree p2 = getInfixParent(mapping.second, LANG2);
				if (p1 == null || p2 == null) return;
				if (p1.getType().name.equals(LANG1.SIMPLE_NAME)) continue;
				if (p2.getType().name.equals(LANG2.SIMPLE_NAME)) continue;

				int m1 = 0;
				int m2 = 0;
				for (Tree descendant : p1.getDescendants())
					if (mappingStore.isSrcMapped(descendant)) m1++;
				for (Tree descendant : p2.getDescendants())
					if (mappingStore.isDstMapped(descendant)) m2++;
				if (mappingStore.isSrcMapped(p1)) m1++;
				if (mappingStore.isDstMapped(p2)) m2++;
				if (m1 == 1 || m2 == 1) {
					candidates.add(mapping);
				}
			}
		}
		for (Mapping candidate : candidates) {
			mappingStore.removeMapping(candidate.first,candidate.second);
		}
	}

	private Tree getInfixParent(Tree input, Constants LANG) {
		Tree curr = input;
		while (curr.getParent() != null && !TreeUtilFunctions.isStatement(curr.getParent().getType().name, LANG)){
			if (curr.getParent() == null) return null;
			if (curr.getParent().getType().name.equals(LANG.INFIX_EXPRESSION))
				return curr;
			curr = curr.getParent();
		}
		return null;
	}

	private Tree makeFakeTree(Tree tree, CompositeStatementObject fragment, Map<Tree, Tree> cpyMap, Constants LANG) {
		Tree cpy = TreeUtilFunctions.makeDefaultTree(tree);
		cpyMap.put(cpy,tree);
		for (AbstractExpression abstractExpression : fragment.getExpressions()) {
			Tree expTree = TreeUtilFunctions.findByLocationInfo(tree,abstractExpression.getLocationInfo(),LANG);
			if (expTree == null) continue;
			Tree expCopy =  TreeUtilFunctions.deepCopyWithMap(expTree,cpyMap);
			cpy.addChild(expCopy);
		}
		if (tree.getType().name.equals(LANG.FOR_STATEMENT)) return cpy;
		for (VariableDeclaration variableDeclaration : fragment.getVariableDeclarations()) {
			Tree varTree = TreeUtilFunctions.findByLocationInfo(tree, variableDeclaration.getLocationInfo(),LANG);
			if (varTree == null) continue;
			Tree varCopy = TreeUtilFunctions.deepCopyWithMap(varTree, cpyMap);
			cpy.addChild(varCopy);
		}
		return cpy;
	}
}
