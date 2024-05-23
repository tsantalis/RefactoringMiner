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

	public CompositeMatcher(AbstractCodeMapping abstractCodeMapping) {
		CompositeStatementObjectMapping compositeStatementObjectMapping = (CompositeStatementObjectMapping) abstractCodeMapping;
		fragment1 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment1();
		fragment2 = (CompositeStatementObject) compositeStatementObjectMapping.getFragment2();
	}

	public CompositeMatcher(CompositeStatementObject fragment1, CompositeStatementObject fragment2) {
		this.fragment1 = fragment1;
		this.fragment2 = fragment2;
	}

	@Override
	public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		String labeled = Constants.LABELED_STATEMENT;
		if (src.getType().name.equals(labeled) && dst.getType().name.equals(labeled))
			mappingStore.addMapping(src.getChild(0),dst.getChild(0));
		else
			process(src,dst,mappingStore);
	}
	private void process(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		Map<Tree, Tree> cpyToSrc = new HashMap<>();
		Map<Tree, Tree> cpyToDst = new HashMap<>();
		Tree srcFakeTree = makeFakeTree(src,fragment1, cpyToSrc);
		Tree dstFakeTree = makeFakeTree(dst,fragment2, cpyToDst);
		ExtendedMultiMappingStore tempMapping = new ExtendedMultiMappingStore(null,null);
		super.match(srcFakeTree,dstFakeTree,tempMapping);
		postOptimizationForComposites(srcFakeTree,dstFakeTree,tempMapping);
		for(Mapping mapping : tempMapping) {
//			if (mapping.first == srcFakeTree) continue;
			mappingStore.addMapping(cpyToSrc.get(mapping.first), cpyToDst.get(mapping.second));
		}
	}

	private void postOptimizationForComposites(Tree srcFakeTree, Tree dstFakeTree, ExtendedMultiMappingStore mappingStore) {
		if (srcFakeTree.isIsoStructuralTo(dstFakeTree)) return;
		if (srcFakeTree.getType().name.equals(Constants.IF_STATEMENT) && dstFakeTree.getType().name.equals(Constants.IF_STATEMENT)) {
			findSoloMappedSimpleNameInCondition(srcFakeTree,dstFakeTree,mappingStore);

		}
	}

	private void findSoloMappedSimpleNameInCondition(Tree srcFakeTree, Tree dstFakeTree, ExtendedMultiMappingStore mappingStore) {
		List<Mapping> candidates = new ArrayList<>();
		for (Mapping mapping : mappingStore) {
			if (mapping.first.getType().name.equals(Constants.SIMPLE_NAME)) {
				Tree p1 = getInfixParent(mapping.first);
				Tree p2 = getInfixParent(mapping.first);
				if (p1 == null || p2 == null) return;
				if (p1.getType().name.equals(Constants.SIMPLE_NAME)) continue;
				if (p2.getType().name.equals(Constants.SIMPLE_NAME)) continue;

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

	private Tree getInfixParent(Tree input) {
		Tree curr = input;
		while (curr.getParent() != null && !TreeUtilFunctions.isStatement(curr.getParent().getType().name)){
			if (curr.getParent() == null) return null;
			if (curr.getParent().getType().name.equals(Constants.INFIX_EXPRESSION))
				return curr;
			curr = curr.getParent();
		}
		return null;
	}

	private Tree makeFakeTree(Tree tree, CompositeStatementObject fragment, Map<Tree, Tree> cpyMap) {
		Tree cpy = TreeUtilFunctions.makeDefaultTree(tree);
		cpyMap.put(cpy,tree);
		for (AbstractExpression abstractExpression : fragment.getExpressions()) {
			Tree expTree = TreeUtilFunctions.findByLocationInfo(tree,abstractExpression.getLocationInfo());
			Tree expCopy =  TreeUtilFunctions.deepCopyWithMap(expTree,cpyMap);
			cpy.addChild(expCopy);
		}
		if (tree.getType().name.equals(Constants.FOR_STATEMENT)) return cpy;
		for (VariableDeclaration variableDeclaration : fragment.getVariableDeclarations()) {
			Tree varTree = TreeUtilFunctions.findByLocationInfo(tree, variableDeclaration.getLocationInfo());
			Tree varCopy = TreeUtilFunctions.deepCopyWithMap(varTree, cpyMap);
			cpy.addChild(varCopy);
		}
		return cpy;
	}
}
